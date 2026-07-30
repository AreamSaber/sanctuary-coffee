package com.coffee.service.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.entity.*;
import com.coffee.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 每日运营数据统计定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatisticsTask {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final SalesStatisticsDailyMapper salesStatsMapper;
    private final ProductSalesStatisticsMapper productStatsMapper;
    private final UserActiveStatisticsMapper userStatsMapper;
    private final OperationSummaryMapper operationSummaryMapper;

    @Scheduled(cron = "0 5 0 * * ?")
    public void generateDailyStatistics() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始生成 {} 的每日运营统计数据", yesterday);

        try {
            generateStatisticsForDate(yesterday);
            log.info("{} 每日运营统计数据生成完成", yesterday);
        } catch (Exception e) {
            log.error("生成每日运营统计数据失败: {}", e.getMessage(), e);
        }
    }

    void generateStatisticsForDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<Order> dayOrders = selectDayOrders(start, end);
        List<UserBehavior> dayBehaviors = selectDayBehaviors(start, end);

        generateSalesStatistics(date, start, end, dayOrders, dayBehaviors);
        generateProductSalesStatistics(date, dayOrders, dayBehaviors);
        generateUserActiveStatistics(date, dayOrders, dayBehaviors);
        generateOperationSummary(date, start, end, dayOrders, dayBehaviors);
    }

    private List<Order> selectDayOrders(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .between(Order::getCreateTime, start, end)
        );
    }

    private List<UserBehavior> selectDayBehaviors(LocalDateTime start, LocalDateTime end) {
        return behaviorMapper.selectList(
            new LambdaQueryWrapper<UserBehavior>()
                .between(UserBehavior::getCreateTime, start, end)
        );
    }

    private void generateSalesStatistics(
        LocalDate date,
        LocalDateTime start,
        LocalDateTime end,
        List<Order> orders,
        List<UserBehavior> behaviors
    ) {
        int orderCount = orders.size();
        BigDecimal orderAmount = orders.stream()
            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long payCount = orders.stream().filter(o -> o.getPayTime() != null).count();
        BigDecimal payAmount = orders.stream()
            .filter(o -> o.getPayTime() != null)
            .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Refund> refunds = refundMapper.selectList(
            new LambdaQueryWrapper<Refund>()
                .between(Refund::getCreateTime, start, end)
                .eq(Refund::getRefundStatus, 1)
        );
        int refundCount = refunds.size();
        BigDecimal refundAmount = refunds.stream()
            .map(r -> r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long newUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .between(User::getCreateTime, start, end)
        );

        long activeUsers = collectActiveUserIds(orders, behaviors).size();

        SalesStatisticsDaily stats = new SalesStatisticsDaily();
        stats.setStatDate(date);
        stats.setOrderCount(orderCount);
        stats.setOrderAmount(orderAmount);
        stats.setPayCount((int) payCount);
        stats.setPayAmount(payAmount);
        stats.setRefundCount(refundCount);
        stats.setRefundAmount(refundAmount);
        stats.setNewUserCount((int) newUsers);
        stats.setActiveUserCount((int) activeUsers);
        upsertSalesStatistics(stats);
    }

    private void generateProductSalesStatistics(LocalDate date, List<Order> dayOrders, List<UserBehavior> dayBehaviors) {
        List<Long> orderIds = dayOrders.stream()
            .map(Order::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        List<OrderItem> items = orderIds.isEmpty()
            ? List.of()
            : orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds)
            );

        Map<Long, List<OrderItem>> productItems = items.stream()
            .filter(item -> item.getProductId() != null)
            .collect(Collectors.groupingBy(OrderItem::getProductId));
        Map<Long, List<UserBehavior>> productBehaviors = dayBehaviors.stream()
            .filter(this::isProductBehavior)
            .collect(Collectors.groupingBy(UserBehavior::getTargetId));

        Set<Long> productIds = new TreeSet<>();
        productIds.addAll(productItems.keySet());
        productIds.addAll(productBehaviors.keySet());

        for (Long productId : productIds) {
            List<OrderItem> productOrderItems = productItems.getOrDefault(productId, List.of());
            List<UserBehavior> behaviors = productBehaviors.getOrDefault(productId, List.of());
            int salesCount = productOrderItems.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
            BigDecimal salesAmount = productOrderItems.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            ProductSalesStatistics stats = new ProductSalesStatistics();
            stats.setProductId(productId);
            stats.setStatDate(date);
            stats.setSalesCount(salesCount);
            stats.setSalesAmount(salesAmount);
            stats.setViewCount(countAction(behaviors, "VIEW"));
            stats.setCollectCount(countAction(behaviors, "COLLECT") + countAction(behaviors, "FAVORITE"));
            stats.setCartCount(countAction(behaviors, "ADD_CART"));
            upsertProductStatistics(stats);
        }
    }

    private void generateUserActiveStatistics(LocalDate date, List<Order> dayOrders, List<UserBehavior> behaviors) {
        Map<Long, List<Order>> userOrders = dayOrders.stream()
            .filter(order -> order.getUserId() != null)
            .collect(Collectors.groupingBy(Order::getUserId));

        Map<Long, List<UserBehavior>> userBehaviors = behaviors.stream()
            .filter(b -> b.getUserId() != null)
            .collect(Collectors.groupingBy(UserBehavior::getUserId));

        Set<Long> allUserIds = new TreeSet<>();
        allUserIds.addAll(userOrders.keySet());
        allUserIds.addAll(userBehaviors.keySet());

        for (Long userId : allUserIds) {
            List<Order> orders = userOrders.getOrDefault(userId, List.of());
            List<UserBehavior> userActs = userBehaviors.getOrDefault(userId, List.of());

            int loginCount = countAction(userActs, "LOGIN");
            int browseCount = countAction(userActs, "VIEW");
            long orderCount = orders.size();
            long payCount = orders.stream().filter(o -> o.getPayTime() != null).count();
            int onlineTime = userActs.stream().mapToInt(b -> b.getDuration() != null ? b.getDuration() : 0).sum();

            UserActiveStatistics stats = new UserActiveStatistics();
            stats.setUserId(userId);
            stats.setStatDate(date);
            stats.setLoginCount(loginCount);
            stats.setBrowseCount(browseCount);
            stats.setOrderCount((int) orderCount);
            stats.setPayCount((int) payCount);
            stats.setOnlineTime(onlineTime);
            upsertUserActiveStatistics(stats);
        }
    }

    private void generateOperationSummary(
        LocalDate date,
        LocalDateTime start,
        LocalDateTime end,
        List<Order> dayOrders,
        List<UserBehavior> behaviors
    ) {
        long totalUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>().le(User::getCreateTime, end)
        );
        long newUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>().between(User::getCreateTime, start, end)
        );

        int totalOrders = dayOrders.size();
        BigDecimal totalAmount = dayOrders.stream()
            .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeUsers = collectActiveUserIds(dayOrders, behaviors).size();

        BigDecimal avgOrderAmount = totalOrders > 0
            ? totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // repurchase rate: users with >=2 completed orders / total users with orders
        List<Order> completedOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderStatus, 4)
        );
        Map<Long, Long> userOrderCount = completedOrders.stream()
            .filter(order -> order.getUserId() != null)
            .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));
        long usersWithOrders = userOrderCount.size();
        long repurchaseUsers = userOrderCount.values().stream().filter(c -> c >= 2).count();
        BigDecimal repurchaseRate = usersWithOrders > 0
            ? BigDecimal.valueOf(repurchaseUsers).divide(BigDecimal.valueOf(usersWithOrders), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long productCount = productMapper.selectCount(
            new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        );
        long categoryCount = categoryMapper.selectCount(
            new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getStatus, 1)
        );

        OperationSummary summary = new OperationSummary();
        summary.setStatDate(date);
        summary.setTotalUser((int) totalUsers);
        summary.setNewUser((int) newUsers);
        summary.setActiveUser((int) activeUsers);
        summary.setTotalOrder(totalOrders);
        summary.setTotalAmount(totalAmount);
        summary.setAvgOrderAmount(avgOrderAmount);
        summary.setConversionRate(calculateConversionRate(dayOrders, behaviors));
        summary.setRepurchaseRate(repurchaseRate);
        summary.setProductCount((int) productCount);
        summary.setCategoryCount((int) categoryCount);
        upsertOperationSummary(summary);
    }

    private Set<Long> collectActiveUserIds(List<Order> orders, List<UserBehavior> behaviors) {
        Set<Long> userIds = new HashSet<>();
        orders.stream()
            .map(Order::getUserId)
            .filter(Objects::nonNull)
            .forEach(userIds::add);
        behaviors.stream()
            .map(UserBehavior::getUserId)
            .filter(Objects::nonNull)
            .forEach(userIds::add);
        return userIds;
    }

    private BigDecimal calculateConversionRate(List<Order> dayOrders, List<UserBehavior> behaviors) {
        Set<Long> visitors = behaviors.stream()
            .filter(behavior -> matchesAction(behavior, "VIEW"))
            .map(UserBehavior::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (visitors.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<Long> buyers = new HashSet<>();
        dayOrders.stream()
            .filter(order -> order.getPayTime() != null)
            .map(Order::getUserId)
            .filter(Objects::nonNull)
            .forEach(buyers::add);
        behaviors.stream()
            .filter(behavior -> matchesAction(behavior, "PAY"))
            .map(UserBehavior::getUserId)
            .filter(Objects::nonNull)
            .forEach(buyers::add);

        List<Long> convertedVisitors = new ArrayList<>(buyers);
        convertedVisitors.retainAll(visitors);
        return BigDecimal.valueOf(convertedVisitors.size())
            .divide(BigDecimal.valueOf(visitors.size()), 4, RoundingMode.HALF_UP);
    }

    private boolean isProductBehavior(UserBehavior behavior) {
        return behavior != null
            && behavior.getTargetId() != null
            && "PRODUCT".equalsIgnoreCase(behavior.getTargetType());
    }

    private int countAction(List<UserBehavior> behaviors, String actionType) {
        return (int) behaviors.stream()
            .filter(behavior -> matchesAction(behavior, actionType))
            .count();
    }

    private boolean matchesAction(UserBehavior behavior, String actionType) {
        return behavior != null
            && behavior.getActionType() != null
            && behavior.getActionType().equalsIgnoreCase(actionType);
    }

    private void upsertSalesStatistics(SalesStatisticsDaily stats) {
        SalesStatisticsDaily existing = salesStatsMapper.selectOne(
            new LambdaQueryWrapper<SalesStatisticsDaily>()
                .eq(SalesStatisticsDaily::getStatDate, stats.getStatDate())
                .last("LIMIT 1")
        );
        if (existing == null) {
            salesStatsMapper.insert(stats);
            return;
        }
        stats.setId(existing.getId());
        salesStatsMapper.updateById(stats);
    }

    private void upsertProductStatistics(ProductSalesStatistics stats) {
        ProductSalesStatistics existing = productStatsMapper.selectOne(
            new LambdaQueryWrapper<ProductSalesStatistics>()
                .eq(ProductSalesStatistics::getProductId, stats.getProductId())
                .eq(ProductSalesStatistics::getStatDate, stats.getStatDate())
                .last("LIMIT 1")
        );
        if (existing == null) {
            productStatsMapper.insert(stats);
            return;
        }
        stats.setId(existing.getId());
        productStatsMapper.updateById(stats);
    }

    private void upsertUserActiveStatistics(UserActiveStatistics stats) {
        UserActiveStatistics existing = userStatsMapper.selectOne(
            new LambdaQueryWrapper<UserActiveStatistics>()
                .eq(UserActiveStatistics::getUserId, stats.getUserId())
                .eq(UserActiveStatistics::getStatDate, stats.getStatDate())
                .last("LIMIT 1")
        );
        if (existing == null) {
            userStatsMapper.insert(stats);
            return;
        }
        stats.setId(existing.getId());
        userStatsMapper.updateById(stats);
    }

    private void upsertOperationSummary(OperationSummary summary) {
        OperationSummary existing = operationSummaryMapper.selectOne(
            new LambdaQueryWrapper<OperationSummary>()
                .eq(OperationSummary::getStatDate, summary.getStatDate())
                .last("LIMIT 1")
        );
        if (existing == null) {
            operationSummaryMapper.insert(summary);
            return;
        }
        summary.setId(existing.getId());
        operationSummaryMapper.updateById(summary);
    }
}
