package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.TradeConstants;
import com.coffee.dto.AfterSaleApplyDTO;
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
import com.coffee.dto.PaymentDTO;
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
import com.coffee.service.PointsService;
import com.coffee.service.support.TradeInventoryService;
import com.coffee.vo.MemberBenefitVO;
import com.coffee.vo.MemberInfoVO;
import com.coffee.vo.OrderSettlementVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PointsRecordMapper pointsRecordMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private PromotionProductMapper promotionProductMapper;

    @Mock
    private RefundMapper refundMapper;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private MemberBenefitService memberBenefitService;

    @Mock
    private MemberService memberService;

    @Mock
    private PointsService pointsService;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private DeliveryService deliveryService;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private OrderAfterSaleMapper orderAfterSaleMapper;

    @Mock
    private OrderAfterSaleLogMapper orderAfterSaleLogMapper;

    @Mock
    private TradeInventoryService tradeInventoryService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(Payment.class);
        initTableInfo(Order.class);
        initTableInfo(Refund.class);
        initTableInfo(OrderAfterSale.class);
        initTableInfo(OrderAfterSaleLog.class);
        initTableInfo(OrderItem.class);
        initTableInfo(Invoice.class);
        initTableInfo(UserCoupon.class);
        initTableInfo(Coupon.class);
        initTableInfo(PointsRecord.class);
        initTableInfo(Promotion.class);
        initTableInfo(PromotionProduct.class);
    }

    @Test
    void confirmPaymentSkipsWhenPaymentIsAlreadySuccessful() {
        Long userId = 100L;
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_SUCCESS);

        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(payment);

        paymentService.confirmPayment(userId, false, payment.getPaymentNo());

        verify(orderMapper, never()).selectById(any(Long.class));
        verify(tradeInventoryService, never()).deductStock(any(Long.class), any(), any(), any(), any());
        verify(pointsService, never()).addPoints(any(), any(), any(), any(), any());
        verify(deliveryService, never()).createDeliveryOrder(any());
    }

    @Test
    void confirmPaymentSkipsWhenPaymentClaimLosesToAlreadySuccessfulUpdate() {
        Long userId = 100L;
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_UNPAID);
        Payment latestPayment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_SUCCESS);
        Order order = pendingOrder(10L, "ORD100", userId);

        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(payment);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(paymentMapper.update(isNull(), anyPaymentUpdateWrapper())).thenReturn(0);
        when(paymentMapper.selectById(1L)).thenReturn(latestPayment);

        paymentService.confirmPayment(userId, false, payment.getPaymentNo());

        verify(orderMapper, never()).update(isNull(), anyOrderUpdateWrapper());
        verify(orderItemMapper, never()).selectList(anyOrderItemQueryWrapper());
        verify(tradeInventoryService, never()).deductStock(any(Long.class), any(), any(), any(), any());
        verify(pointsService, never()).addPoints(any(), any(), any(), any(), any());
        verify(deliveryService, never()).createDeliveryOrder(any());
    }

    @Test
    void confirmPaymentDeductsInventoryAndTriggersPostPaymentEffects() {
        Long userId = 100L;
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_UNPAID);
        Order order = pendingOrder(10L, "ORD100", userId);
        OrderItem skuItem = orderItem(10L, 20L, 200L, 2);
        OrderItem productItem = orderItem(10L, 21L, null, 1);

        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(payment);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(paymentMapper.update(isNull(), anyPaymentUpdateWrapper())).thenReturn(1);
        when(orderMapper.update(isNull(), anyOrderUpdateWrapper())).thenReturn(1);
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(skuItem, productItem));
        when(productMapper.increaseSales(any(Long.class), any(Integer.class))).thenReturn(1);
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of());

        paymentService.confirmPayment(userId, false, payment.getPaymentNo());

        verify(tradeInventoryService).deductStock(20L, 200L, 2, "订单 ORD100 支付成功扣减库存", userId);
        verify(tradeInventoryService).deductStock(21L, null, 1, "订单 ORD100 支付成功扣减库存", userId);
        verify(productMapper).increaseSales(20L, 2);
        verify(productMapper).increaseSales(21L, 1);
        verify(memberService).updateGrowthValue(userId, 66);
        verify(pointsService).addPoints(userId, 66, 1, 10L, "Payment reward");
        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals(userId, behavior.getUserId());
        assertEquals("PAY", behavior.getActionType());
        assertEquals("ORDER", behavior.getTargetType());
        assertEquals(10L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"paymentNo\":\"PAY100\""));
        assertTrue(behavior.getActionData().contains("\"rewardPoints\":66"));
        verify(deliveryService).createDeliveryOrder(10L);
        assertEquals(TradeConstants.PAY_STATUS_SUCCESS, payment.getPayStatus());
        assertEquals(TradeConstants.ORDER_STATUS_PAID, order.getOrderStatus());
        assertEquals(TradeConstants.PAY_STATUS_SUCCESS, order.getPayStatus());
    }

    @Test
    void confirmPaymentAppliesMemberPointsMultiplier() {
        Long userId = 100L;
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_UNPAID);
        Order order = pendingOrder(10L, "ORD100", userId);
        OrderItem item = orderItem(10L, 20L, 200L, 1);
        MemberBenefitVO benefit = pointsMultiplierBenefit(new BigDecimal("1.50"));

        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(payment);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(paymentMapper.update(isNull(), anyPaymentUpdateWrapper())).thenReturn(1);
        when(orderMapper.update(isNull(), anyOrderUpdateWrapper())).thenReturn(1);
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(item));
        when(productMapper.increaseSales(any(Long.class), any(Integer.class))).thenReturn(1);
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of(benefit));

        paymentService.confirmPayment(userId, false, payment.getPaymentNo());

        verify(memberService).updateGrowthValue(userId, 66);
        verify(pointsService).addPoints(userId, 99, 1, 10L, "Payment reward x1.5");
        verify(memberBenefitService).recordBenefitUsage(
            userId,
            benefit,
            "PAYMENT_REWARD",
            10L,
            BigDecimal.ZERO,
            99,
            "积分倍率 1.5x，支付奖励 99 积分"
        );
    }

    @Test
    void createPaymentAppliesFreeShippingBenefit() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        order.setFreightAmount(new BigDecimal("8.00"));
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(10L);
        paymentDTO.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        MemberBenefitVO benefit = freeShippingBenefit();

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of());
        when(pointsRecordMapper.selectList(anyPointsRecordQueryWrapper())).thenReturn(List.of());
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of(benefit));
        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(null);

        paymentService.createPayment(userId, paymentDTO);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(orderMapper).updateById(order);
        verify(paymentMapper).insert(paymentCaptor.capture());
        assertEquals(new BigDecimal("8.00"), order.getDiscountAmount());
        assertEquals(new BigDecimal("66.00"), order.getPayAmount());
        assertEquals(new BigDecimal("66.00"), paymentCaptor.getValue().getPayAmount());
        verify(memberBenefitService).recordBenefitUsage(
            userId,
            benefit,
            "PAYMENT_CREATE",
            10L,
            new BigDecimal("8.00"),
            null,
            "会员免配送费抵扣 8 元"
        );
    }

    @Test
    void createPaymentAppliesFreeShippingCouponOnlyToRemainingFreight() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setPayAmount(new BigDecimal("100.00"));
        order.setFreightAmount(new BigDecimal("8.00"));
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(10L);
        paymentDTO.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        paymentDTO.setCouponId(20L);
        UserCoupon userCoupon = userCoupon(20L, 30L, userId, LocalDateTime.now().plusDays(1));
        Coupon coupon = freeShippingCoupon(30L);

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of());
        when(pointsRecordMapper.selectList(anyPointsRecordQueryWrapper())).thenReturn(List.of());
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of());
        when(userCouponMapper.selectOne(anyUserCouponQueryWrapper())).thenReturn(userCoupon);
        when(couponMapper.selectById(30L)).thenReturn(coupon);
        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(null);

        paymentService.createPayment(userId, paymentDTO);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(orderMapper).updateById(order);
        verify(paymentMapper).insert(paymentCaptor.capture());
        assertEquals(new BigDecimal("8.00"), order.getDiscountAmount());
        assertEquals(new BigDecimal("100.00"), order.getPayAmount());
        assertEquals(new BigDecimal("100.00"), paymentCaptor.getValue().getPayAmount());
        assertEquals(1, userCoupon.getStatus());
        assertEquals(10L, userCoupon.getOrderId());
    }

    @Test
    void createPaymentRejectsFreeShippingCouponWhenMemberBenefitAlreadyCoversFreight() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setPayAmount(new BigDecimal("100.00"));
        order.setFreightAmount(new BigDecimal("8.00"));
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(10L);
        paymentDTO.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        paymentDTO.setCouponId(20L);
        UserCoupon userCoupon = userCoupon(20L, 30L, userId, LocalDateTime.now().plusDays(1));

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of());
        when(pointsRecordMapper.selectList(anyPointsRecordQueryWrapper())).thenReturn(List.of());
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of(freeShippingBenefit()));
        when(userCouponMapper.selectOne(anyUserCouponQueryWrapper())).thenReturn(userCoupon);
        when(couponMapper.selectById(30L)).thenReturn(freeShippingCoupon(30L));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> paymentService.createPayment(userId, paymentDTO)
        );

        assertEquals("当前订单无可抵扣配送费", exception.getMessage());
        verify(orderMapper, never()).updateById(order);
        verify(paymentMapper, never()).insert(any(Payment.class));
        verify(userCouponMapper, never()).updateById(userCoupon);
    }

    @Test
    void createPaymentMarksExpiredCouponUnavailable() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(10L);
        paymentDTO.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        paymentDTO.setCouponId(20L);
        UserCoupon expiredCoupon = userCoupon(20L, 30L, userId, LocalDateTime.now().minusDays(1));

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of());
        when(pointsRecordMapper.selectList(anyPointsRecordQueryWrapper())).thenReturn(List.of());
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of());
        when(userCouponMapper.selectOne(anyUserCouponQueryWrapper())).thenReturn(expiredCoupon);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> paymentService.createPayment(userId, paymentDTO)
        );

        assertEquals("优惠券不可用", exception.getMessage());
        assertEquals(2, expiredCoupon.getStatus());
        verify(userCouponMapper).updateById(expiredCoupon);
        verify(couponMapper, never()).selectById(any(Long.class));
    }

    @Test
    void getOrderSettlementAppliesMemberBenefitsPreview() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setFreightAmount(new BigDecimal("8.00"));
        OrderItem item = orderItem(10L, 20L, null, 1);
        MemberBenefitVO freeShipping = freeShippingBenefit();
        MemberBenefitVO memberDiscount = memberDiscountBenefit(new BigDecimal("0.80"));
        MemberBenefitVO pointsMultiplier = pointsMultiplierBenefit(new BigDecimal("1.50"));
        MemberInfoVO memberInfo = new MemberInfoVO();
        memberInfo.setDiscountRate(new BigDecimal("0.90"));

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(item));
        when(pointsService.getPointsBalance(userId)).thenReturn(500);
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of(freeShipping, memberDiscount, pointsMultiplier));
        when(memberService.getMemberInfo(userId)).thenReturn(memberInfo);

        OrderSettlementVO settlement = paymentService.getOrderSettlement(userId, 10L);

        assertEquals(new BigDecimal("8.00"), settlement.getFreightDiscount());
        assertEquals(new BigDecimal("0.80"), settlement.getMemberDiscountRate());
        assertEquals(new BigDecimal("20.00"), settlement.getMemberDiscount());
        assertEquals(new BigDecimal("1.50"), settlement.getPointRewardMultiplier());
        assertEquals(120, settlement.getEstimatedRewardPoints());
        assertEquals(new BigDecimal("80.00"), settlement.getPayAmount());
        assertEquals(500, settlement.getAvailablePoints());
    }

    @Test
    void createPaymentAppliesMemberDiscountBenefitBeforePaymentCreation() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setPayAmount(new BigDecimal("100.00"));
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(10L);
        paymentDTO.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        MemberBenefitVO benefit = memberDiscountBenefit(new BigDecimal("0.80"));
        MemberInfoVO memberInfo = new MemberInfoVO();
        memberInfo.setDiscountRate(new BigDecimal("0.90"));

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of());
        when(pointsRecordMapper.selectList(anyPointsRecordQueryWrapper())).thenReturn(List.of());
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of(benefit));
        when(memberService.getMemberInfo(userId)).thenReturn(memberInfo);
        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(null);

        paymentService.createPayment(userId, paymentDTO);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(orderMapper).updateById(order);
        verify(paymentMapper).insert(paymentCaptor.capture());
        assertEquals(new BigDecimal("20.00"), order.getDiscountAmount());
        assertEquals(new BigDecimal("80.00"), order.getPayAmount());
        assertEquals(new BigDecimal("80.00"), paymentCaptor.getValue().getPayAmount());
        verify(memberBenefitService).recordBenefitUsage(
            userId,
            benefit,
            "PAYMENT_CREATE",
            10L,
            new BigDecimal("20.00"),
            null,
            "会员折扣 8折，抵扣 20 元"
        );
    }

    @Test
    void createPaymentAppliesPromotionDiscountBeforeUserDiscounts() {
        Long userId = 100L;
        Order order = pendingOrder(10L, "ORD100", userId);
        OrderItem item = orderItem(10L, 20L, null, 2);
        item.setPrice(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setPayAmount(new BigDecimal("20.00"));
        PromotionProduct relation = new PromotionProduct();
        relation.setPromotionId(30L);
        relation.setProductId(20L);
        relation.setPromotionPrice(new BigDecimal("8.00"));
        Promotion promotion = new Promotion();
        promotion.setId(30L);
        promotion.setType(1);
        promotion.setStatus(1);
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(10L);
        paymentDTO.setPayType(TradeConstants.PAY_TYPE_ALIPAY);

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of());
        when(pointsRecordMapper.selectList(anyPointsRecordQueryWrapper())).thenReturn(List.of());
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(item));
        when(promotionProductMapper.selectList(anyPromotionProductQueryWrapper())).thenReturn(List.of(relation));
        when(promotionMapper.selectById(30L)).thenReturn(promotion);
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of());
        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(null);

        paymentService.createPayment(userId, paymentDTO);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(orderMapper).updateById(order);
        verify(paymentMapper).insert(paymentCaptor.capture());
        assertEquals(new BigDecimal("4.00"), order.getDiscountAmount());
        assertEquals(new BigDecimal("16.00"), order.getPayAmount());
        assertEquals(new BigDecimal("16.00"), paymentCaptor.getValue().getPayAmount());
    }

    @Test
    void confirmPaymentThrowsWhenOrderClaimFailsBeforeInventoryDeduction() {
        Long userId = 100L;
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_UNPAID);
        Order order = pendingOrder(10L, "ORD100", userId);

        when(paymentMapper.selectOne(anyPaymentQueryWrapper())).thenReturn(payment);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(paymentMapper.update(isNull(), anyPaymentUpdateWrapper())).thenReturn(1);
        when(orderMapper.update(isNull(), anyOrderUpdateWrapper())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> paymentService.confirmPayment(userId, false, payment.getPaymentNo()));

        assertEquals("订单已取消或状态已变更，无法确认支付", exception.getMessage());
        verify(orderItemMapper, never()).selectList(anyOrderItemQueryWrapper());
        verify(tradeInventoryService, never()).deductStock(any(Long.class), any(), any(), any(), any());
        verify(pointsService, never()).addPoints(any(), any(), any(), any(), any());
        verify(deliveryService, never()).createDeliveryOrder(any());
    }

    @Test
    void applyRefundCreatesPendingRefundAndAfterSaleWithoutRestoringInventory() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(null);
        when(refundMapper.selectOne(anyRefundQueryWrapper())).thenReturn(null);

        paymentService.applyRefund(userId, 10L, "user requested");

        ArgumentCaptor<OrderAfterSale> afterSaleCaptor = ArgumentCaptor.forClass(OrderAfterSale.class);
        ArgumentCaptor<OrderAfterSaleLog> afterSaleLogCaptor = ArgumentCaptor.forClass(OrderAfterSaleLog.class);
        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(orderAfterSaleMapper).insert(afterSaleCaptor.capture());
        verify(orderAfterSaleLogMapper).insert(afterSaleLogCaptor.capture());
        verify(refundMapper).insert(refundCaptor.capture());
        verify(orderMapper).updateById(order);
        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals("REFUND_APPLY", behavior.getActionType());
        assertEquals("ORDER", behavior.getTargetType());
        assertEquals(10L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"orderNo\":\"ORD100\""));
        assertTrue(behavior.getActionData().contains("\"reason\":\"user requested\""));
        verify(tradeInventoryService, never()).restoreStock(any(Long.class), any(), any(), any(), any());
        verify(paymentMapper, never()).selectList(anyPaymentQueryWrapper());
        verify(invoiceMapper, never()).selectOne(anyInvoiceQueryWrapper());
        verify(pointsService, never()).deductPoints(any(), any(), any(), any(), any());

        assertEquals(TradeConstants.AFTER_SALE_STATUS_PENDING, afterSaleCaptor.getValue().getStatus());
        assertEquals(TradeConstants.AFTER_SALE_LOG_ACTION_APPLY, afterSaleLogCaptor.getValue().getAction());
        assertEquals(TradeConstants.AFTER_SALE_OPERATOR_USER, afterSaleLogCaptor.getValue().getOperatorType());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_PENDING, afterSaleLogCaptor.getValue().getStatusTo());
        assertEquals(TradeConstants.REFUND_STATUS_APPLYING, refundCaptor.getValue().getRefundStatus());
        assertNull(refundCaptor.getValue().getRefundTime());
        assertEquals(TradeConstants.ORDER_STATUS_REFUNDING, order.getOrderStatus());
        assertEquals(TradeConstants.PAY_STATUS_SUCCESS, order.getPayStatus());
    }

    @Test
    void applyReturnRefundUsesRequestedAmountAndEvidenceImages() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        AfterSaleApplyDTO dto = afterSaleApplyDTO(
            10L,
            TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND,
            "商品破损",
            "杯盖破损并有洒漏",
            "[\"/uploads/after-sale.jpg\"]",
            new BigDecimal("20.00")
        );

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(null);
        when(refundMapper.selectOne(anyRefundQueryWrapper())).thenReturn(null);

        paymentService.applyAfterSale(userId, dto);

        ArgumentCaptor<OrderAfterSale> afterSaleCaptor = ArgumentCaptor.forClass(OrderAfterSale.class);
        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(orderAfterSaleMapper).insert(afterSaleCaptor.capture());
        verify(refundMapper).insert(refundCaptor.capture());
        verify(orderMapper).updateById(order);

        OrderAfterSale afterSale = afterSaleCaptor.getValue();
        assertEquals(TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND, afterSale.getType());
        assertEquals("商品破损", afterSale.getReason());
        assertEquals("杯盖破损并有洒漏", afterSale.getDescription());
        assertEquals("[\"/uploads/after-sale.jpg\"]", afterSale.getImages());
        assertEquals(new BigDecimal("20.00"), afterSale.getRefundAmount());
        assertEquals(new BigDecimal("20.00"), refundCaptor.getValue().getRefundAmount());
        assertEquals(TradeConstants.ORDER_STATUS_REFUNDING, order.getOrderStatus());
    }

    @Test
    void applyDeliveryAfterSaleCreatesOnlyAfterSaleWithoutRefundOrOrderStatusChange() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_DELIVERING);
        order.setStatus(TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_DELIVERING));
        AfterSaleApplyDTO dto = afterSaleApplyDTO(
            10L,
            TradeConstants.AFTER_SALE_TYPE_DELIVERY,
            "配送超时",
            "配送员长时间未送达",
            null,
            null
        );

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(null);

        paymentService.applyAfterSale(userId, dto);

        ArgumentCaptor<OrderAfterSale> afterSaleCaptor = ArgumentCaptor.forClass(OrderAfterSale.class);
        verify(orderAfterSaleMapper).insert(afterSaleCaptor.capture());
        verify(refundMapper, never()).selectOne(anyRefundQueryWrapper());
        verify(refundMapper, never()).insert(any(Refund.class));
        verify(orderMapper, never()).updateById(order);

        OrderAfterSale afterSale = afterSaleCaptor.getValue();
        assertEquals(TradeConstants.AFTER_SALE_TYPE_DELIVERY, afterSale.getType());
        assertEquals(BigDecimal.ZERO, afterSale.getRefundAmount());
        assertEquals("[]", afterSale.getImages());
        assertEquals(TradeConstants.ORDER_STATUS_DELIVERING, order.getOrderStatus());
    }

    @Test
    void applyAfterSaleRejectsRefundAmountGreaterThanPayAmount() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        AfterSaleApplyDTO dto = afterSaleApplyDTO(
            10L,
            TradeConstants.AFTER_SALE_TYPE_REFUND,
            "退款金额错误",
            "申请金额超过实付金额",
            null,
            new BigDecimal("100.00")
        );

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(null);
        when(refundMapper.selectOne(anyRefundQueryWrapper())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> paymentService.applyAfterSale(userId, dto));

        assertEquals("退款金额不能超过订单实付金额", exception.getMessage());
        verify(orderAfterSaleMapper, never()).insert(any(OrderAfterSale.class));
        verify(refundMapper, never()).insert(any(Refund.class));
    }

    @Test
    void applyAfterSaleRejectsExistingPendingAfterSale() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        OrderAfterSale existing = afterSale(50L, 10L, userId, TradeConstants.AFTER_SALE_STATUS_PENDING);
        AfterSaleApplyDTO dto = afterSaleApplyDTO(
            10L,
            TradeConstants.AFTER_SALE_TYPE_REFUND,
            "重复申请",
            "重复申请退款",
            null,
            null
        );

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> paymentService.applyAfterSale(userId, dto));

        assertEquals("售后申请处理中", exception.getMessage());
        verify(refundMapper, never()).selectOne(anyRefundQueryWrapper());
        verify(orderAfterSaleMapper, never()).insert(any(OrderAfterSale.class));
    }

    @Test
    void approveRefundRestoresInventoryAndMarksPaymentOrderAndInvoiceRefunded() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_REFUNDING);
        order.setStatus(TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_REFUNDING));
        Refund refund = refund(40L, 10L, "ORD100", userId, TradeConstants.REFUND_STATUS_APPLYING);
        OrderAfterSale afterSale = afterSale(50L, 10L, userId, TradeConstants.AFTER_SALE_STATUS_PENDING);
        OrderItem skuItem = orderItem(10L, 20L, 200L, 2);
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_SUCCESS);
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        invoice.setOrderId(10L);

        when(refundMapper.selectById(40L)).thenReturn(refund);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(skuItem));
        when(productMapper.decreaseSales(20L, 2)).thenReturn(1);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(afterSale);
        when(paymentMapper.selectList(anyPaymentQueryWrapper())).thenReturn(List.of(payment));
        when(invoiceMapper.selectOne(anyInvoiceQueryWrapper())).thenReturn(invoice);
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of());

        paymentService.approveRefund(40L, "approved");

        ArgumentCaptor<OrderAfterSaleLog> afterSaleLogCaptor = ArgumentCaptor.forClass(OrderAfterSaleLog.class);
        verify(tradeInventoryService).restoreStock(20L, 200L, 2, "订单 ORD100 退款审核通过回补库存", userId);
        verify(productMapper).decreaseSales(20L, 2);
        verify(refundMapper).updateById(refund);
        verify(orderAfterSaleMapper).updateById(afterSale);
        verify(orderAfterSaleLogMapper).insert(afterSaleLogCaptor.capture());
        verify(paymentMapper).updateById(payment);
        verify(orderMapper).updateById(order);
        verify(invoiceMapper).updateById(invoice);
        verify(pointsService).deductPoints(userId, 66, 5, 10L, "Refund points rollback");
        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals("REFUND", behavior.getActionType());
        assertEquals("ORDER", behavior.getTargetType());
        assertEquals(10L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"refundNo\":\"REF100\""));
        assertTrue(behavior.getActionData().contains("\"reviewRemark\":\"approved\""));
        assertEquals(TradeConstants.REFUND_STATUS_SUCCESS, refund.getRefundStatus());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_COMPLETED, afterSale.getStatus());
        assertEquals("approved", afterSale.getHandleRemark());
        assertEquals(TradeConstants.AFTER_SALE_LOG_ACTION_REFUND_COMPLETE, afterSaleLogCaptor.getValue().getAction());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_PENDING, afterSaleLogCaptor.getValue().getStatusFrom());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_COMPLETED, afterSaleLogCaptor.getValue().getStatusTo());
        assertEquals(TradeConstants.PAY_STATUS_REFUNDED, payment.getPayStatus());
        assertEquals(TradeConstants.ORDER_STATUS_REFUNDED, order.getOrderStatus());
        assertEquals(TradeConstants.PAY_STATUS_REFUNDED, order.getPayStatus());
        assertEquals(3, invoice.getStatus());
    }

    @Test
    void approveRefundRecordsPointsMultiplierRollbackUsage() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_REFUNDING);
        order.setStatus(TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_REFUNDING));
        Refund refund = refund(40L, 10L, "ORD100", userId, TradeConstants.REFUND_STATUS_APPLYING);
        OrderAfterSale afterSale = afterSale(50L, 10L, userId, TradeConstants.AFTER_SALE_STATUS_PENDING);
        OrderItem item = orderItem(10L, 20L, 200L, 1);
        Payment payment = payment(1L, "PAY100", 10L, "ORD100", userId, TradeConstants.PAY_STATUS_SUCCESS);
        MemberBenefitVO benefit = pointsMultiplierBenefit(new BigDecimal("1.50"));

        when(refundMapper.selectById(40L)).thenReturn(refund);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(item));
        when(productMapper.decreaseSales(20L, 1)).thenReturn(1);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(afterSale);
        when(paymentMapper.selectList(anyPaymentQueryWrapper())).thenReturn(List.of(payment));
        when(invoiceMapper.selectOne(anyInvoiceQueryWrapper())).thenReturn(null);
        when(memberBenefitService.getUserBenefits(userId)).thenReturn(List.of(benefit));

        paymentService.approveRefund(40L, "approved");

        verify(pointsService).deductPoints(userId, 99, 5, 10L, "Refund points rollback");
        verify(memberBenefitService).recordBenefitUsage(
            userId,
            benefit,
            "REFUND_ROLLBACK",
            40L,
            BigDecimal.ZERO,
            -99,
            "退款扣回积分倍率 1.5x，扣回 99 积分"
        );
    }

    @Test
    void rejectRefundMarksFailedAndRestoresPaidOrderWithoutInventory() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_REFUNDING);
        order.setStatus(TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_REFUNDING));
        Refund refund = refund(40L, 10L, "ORD100", userId, TradeConstants.REFUND_STATUS_APPLYING);
        OrderAfterSale afterSale = afterSale(50L, 10L, userId, TradeConstants.AFTER_SALE_STATUS_PENDING);

        when(refundMapper.selectById(40L)).thenReturn(refund);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderAfterSaleMapper.selectOne(anyAfterSaleQueryWrapper())).thenReturn(afterSale);

        paymentService.rejectRefund(40L, "not eligible");

        ArgumentCaptor<OrderAfterSaleLog> afterSaleLogCaptor = ArgumentCaptor.forClass(OrderAfterSaleLog.class);
        verify(refundMapper).updateById(refund);
        verify(orderAfterSaleMapper).updateById(afterSale);
        verify(orderAfterSaleLogMapper).insert(afterSaleLogCaptor.capture());
        verify(orderMapper).updateById(order);
        ArgumentCaptor<UserBehavior> behaviorCaptor = ArgumentCaptor.forClass(UserBehavior.class);
        verify(analyticsService).recordUserBehavior(behaviorCaptor.capture());
        UserBehavior behavior = behaviorCaptor.getValue();
        assertEquals("REFUND_REJECT", behavior.getActionType());
        assertEquals("ORDER", behavior.getTargetType());
        assertEquals(10L, behavior.getTargetId());
        assertTrue(behavior.getActionData().contains("\"refundNo\":\"REF100\""));
        assertTrue(behavior.getActionData().contains("\"reviewRemark\":\"not eligible\""));
        verify(tradeInventoryService, never()).restoreStock(any(Long.class), any(), any(), any(), any());
        verify(paymentMapper, never()).selectList(anyPaymentQueryWrapper());
        assertEquals(TradeConstants.REFUND_STATUS_FAILED, refund.getRefundStatus());
        assertNull(refund.getRefundTime());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_REJECTED, afterSale.getStatus());
        assertEquals("not eligible", afterSale.getHandleRemark());
        assertEquals(TradeConstants.AFTER_SALE_LOG_ACTION_REJECT, afterSaleLogCaptor.getValue().getAction());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_PENDING, afterSaleLogCaptor.getValue().getStatusFrom());
        assertEquals(TradeConstants.AFTER_SALE_STATUS_REJECTED, afterSaleLogCaptor.getValue().getStatusTo());
        assertEquals(TradeConstants.ORDER_STATUS_PAID, order.getOrderStatus());
        assertEquals(TradeConstants.PAY_STATUS_SUCCESS, order.getPayStatus());
    }

    @Test
    void applyRefundRejectsAlreadyRefundedOrderWithoutRestoringInventory() {
        Long userId = 100L;
        Order order = paidOrder(10L, "ORD100", userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_REFUNDED);

        when(orderMapper.selectById(10L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> paymentService.applyRefund(userId, 10L, "duplicate"));

        assertEquals("订单已退款", exception.getMessage());
        verify(tradeInventoryService, never()).restoreStock(any(Long.class), any(), any(), any(), any());
        verify(refundMapper, never()).insert(any(Refund.class));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    private Payment payment(Long id, String paymentNo, Long orderId, String orderNo, Long userId, Integer payStatus) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(orderId);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setPayAmount(new BigDecimal("66.00"));
        payment.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        payment.setPayStatus(payStatus);
        return payment;
    }

    private Order pendingOrder(Long id, String orderNo, Long userId) {
        Order order = baseOrder(id, orderNo, userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_PENDING);
        order.setPayStatus(TradeConstants.PAY_STATUS_UNPAID);
        order.setStatus(TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_PENDING));
        return order;
    }

    private Order paidOrder(Long id, String orderNo, Long userId) {
        Order order = baseOrder(id, orderNo, userId);
        order.setOrderStatus(TradeConstants.ORDER_STATUS_PAID);
        order.setPayStatus(TradeConstants.PAY_STATUS_SUCCESS);
        order.setPayType(TradeConstants.PAY_TYPE_ALIPAY);
        order.setPaymentMethod(TradeConstants.resolvePaymentMethod(TradeConstants.PAY_TYPE_ALIPAY));
        order.setStatus(TradeConstants.resolveOrderStatusCode(TradeConstants.ORDER_STATUS_PAID));
        return order;
    }

    private Order baseOrder(Long id, String orderNo, Long userId) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("66.00"));
        order.setPayAmount(new BigDecimal("66.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        return order;
    }

    private OrderItem orderItem(Long orderId, Long productId, Long skuId, Integer quantity) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        return item;
    }

    private UserCoupon userCoupon(Long id, Long couponId, Long userId, LocalDateTime expireTime) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(id);
        userCoupon.setCouponId(couponId);
        userCoupon.setUserId(userId);
        userCoupon.setStatus(0);
        userCoupon.setExpireTime(expireTime);
        return userCoupon;
    }

    private Coupon freeShippingCoupon(Long id) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCouponName("免配送费券");
        coupon.setCouponType(3);
        coupon.setDiscountType(1);
        coupon.setDiscountValue(BigDecimal.ZERO);
        coupon.setMinAmount(BigDecimal.ZERO);
        coupon.setStartTime(LocalDateTime.now().minusDays(1));
        coupon.setEndTime(LocalDateTime.now().plusDays(7));
        coupon.setStatus(1);
        return coupon;
    }

    private Refund refund(Long id, Long orderId, String orderNo, Long userId, Integer refundStatus) {
        Refund refund = new Refund();
        refund.setId(id);
        refund.setRefundNo("REF100");
        refund.setOrderId(orderId);
        refund.setOrderNo(orderNo);
        refund.setUserId(userId);
        refund.setRefundAmount(new BigDecimal("66.00"));
        refund.setRefundReason("user requested");
        refund.setRefundStatus(refundStatus);
        return refund;
    }

    private OrderAfterSale afterSale(Long id, Long orderId, Long userId, Integer status) {
        OrderAfterSale afterSale = new OrderAfterSale();
        afterSale.setId(id);
        afterSale.setOrderId(orderId);
        afterSale.setUserId(userId);
        afterSale.setAfterSaleNo("AS100");
        afterSale.setType(TradeConstants.AFTER_SALE_TYPE_REFUND);
        afterSale.setStatus(status);
        afterSale.setRefundAmount(new BigDecimal("66.00"));
        return afterSale;
    }

    private AfterSaleApplyDTO afterSaleApplyDTO(
            Long orderId,
            Integer type,
            String reason,
            String description,
            String images,
            BigDecimal refundAmount) {
        AfterSaleApplyDTO dto = new AfterSaleApplyDTO();
        dto.setOrderId(orderId);
        dto.setType(type);
        dto.setReason(reason);
        dto.setDescription(description);
        dto.setImages(images);
        dto.setRefundAmount(refundAmount);
        return dto;
    }

    private MemberBenefitVO pointsMultiplierBenefit(BigDecimal value) {
        MemberBenefitVO benefit = new MemberBenefitVO();
        benefit.setId(1L);
        benefit.setBenefitName("积分加速");
        benefit.setBenefitType(2);
        benefit.setBenefitValue(value);
        return benefit;
    }

    private MemberBenefitVO freeShippingBenefit() {
        MemberBenefitVO benefit = new MemberBenefitVO();
        benefit.setId(2L);
        benefit.setBenefitName("免配送费");
        benefit.setBenefitType(3);
        return benefit;
    }

    private MemberBenefitVO memberDiscountBenefit(BigDecimal value) {
        MemberBenefitVO benefit = new MemberBenefitVO();
        benefit.setId(3L);
        benefit.setBenefitName("会员专属折扣");
        benefit.setBenefitType(1);
        benefit.setBenefitValue(value);
        return benefit;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Payment> anyPaymentQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<OrderItem> anyOrderItemQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Refund> anyRefundQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Invoice> anyInvoiceQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<OrderAfterSale> anyAfterSaleQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<UserCoupon> anyUserCouponQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<PointsRecord> anyPointsRecordQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<PromotionProduct> anyPromotionProductQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<Payment> anyPaymentUpdateWrapper() {
        return any(LambdaUpdateWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<Order> anyOrderUpdateWrapper() {
        return any(LambdaUpdateWrapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
