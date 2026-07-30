package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.TradeConstants;
import com.coffee.entity.Order;
import com.coffee.entity.OrderAfterSale;
import com.coffee.entity.OrderAfterSaleLog;
import com.coffee.entity.OrderItem;
import com.coffee.mapper.OrderAfterSaleMapper;
import com.coffee.mapper.OrderAfterSaleLogMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.vo.AfterSaleVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfterSaleServiceImplTest {

    @Mock
    private OrderAfterSaleMapper orderAfterSaleMapper;

    @Mock
    private OrderAfterSaleLogMapper orderAfterSaleLogMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private AfterSaleServiceImpl afterSaleService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(OrderAfterSale.class);
        initTableInfo(OrderAfterSaleLog.class);
        initTableInfo(Order.class);
        initTableInfo(OrderItem.class);
    }

    @Test
    void getUserAfterSaleDetailRejectsOtherUsersRecord() {
        OrderAfterSale afterSale = afterSale(1L, 10L, 200L);

        when(orderAfterSaleMapper.selectById(1L)).thenReturn(afterSale);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> afterSaleService.getUserAfterSaleDetail(100L, 1L));

        assertEquals("售后记录不存在", exception.getMessage());
        verify(orderMapper, never()).selectList(anyOrderQueryWrapper());
        verify(orderItemMapper, never()).selectList(anyOrderItemQueryWrapper());
    }

    @Test
    void getUserAfterSaleDetailBuildsOrderAndItemSnapshot() {
        OrderAfterSale afterSale = afterSale(1L, 10L, 100L);
        Order order = order(10L, "ORD100", 100L);
        OrderItem item = orderItem(10L, 20L, "拿铁", "中杯 / 少糖");
        OrderAfterSaleLog log = afterSaleLog(1L, afterSale.getId());

        when(orderAfterSaleMapper.selectById(1L)).thenReturn(afterSale);
        when(orderMapper.selectList(anyOrderQueryWrapper())).thenReturn(List.of(order));
        when(orderItemMapper.selectList(anyOrderItemQueryWrapper())).thenReturn(List.of(item));
        when(orderAfterSaleLogMapper.selectList(anyAfterSaleLogQueryWrapper())).thenReturn(List.of(log));

        AfterSaleVO result = afterSaleService.getUserAfterSaleDetail(100L, 1L);

        assertEquals("AS100", result.getAfterSaleNo());
        assertEquals("ORD100", result.getOrderNo());
        assertEquals("仅退款", result.getTypeText());
        assertEquals("待处理", result.getStatusText());
        assertEquals("退款中", result.getOrderStatusText());
        assertEquals(50L, result.getReviewerId());
        assertEquals("审核通过", result.getReviewRemark());
        assertEquals(1, result.getItems().size());
        assertEquals("拿铁", result.getItems().get(0).getProductName());
        assertEquals("中杯 / 少糖", result.getItems().get(0).getSpecInfo());
        assertEquals(1, result.getLogs().size());
        assertEquals("提交申请", result.getLogs().get(0).getActionText());
        assertEquals("用户", result.getLogs().get(0).getOperatorTypeText());
        assertEquals("待处理", result.getLogs().get(0).getStatusToText());
    }

    private OrderAfterSale afterSale(Long id, Long orderId, Long userId) {
        OrderAfterSale afterSale = new OrderAfterSale();
        afterSale.setId(id);
        afterSale.setOrderId(orderId);
        afterSale.setUserId(userId);
        afterSale.setAfterSaleNo("AS100");
        afterSale.setType(TradeConstants.AFTER_SALE_TYPE_REFUND);
        afterSale.setReason("不想喝了");
        afterSale.setRefundAmount(new BigDecimal("32.00"));
        afterSale.setStatus(TradeConstants.AFTER_SALE_STATUS_PENDING);
        afterSale.setReviewerId(50L);
        afterSale.setHandleRemark("审核通过");
        return afterSale;
    }

    private Order order(Long id, String orderNo, Long userId) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("32.00"));
        order.setPayAmount(new BigDecimal("32.00"));
        order.setOrderStatus(TradeConstants.ORDER_STATUS_REFUNDING);
        order.setPayStatus(TradeConstants.PAY_STATUS_SUCCESS);
        return order;
    }

    private OrderItem orderItem(Long orderId, Long productId, String productName, String specInfo) {
        OrderItem item = new OrderItem();
        item.setId(100L);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setSpecInfo(specInfo);
        item.setPrice(new BigDecimal("32.00"));
        item.setQuantity(1);
        item.setTotalAmount(new BigDecimal("32.00"));
        return item;
    }

    private OrderAfterSaleLog afterSaleLog(Long id, Long afterSaleId) {
        OrderAfterSaleLog log = new OrderAfterSaleLog();
        log.setId(id);
        log.setAfterSaleId(afterSaleId);
        log.setAction(TradeConstants.AFTER_SALE_LOG_ACTION_APPLY);
        log.setOperatorType(TradeConstants.AFTER_SALE_OPERATOR_USER);
        log.setStatusTo(TradeConstants.AFTER_SALE_STATUS_PENDING);
        log.setRemark("用户提交退款申请");
        return log;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Order> anyOrderQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<OrderItem> anyOrderItemQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<OrderAfterSaleLog> anyAfterSaleLogQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
