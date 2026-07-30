package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.entity.Invoice;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.Promotion;
import com.coffee.entity.PromotionProduct;
import com.coffee.entity.Refund;
import com.coffee.mapper.InvoiceMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.PromotionMapper;
import com.coffee.mapper.PromotionProductMapper;
import com.coffee.mapper.RefundMapper;
import com.coffee.vo.FinancialReportVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private InvoiceMapper invoiceMapper;
    @Mock private RefundMapper refundMapper;
    @Mock private PromotionProductMapper promotionProductMapper;
    @Mock private PromotionMapper promotionMapper;

    private FinancialServiceImpl financialService;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisConfiguration cfg = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
        initTableInfo(assistant, Order.class);
        initTableInfo(assistant, OrderItem.class);
        initTableInfo(assistant, Invoice.class);
        initTableInfo(assistant, Refund.class);
        initTableInfo(assistant, Promotion.class);
        initTableInfo(assistant, PromotionProduct.class);
    }

    private static void initTableInfo(MapperBuilderAssistant assistant, Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }

    @BeforeEach
    void setUp() {
        financialService = new FinancialServiceImpl(
            orderMapper,
            orderItemMapper,
            invoiceMapper,
            refundMapper,
            promotionProductMapper,
            promotionMapper
        );
    }

    @Test
    void generateFinancialReportUsesRefundRecordsAndSplitsPromotionDiscount() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);
        LocalDateTime payTime = start.atTime(10, 0);

        Order paidOrder = order(101L, 1L, "100.00", "80.00", "20.00", 1, payTime, 1);
        Order refundedOrder = order(102L, 2L, "40.00", "30.00", "5.00", 3, payTime.plusHours(1), 2);
        List<Order> orders = List.of(paidOrder, refundedOrder);

        OrderItem promotionItem = orderItem(101L, 11L, "Latte", "50.00", 2, "100.00");
        OrderItem normalItem = orderItem(102L, 12L, "Mocha", "40.00", 1, "40.00");
        List<OrderItem> items = List.of(promotionItem, normalItem);

        Refund successRefund = new Refund();
        successRefund.setRefundAmount(new BigDecimal("30.00"));
        successRefund.setRefundStatus(1);
        successRefund.setRefundTime(start.atTime(12, 0));

        PromotionProduct promotionProduct = new PromotionProduct();
        promotionProduct.setPromotionId(201L);
        promotionProduct.setProductId(11L);
        promotionProduct.setPromotionPrice(new BigDecimal("45.00"));

        Promotion promotion = new Promotion();
        promotion.setId(201L);
        promotion.setStatus(1);
        promotion.setStartTime(start.minusDays(1).atStartOfDay());
        promotion.setEndTime(start.plusDays(1).atStartOfDay());

        when(orderMapper.selectList(any())).thenReturn(orders);
        when(refundMapper.selectList(any())).thenReturn(List.of(successRefund));
        when(orderItemMapper.selectList(any())).thenReturn(items);
        when(promotionProductMapper.selectList(any())).thenReturn(List.of(promotionProduct), List.of());
        when(promotionMapper.selectById(201L)).thenReturn(promotion);

        FinancialReportVO report = financialService.generateFinancialReport(start, end);

        assertEquals(new BigDecimal("110.00"), report.getTotalRevenue());
        assertEquals(2, report.getTotalOrders());
        assertEquals(new BigDecimal("55.00"), report.getAverageOrderAmount());
        assertEquals(new BigDecimal("30.00"), report.getTotalRefund());
        assertEquals(new BigDecimal("80.00"), report.getNetRevenue());
        assertEquals(new BigDecimal("10.00"), report.getPromotionDeductAmount());
        assertEquals(new BigDecimal("15.00"), report.getCouponDeductAmount());
        assertEquals(new BigDecimal("80.00"), report.getPaymentMethodDistribution().get("ALIPAY"));
        assertEquals(new BigDecimal("30.00"), report.getPaymentMethodDistribution().get("WECHAT"));
    }

    private Order order(
        Long id,
        Long userId,
        String totalAmount,
        String payAmount,
        String discountAmount,
        Integer payStatus,
        LocalDateTime payTime,
        Integer payType
    ) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal(totalAmount));
        order.setPayAmount(new BigDecimal(payAmount));
        order.setDiscountAmount(new BigDecimal(discountAmount));
        order.setPayStatus(payStatus);
        order.setPayTime(payTime);
        order.setPayType(payType);
        return order;
    }

    private OrderItem orderItem(
        Long orderId,
        Long productId,
        String productName,
        String price,
        Integer quantity,
        String totalAmount
    ) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        item.setTotalAmount(new BigDecimal(totalAmount));
        return item;
    }
}
