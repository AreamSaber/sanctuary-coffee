package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.common.util.TradeConstants;
import com.coffee.dto.AfterSaleApplyDTO;
import com.coffee.dto.PaymentDTO;
import com.coffee.entity.Coupon;
import com.coffee.entity.Invoice;
import com.coffee.entity.Order;
import com.coffee.entity.OrderAfterSale;
import com.coffee.entity.OrderAfterSaleLog;
import com.coffee.entity.OrderItem;
import com.coffee.entity.Payment;
import com.coffee.entity.PointsRecord;
import com.coffee.entity.Promotion;
import com.coffee.entity.PromotionProduct;
import com.coffee.entity.Refund;
import com.coffee.entity.UserBehavior;
import com.coffee.entity.UserCoupon;
import com.coffee.mapper.CouponMapper;
import com.coffee.mapper.InvoiceMapper;
import com.coffee.mapper.OrderAfterSaleMapper;
import com.coffee.mapper.OrderAfterSaleLogMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.PaymentMapper;
import com.coffee.mapper.PointsRecordMapper;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.PromotionMapper;
import com.coffee.mapper.PromotionProductMapper;
import com.coffee.mapper.RefundMapper;
import com.coffee.mapper.UserCouponMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.DeliveryService;
import com.coffee.service.MemberBenefitService;
import com.coffee.service.MemberService;
import com.coffee.service.PaymentService;
import com.coffee.service.PointsService;
import com.coffee.service.support.TradeInventoryService;
import com.coffee.vo.AfterSaleLogVO;
import com.coffee.vo.MemberBenefitVO;
import com.coffee.vo.MemberInfoVO;
import com.coffee.vo.OrderItemVO;
import com.coffee.vo.OrderSettlementVO;
import com.coffee.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Payment service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int USER_COUPON_STATUS_UNUSED = 0;
    private static final int USER_COUPON_STATUS_USED = 1;
    private static final int USER_COUPON_STATUS_EXPIRED = 2;
    private static final int COUPON_TYPE_FREE_SHIPPING = 3;
    private static final int DISCOUNT_TYPE_AMOUNT = 1;
    private static final int DISCOUNT_TYPE_RATE = 2;
    private static final int POINTS_RATE = 100;
    private static final int INVOICE_STATUS_VOID = 3;
    private static final int MEMBER_BENEFIT_TYPE_DISCOUNT = 1;
    private static final int MEMBER_BENEFIT_TYPE_POINTS_MULTIPLIER = 2;
    private static final int MEMBER_BENEFIT_TYPE_FREE_SHIPPING = 3;
    private static final String PAYMENT_POINTS_SOURCE_TYPE = "4";
    private static final String PAYMENT_PENDING_ROLLBACK_DESC = "Payment pending rollback";
    private static final String MEMBER_BENEFIT_BUSINESS_PAYMENT_CREATE = "PAYMENT_CREATE";
    private static final String MEMBER_BENEFIT_BUSINESS_PAYMENT_REWARD = "PAYMENT_REWARD";
    private static final String MEMBER_BENEFIT_BUSINESS_REFUND_ROLLBACK = "REFUND_ROLLBACK";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final ProductMapper productMapper;
    private final RefundMapper refundMapper;
    private final PromotionMapper promotionMapper;
    private final PromotionProductMapper promotionProductMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final MemberBenefitService memberBenefitService;
    private final MemberService memberService;
    private final PointsService pointsService;
    private final AnalyticsService analyticsService;
    private final DeliveryService deliveryService;
    private final InvoiceMapper invoiceMapper;
    private final OrderAfterSaleMapper orderAfterSaleMapper;
    private final OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    private final TradeInventoryService tradeInventoryService;

    @Override
    public OrderSettlementVO getOrderSettlement(Long userId, Long orderId) {
        Order order = requirePendingOrder(userId, orderId);

        OrderSettlementVO vo = new OrderSettlementVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setPromotionDiscount(BigDecimal.ZERO);
        vo.setFreightDiscount(BigDecimal.ZERO);
        vo.setCouponDiscount(BigDecimal.ZERO);
        vo.setPointsDiscount(BigDecimal.ZERO);
        vo.setMemberDiscount(BigDecimal.ZERO);
        vo.setMemberDiscountRate(BigDecimal.ONE);
        vo.setPointRewardMultiplier(BigDecimal.ONE);
        vo.setEstimatedRewardPoints(0);

        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
        );

        List<OrderItemVO> itemVOList = orderItems.stream()
            .map(item -> {
                OrderItemVO itemVO = new OrderItemVO();
                itemVO.setId(item.getId());
                itemVO.setProductId(item.getProductId());
                itemVO.setSkuId(item.getSkuId());
                itemVO.setProductName(item.getProductName());
                itemVO.setProductImage(item.getProductImage());
                itemVO.setSpecInfo(item.getSpecInfo());
                itemVO.setPrice(item.getPrice());
                itemVO.setQuantity(item.getQuantity());
                itemVO.setTotalAmount(item.getTotalAmount());
                return itemVO;
            })
            .collect(Collectors.toList());

        BigDecimal promotionDiscount = calculateOrderPromotionDiscount(orderItems);
        vo.setPromotionDiscount(promotionDiscount);
        vo.setItems(itemVOList);
        vo.setAvailablePoints(pointsService.getPointsBalance(userId));
        vo.setPointsRate(POINTS_RATE);

        List<MemberBenefitVO> memberBenefits = getUserBenefits(userId);
        BigDecimal freightDiscount = calculateFreightDiscount(order.getFreightAmount(), memberBenefits);
        BigDecimal settlementAmount = defaultAmount(order.getTotalAmount())
            .add(defaultAmount(order.getFreightAmount()))
            .subtract(promotionDiscount)
            .subtract(freightDiscount);
        if (settlementAmount.compareTo(BigDecimal.ZERO) < 0) {
            settlementAmount = BigDecimal.ZERO;
        }
        MemberInfoVO memberInfo = memberService.getMemberInfo(userId);
        BigDecimal memberDiscountRate = resolveMemberDiscountRate(memberInfo, memberBenefits);
        BigDecimal memberDiscount = calculateMemberDiscount(settlementAmount, memberDiscountRate);
        BigDecimal pointRewardMultiplier = resolvePointRewardMultiplier(memberBenefits);
        BigDecimal payAmount = settlementAmount.subtract(memberDiscount);
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }

        vo.setFreightDiscount(freightDiscount);
        vo.setMemberDiscountRate(memberDiscountRate);
        vo.setMemberDiscount(memberDiscount);
        vo.setPointRewardMultiplier(pointRewardMultiplier);
        vo.setEstimatedRewardPoints(calculateRewardPoints(payAmount, pointRewardMultiplier));
        vo.setMemberBenefits(memberBenefits);
        vo.setPayAmount(payAmount);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createPayment(Long userId, PaymentDTO paymentDTO) {
        Order order = requirePendingOrder(userId, paymentDTO.getOrderId());
        LocalDateTime now = LocalDateTime.now();
        rollbackPendingBenefits(userId, order.getId());

        List<MemberBenefitVO> memberBenefits = getUserBenefits(userId);
        MemberBenefitVO freeShippingBenefit = findBenefitByType(memberBenefits, MEMBER_BENEFIT_TYPE_FREE_SHIPPING);
        BigDecimal promotionDiscount = calculateOrderPromotionDiscount(order.getId());
        BigDecimal finalAmount = defaultAmount(order.getTotalAmount())
            .add(defaultAmount(order.getFreightAmount()))
            .subtract(promotionDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        BigDecimal totalDiscount = promotionDiscount;
        BigDecimal freightDiscount = calculateFreightDiscount(order.getFreightAmount(), memberBenefits);
        BigDecimal remainingFreightAmount = calculateRemainingFreightAmount(order.getFreightAmount(), freightDiscount);
        if (freightDiscount.compareTo(BigDecimal.ZERO) > 0) {
            totalDiscount = totalDiscount.add(freightDiscount);
            finalAmount = finalAmount.subtract(freightDiscount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }
        }

        if (paymentDTO.getCouponId() != null) {
            UserCoupon userCoupon = findAvailableUserCoupon(userId, paymentDTO.getCouponId(), now);
            if (userCoupon == null) {
                throw new BusinessException("优惠券不可用");
            }

            Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
            if (coupon == null) {
                throw new BusinessException("优惠券不存在");
            }
            if (!isCouponActive(coupon, now)) {
                throw new BusinessException("优惠券未生效");
            }
            if (isUserCouponExpired(userCoupon, now)) {
                markUserCouponExpired(userCoupon);
                throw new BusinessException("优惠券已过期");
            }
            if (finalAmount.compareTo(defaultAmount(coupon.getMinAmount())) < 0) {
                throw new BusinessException("订单金额未达到优惠券使用门槛");
            }

            BigDecimal couponDiscount = calculateCouponDiscount(finalAmount, coupon, remainingFreightAmount);
            if (couponDiscount.compareTo(finalAmount) > 0) {
                couponDiscount = finalAmount;
            }

            totalDiscount = totalDiscount.add(couponDiscount);
            finalAmount = finalAmount.subtract(couponDiscount);

            userCoupon.setStatus(USER_COUPON_STATUS_USED);
            userCoupon.setUseTime(now);
            userCoupon.setOrderId(order.getId());
            userCouponMapper.updateById(userCoupon);
        }

        if (paymentDTO.getUsePoints() != null && paymentDTO.getUsePoints() > 0) {
            Integer availablePoints = pointsService.getPointsBalance(userId);
            if (paymentDTO.getUsePoints() > availablePoints) {
                throw new BusinessException("积分不足");
            }

            BigDecimal pointsDiscount = new BigDecimal(paymentDTO.getUsePoints())
                .divide(new BigDecimal(POINTS_RATE), 2, RoundingMode.HALF_UP);
            if (pointsDiscount.compareTo(finalAmount) > 0) {
                pointsDiscount = finalAmount;
            }

            totalDiscount = totalDiscount.add(pointsDiscount);
            finalAmount = finalAmount.subtract(pointsDiscount);
            pointsService.deductPoints(userId, paymentDTO.getUsePoints(), 4, order.getId(), "Payment points deduction");
        }

        MemberInfoVO memberInfo = memberService.getMemberInfo(userId);
        BigDecimal memberDiscountRate = resolveMemberDiscountRate(memberInfo, memberBenefits);
        MemberBenefitVO memberDiscountBenefit = findMemberDiscountBenefit(memberBenefits, memberDiscountRate);
        if (memberDiscountRate.compareTo(BigDecimal.ONE) < 0) {
            BigDecimal memberDiscount = calculateMemberDiscount(finalAmount, memberDiscountRate);
            totalDiscount = totalDiscount.add(memberDiscount);
            finalAmount = finalAmount.subtract(memberDiscount);
            order.setDiscountAmount(totalDiscount);
            recordMemberBenefitUsage(
                userId,
                memberDiscountBenefit,
                MEMBER_BENEFIT_BUSINESS_PAYMENT_CREATE,
                order.getId(),
                memberDiscount,
                null,
                buildMemberDiscountRemark(memberDiscountRate, memberDiscount)
            );
        }

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        order.setDiscountAmount(totalDiscount);
        order.setPayAmount(finalAmount);
        TradeConstants.syncOrderPayment(order, paymentDTO.getPayType(), TradeConstants.PAY_STATUS_UNPAID);
        orderMapper.updateById(order);

        Payment payment = paymentMapper.selectOne(
            new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, order.getId())
                .eq(Payment::getUserId, userId)
                .in(Payment::getPayStatus, TradeConstants.PAY_STATUS_UNPAID, TradeConstants.PAY_STATUS_FAILED)
                .orderByDesc(Payment::getCreateTime)
                .last("LIMIT 1")
        );
        if (payment == null) {
            payment = new Payment();
            payment.setPaymentNo(generatePaymentNo());
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(userId);
            payment.setCreateTime(now);
        }
        payment.setPayAmount(finalAmount);
        payment.setPayType(paymentDTO.getPayType());
        payment.setPayStatus(TradeConstants.PAY_STATUS_UNPAID);
        payment.setTradeNo(null);
        payment.setPayTime(null);

        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }

        recordMemberBenefitUsage(
            userId,
            freeShippingBenefit,
            MEMBER_BENEFIT_BUSINESS_PAYMENT_CREATE,
            order.getId(),
            freightDiscount,
            null,
            "会员免配送费抵扣 " + freightDiscount.stripTrailingZeros().toPlainString() + " 元"
        );

        log.info("Payment created: paymentNo={}, amount={}", payment.getPaymentNo(), finalAmount);
        if (BigDecimal.ZERO.compareTo(finalAmount) == 0) {
            confirmPayment(userId, false, payment.getPaymentNo());
        }

        return payment.getPaymentNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long currentUserId, boolean currentUserIsAdmin, String paymentNo) {
        Payment payment = paymentMapper.selectOne(
            new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo)
        );
        if (payment == null) {
            throw new BusinessException("支付记录不存在");
        }

        if (!currentUserIsAdmin && (currentUserId == null || !payment.getUserId().equals(currentUserId))) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (Integer.valueOf(TradeConstants.PAY_STATUS_SUCCESS).equals(payment.getPayStatus())) {
            log.info("Skip duplicate payment confirmation: paymentNo={}", paymentNo);
            return;
        }

        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!Integer.valueOf(TradeConstants.ORDER_STATUS_PENDING).equals(order.getOrderStatus())) {
            throw new BusinessException("订单已取消或状态已变更，无法确认支付");
        }

        LocalDateTime payTime = LocalDateTime.now();
        String tradeNo = "PAYCONFIRM" + System.currentTimeMillis();
        int paymentClaimed = paymentMapper.update(null,
            new LambdaUpdateWrapper<Payment>()
                .eq(Payment::getId, payment.getId())
                .eq(Payment::getPayStatus, TradeConstants.PAY_STATUS_UNPAID)
                .set(Payment::getPayStatus, TradeConstants.PAY_STATUS_SUCCESS)
                .set(Payment::getPayTime, payTime)
                .set(Payment::getTradeNo, tradeNo)
                .set(Payment::getUpdateTime, payTime)
        );
        if (paymentClaimed == 0) {
            Payment latestPayment = paymentMapper.selectById(payment.getId());
            if (latestPayment != null
                && Integer.valueOf(TradeConstants.PAY_STATUS_SUCCESS).equals(latestPayment.getPayStatus())) {
                log.info("Skip duplicate payment confirmation: paymentNo={}", paymentNo);
                return;
            }
            throw new BusinessException("支付记录状态已变更，无法确认支付");
        }
        payment.setPayStatus(TradeConstants.PAY_STATUS_SUCCESS);
        payment.setPayTime(payTime);
        payment.setTradeNo(tradeNo);

        int orderClaimed = orderMapper.update(null,
            new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, TradeConstants.ORDER_STATUS_PENDING)
                .set(Order::getOrderStatus, TradeConstants.ORDER_STATUS_PAID)
                .set(Order::getStatus, TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_PAID))
                .set(Order::getPayStatus, TradeConstants.PAY_STATUS_SUCCESS)
                .set(Order::getPayType, payment.getPayType())
                .set(Order::getPaymentMethod, TradeConstants.resolvePaymentMethod(payment.getPayType()))
                .set(Order::getPayTime, payTime)
                .set(Order::getUpdateTime, payTime)
        );
        if (orderClaimed == 0) {
            throw new BusinessException("订单已取消或状态已变更，无法确认支付");
        }
        TradeConstants.syncOrderPayment(order, payment.getPayType(), TradeConstants.PAY_STATUS_SUCCESS);
        TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_PAID);
        order.setPayTime(payTime);

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, payment.getOrderId())
        );
        if (items.isEmpty()) {
            throw new BusinessException("订单明细为空，无法确认支付");
        }
        for (OrderItem item : items) {
            int confirmed = tradeInventoryService.confirmLockedStock(
                item.getProductId(),
                item.getSkuId(),
                item.getQuantity(),
                "订单 " + order.getOrderNo() + " 支付确认预占库存",
                payment.getUserId()
            );
            if (confirmed == 0) {
                tradeInventoryService.deductStock(
                    item.getProductId(),
                    item.getSkuId(),
                    item.getQuantity(),
                    "订单 " + order.getOrderNo() + " 支付成功扣减库存",
                    payment.getUserId()
                );
            }
        }
        items.forEach(item -> {
            int salesIncreased = productMapper.increaseSales(item.getProductId(), item.getQuantity());
            if (salesIncreased > 0) {
                log.debug("Increase product sales after payment: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
        });

        List<MemberBenefitVO> memberBenefits = getUserBenefits(payment.getUserId());
        MemberBenefitVO pointMultiplierBenefit = findPointMultiplierBenefit(memberBenefits);
        BigDecimal pointRewardMultiplier = resolvePointRewardMultiplier(memberBenefits);
        int rewardPoints = calculateRewardPoints(payment.getPayAmount(), pointRewardMultiplier);
        memberService.updateGrowthValue(payment.getUserId(), payment.getPayAmount().intValue());
        pointsService.addPoints(payment.getUserId(), rewardPoints, 1, order.getId(), buildPaymentRewardRemark(pointRewardMultiplier));
        recordMemberBenefitUsage(
            payment.getUserId(),
            pointMultiplierBenefit,
            MEMBER_BENEFIT_BUSINESS_PAYMENT_REWARD,
            order.getId(),
            BigDecimal.ZERO,
            rewardPoints,
            "积分倍率 " + pointRewardMultiplier.stripTrailingZeros().toPlainString() + "x，支付奖励 " + rewardPoints + " 积分"
        );

        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(payment.getUserId());
            behavior.setActionType("PAY");
            behavior.setTargetType("ORDER");
            behavior.setTargetId(order.getId());
            behavior.setPageUrl("/payment/" + payment.getPaymentNo());
            behavior.setActionData(JSONUtil.createObj()
                    .set("paymentNo", payment.getPaymentNo())
                    .set("orderNo", payment.getOrderNo())
                    .set("payAmount", payment.getPayAmount())
                    .set("payType", payment.getPayType())
                    .set("rewardPoints", rewardPoints)
                    .set("pointMultiplier", pointRewardMultiplier)
                    .toString());
            analyticsService.recordUserBehavior(behavior);
        } catch (Exception e) {
            log.warn("Failed to record payment behavior: {}", e.getMessage());
        }

        try {
            deliveryService.createDeliveryOrder(order.getId());
            log.info("Delivery order created after payment: orderId={}", order.getId());
        } catch (Exception e) {
            log.warn("Failed to create delivery order automatically: {}", e.getMessage());
        }

        log.info("Payment confirmed: paymentNo={}", paymentNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long userId, Long orderId, String reason) {
        AfterSaleApplyDTO applyDTO = new AfterSaleApplyDTO();
        applyDTO.setOrderId(orderId);
        applyDTO.setType(TradeConstants.AFTER_SALE_TYPE_REFUND);
        applyDTO.setReason(reason);
        applyDTO.setDescription(reason);
        applyAfterSale(userId, applyDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAfterSale(Long userId, AfterSaleApplyDTO applyDTO) {
        Order order = orderMapper.selectById(applyDTO.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }

        int afterSaleType = resolveAfterSaleType(applyDTO.getType());
        boolean refundRelated = isRefundRelatedAfterSale(afterSaleType);
        if (refundRelated && !Integer.valueOf(TradeConstants.PAY_STATUS_SUCCESS).equals(order.getPayStatus())) {
            throw new BusinessException("订单未支付");
        }
        if (refundRelated && Integer.valueOf(TradeConstants.ORDER_STATUS_REFUNDED).equals(order.getOrderStatus())) {
            throw new BusinessException("订单已退款");
        }
        if (Integer.valueOf(TradeConstants.ORDER_STATUS_REFUNDING).equals(order.getOrderStatus())) {
            throw new BusinessException("售后申请处理中");
        }
        if (!isAfterSaleSupportedOrderStatus(order.getOrderStatus(), afterSaleType)) {
            throw new BusinessException("当前订单状态不支持该售后类型");
        }

        OrderAfterSale existingAfterSale = orderAfterSaleMapper.selectOne(
            new LambdaQueryWrapper<OrderAfterSale>()
                .eq(OrderAfterSale::getOrderId, order.getId())
                .in(OrderAfterSale::getStatus,
                    TradeConstants.AFTER_SALE_STATUS_PENDING,
                    TradeConstants.AFTER_SALE_STATUS_APPROVED,
                    TradeConstants.AFTER_SALE_STATUS_PROCESSING)
                .orderByDesc(OrderAfterSale::getCreateTime)
                .last("LIMIT 1")
        );
        if (existingAfterSale != null) {
            throw new BusinessException("售后申请处理中");
        }

        Refund existingRefund = null;
        if (refundRelated) {
            existingRefund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>()
                    .eq(Refund::getOrderId, order.getId())
                    .in(Refund::getRefundStatus, TradeConstants.REFUND_STATUS_APPLYING, TradeConstants.REFUND_STATUS_SUCCESS)
                    .orderByDesc(Refund::getCreateTime)
                    .last("LIMIT 1")
            );
            if (existingRefund != null) {
                if (Integer.valueOf(TradeConstants.REFUND_STATUS_SUCCESS).equals(existingRefund.getRefundStatus())) {
                    throw new BusinessException("订单已退款");
                }
                throw new BusinessException("退款申请处理中");
            }
        }

        BigDecimal refundAmount = resolveAfterSaleRefundAmount(order, applyDTO.getRefundAmount(), refundRelated);
        String reason = normalizeRequiredText(applyDTO.getReason(), "售后原因不能为空");
        String description = normalizeOptionalText(applyDTO.getDescription());
        if (description == null) {
            description = reason;
        }
        String images = normalizeAfterSaleImages(applyDTO.getImages());

        OrderAfterSale afterSale = new OrderAfterSale();
        afterSale.setOrderId(order.getId());
        afterSale.setUserId(userId);
        afterSale.setAfterSaleNo(generateAfterSaleNo());
        afterSale.setType(afterSaleType);
        afterSale.setReason(reason);
        afterSale.setDescription(description);
        afterSale.setImages(images);
        afterSale.setRefundAmount(refundAmount);
        afterSale.setStatus(TradeConstants.AFTER_SALE_STATUS_PENDING);
        orderAfterSaleMapper.insert(afterSale);
        insertAfterSaleLog(
            afterSale,
            userId,
            TradeConstants.AFTER_SALE_OPERATOR_USER,
            TradeConstants.AFTER_SALE_LOG_ACTION_APPLY,
            null,
            TradeConstants.AFTER_SALE_STATUS_PENDING,
            buildAfterSaleApplyLogRemark(afterSaleType, reason)
        );

        Refund refund = null;
        if (refundRelated) {
            refund = new Refund();
            refund.setRefundNo(generateRefundNo());
            refund.setOrderId(order.getId());
            refund.setOrderNo(order.getOrderNo());
            refund.setUserId(userId);
            refund.setRefundAmount(refundAmount);
            refund.setRefundReason(reason);
            refund.setRefundStatus(TradeConstants.REFUND_STATUS_APPLYING);
            refundMapper.insert(refund);

            TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_REFUNDING);
            orderMapper.updateById(order);
        }

        recordAfterSaleApplyBehavior(userId, order, afterSale, refund, reason, refundRelated);
        log.info("After-sale applied: orderId={}, afterSaleNo={}, type={}", order.getId(), afterSale.getAfterSaleNo(), afterSaleType);
    }

    private Order requirePendingOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (!Integer.valueOf(TradeConstants.ORDER_STATUS_PENDING).equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不合法");
        }
        return order;
    }

    private UserCoupon findAvailableUserCoupon(Long userId, Long couponIdentifier, LocalDateTime now) {
        UserCoupon userCoupon = userCouponMapper.selectOne(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, couponIdentifier)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_UNUSED)
        );
        if (userCoupon != null) {
            if (!isUserCouponExpired(userCoupon, now)) {
                return userCoupon;
            }
            markUserCouponExpired(userCoupon);
        }

        List<UserCoupon> userCoupons = userCouponMapper.selectList(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponIdentifier)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_UNUSED)
                .orderByAsc(UserCoupon::getExpireTime)
                .orderByAsc(UserCoupon::getReceiveTime)
        );

        for (UserCoupon item : userCoupons) {
            if (isUserCouponExpired(item, now)) {
                markUserCouponExpired(item);
                continue;
            }
            return item;
        }
        return null;
    }

    private boolean isCouponActive(Coupon coupon, LocalDateTime now) {
        if (!Integer.valueOf(1).equals(coupon.getStatus())) {
            return false;
        }
        if (coupon.getStartTime() != null && coupon.getStartTime().isAfter(now)) {
            return false;
        }
        return coupon.getEndTime() == null || !coupon.getEndTime().isBefore(now);
    }

    private boolean isUserCouponExpired(UserCoupon userCoupon, LocalDateTime now) {
        return userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(now);
    }

    private void markUserCouponExpired(UserCoupon userCoupon) {
        if (userCoupon == null || !Integer.valueOf(USER_COUPON_STATUS_UNUSED).equals(userCoupon.getStatus())) {
            return;
        }
        userCoupon.setStatus(USER_COUPON_STATUS_EXPIRED);
        userCoupon.setUseTime(null);
        userCoupon.setOrderId(null);
        userCouponMapper.updateById(userCoupon);
    }

    @Override
    public IPage<RefundVO> getAdminRefundPage(
            Integer pageNum,
            Integer pageSize,
            String refundNo,
            String orderNo,
            Long userId,
            Integer refundStatus) {
        Page<Refund> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Refund> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(refundNo != null && !refundNo.isBlank(), Refund::getRefundNo, refundNo)
            .like(orderNo != null && !orderNo.isBlank(), Refund::getOrderNo, orderNo)
            .eq(userId != null, Refund::getUserId, userId)
            .eq(refundStatus != null, Refund::getRefundStatus, refundStatus)
            .orderByDesc(Refund::getCreateTime);

        IPage<Refund> refundPage = refundMapper.selectPage(page, wrapper);
        IPage<RefundVO> voPage = new Page<>(refundPage.getCurrent(), refundPage.getSize(), refundPage.getTotal());
        List<Refund> refunds = refundPage.getRecords();
        if (refunds == null || refunds.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }

        List<Long> orderIds = refunds.stream()
            .map(Refund::getOrderId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, Order> orderMap = getOrderMap(orderIds);
        Map<Long, OrderAfterSale> afterSaleMap = getRefundAfterSaleMap(orderIds);
        Map<Long, List<AfterSaleLogVO>> afterSaleLogMap = getRefundAfterSaleLogMap(afterSaleMap);
        Map<Long, List<OrderItemVO>> orderItemMap = getOrderItemVOMap(orderIds);

        voPage.setRecords(refunds.stream()
            .map(refund -> buildRefundVO(refund, orderMap, afterSaleMap, afterSaleLogMap, orderItemMap))
            .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public RefundVO getAdminRefundDetail(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }

        List<Long> orderIds = refund.getOrderId() == null ? List.of() : List.of(refund.getOrderId());
        Map<Long, OrderAfterSale> afterSaleMap = getRefundAfterSaleMap(orderIds);
        return buildRefundVO(
            refund,
            getOrderMap(orderIds),
            afterSaleMap,
            getRefundAfterSaleLogMap(afterSaleMap),
            getOrderItemVOMap(orderIds)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long refundId, String remark) {
        Refund refund = requireRefund(refundId);
        if (Integer.valueOf(TradeConstants.REFUND_STATUS_SUCCESS).equals(refund.getRefundStatus())) {
            log.info("Skip duplicate refund approval: refundId={}, refundNo={}", refundId, refund.getRefundNo());
            return;
        }
        if (Integer.valueOf(TradeConstants.REFUND_STATUS_FAILED).equals(refund.getRefundStatus())) {
            throw new BusinessException("退款已驳回，不能再次审核通过");
        }
        if (!Integer.valueOf(TradeConstants.REFUND_STATUS_APPLYING).equals(refund.getRefundStatus())) {
            throw new BusinessException("退款状态不支持审核");
        }

        Order order = requireRefundOrder(refund);
        if (Integer.valueOf(TradeConstants.ORDER_STATUS_REFUNDED).equals(order.getOrderStatus())) {
            throw new BusinessException("订单已退款");
        }
        if (!Integer.valueOf(TradeConstants.PAY_STATUS_SUCCESS).equals(order.getPayStatus())) {
            throw new BusinessException("订单支付状态不支持退款");
        }

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, refund.getOrderId())
        );
        if (items.isEmpty()) {
            throw new BusinessException("订单明细为空，无法退款");
        }

        for (OrderItem item : items) {
            tradeInventoryService.restoreStock(
                item.getProductId(),
                item.getSkuId(),
                item.getQuantity(),
                "订单 " + order.getOrderNo() + " 退款审核通过回补库存",
                refund.getUserId()
            );
            int salesDecreased = productMapper.decreaseSales(item.getProductId(), item.getQuantity());
            if (salesDecreased > 0) {
                log.debug("Decrease product sales after refund: productId={}, quantity={}", item.getProductId(), item.getQuantity());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Long reviewerId = SecurityUtils.getCurrentUserId();
        refund.setReviewerId(reviewerId);
        refund.setReviewTime(now);
        refund.setReviewRemark(buildReviewRemark(remark, "管理员审核通过，模拟退款完成"));
        refund.setRefundStatus(TradeConstants.REFUND_STATUS_SUCCESS);
        refund.setRefundTime(now);
        refund.setTradeNo("REFUND" + System.currentTimeMillis());
        refundMapper.updateById(refund);

        OrderAfterSale afterSale = findLatestRefundAfterSale(refund.getOrderId());
        if (afterSale != null) {
            Integer oldStatus = afterSale.getStatus();
            afterSale.setStatus(TradeConstants.AFTER_SALE_STATUS_COMPLETED);
            afterSale.setHandleTime(now);
            afterSale.setHandleRemark(buildReviewRemark(remark, "管理员审核通过，模拟退款完成"));
            afterSale.setReviewerId(reviewerId);
            orderAfterSaleMapper.updateById(afterSale);
            insertAfterSaleLog(
                afterSale,
                SecurityUtils.getCurrentUserId(),
                TradeConstants.AFTER_SALE_OPERATOR_ADMIN,
                TradeConstants.AFTER_SALE_LOG_ACTION_REFUND_COMPLETE,
                oldStatus,
                TradeConstants.AFTER_SALE_STATUS_COMPLETED,
                afterSale.getHandleRemark()
            );
        }

        List<Payment> payments = paymentMapper.selectList(
            new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, refund.getOrderId())
        );
        payments.forEach(item -> {
            if (Integer.valueOf(TradeConstants.PAY_STATUS_SUCCESS).equals(item.getPayStatus())) {
                item.setPayStatus(TradeConstants.PAY_STATUS_REFUNDED);
                paymentMapper.updateById(item);
            }
        });

        TradeConstants.syncOrderPayment(order, order.getPayType(), TradeConstants.PAY_STATUS_REFUNDED);
        TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_REFUNDED);
        orderMapper.updateById(order);

        Invoice invoice = invoiceMapper.selectOne(
            new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getOrderId, refund.getOrderId())
                .last("LIMIT 1")
        );
        if (invoice != null) {
            invoice.setStatus(INVOICE_STATUS_VOID);
            invoiceMapper.updateById(invoice);
        }

        try {
            List<MemberBenefitVO> memberBenefits = getUserBenefits(refund.getUserId());
            MemberBenefitVO pointMultiplierBenefit = findPointMultiplierBenefit(memberBenefits);
            BigDecimal pointRewardMultiplier = resolvePointRewardMultiplier(memberBenefits);
            int rollbackPoints = calculateRewardPoints(order.getPayAmount(), pointRewardMultiplier);
            pointsService.deductPoints(refund.getUserId(), rollbackPoints, 5, order.getId(), "Refund points rollback");
            recordMemberBenefitUsage(
                refund.getUserId(),
                pointMultiplierBenefit,
                MEMBER_BENEFIT_BUSINESS_REFUND_ROLLBACK,
                refund.getId(),
                BigDecimal.ZERO,
                -rollbackPoints,
                "退款扣回积分倍率 " + pointRewardMultiplier.stripTrailingZeros().toPlainString() + "x，扣回 " + rollbackPoints + " 积分"
            );
        } catch (Exception ex) {
            log.warn("退款扣回积分失败，已忽略: orderId={}, reason={}", refund.getOrderId(), ex.getMessage());
        }

        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(refund.getUserId());
            behavior.setActionType("REFUND");
            behavior.setTargetType("ORDER");
            behavior.setTargetId(order.getId());
            behavior.setPageUrl("/admin/refund");
            behavior.setActionData(JSONUtil.createObj()
                    .set("refundNo", refund.getRefundNo())
                    .set("orderNo", refund.getOrderNo())
                    .set("refundAmount", refund.getRefundAmount())
                    .set("reviewRemark", remark)
                    .toString());
            analyticsService.recordUserBehavior(behavior);
        } catch (Exception e) {
            log.warn("Failed to record refund behavior: {}", e.getMessage());
        }

        log.info("Refund approved: orderId={}, refundNo={}", refund.getOrderId(), refund.getRefundNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long refundId, String remark) {
        Refund refund = requireRefund(refundId);
        if (Integer.valueOf(TradeConstants.REFUND_STATUS_SUCCESS).equals(refund.getRefundStatus())) {
            throw new BusinessException("退款已完成，不能驳回");
        }
        if (Integer.valueOf(TradeConstants.REFUND_STATUS_FAILED).equals(refund.getRefundStatus())) {
            log.info("Skip duplicate refund rejection: refundId={}, refundNo={}", refundId, refund.getRefundNo());
            return;
        }
        if (!Integer.valueOf(TradeConstants.REFUND_STATUS_APPLYING).equals(refund.getRefundStatus())) {
            throw new BusinessException("退款状态不支持驳回");
        }

        Order order = requireRefundOrder(refund);
        LocalDateTime now = LocalDateTime.now();
        Long reviewerId = SecurityUtils.getCurrentUserId();

        refund.setReviewerId(reviewerId);
        refund.setReviewTime(now);
        refund.setReviewRemark(buildReviewRemark(remark, "管理员驳回退款申请"));
        refund.setRefundStatus(TradeConstants.REFUND_STATUS_FAILED);
        refund.setRefundTime(null);
        refund.setTradeNo(null);
        refundMapper.updateById(refund);

        OrderAfterSale afterSale = findLatestRefundAfterSale(refund.getOrderId());
        if (afterSale != null) {
            Integer oldStatus = afterSale.getStatus();
            afterSale.setStatus(TradeConstants.AFTER_SALE_STATUS_REJECTED);
            afterSale.setHandleTime(now);
            afterSale.setHandleRemark(buildReviewRemark(remark, "管理员驳回退款申请"));
            afterSale.setReviewerId(reviewerId);
            orderAfterSaleMapper.updateById(afterSale);
            insertAfterSaleLog(
                afterSale,
                SecurityUtils.getCurrentUserId(),
                TradeConstants.AFTER_SALE_OPERATOR_ADMIN,
                TradeConstants.AFTER_SALE_LOG_ACTION_REJECT,
                oldStatus,
                TradeConstants.AFTER_SALE_STATUS_REJECTED,
                afterSale.getHandleRemark()
            );
        }

        if (Integer.valueOf(TradeConstants.ORDER_STATUS_REFUNDING).equals(order.getOrderStatus())) {
            TradeConstants.syncOrderStatus(order, resolveOrderStatusBeforeRefund(order));
            orderMapper.updateById(order);
        }

        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(refund.getUserId());
            behavior.setActionType("REFUND_REJECT");
            behavior.setTargetType("ORDER");
            behavior.setTargetId(order.getId());
            behavior.setPageUrl("/admin/refund");
            behavior.setActionData(JSONUtil.createObj()
                    .set("refundNo", refund.getRefundNo())
                    .set("orderNo", refund.getOrderNo())
                    .set("reviewRemark", remark)
                    .toString());
            analyticsService.recordUserBehavior(behavior);
        } catch (Exception e) {
            log.warn("Failed to record refund reject behavior: {}", e.getMessage());
        }

        log.info("Refund rejected: orderId={}, refundNo={}", refund.getOrderId(), refund.getRefundNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefundAfterSale(Long afterSaleId, String remark) {
        OrderAfterSale afterSale = requireRefundAfterSale(afterSaleId);
        Refund refund = requireApplyingRefundByOrderId(afterSale.getOrderId());
        approveRefund(refund.getId(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefundAfterSale(Long afterSaleId, String remark) {
        OrderAfterSale afterSale = requireRefundAfterSale(afterSaleId);
        Refund refund = requireApplyingRefundByOrderId(afterSale.getOrderId());
        rejectRefund(refund.getId(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackPendingBenefits(Long userId, Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> usedCoupons = userCouponMapper.selectList(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_USED)
        );
        usedCoupons.forEach(item -> {
            item.setStatus(isUserCouponExpired(item, now) ? USER_COUPON_STATUS_EXPIRED : USER_COUPON_STATUS_UNUSED);
            item.setUseTime(null);
            item.setOrderId(null);
            userCouponMapper.updateById(item);
        });

        List<PointsRecord> deductionRecords = pointsRecordMapper.selectList(
            new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getType, 2)
                .eq(PointsRecord::getBizId, orderId)
                .eq(PointsRecord::getSourceType, PAYMENT_POINTS_SOURCE_TYPE)
        );
        int totalDeductionPoints = deductionRecords.stream()
            .map(PointsRecord::getPoints)
            .filter(value -> value != null && value > 0)
            .mapToInt(Integer::intValue)
            .sum();

        List<PointsRecord> rollbackRecords = pointsRecordMapper.selectList(
            new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getType, 1)
                .eq(PointsRecord::getBizId, orderId)
                .eq(PointsRecord::getSourceType, PAYMENT_POINTS_SOURCE_TYPE)
                .eq(PointsRecord::getDescription, PAYMENT_PENDING_ROLLBACK_DESC)
        );
        int totalRollbackPoints = rollbackRecords.stream()
            .map(PointsRecord::getPoints)
            .filter(value -> value != null && value > 0)
            .mapToInt(Integer::intValue)
            .sum();

        int rollbackablePoints = totalDeductionPoints - totalRollbackPoints;
        if (rollbackablePoints > 0) {
            pointsService.addPoints(userId, rollbackablePoints, 4, orderId, PAYMENT_PENDING_ROLLBACK_DESC);
        }
    }

    private Map<Long, Order> getOrderMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
        ).stream().collect(Collectors.toMap(Order::getId, item -> item));
    }

    private Refund requireRefund(Long refundId) {
        Refund refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }
        return refund;
    }

    private Order requireRefundOrder(Refund refund) {
        Order order = orderMapper.selectById(refund.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(refund.getUserId())) {
            throw new BusinessException("退款记录与订单用户不匹配");
        }
        return order;
    }

    private OrderAfterSale requireRefundAfterSale(Long afterSaleId) {
        OrderAfterSale afterSale = orderAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new BusinessException("售后记录不存在");
        }
        if (!isRefundRelatedAfterSale(afterSale.getType())) {
            throw new BusinessException("退款类售后支持该操作");
        }
        if (Integer.valueOf(TradeConstants.AFTER_SALE_STATUS_COMPLETED).equals(afterSale.getStatus())) {
            throw new BusinessException("售后已完成");
        }
        if (Integer.valueOf(TradeConstants.AFTER_SALE_STATUS_REJECTED).equals(afterSale.getStatus())) {
            throw new BusinessException("售后已驳回");
        }
        return afterSale;
    }

    private Refund requireApplyingRefundByOrderId(Long orderId) {
        Refund refund = refundMapper.selectOne(
            new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, orderId)
                .eq(Refund::getRefundStatus, TradeConstants.REFUND_STATUS_APPLYING)
                .orderByDesc(Refund::getCreateTime)
                .last("LIMIT 1")
        );
        if (refund == null) {
            throw new BusinessException("没有待审核退款记录");
        }
        return refund;
    }

    private OrderAfterSale findLatestRefundAfterSale(Long orderId) {
        return orderAfterSaleMapper.selectOne(
            new LambdaQueryWrapper<OrderAfterSale>()
                .eq(OrderAfterSale::getOrderId, orderId)
                .in(OrderAfterSale::getType,
                    TradeConstants.AFTER_SALE_TYPE_REFUND,
                    TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND)
                .orderByDesc(OrderAfterSale::getCreateTime)
                .last("LIMIT 1")
        );
    }

    private boolean isRefundableOrderStatus(Integer orderStatus) {
        return Integer.valueOf(TradeConstants.ORDER_STATUS_PAID).equals(orderStatus)
            || Integer.valueOf(TradeConstants.ORDER_STATUS_DELIVERING).equals(orderStatus)
            || Integer.valueOf(TradeConstants.ORDER_STATUS_COMPLETED).equals(orderStatus);
    }

    private boolean isAfterSaleSupportedOrderStatus(Integer orderStatus, Integer afterSaleType) {
        if (Integer.valueOf(TradeConstants.AFTER_SALE_TYPE_DELIVERY).equals(afterSaleType)) {
            return Integer.valueOf(TradeConstants.ORDER_STATUS_DELIVERING).equals(orderStatus)
                || Integer.valueOf(TradeConstants.ORDER_STATUS_COMPLETED).equals(orderStatus);
        }
        return isRefundableOrderStatus(orderStatus);
    }

    private boolean isRefundRelatedAfterSale(Integer afterSaleType) {
        return Integer.valueOf(TradeConstants.AFTER_SALE_TYPE_REFUND).equals(afterSaleType)
            || Integer.valueOf(TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND).equals(afterSaleType);
    }

    private int resolveAfterSaleType(Integer type) {
        if (Integer.valueOf(TradeConstants.AFTER_SALE_TYPE_REFUND).equals(type)
            || Integer.valueOf(TradeConstants.AFTER_SALE_TYPE_DELIVERY).equals(type)
            || Integer.valueOf(TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND).equals(type)) {
            return type;
        }
        throw new BusinessException("售后类型不支持");
    }

    private BigDecimal resolveAfterSaleRefundAmount(Order order, BigDecimal requestedAmount, boolean refundRelated) {
        if (!refundRelated) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxRefundAmount = defaultAmount(order.getPayAmount());
        BigDecimal refundAmount = requestedAmount == null ? maxRefundAmount : requestedAmount;
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("退款金额必须大于0");
        }
        if (refundAmount.compareTo(maxRefundAmount) > 0) {
            throw new BusinessException("退款金额不能超过订单实付金额");
        }
        return refundAmount;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(errorMessage);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAfterSaleImages(String images) {
        if (images == null || images.isBlank()) {
            return "[]";
        }
        try {
            Object parsed = JSONUtil.parse(images.trim());
            if (!(parsed instanceof cn.hutool.json.JSONArray)) {
                throw new BusinessException("凭证图片格式不正确");
            }
            return parsed.toString();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("凭证图片格式不正确");
        }
    }

    private String buildAfterSaleApplyLogRemark(Integer afterSaleType, String reason) {
        String typeText = switch (afterSaleType) {
            case TradeConstants.AFTER_SALE_TYPE_DELIVERY -> "配送问题";
            case TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND -> "退货退款";
            default -> "退款";
        };
        return "用户提交" + typeText + "申请：" + reason;
    }

    private void recordAfterSaleApplyBehavior(
            Long userId,
            Order order,
            OrderAfterSale afterSale,
            Refund refund,
            String reason,
            boolean refundRelated) {
        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setActionType(refundRelated ? "REFUND_APPLY" : "AFTER_SALE_APPLY");
            behavior.setTargetType("ORDER");
            behavior.setTargetId(order.getId());
            behavior.setPageUrl("/order/after-sale/apply");
            behavior.setActionData(JSONUtil.createObj()
                    .set("afterSaleNo", afterSale.getAfterSaleNo())
                    .set("refundNo", refund == null ? null : refund.getRefundNo())
                    .set("orderNo", order.getOrderNo())
                    .set("afterSaleType", afterSale.getType())
                    .set("refundAmount", afterSale.getRefundAmount())
                    .set("reason", reason)
                    .toString());
            analyticsService.recordUserBehavior(behavior);
        } catch (Exception e) {
            log.warn("Failed to record after-sale apply behavior: {}", e.getMessage());
        }
    }

    private Integer resolveOrderStatusBeforeRefund(Order order) {
        if (order.getReceiveTime() != null) {
            return TradeConstants.ORDER_STATUS_COMPLETED;
        }
        if (order.getDeliveryTime() != null) {
            return TradeConstants.ORDER_STATUS_DELIVERING;
        }
        return TradeConstants.ORDER_STATUS_PAID;
    }

    private String buildReviewRemark(String remark, String fallback) {
        if (remark == null || remark.isBlank()) {
            return fallback;
        }
        return remark.trim();
    }

    private void insertAfterSaleLog(
            OrderAfterSale afterSale,
            Long operatorId,
            String operatorType,
            String action,
            Integer statusFrom,
            Integer statusTo,
            String remark) {
        if (afterSale == null) {
            return;
        }

        OrderAfterSaleLog log = new OrderAfterSaleLog();
        log.setAfterSaleId(afterSale.getId());
        log.setOrderId(afterSale.getOrderId());
        log.setUserId(afterSale.getUserId());
        log.setOperatorId(operatorId);
        log.setOperatorType(operatorType);
        log.setAction(action);
        log.setStatusFrom(statusFrom);
        log.setStatusTo(statusTo);
        log.setRemark(remark);
        orderAfterSaleLogMapper.insert(log);
    }

    private Map<Long, OrderAfterSale> getRefundAfterSaleMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OrderAfterSale> records = orderAfterSaleMapper.selectList(
            new LambdaQueryWrapper<OrderAfterSale>()
                .in(OrderAfterSale::getOrderId, orderIds)
                .in(OrderAfterSale::getType,
                    TradeConstants.AFTER_SALE_TYPE_REFUND,
                    TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND)
                .orderByDesc(OrderAfterSale::getCreateTime)
        );

        Map<Long, OrderAfterSale> result = new HashMap<>();
        for (OrderAfterSale item : records) {
            result.putIfAbsent(item.getOrderId(), item);
        }
        return result;
    }

    private Map<Long, List<AfterSaleLogVO>> getRefundAfterSaleLogMap(Map<Long, OrderAfterSale> afterSaleMap) {
        if (afterSaleMap == null || afterSaleMap.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> afterSaleIds = afterSaleMap.values().stream()
            .map(OrderAfterSale::getId)
            .filter(id -> id != null && id > 0)
            .distinct()
            .collect(Collectors.toList());
        if (afterSaleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> afterSaleIdToOrderId = afterSaleMap.values().stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(OrderAfterSale::getId, OrderAfterSale::getOrderId, (left, right) -> left));

        List<OrderAfterSaleLog> logs = orderAfterSaleLogMapper.selectList(
            new LambdaQueryWrapper<OrderAfterSaleLog>()
                .in(OrderAfterSaleLog::getAfterSaleId, afterSaleIds)
                .orderByAsc(OrderAfterSaleLog::getCreateTime)
                .orderByAsc(OrderAfterSaleLog::getId)
        );
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyMap();
        }

        return logs.stream()
            .map(this::buildAfterSaleLogVO)
            .collect(Collectors.groupingBy(log -> afterSaleIdToOrderId.get(log.getAfterSaleId())));
    }

    private Map<Long, List<OrderItemVO>> getOrderItemVOMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
        ).stream().collect(Collectors.groupingBy(
            OrderItem::getOrderId,
            Collectors.mapping(item -> BeanUtil.copyProperties(item, OrderItemVO.class), Collectors.toList())
        ));
    }

    private RefundVO buildRefundVO(
            Refund refund,
            Map<Long, Order> orderMap,
            Map<Long, OrderAfterSale> afterSaleMap,
            Map<Long, List<AfterSaleLogVO>> afterSaleLogMap,
            Map<Long, List<OrderItemVO>> orderItemMap) {
        RefundVO vo = BeanUtil.copyProperties(refund, RefundVO.class);
        vo.setRefundStatusText(resolveRefundStatusText(refund.getRefundStatus()));
        vo.setItems(orderItemMap.getOrDefault(refund.getOrderId(), List.of()));
        vo.setLogs(afterSaleLogMap.getOrDefault(refund.getOrderId(), List.of()));

        Order order = orderMap.get(refund.getOrderId());
        if (order != null) {
            vo.setOrderStatus(order.getOrderStatus());
            vo.setOrderStatusText(TradeConstants.resolveOrderStatusText(order.getOrderStatus()));
            vo.setPayStatus(order.getPayStatus());
            vo.setReceiverName(order.getReceiverName());
            vo.setReceiverPhone(order.getReceiverPhone());
            vo.setReceiverAddress(order.getReceiverAddress());
            vo.setRemark(order.getRemark());
        }

        OrderAfterSale afterSale = afterSaleMap.get(refund.getOrderId());
        if (afterSale != null) {
            vo.setAfterSaleNo(afterSale.getAfterSaleNo());
            vo.setAfterSaleStatus(afterSale.getStatus());
            vo.setAfterSaleStatusText(resolveAfterSaleStatusText(afterSale.getStatus()));
            vo.setHandleTime(afterSale.getHandleTime());
            vo.setHandleRemark(afterSale.getHandleRemark());
        }
        return vo;
    }

    private AfterSaleLogVO buildAfterSaleLogVO(OrderAfterSaleLog log) {
        AfterSaleLogVO vo = BeanUtil.copyProperties(log, AfterSaleLogVO.class);
        vo.setActionText(resolveAfterSaleLogActionText(log.getAction()));
        vo.setOperatorTypeText(resolveOperatorTypeText(log.getOperatorType()));
        vo.setStatusFromText(resolveAfterSaleStatusText(log.getStatusFrom()));
        vo.setStatusToText(resolveAfterSaleStatusText(log.getStatusTo()));
        return vo;
    }

    private String resolveRefundStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case TradeConstants.REFUND_STATUS_APPLYING -> "退款处理中";
            case TradeConstants.REFUND_STATUS_SUCCESS -> "已退款";
            case TradeConstants.REFUND_STATUS_FAILED -> "已驳回";
            default -> "未知";
        };
    }

    private String resolveAfterSaleStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case TradeConstants.AFTER_SALE_STATUS_PENDING -> "待处理";
            case TradeConstants.AFTER_SALE_STATUS_APPROVED -> "已同意";
            case TradeConstants.AFTER_SALE_STATUS_REJECTED -> "已驳回";
            case TradeConstants.AFTER_SALE_STATUS_PROCESSING -> "处理中";
            case TradeConstants.AFTER_SALE_STATUS_COMPLETED -> "已完成";
            default -> "未知";
        };
    }

    private String resolveAfterSaleLogActionText(String action) {
        if (action == null || action.isBlank()) {
            return "处理记录";
        }
        return switch (action) {
            case TradeConstants.AFTER_SALE_LOG_ACTION_APPLY -> "提交申请";
            case TradeConstants.AFTER_SALE_LOG_ACTION_APPROVE -> "审核通过";
            case TradeConstants.AFTER_SALE_LOG_ACTION_REJECT -> "审核驳回";
            case TradeConstants.AFTER_SALE_LOG_ACTION_REFUND_COMPLETE -> "退款完成";
            default -> "处理记录";
        };
    }

    private String resolveOperatorTypeText(String operatorType) {
        if (TradeConstants.AFTER_SALE_OPERATOR_USER.equals(operatorType)) {
            return "用户";
        }
        if (TradeConstants.AFTER_SALE_OPERATOR_ADMIN.equals(operatorType)) {
            return "管理员";
        }
        return "系统";
    }

    private List<MemberBenefitVO> getUserBenefits(Long userId) {
        List<MemberBenefitVO> benefits = memberBenefitService.getUserBenefits(userId);
        return benefits == null ? List.of() : benefits;
    }

    private BigDecimal calculateFreightDiscount(BigDecimal freightAmount, List<MemberBenefitVO> memberBenefits) {
        BigDecimal freight = defaultAmount(freightAmount);
        if (freight.compareTo(BigDecimal.ZERO) <= 0 || !hasBenefitType(memberBenefits, MEMBER_BENEFIT_TYPE_FREE_SHIPPING)) {
            return BigDecimal.ZERO;
        }
        return freight;
    }

    private BigDecimal calculateRemainingFreightAmount(BigDecimal freightAmount, BigDecimal freightDiscount) {
        BigDecimal remaining = defaultAmount(freightAmount).subtract(defaultAmount(freightDiscount));
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    private BigDecimal resolveMemberDiscountRate(MemberInfoVO memberInfo, List<MemberBenefitVO> memberBenefits) {
        BigDecimal levelDiscountRate = normalizeDiscountRate(memberInfo == null ? null : memberInfo.getDiscountRate());
        BigDecimal benefitDiscountRate = resolveBenefitDiscountRate(memberBenefits);
        return levelDiscountRate.min(benefitDiscountRate);
    }

    private BigDecimal resolveBenefitDiscountRate(List<MemberBenefitVO> memberBenefits) {
        if (memberBenefits == null || memberBenefits.isEmpty()) {
            return BigDecimal.ONE;
        }

        return memberBenefits.stream()
            .filter(benefit -> benefit != null && Integer.valueOf(MEMBER_BENEFIT_TYPE_DISCOUNT).equals(benefit.getBenefitType()))
            .map(MemberBenefitVO::getBenefitValue)
            .map(this::normalizeDiscountRate)
            .filter(value -> value.compareTo(BigDecimal.ONE) < 0)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ONE);
    }

    private BigDecimal normalizeDiscountRate(BigDecimal discountRate) {
        if (discountRate == null || discountRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return discountRate.min(BigDecimal.ONE);
    }

    private BigDecimal calculateMemberDiscount(BigDecimal amount, BigDecimal discountRate) {
        BigDecimal baseAmount = defaultAmount(amount);
        if (baseAmount.compareTo(BigDecimal.ZERO) <= 0 || discountRate == null || discountRate.compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(BigDecimal.ONE.subtract(discountRate))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolvePointRewardMultiplier(List<MemberBenefitVO> memberBenefits) {
        if (memberBenefits == null || memberBenefits.isEmpty()) {
            return BigDecimal.ONE;
        }

        return memberBenefits.stream()
            .filter(benefit -> benefit != null && Integer.valueOf(MEMBER_BENEFIT_TYPE_POINTS_MULTIPLIER).equals(benefit.getBenefitType()))
            .map(MemberBenefitVO::getBenefitValue)
            .filter(value -> value != null && value.compareTo(BigDecimal.ONE) > 0)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ONE);
    }

    private int calculateRewardPoints(BigDecimal amount, BigDecimal multiplier) {
        BigDecimal safeAmount = defaultAmount(amount);
        BigDecimal safeMultiplier = multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : multiplier;
        return safeAmount.multiply(safeMultiplier).setScale(0, RoundingMode.DOWN).intValue();
    }

    private String buildPaymentRewardRemark(BigDecimal pointRewardMultiplier) {
        if (pointRewardMultiplier != null && pointRewardMultiplier.compareTo(BigDecimal.ONE) > 0) {
            return "Payment reward x" + pointRewardMultiplier.stripTrailingZeros().toPlainString();
        }
        return "Payment reward";
    }

    private boolean hasBenefitType(List<MemberBenefitVO> memberBenefits, Integer benefitType) {
        return memberBenefits != null
            && memberBenefits.stream().anyMatch(benefit -> benefit != null && Integer.valueOf(benefitType).equals(benefit.getBenefitType()));
    }

    private MemberBenefitVO findBenefitByType(List<MemberBenefitVO> memberBenefits, Integer benefitType) {
        if (memberBenefits == null || memberBenefits.isEmpty()) {
            return null;
        }
        return memberBenefits.stream()
            .filter(benefit -> benefit != null && Integer.valueOf(benefitType).equals(benefit.getBenefitType()))
            .findFirst()
            .orElse(null);
    }

    private MemberBenefitVO findPointMultiplierBenefit(List<MemberBenefitVO> memberBenefits) {
        if (memberBenefits == null || memberBenefits.isEmpty()) {
            return null;
        }
        return memberBenefits.stream()
            .filter(benefit -> benefit != null
                && Integer.valueOf(MEMBER_BENEFIT_TYPE_POINTS_MULTIPLIER).equals(benefit.getBenefitType())
                && benefit.getBenefitValue() != null
                && benefit.getBenefitValue().compareTo(BigDecimal.ONE) > 0)
            .max((left, right) -> left.getBenefitValue().compareTo(right.getBenefitValue()))
            .orElse(null);
    }

    private MemberBenefitVO findMemberDiscountBenefit(List<MemberBenefitVO> memberBenefits, BigDecimal memberDiscountRate) {
        if (memberBenefits == null || memberBenefits.isEmpty()
            || memberDiscountRate == null || memberDiscountRate.compareTo(BigDecimal.ONE) >= 0) {
            return null;
        }
        return memberBenefits.stream()
            .filter(benefit -> benefit != null
                && Integer.valueOf(MEMBER_BENEFIT_TYPE_DISCOUNT).equals(benefit.getBenefitType())
                && normalizeDiscountRate(benefit.getBenefitValue()).compareTo(memberDiscountRate) == 0)
            .findFirst()
            .orElse(null);
    }

    private String buildMemberDiscountRemark(BigDecimal memberDiscountRate, BigDecimal memberDiscount) {
        BigDecimal displayDiscount = memberDiscountRate.multiply(BigDecimal.TEN).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        BigDecimal displayAmount = defaultAmount(memberDiscount).stripTrailingZeros();
        return "会员折扣 " + displayDiscount.toPlainString() + "折，抵扣 "
            + displayAmount.toPlainString() + " 元";
    }

    private void recordMemberBenefitUsage(Long userId, MemberBenefitVO benefit, String businessType, Long businessId,
                                          BigDecimal effectAmount, Integer effectPoints, String remark) {
        if (benefit == null || !hasUsageEffect(effectAmount, effectPoints)) {
            return;
        }
        memberBenefitService.recordBenefitUsage(userId, benefit, businessType, businessId, effectAmount, effectPoints, remark);
    }

    private boolean hasUsageEffect(BigDecimal effectAmount, Integer effectPoints) {
        return (effectAmount != null && effectAmount.compareTo(BigDecimal.ZERO) != 0)
            || (effectPoints != null && effectPoints != 0);
    }

    private BigDecimal calculateCouponDiscount(BigDecimal amount, Coupon coupon, BigDecimal remainingFreightAmount) {
        if (Integer.valueOf(COUPON_TYPE_FREE_SHIPPING).equals(coupon.getCouponType())) {
            return calculateFreeShippingCouponDiscount(amount, remainingFreightAmount);
        }
        if (coupon.getDiscountValue() == null) {
            throw new BusinessException("缺少优惠券优惠值");
        }
        if (Integer.valueOf(DISCOUNT_TYPE_AMOUNT).equals(coupon.getDiscountType())) {
            return coupon.getDiscountValue();
        }
        if (Integer.valueOf(DISCOUNT_TYPE_RATE).equals(coupon.getDiscountType())) {
            return amount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountValue()))
                .setScale(2, RoundingMode.HALF_UP);
        }
        throw new BusinessException("不支持的优惠类型");
    }

    private BigDecimal calculateFreeShippingCouponDiscount(BigDecimal amount, BigDecimal remainingFreightAmount) {
        BigDecimal remainingFreight = defaultAmount(remainingFreightAmount);
        if (remainingFreight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("当前订单无可抵扣配送费");
        }
        BigDecimal payableAmount = defaultAmount(amount);
        if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return remainingFreight.min(payableAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOrderPromotionDiscount(Long orderId) {
        if (orderId == null) {
            return BigDecimal.ZERO;
        }
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
        );
        return calculateOrderPromotionDiscount(orderItems);
    }

    private BigDecimal calculateOrderPromotionDiscount(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            totalDiscount = totalDiscount.add(calculateItemPromotionDiscount(item));
        }
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateItemPromotionDiscount(OrderItem item) {
        if (item == null || item.getProductId() == null) {
            return BigDecimal.ZERO;
        }

        PromotionProduct relation = findBestActivePromotionProduct(item.getProductId(), defaultAmount(item.getPrice()));
        if (relation == null || relation.getPromotionPrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal originUnitPrice = defaultAmount(item.getPrice());
        BigDecimal promotionUnitPrice = defaultAmount(relation.getPromotionPrice());
        if (promotionUnitPrice.compareTo(originUnitPrice) >= 0) {
            return BigDecimal.ZERO;
        }

        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        if (relation.getStockLimit() != null && relation.getStockLimit() > 0) {
            quantity = Math.min(quantity, relation.getStockLimit());
        }
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }

        return originUnitPrice.subtract(promotionUnitPrice)
            .multiply(BigDecimal.valueOf(quantity))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private PromotionProduct findBestActivePromotionProduct(Long productId, BigDecimal originUnitPrice) {
        List<PromotionProduct> relations = promotionProductMapper.selectList(
            new LambdaQueryWrapper<PromotionProduct>()
                .eq(PromotionProduct::getProductId, productId)
        );
        if (relations == null || relations.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        PromotionProduct bestRelation = null;
        BigDecimal bestPrice = originUnitPrice == null ? BigDecimal.ZERO : originUnitPrice;
        for (PromotionProduct relation : relations) {
            Promotion promotion = promotionMapper.selectById(relation.getPromotionId());
            if (!isActiveTradePromotion(promotion, now)) {
                continue;
            }
            BigDecimal promotionPrice = relation.getPromotionPrice();
            if (promotionPrice == null || promotionPrice.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }
            if (bestRelation == null || promotionPrice.compareTo(bestPrice) < 0) {
                bestRelation = relation;
                bestPrice = promotionPrice;
            }
        }
        return bestRelation != null && bestPrice.compareTo(originUnitPrice) < 0 ? bestRelation : null;
    }

    private boolean isActiveTradePromotion(Promotion promotion, LocalDateTime now) {
        if (promotion == null || !Integer.valueOf(1).equals(promotion.getStatus())) {
            return false;
        }
        if (promotion.getStartTime() != null && promotion.getStartTime().isAfter(now)) {
            return false;
        }
        return promotion.getEndTime() == null || !promotion.getEndTime().isBefore(now);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String generatePaymentNo() {
        return "PAY" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss")
            + String.format("%04d", (int) (Math.random() * 10000));
    }

    private String generateRefundNo() {
        return "REF" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss")
            + String.format("%04d", (int) (Math.random() * 10000));
    }

    private String generateAfterSaleNo() {
        return "AS" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss")
            + String.format("%04d", (int) (Math.random() * 10000));
    }
}
