package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.util.TradeConstants;
import com.coffee.entity.Order;
import com.coffee.entity.OrderItem;
import com.coffee.entity.Product;
import com.coffee.entity.User;
import com.coffee.entity.UserBehavior;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.UserBehaviorMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.service.AnalyticsService;
import com.coffee.vo.UserAnalyticsVO;
import com.coffee.vo.UserAnalyticsVO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为分析服务实现类
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    
    private final UserBehaviorMapper behaviorMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    
    @Override
    @Async
    public void recordUserBehavior(UserBehavior behavior) {
        try {
            normalizeBehavior(behavior);
            behaviorMapper.insert(behavior);
            log.debug("记录用户行为: userId={}, action={}, target={}", 
                behavior.getUserId(), behavior.getActionType(), behavior.getTargetId());
        } catch (Exception e) {
            log.error("记录用户行为失败", e);
        }
    }
    
    @Override
    @Async
    public void recordUserBehaviorBatch(List<UserBehavior> behaviors) {
        try {
            behaviors.forEach(behavior -> {
                normalizeBehavior(behavior);
                behaviorMapper.insert(behavior);
            });
            log.debug("批量记录用户行为: {} 条", behaviors.size());
        } catch (Exception e) {
            log.error("批量记录用户行为失败", e);
        }
    }
    
    @Override
    public UserAnalyticsVO getUserAnalytics(LocalDate startDate, LocalDate endDate) {
        UserAnalyticsVO analytics = new UserAnalyticsVO();
        analytics.setStartDate(startDate);
        analytics.setEndDate(endDate);
        
        // 时间范围
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        
        // 获取活跃用户统计
        analytics.setActiveUsers(calculateActiveUsers(startTime, endTime));
        
        // 获取行为分布
        analytics.setBehaviorDistribution(calculateBehaviorDistribution(startTime, endTime));
        
        // 获取页面访问统计
        analytics.setPageViews(calculatePageViews(startTime, endTime));
        
        // 获取商品偏好分析
        analytics.setProductPreferences(calculateProductPreferences(startTime, endTime));
        
        // 获取设备分布
        analytics.setDeviceDistribution(calculateDeviceDistribution(startTime, endTime));
        
        // 获取时段活跃度
        analytics.setHourlyActivity(calculateHourlyActivity(startTime, endTime));
        
        // 获取转化漏斗
        analytics.setConversionFunnel(calculateConversionFunnel(startTime, endTime));
        
        return analytics;
    }
    
    @Override
    public Map<String, Object> getRealTimeStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        
        // 今日活跃用户（去重统计）
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserBehavior::getCreateTime, todayStart)
               .select(UserBehavior::getUserId)
               .groupBy(UserBehavior::getUserId);
        List<UserBehavior> activeUserList = behaviorMapper.selectList(wrapper);
        Long activeUsers = (long) activeUserList.size();
        stats.put("todayActiveUsers", activeUsers);
        
        // 今日页面浏览量
        wrapper.clear();
        wrapper.ge(UserBehavior::getCreateTime, todayStart)
               .eq(UserBehavior::getActionType, "VIEW");
        Long pageViews = behaviorMapper.selectCount(wrapper);
        stats.put("todayPageViews", pageViews);
        
        // 实时在线用户（最近5分钟有活动）（去重统计）
        wrapper.clear();
        wrapper.ge(UserBehavior::getCreateTime, now.minusMinutes(5))
               .select(UserBehavior::getUserId)
               .groupBy(UserBehavior::getUserId);
        List<UserBehavior> onlineUserList = behaviorMapper.selectList(wrapper);
        Long onlineUsers = (long) onlineUserList.size();
        stats.put("onlineUsers", onlineUsers);
        
        // 今日新增用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ge(User::getCreateTime, todayStart);
        Long newUsers = userMapper.selectCount(userWrapper);
        stats.put("todayNewUsers", newUsers);
        
        return stats;
    }
    
    @Override
    public Map<String, Object> getUserProfile(Long userId) {
        Map<String, Object> profile = new HashMap<>();
        
        // 用户基本信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            return profile;
        }
        
        profile.put("userId", userId);
        profile.put("registerTime", user.getCreateTime());
        
        // 行为统计
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId);
        List<UserBehavior> behaviors = behaviorMapper.selectList(wrapper);
        
        // 行为频次
        Map<String, Long> actionCounts = behaviors.stream()
            .collect(Collectors.groupingBy(UserBehavior::getActionType, Collectors.counting()));
        profile.put("actionCounts", actionCounts);
        
        // 最常访问的商品
        List<Long> topProducts = behaviors.stream()
            .filter(b -> "PRODUCT".equals(b.getTargetType()))
            .filter(b -> b.getTargetId() != null)
            .collect(Collectors.groupingBy(UserBehavior::getTargetId, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        profile.put("topProducts", topProducts);
        
        // 活跃时段
        Map<Integer, Long> activeHours = behaviors.stream()
            .collect(Collectors.groupingBy(b -> b.getCreateTime().getHour(), Collectors.counting()));
        profile.put("activeHours", activeHours);
        
        // 设备偏好
        Map<String, Long> devicePreference = behaviors.stream()
            .filter(b -> b.getDeviceType() != null)
            .collect(Collectors.groupingBy(UserBehavior::getDeviceType, Collectors.counting()));
        profile.put("devicePreference", devicePreference);
        
        return profile;
    }
    
    @Override
    public IPage<UserBehavior> getUserBehaviors(Long userId, Integer pageNum, Integer pageSize) {
        Page<UserBehavior> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(UserBehavior::getUserId, userId);
        }
        wrapper.orderByDesc(UserBehavior::getCreateTime);
        
        return behaviorMapper.selectPage(page, wrapper);
    }
    
    @Override
    public List<ProductPreference> getHotProducts(Integer limit) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7); // 最近7天
        
        return calculateProductPreferences(startTime, endTime).stream()
            .sorted((p1, p2) -> Integer.compare(p2.getViewCount(), p1.getViewCount()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<LocalDate, Integer> getUserActivityTrend(Integer days) {
        Map<LocalDate, Integer> trend = new HashMap<>();
        LocalDate today = LocalDate.now();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();
            
            LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(UserBehavior::getCreateTime, startTime, endTime)
                   .select(UserBehavior::getUserId)
                   .groupBy(UserBehavior::getUserId);
            
            List<UserBehavior> userList = behaviorMapper.selectList(wrapper);
            trend.put(date, userList != null ? userList.size() : 0);
        }
        
        return trend;
    }
    
    @Override
    public void cleanExpiredBehaviorData(Integer daysToKeep) {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(daysToKeep);
        
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(UserBehavior::getCreateTime, expireTime);
        
        int deleted = behaviorMapper.delete(wrapper);
        log.info("清理过期行为数据: {} 条", deleted);
    }
    
    // ========== 私有方法 ==========
    
    private ActiveUsersStats calculateActiveUsers(LocalDateTime startTime, LocalDateTime endTime) {
        ActiveUsersStats stats = new ActiveUsersStats();
        
        // DAU（去重统计）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserBehavior::getCreateTime, todayStart)
               .select(UserBehavior::getUserId)
               .groupBy(UserBehavior::getUserId);
        List<UserBehavior> dauList = behaviorMapper.selectList(wrapper);
        stats.setDailyActiveUsers(dauList != null ? dauList.size() : 0);
        
        // WAU（去重统计）
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        wrapper.clear();
        wrapper.ge(UserBehavior::getCreateTime, weekStart)
               .select(UserBehavior::getUserId)
               .groupBy(UserBehavior::getUserId);
        List<UserBehavior> wauList = behaviorMapper.selectList(wrapper);
        stats.setWeeklyActiveUsers(wauList != null ? wauList.size() : 0);
        
        // MAU（去重统计）
        LocalDateTime monthStart = LocalDate.now().minusDays(29).atStartOfDay();
        wrapper.clear();
        wrapper.ge(UserBehavior::getCreateTime, monthStart)
               .select(UserBehavior::getUserId)
               .groupBy(UserBehavior::getUserId);
        List<UserBehavior> mauList = behaviorMapper.selectList(wrapper);
        stats.setMonthlyActiveUsers(mauList != null ? mauList.size() : 0);
        
        // 新增用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.between(User::getCreateTime, startTime, endTime);
        Long newUserCount = userMapper.selectCount(userWrapper);
        stats.setNewUsers(newUserCount != null ? newUserCount.intValue() : 0);
        
        return stats;
    }
    
    private Map<String, Integer> calculateBehaviorDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserBehavior::getCreateTime, startTime, endTime);
        
        List<UserBehavior> behaviors = behaviorMapper.selectList(wrapper);
        
        return behaviors.stream()
            .collect(Collectors.groupingBy(
                UserBehavior::getActionType,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }
    
    private List<PageViewStats> calculatePageViews(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserBehavior::getCreateTime, startTime, endTime)
               .eq(UserBehavior::getActionType, "VIEW");
        
        List<UserBehavior> views = behaviorMapper.selectList(wrapper);
        
        Map<String, List<UserBehavior>> pageGroups = views.stream()
            .filter(v -> v.getPageUrl() != null)
            .collect(Collectors.groupingBy(UserBehavior::getPageUrl));
        
        return pageGroups.entrySet().stream()
            .map(entry -> {
                PageViewStats stats = new PageViewStats();
                stats.setPageUrl(entry.getKey());
                stats.setViews(entry.getValue().size());
                
                Set<Long> uniqueUsers = entry.getValue().stream()
                    .map(UserBehavior::getUserId)
                    .collect(Collectors.toSet());
                stats.setUniqueVisitors(uniqueUsers.size());
                
                Double avgDuration = entry.getValue().stream()
                    .filter(v -> v.getDuration() != null)
                    .mapToInt(UserBehavior::getDuration)
                    .average()
                    .orElse(0.0);
                stats.setAvgDuration(BigDecimal.valueOf(avgDuration));
                
                return stats;
            })
            .sorted((p1, p2) -> Integer.compare(p2.getViews(), p1.getViews()))
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private List<ProductPreference> calculateProductPreferences(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserBehavior::getCreateTime, startTime, endTime)
               .eq(UserBehavior::getTargetType, "PRODUCT");
        
        List<UserBehavior> productBehaviors = behaviorMapper.selectList(wrapper);
        if (productBehaviors == null) {
            productBehaviors = List.of();
        }
        
        Map<Long, List<UserBehavior>> productGroups = productBehaviors.stream()
            .filter(behavior -> behavior.getTargetId() != null)
            .collect(Collectors.groupingBy(UserBehavior::getTargetId));

        Map<Long, ProductPurchaseStats> purchaseStats = calculatePaidProductPurchaseStats(startTime, endTime);
        Set<Long> productIds = new LinkedHashSet<>(productGroups.keySet());
        productIds.addAll(purchaseStats.keySet());
        
        List<ProductPreference> preferences = new ArrayList<>();
        
        for (Long productId : productIds) {
            List<UserBehavior> behaviors = productGroups.getOrDefault(productId, List.of());
            
            Product product = productMapper.selectById(productId);
            if (product == null) continue;
            
            ProductPreference pref = new ProductPreference();
            pref.setProductId(productId);
            pref.setProductName(product.getProductName());
            
            // 统计各种行为
            Map<String, Long> actionCounts = behaviors.stream()
                .filter(behavior -> behavior.getActionType() != null)
                .collect(Collectors.groupingBy(UserBehavior::getActionType, Collectors.counting()));
            ProductPurchaseStats stats = purchaseStats.getOrDefault(productId, ProductPurchaseStats.empty());
            
            pref.setViewCount(actionCounts.getOrDefault("VIEW", 0L).intValue());
            pref.setAddToCartCount(actionCounts.getOrDefault("ADD_CART", 0L).intValue());
            pref.setPurchaseCount(resolvePurchaseCount(stats, actionCounts));
            pref.setRevenue(stats.revenue());
            
            // 计算转化率
            if (pref.getViewCount() > 0) {
                BigDecimal rate = BigDecimal.valueOf(pref.getPurchaseCount() * 100.0 / pref.getViewCount())
                    .setScale(2, RoundingMode.HALF_UP);
                pref.setConversionRate(rate);
            } else {
                pref.setConversionRate(BigDecimal.ZERO);
            }
            
            preferences.add(pref);
        }
        
        return preferences.stream()
            .sorted((left, right) -> {
                int byView = Integer.compare(right.getViewCount(), left.getViewCount());
                if (byView != 0) {
                    return byView;
                }
                return Integer.compare(right.getPurchaseCount(), left.getPurchaseCount());
            })
            .collect(Collectors.toList());
    }

    private Map<Long, ProductPurchaseStats> calculatePaidProductPurchaseStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<Order> paidOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .ge(Order::getPayTime, startTime)
                .lt(Order::getPayTime, endTime)
                .in(Order::getPayStatus, TradeConstants.PAY_STATUS_SUCCESS, TradeConstants.PAY_STATUS_REFUNDED)
                .isNotNull(Order::getPayTime)
        );
        if (paidOrders == null || paidOrders.isEmpty()) {
            return Map.of();
        }

        List<Long> orderIds = paidOrders.stream()
            .map(Order::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return Map.of();
        }

        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds)
        );
        if (orderItems == null || orderItems.isEmpty()) {
            return Map.of();
        }

        Map<Long, ProductPurchaseStats> statsMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            if (item.getProductId() == null) {
                continue;
            }
            ProductPurchaseStats current = statsMap.getOrDefault(item.getProductId(), ProductPurchaseStats.empty());
            statsMap.put(
                item.getProductId(),
                current.add(safeQuantity(item.getQuantity()), defaultAmount(item.getTotalAmount()))
            );
        }
        return statsMap;
    }

    private int resolvePurchaseCount(ProductPurchaseStats stats, Map<String, Long> actionCounts) {
        if (stats.purchaseCount() > 0) {
            return stats.purchaseCount();
        }
        return actionCounts.getOrDefault("PAY", 0L).intValue();
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
    
    private Map<String, BigDecimal> calculateDeviceDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserBehavior::getCreateTime, startTime, endTime)
               .isNotNull(UserBehavior::getDeviceType);
        
        List<UserBehavior> behaviors = behaviorMapper.selectList(wrapper);
        
        Map<String, Long> deviceCounts = behaviors.stream()
            .collect(Collectors.groupingBy(UserBehavior::getDeviceType, Collectors.counting()));
        
        long total = deviceCounts.values().stream().mapToLong(Long::longValue).sum();
        
        Map<String, BigDecimal> distribution = new HashMap<>();
        for (Map.Entry<String, Long> entry : deviceCounts.entrySet()) {
            BigDecimal percentage = BigDecimal.valueOf(entry.getValue() * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP);
            distribution.put(entry.getKey(), percentage);
        }
        
        return distribution;
    }
    
    private Map<Integer, Integer> calculateHourlyActivity(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserBehavior::getCreateTime, startTime, endTime);
        
        List<UserBehavior> behaviors = behaviorMapper.selectList(wrapper);
        
        Map<Integer, Integer> hourlyActivity = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyActivity.put(i, 0);
        }
        
        behaviors.forEach(b -> {
            int hour = b.getCreateTime().getHour();
            hourlyActivity.merge(hour, 1, Integer::sum);
        });
        
        return hourlyActivity;
    }
    
    private ConversionFunnel calculateConversionFunnel(LocalDateTime startTime, LocalDateTime endTime) {
        ConversionFunnel funnel = new ConversionFunnel();
        
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(UserBehavior::getCreateTime, startTime, endTime);
        List<UserBehavior> behaviors = behaviorMapper.selectList(wrapper);
        
        // 统计各阶段用户数
        Set<Long> visitors = new HashSet<>();
        Set<Long> viewers = new HashSet<>();
        Set<Long> carters = new HashSet<>();
        Set<Long> checkouters = new HashSet<>();
        Set<Long> purchasers = new HashSet<>();
        
        behaviors.forEach(b -> {
            Long userId = b.getUserId();
            if (userId != null) {
                visitors.add(userId);
                
                switch (b.getActionType()) {
                    case "VIEW":
                        if ("PRODUCT".equals(b.getTargetType())) {
                            viewers.add(userId);
                        }
                        break;
                    case "ADD_CART":
                        carters.add(userId);
                        break;
                    case "ORDER":
                        checkouters.add(userId);
                        break;
                    case "PAY":
                        purchasers.add(userId);
                        break;
                }
            }
        });
        
        funnel.setTotalVisitors(visitors.size());
        funnel.setViewProduct(viewers.size());
        funnel.setAddToCart(carters.size());
        funnel.setCheckout(checkouters.size());
        funnel.setPurchase(purchasers.size());
        
        // 计算转化率
        if (viewers.size() > 0) {
            funnel.setViewToCartRate(BigDecimal.valueOf(carters.size() * 100.0 / viewers.size())
                .setScale(2, RoundingMode.HALF_UP));
        }
        if (carters.size() > 0) {
            funnel.setCartToCheckoutRate(BigDecimal.valueOf(checkouters.size() * 100.0 / carters.size())
                .setScale(2, RoundingMode.HALF_UP));
        }
        if (checkouters.size() > 0) {
            funnel.setCheckoutToPurchaseRate(BigDecimal.valueOf(purchasers.size() * 100.0 / checkouters.size())
                .setScale(2, RoundingMode.HALF_UP));
        }
        if (visitors.size() > 0) {
            funnel.setOverallConversionRate(BigDecimal.valueOf(purchasers.size() * 100.0 / visitors.size())
                .setScale(2, RoundingMode.HALF_UP));
        }
        
        return funnel;
    }

    private void normalizeBehavior(UserBehavior behavior) {
        if (behavior == null || behavior.getActionType() == null) {
            return;
        }

        String normalizedAction = behavior.getActionType().trim().toUpperCase(Locale.ROOT);
        if ("PAYMENT".equals(normalizedAction)) {
            normalizedAction = "PAY";
        }
        behavior.setActionType(normalizedAction);
    }

    @Override
    public Map<String, Object> getRepurchaseRate(String startTime, String endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderStatus, 4); // 已完成订单
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(Order::getCreateTime, LocalDateTime.parse(startTime, formatter));
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(Order::getCreateTime, LocalDateTime.parse(endTime, formatter));
        }

        List<Order> orders = orderMapper.selectList(wrapper);

        Map<Long, Long> userOrderCount = orders.stream()
                .filter(o -> o.getUserId() != null)
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        long totalUsers = userOrderCount.size();
        long repurchaseUsers = userOrderCount.values().stream()
                .filter(count -> count >= 2)
                .count();

        BigDecimal repurchaseRate = totalUsers > 0
                ? BigDecimal.valueOf(repurchaseUsers * 100.0 / totalUsers).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("repurchaseUsers", repurchaseUsers);
        result.put("repurchaseRate", repurchaseRate);
        return result;
    }

    private record ProductPurchaseStats(int purchaseCount, BigDecimal revenue) {
        static ProductPurchaseStats empty() {
            return new ProductPurchaseStats(0, BigDecimal.ZERO);
        }

        ProductPurchaseStats add(int count, BigDecimal amount) {
            return new ProductPurchaseStats(purchaseCount + count, revenue.add(amount));
        }
    }
}
