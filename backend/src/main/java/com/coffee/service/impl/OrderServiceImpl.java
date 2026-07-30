package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.TradeConstants;
import com.coffee.dto.OrderCreateDTO;
import com.coffee.dto.PaymentDTO;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryOrder;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.ShoppingCart;
import com.coffee.entity.UserAddress;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.DeliveryMethodMapper;
import com.coffee.mapper.DeliveryOrderMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.ShoppingCartMapper;
import com.coffee.mapper.UserAddressMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.service.OrderService;
import com.coffee.service.PaymentService;
import com.coffee.service.support.TradeInventoryService;
import com.coffee.vo.OrderItemVO;
import com.coffee.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final int DELIVERY_STATUS_DELIVERED = 4;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShoppingCartMapper cartMapper;
    private final UserAddressMapper addressMapper;
    private final DeliveryMethodMapper deliveryMethodMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final AnalyticsService analyticsService;
    private final PaymentService paymentService;
    private final TradeInventoryService tradeInventoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, OrderCreateDTO orderCreateDTO) {
        UserAddress address = addressMapper.selectById(orderCreateDTO.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ADDRESS_NOT_EXIST);
        }

        List<ShoppingCart> cartItems = cartMapper.selectList(
            new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId)
                .in(ShoppingCart::getId, orderCreateDTO.getCartIds())
        );
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.CART_EMPTY);
        }
        if (cartItems.size() != orderCreateDTO.getCartIds().size()) {
            throw new BusinessException("部分购物车商品不存在或无权限结算");
        }

        DeliveryMethod deliveryMethod = resolveDeliveryMethod(orderCreateDTO.getDeliveryMethodId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, TradeInventoryService.ResolvedTradeItem> itemSnapshotMap = cartItems.stream()
            .collect(Collectors.toMap(
                ShoppingCart::getId,
                item -> {
                    tradeInventoryService.validateSaleQuantity(item.getProductId(), item.getSkuId(), item.getQuantity());
                    return tradeInventoryService.resolveTradeItem(item.getProductId(), item.getSkuId());
                }
            ));

        for (ShoppingCart cartItem : cartItems) {
            TradeInventoryService.ResolvedTradeItem resolvedItem = itemSnapshotMap.get(cartItem.getId());
            BigDecimal itemTotal = resolvedItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        BigDecimal freightAmount = calculateFreightAmount(totalAmount, deliveryMethod);

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(freightAmount);
        order.setPayAmount(totalAmount.add(freightAmount));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setDeliveryMethodId(deliveryMethod.getId());
        order.setDeliveryMethodName(deliveryMethod.getMethodName());
        TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_PENDING);
        TradeConstants.syncOrderPayment(order, null, TradeConstants.PAY_STATUS_UNPAID);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity()
            + address.getDistrict() + address.getDetailAddress());
        order.setRemark(orderCreateDTO.getRemark());
        orderMapper.insert(order);

        for (ShoppingCart cartItem : cartItems) {
            TradeInventoryService.ResolvedTradeItem resolvedItem = itemSnapshotMap.get(cartItem.getId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(resolvedItem.getProductId());
            orderItem.setSkuId(resolvedItem.getSkuId());
            orderItem.setProductName(resolvedItem.getProductName());
            orderItem.setProductImage(resolvedItem.getProductImage());
            orderItem.setSpecInfo(resolvedItem.getSpecInfo());
            orderItem.setPrice(resolvedItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalAmount(resolvedItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemMapper.insert(orderItem);
        }

        cartMapper.deleteBatchIds(orderCreateDTO.getCartIds());

        for (ShoppingCart cartItem : cartItems) {
            TradeInventoryService.ResolvedTradeItem resolvedItem = itemSnapshotMap.get(cartItem.getId());
            tradeInventoryService.lockStock(
                resolvedItem.getProductId(),
                resolvedItem.getSkuId(),
                cartItem.getQuantity(),
                "订单 " + order.getOrderNo() + " 创建锁定库存",
                userId
            );
        }

        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setActionType("ORDER");
            behavior.setTargetType("ORDER");
            behavior.setTargetId(order.getId());
            behavior.setPageUrl("/order/confirm");
            behavior.setActionData(JSONUtil.createObj()
                    .set("orderNo", order.getOrderNo())
                    .set("totalAmount", order.getTotalAmount())
                    .set("freightAmount", order.getFreightAmount())
                    .set("payAmount", order.getPayAmount())
                    .set("deliveryMethodId", order.getDeliveryMethodId())
                    .set("deliveryMethodName", order.getDeliveryMethodName())
                    .set("itemCount", cartItems.size())
                    .toString());
            analyticsService.recordUserBehavior(behavior);
        } catch (Exception e) {
            log.warn("记录用户下单行为失败: {}", e.getMessage());
        }

        log.info("订单创建成功: orderNo={}, userId={}", order.getOrderNo(), userId);
        return order.getId();
    }

    @Override
    public IPage<OrderVO> getOrderPage(Long userId, Integer pageNum, Integer pageSize, Integer orderStatus) {
        Page<Order> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
            .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
            .orderByDesc(Order::getCreateTime);

        IPage<Order> orderPage = orderMapper.selectPage(page, wrapper);

        IPage<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        Map<Long, List<OrderItemVO>> orderItemMap = getOrderItemVOMap(orderPage.getRecords().stream()
            .map(Order::getId)
            .collect(Collectors.toList()));
        List<OrderVO> voList = orderPage.getRecords().stream()
            .map(order -> {
                OrderVO orderVO = convertToVO(order);
                orderVO.setItems(orderItemMap.getOrDefault(order.getId(), List.of()));
                return orderVO;
            })
            .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public OrderVO getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }

        OrderVO orderVO = convertToVO(order);
        orderVO.setItems(getOrderItemVOMap(List.of(orderId)).getOrDefault(orderId, List.of()));
        return orderVO;
    }

    @Override
    public IPage<OrderVO> getAdminOrderPage(
            Integer pageNum,
            Integer pageSize,
            String orderNo,
            Long userId,
            Integer orderStatus,
            Integer payStatus) {
        Page<Order> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(orderNo != null && !orderNo.trim().isEmpty(), Order::getOrderNo, orderNo == null ? null : orderNo.trim())
            .eq(userId != null, Order::getUserId, userId)
            .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
            .eq(payStatus != null, Order::getPayStatus, payStatus)
            .orderByDesc(Order::getCreateTime);

        IPage<Order> orderPage = orderMapper.selectPage(page, wrapper);

        IPage<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        Map<Long, List<OrderItemVO>> orderItemMap = getOrderItemVOMap(orderPage.getRecords().stream()
            .map(Order::getId)
            .collect(Collectors.toList()));
        List<OrderVO> voList = orderPage.getRecords().stream()
            .map(order -> {
                OrderVO orderVO = convertToVO(order);
                orderVO.setItems(orderItemMap.getOrDefault(order.getId(), List.of()));
                return orderVO;
            })
            .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public OrderVO getAdminOrderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }

        OrderVO orderVO = convertToVO(order);
        orderVO.setItems(getOrderItemVOMap(List.of(orderId)).getOrDefault(orderId, List.of()));
        return orderVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!Integer.valueOf(TradeConstants.ORDER_STATUS_PENDING).equals(order.getOrderStatus())) {
            throw new BusinessException("只能取消待付款订单");
        }

        paymentService.rollbackPendingBenefits(userId, orderId);

        releaseOrderLockedStock(orderId, "订单 " + order.getOrderNo() + " 取消释放库存", userId);

        TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        log.info("订单取消成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long userId, Long orderId, String paymentMethod) {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(orderId);
        paymentDTO.setPayType(TradeConstants.resolvePayType(paymentMethod));
        String paymentNo = paymentService.createPayment(userId, paymentDTO);
        paymentService.confirmPayment(userId, false, paymentNo);
        log.info("兼容旧支付接口成功: orderId={}, paymentMethod={}", orderId, paymentMethod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceipt(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }

        if (!Integer.valueOf(TradeConstants.ORDER_STATUS_DELIVERING).equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态错误，当前状态无法确认收货");
        }
        if (!Integer.valueOf(TradeConstants.PAY_STATUS_SUCCESS).equals(order.getPayStatus())) {
            throw new BusinessException("订单未支付，无法确认收货");
        }

        DeliveryOrder deliveryOrder = deliveryOrderMapper.selectOne(
            new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getOrderId, orderId)
                .last("LIMIT 1")
        );
        if (deliveryOrder == null || !Integer.valueOf(DELIVERY_STATUS_DELIVERED).equals(deliveryOrder.getDeliveryStatus())) {
            throw new BusinessException("订单尚未送达，无法确认收货");
        }

        TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_COMPLETED);
        order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单确认收货成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancelOrder(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!Integer.valueOf(TradeConstants.ORDER_STATUS_PENDING).equals(order.getOrderStatus())) {
            throw new BusinessException("只能取消待付款订单");
        }

        paymentService.rollbackPendingBenefits(order.getUserId(), orderId);

        releaseOrderLockedStock(orderId, "订单 " + order.getOrderNo() + " 管理员取消释放库存", order.getUserId());

        TradeConstants.syncOrderStatus(order, TradeConstants.ORDER_STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason((reason == null || reason.isBlank()) ? "管理员后台取消订单" : reason.trim());
        orderMapper.updateById(order);
        log.info("管理员取消订单成功: orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }

        if (!List.of(
            TradeConstants.ORDER_STATUS_COMPLETED,
            TradeConstants.ORDER_STATUS_CANCELLED,
            TradeConstants.ORDER_STATUS_REFUNDED
        ).contains(order.getOrderStatus())) {
            throw new BusinessException("只能删除已完成、已取消或已退款订单");
        }

        orderMapper.deleteById(orderId);
        log.info("订单删除成功: orderId={}", orderId);
    }

    private String generateOrderNo() {
        return "ORD" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss")
            + String.format("%04d", (int) (Math.random() * 10000));
    }

    private DeliveryMethod resolveDeliveryMethod(Long deliveryMethodId) {
        DeliveryMethod method = null;
        if (deliveryMethodId != null) {
            method = deliveryMethodMapper.selectById(deliveryMethodId);
            if (method == null || !Integer.valueOf(1).equals(method.getStatus())) {
                throw new BusinessException("配送方式不可用");
            }
        } else {
            method = deliveryMethodMapper.selectOne(
                new LambdaQueryWrapper<DeliveryMethod>()
                    .eq(DeliveryMethod::getStatus, 1)
                    .orderByAsc(DeliveryMethod::getId)
                    .last("LIMIT 1")
            );
        }

        if (method == null) {
            throw new BusinessException("暂无可用配送方式");
        }
        if (method.getFreight() == null) {
            method.setFreight(BigDecimal.ZERO);
        }
        if (method.getFreeThreshold() == null) {
            method.setFreeThreshold(BigDecimal.ZERO);
        }
        return method;
    }

    private BigDecimal calculateFreightAmount(BigDecimal totalAmount, DeliveryMethod deliveryMethod) {
        BigDecimal freight = deliveryMethod.getFreight() == null ? BigDecimal.ZERO : deliveryMethod.getFreight();
        BigDecimal freeThreshold = deliveryMethod.getFreeThreshold() == null ? BigDecimal.ZERO : deliveryMethod.getFreeThreshold();
        BigDecimal safeTotalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (freeThreshold.compareTo(BigDecimal.ZERO) > 0 && safeTotalAmount.compareTo(freeThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return freight;
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = BeanUtil.copyProperties(order, OrderVO.class);
        vo.setOrderStatusText(TradeConstants.resolveOrderStatusText(order.getOrderStatus()));
        return vo;
    }

    private void releaseOrderLockedStock(Long orderId, String remark, Long operatorId) {
        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        for (OrderItem item : items) {
            try {
                tradeInventoryService.releaseLockedStock(
                    item.getProductId(), item.getSkuId(), item.getQuantity(), remark, operatorId);
            } catch (Exception e) {
                log.warn("释放锁定库存失败: orderItemId={}, reason={}", item.getId(), e.getMessage());
            }
        }
    }

    private Map<Long, List<OrderItemVO>> getOrderItemVOMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
        );

        return items.stream().collect(Collectors.groupingBy(
            OrderItem::getOrderId,
            Collectors.mapping(item -> BeanUtil.copyProperties(item, OrderItemVO.class), Collectors.toList())
        ));
    }
}
