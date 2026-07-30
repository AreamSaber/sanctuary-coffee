package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
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
import com.coffee.service.FinancialService;
import com.coffee.vo.FinancialReportVO;
import com.coffee.vo.FinancialReportVO.DailyRevenueVO;
import com.coffee.vo.FinancialReportVO.ProductSalesRankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Financial service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialServiceImpl implements FinancialService {

    private static final int PAY_STATUS_PAID = 1;
    private static final int PAY_STATUS_REFUNDED = 3;
    private static final int REFUND_STATUS_SUCCESS = 1;
    private static final int INVOICE_TYPE_NORMAL = 1;
    private static final int TITLE_TYPE_PERSONAL = 1;
    private static final int TITLE_TYPE_COMPANY = 2;
    private static final int INVOICE_STATUS_PENDING = 1;
    private static final int INVOICE_STATUS_ISSUED = 2;
    private static final int INVOICE_STATUS_VOID = 3;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final InvoiceMapper invoiceMapper;
    private final RefundMapper refundMapper;
    private final PromotionProductMapper promotionProductMapper;
    private final PromotionMapper promotionMapper;

    @Override
    public FinancialReportVO generateFinancialReport(LocalDate startDate, LocalDate endDate) {
        FinancialReportVO report = new FinancialReportVO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        List<Order> paidOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .ge(Order::getPayTime, startTime)
                .lt(Order::getPayTime, endExclusive)
                .in(Order::getPayStatus, PAY_STATUS_PAID, PAY_STATUS_REFUNDED)
                .isNotNull(Order::getPayTime)
        );

        BigDecimal totalRevenue = paidOrders.stream()
            .map(order -> defaultAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalRevenue(totalRevenue);
        report.setTotalOrders(paidOrders.size());

        if (paidOrders.isEmpty()) {
            report.setAverageOrderAmount(BigDecimal.ZERO);
        } else {
            report.setAverageOrderAmount(
                totalRevenue.divide(BigDecimal.valueOf(paidOrders.size()), 2, RoundingMode.HALF_UP)
            );
        }

        List<Refund> successfulRefunds = refundMapper.selectList(
            new LambdaQueryWrapper<Refund>()
                .eq(Refund::getRefundStatus, REFUND_STATUS_SUCCESS)
                .ge(Refund::getRefundTime, startTime)
                .lt(Refund::getRefundTime, endExclusive)
        );
        BigDecimal totalRefund = successfulRefunds.stream()
            .map(refund -> defaultAmount(refund.getRefundAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalRefund(totalRefund);
        report.setNetRevenue(totalRevenue.subtract(totalRefund));

        report.setDailyRevenueTrend(calculateDailyRevenueTrend(paidOrders, startDate, endDate));
        report.setPaymentMethodDistribution(calculatePaymentDistribution(paidOrders));
        report.setHourlyDistribution(calculateHourlyDistribution(paidOrders));

        BigDecimal memberRevenue = paidOrders.stream()
            .filter(order -> order.getUserId() != null)
            .map(order -> defaultAmount(order.getPayAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            report.setMemberRevenueRatio(
                memberRevenue.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            );
        } else {
            report.setMemberRevenueRatio(BigDecimal.ZERO);
        }

        List<OrderItem> paidOrderItems = loadOrderItems(paidOrders);
        DiscountBreakdown discountBreakdown = calculateDiscountBreakdown(paidOrders, paidOrderItems);
        report.setCouponDeductAmount(discountBreakdown.nonPromotionDiscount());
        report.setPromotionDeductAmount(discountBreakdown.promotionDiscount());
        report.setProductSalesRank(calculateProductSalesRank(paidOrderItems, totalRevenue));
        return report;
    }

    @Override
    public FinancialReportVO getDailyReport(LocalDate date) {
        return generateFinancialReport(date, date);
    }

    @Override
    public FinancialReportVO getMonthlyReport(Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return generateFinancialReport(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    @Override
    public FinancialReportVO getYearlyReport(Integer year) {
        return generateFinancialReport(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyInvoice(Long userId, Long orderId, Map<String, Object> invoiceData) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_EXIST);
        }
        if (!Integer.valueOf(PAY_STATUS_PAID).equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅已支付订单可申请发票");
        }

        Long existingCount = invoiceMapper.selectCount(
            new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getOrderId, orderId)
        );
        if (existingCount > 0) {
            throw new BusinessException(ResultCode.INVOICE_ALREADY_EXIST);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setOrderId(orderId);
        invoice.setUserId(userId);
        invoice.setInvoiceType(resolveInvoiceType(invoiceData));
        invoice.setTitleType(resolveTitleType(invoiceData));
        invoice.setTitle(resolveInvoiceTitle(invoiceData));
        invoice.setTaxNo(resolveTaxNo(invoiceData));
        invoice.setAmount(defaultAmount(order.getPayAmount()));
        invoice.setStatus(INVOICE_STATUS_PENDING);
        invoiceMapper.insert(invoice);

        log.info("Invoice applied: orderId={}, invoiceNo={}", orderId, invoice.getInvoiceNo());
    }

    @Override
    public IPage<Invoice> getInvoicePage(Long userId, Integer pageNum, Integer pageSize, String status) {
        Page<Invoice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(Invoice::getUserId, userId);
        }

        Integer statusCode = parseInvoiceStatus(status);
        if (statusCode != null) {
            wrapper.eq(Invoice::getStatus, statusCode);
        }

        wrapper.orderByDesc(Invoice::getCreateTime);
        IPage<Invoice> result = invoiceMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::normalizeInvoice);
        return result;
    }

    @Override
    public Invoice getInvoiceDetail(Long userId, boolean admin, Long invoiceId) {
        Invoice invoice = requireAccessibleInvoice(userId, admin, invoiceId);
        normalizeInvoice(invoice);
        return invoice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueInvoice(Long invoiceId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BusinessException(ResultCode.INVOICE_NOT_EXIST);
        }
        if (Integer.valueOf(INVOICE_STATUS_ISSUED).equals(invoice.getStatus())) {
            return;
        }
        if (!Integer.valueOf(INVOICE_STATUS_PENDING).equals(invoice.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不可开票");
        }

        invoice.setStatus(INVOICE_STATUS_ISSUED);
        invoice.setIssueTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        log.info("Invoice issued: invoiceId={}", invoiceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resendInvoice(Long userId, boolean admin, Long invoiceId) {
        Invoice invoice = requireAccessibleInvoice(userId, admin, invoiceId);
        if (!Integer.valueOf(INVOICE_STATUS_ISSUED).equals(invoice.getStatus())) {
            throw new BusinessException(ResultCode.INVOICE_NOT_ISSUED);
        }

        invoice.setIssueTime(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        log.info("Invoice resend recorded: invoiceId={}", invoiceId);
    }

    @Override
    public byte[] exportFinancialReport(LocalDate startDate, LocalDate endDate, String format) {
        FinancialReportVO report = generateFinancialReport(startDate, endDate);

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("start_date,end_date,total_revenue,total_orders,average_order_amount,total_refund,net_revenue,member_ratio,non_promotion_discount,promotion_discount\n");
        csv.append(csvValue(report.getStartDate()))
            .append(',').append(csvValue(report.getEndDate()))
            .append(',').append(csvValue(report.getTotalRevenue()))
            .append(',').append(csvValue(report.getTotalOrders()))
            .append(',').append(csvValue(report.getAverageOrderAmount()))
            .append(',').append(csvValue(report.getTotalRefund()))
            .append(',').append(csvValue(report.getNetRevenue()))
            .append(',').append(csvValue(report.getMemberRevenueRatio()))
            .append(',').append(csvValue(report.getCouponDeductAmount()))
            .append(',').append(csvValue(report.getPromotionDeductAmount()))
            .append('\n');

        csv.append('\n').append("daily_revenue_trend\n");
        csv.append("date,revenue,order_count\n");
        for (DailyRevenueVO dailyRevenue : report.getDailyRevenueTrend()) {
            csv.append(csvValue(dailyRevenue.getDate()))
                .append(',').append(csvValue(dailyRevenue.getRevenue()))
                .append(',').append(csvValue(dailyRevenue.getOrderCount()))
                .append('\n');
        }

        csv.append('\n').append("payment_method_distribution\n");
        csv.append("payment_method,amount\n");
        report.getPaymentMethodDistribution().forEach((method, amount) ->
            csv.append(csvValue(method))
                .append(',').append(csvValue(amount))
                .append('\n')
        );

        csv.append('\n').append("product_sales_rank\n");
        csv.append("product_id,product_name,sales_count,sales_amount,percentage\n");
        for (ProductSalesRankVO rank : report.getProductSalesRank()) {
            csv.append(csvValue(rank.getProductId()))
                .append(',').append(csvValue(rank.getProductName()))
                .append(',').append(csvValue(rank.getSalesCount()))
                .append(',').append(csvValue(rank.getSalesAmount()))
                .append(',').append(csvValue(rank.getPercentage()))
                .append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<DailyRevenueVO> calculateDailyRevenueTrend(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Order>> dailyOrders = orders.stream()
            .filter(order -> order.getPayTime() != null)
            .collect(Collectors.groupingBy(order -> order.getPayTime().toLocalDate()));

        List<DailyRevenueVO> trend = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            DailyRevenueVO daily = new DailyRevenueVO();
            daily.setDate(current);

            List<Order> dayOrders = dailyOrders.getOrDefault(current, List.of());
            BigDecimal revenue = dayOrders.stream()
                .map(order -> defaultAmount(order.getPayAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            daily.setRevenue(revenue);
            daily.setOrderCount(dayOrders.size());
            trend.add(daily);
            current = current.plusDays(1);
        }
        return trend;
    }

    private Map<String, BigDecimal> calculatePaymentDistribution(List<Order> orders) {
        return orders.stream()
            .collect(Collectors.groupingBy(
                this::resolvePaymentMethod,
                Collectors.reducing(BigDecimal.ZERO, order -> defaultAmount(order.getPayAmount()), BigDecimal::add)
            ));
    }

    private Map<Integer, BigDecimal> calculateHourlyDistribution(List<Order> orders) {
        Map<Integer, BigDecimal> distribution = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            distribution.put(i, BigDecimal.ZERO);
        }

        orders.forEach(order -> {
            if (order.getPayTime() != null) {
                int hour = order.getPayTime().getHour();
                distribution.put(hour, distribution.get(hour).add(defaultAmount(order.getPayAmount())));
            }
        });
        return distribution;
    }

    private List<OrderItem> loadOrderItems(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream()
            .map(Order::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return List.of();
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
        );
        return orderItems == null ? List.of() : orderItems;
    }

    private List<ProductSalesRankVO> calculateProductSalesRank(List<OrderItem> orderItems, BigDecimal totalRevenue) {
        if (orderItems == null || orderItems.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<OrderItem>> productGroups = orderItems.stream()
            .filter(item -> item.getProductId() != null)
            .collect(Collectors.groupingBy(OrderItem::getProductId));

        List<ProductSalesRankVO> rankList = new ArrayList<>();
        productGroups.forEach((productId, items) -> {
            ProductSalesRankVO rank = new ProductSalesRankVO();
            rank.setProductId(productId);
            rank.setProductName(items.get(0).getProductName());
            rank.setSalesCount(items.stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum());

            BigDecimal totalAmount = items.stream()
                .map(item -> defaultAmount(item.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            rank.setSalesAmount(totalAmount);

            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                rank.setPercentage(
                    totalAmount.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                );
            } else {
                rank.setPercentage(BigDecimal.ZERO);
            }
            rankList.add(rank);
        });

        return rankList.stream()
            .sorted((left, right) -> right.getSalesAmount().compareTo(left.getSalesAmount()))
            .limit(10)
            .collect(Collectors.toList());
    }

    private DiscountBreakdown calculateDiscountBreakdown(List<Order> orders, List<OrderItem> orderItems) {
        if (orders == null || orders.isEmpty()) {
            return new DiscountBreakdown(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<OrderItem> safeOrderItems = orderItems == null ? List.of() : orderItems;
        Map<Long, List<OrderItem>> itemMap = safeOrderItems.stream()
            .filter(item -> item.getOrderId() != null)
            .collect(Collectors.groupingBy(OrderItem::getOrderId));

        BigDecimal promotionDiscount = BigDecimal.ZERO;
        BigDecimal nonPromotionDiscount = BigDecimal.ZERO;
        for (Order order : orders) {
            BigDecimal totalDiscount = defaultAmount(order.getDiscountAmount());
            BigDecimal orderPromotionDiscount = calculateOrderPromotionDiscount(
                itemMap.getOrDefault(order.getId(), List.of()),
                order.getPayTime()
            );
            if (orderPromotionDiscount.compareTo(totalDiscount) > 0) {
                orderPromotionDiscount = totalDiscount;
            }
            promotionDiscount = promotionDiscount.add(orderPromotionDiscount);
            nonPromotionDiscount = nonPromotionDiscount.add(totalDiscount.subtract(orderPromotionDiscount));
        }
        return new DiscountBreakdown(
            promotionDiscount.setScale(2, RoundingMode.HALF_UP),
            nonPromotionDiscount.setScale(2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal calculateOrderPromotionDiscount(List<OrderItem> orderItems, LocalDateTime payTime) {
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            totalDiscount = totalDiscount.add(calculateItemPromotionDiscount(item, payTime));
        }
        return totalDiscount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateItemPromotionDiscount(OrderItem item, LocalDateTime payTime) {
        if (item == null || item.getProductId() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal originUnitPrice = defaultAmount(item.getPrice());
        PromotionProduct relation = findBestPromotionProduct(item.getProductId(), originUnitPrice, payTime);
        if (relation == null || relation.getPromotionPrice() == null) {
            return BigDecimal.ZERO;
        }

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

    private PromotionProduct findBestPromotionProduct(Long productId, BigDecimal originUnitPrice, LocalDateTime payTime) {
        List<PromotionProduct> relations = promotionProductMapper.selectList(
            new LambdaQueryWrapper<PromotionProduct>()
                .eq(PromotionProduct::getProductId, productId)
        );
        if (relations == null || relations.isEmpty()) {
            return null;
        }

        LocalDateTime effectiveTime = payTime == null ? LocalDateTime.now() : payTime;
        PromotionProduct bestRelation = null;
        BigDecimal bestPrice = originUnitPrice == null ? BigDecimal.ZERO : originUnitPrice;
        for (PromotionProduct relation : relations) {
            Promotion promotion = promotionMapper.selectById(relation.getPromotionId());
            if (!isActivePromotionAt(promotion, effectiveTime)) {
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

    private boolean isActivePromotionAt(Promotion promotion, LocalDateTime time) {
        if (promotion == null || !Integer.valueOf(1).equals(promotion.getStatus())) {
            return false;
        }
        if (promotion.getStartTime() != null && promotion.getStartTime().isAfter(time)) {
            return false;
        }
        return promotion.getEndTime() == null || !promotion.getEndTime().isBefore(time);
    }

    private void normalizeInvoice(Invoice invoice) {
        invoice.setType(invoice.getTitleType() != null && invoice.getTitleType() == TITLE_TYPE_COMPANY ? "COMPANY" : "PERSONAL");
        invoice.setTaxNumber(invoice.getTaxNo());
        invoice.setStatusText(toInvoiceStatusText(invoice.getStatus()));
        invoice.setTaxAmount(calculateTaxAmount(defaultAmount(invoice.getAmount())));
        invoice.setContent("Catering service");
    }

    private Integer resolveInvoiceType(Map<String, Object> invoiceData) {
        Object rawValue = invoiceData.get("invoiceType");
        if (rawValue == null) {
            return INVOICE_TYPE_NORMAL;
        }
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        String value = String.valueOf(rawValue).trim().toUpperCase();
        return switch (value) {
            case "1", "NORMAL", "STANDARD" -> INVOICE_TYPE_NORMAL;
            case "2", "VAT" -> 2;
            default -> INVOICE_TYPE_NORMAL;
        };
    }

    private Integer resolveTitleType(Map<String, Object> invoiceData) {
        Object rawType = invoiceData.get("titleType");
        if (rawType instanceof Number number) {
            return number.intValue();
        }

        Object rawValue = invoiceData.get("type");
        if (rawValue == null) {
            return TITLE_TYPE_PERSONAL;
        }

        String value = String.valueOf(rawValue).trim().toUpperCase();
        return switch (value) {
            case "2", "COMPANY", "ENTERPRISE" -> TITLE_TYPE_COMPANY;
            default -> TITLE_TYPE_PERSONAL;
        };
    }

    private String resolveInvoiceTitle(Map<String, Object> invoiceData) {
        Object rawTitle = invoiceData.get("title");
        if (rawTitle == null || String.valueOf(rawTitle).isBlank()) {
            return "Personal Invoice";
        }
        return String.valueOf(rawTitle).trim();
    }

    private String resolveTaxNo(Map<String, Object> invoiceData) {
        Object taxNo = invoiceData.get("taxNo");
        if (taxNo != null && !String.valueOf(taxNo).isBlank()) {
            return String.valueOf(taxNo).trim();
        }
        Object taxNumber = invoiceData.get("taxNumber");
        if (taxNumber != null && !String.valueOf(taxNumber).isBlank()) {
            return String.valueOf(taxNumber).trim();
        }
        return null;
    }

    private Integer parseInvoiceStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }

        String value = rawStatus.trim().toUpperCase();
        return switch (value) {
            case "1", "PENDING" -> INVOICE_STATUS_PENDING;
            case "2", "ISSUED", "SENT" -> INVOICE_STATUS_ISSUED;
            case "3", "VOID", "FAILED" -> INVOICE_STATUS_VOID;
            default -> null;
        };
    }

    private String toInvoiceStatusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case INVOICE_STATUS_PENDING -> "PENDING";
            case INVOICE_STATUS_ISSUED -> "ISSUED";
            case INVOICE_STATUS_VOID -> "VOID";
            default -> "UNKNOWN";
        };
    }

    private Invoice requireAccessibleInvoice(Long userId, boolean admin, Long invoiceId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null) {
            throw new BusinessException(ResultCode.INVOICE_NOT_EXIST);
        }
        if (!admin && (userId == null || !userId.equals(invoice.getUserId()))) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return invoice;
    }

    private String resolvePaymentMethod(Order order) {
        if (order.getPaymentMethod() != null && !order.getPaymentMethod().isBlank()) {
            return order.getPaymentMethod();
        }
        if (order.getPayType() == null) {
            return "UNKNOWN";
        }
        return switch (order.getPayType()) {
            case 1 -> "ALIPAY";
            case 2 -> "WECHAT";
            case 3 -> "BALANCE";
            case 4 -> "MOCK";
            default -> "UNKNOWN";
        };
    }

    private String generateInvoiceNo() {
        return "INV" + System.currentTimeMillis();
    }

    private BigDecimal calculateTaxAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(0.06)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private record DiscountBreakdown(BigDecimal promotionDiscount, BigDecimal nonPromotionDiscount) {
    }
}
