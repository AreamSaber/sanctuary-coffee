package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.entity.*;
import com.coffee.mapper.*;
import com.coffee.service.StatisticsService;
import com.coffee.vo.ProductStatisticsVO;
import com.coffee.vo.SalesStatisticsVO;
import com.coffee.vo.UserStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final ProductCategoryMapper categoryMapper;
    private final OrderItemMapper orderItemMapper;
    
    @Override
    public SalesStatisticsVO getSalesStatistics() {
        SalesStatisticsVO vo = new SalesStatisticsVO();
        
        // 总订单数和总销售额
        List<Order> allOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .in(Order::getOrderStatus, Arrays.asList(2, 3, 4))
        );
        vo.setTotalOrders((long) allOrders.size());
        vo.setTotalSales(allOrders.stream()
            .map(Order::getPayAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        // 今日数据
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<Order> todayOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .in(Order::getOrderStatus, Arrays.asList(2, 3, 4))
                .ge(Order::getCreateTime, todayStart)
        );
        vo.setTodayOrders((long) todayOrders.size());
        vo.setTodaySales(todayOrders.stream()
            .map(Order::getPayAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        // 本月数据
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        List<Order> monthOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .in(Order::getOrderStatus, Arrays.asList(2, 3, 4))
                .ge(Order::getCreateTime, monthStart)
        );
        vo.setMonthOrders((long) monthOrders.size());
        vo.setMonthSales(monthOrders.stream()
            .map(Order::getPayAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        // 待处理订单
        Long pendingOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .in(Order::getOrderStatus, Arrays.asList(1, 2, 3))
        );
        vo.setPendingOrders(pendingOrders);
        
        // 已完成订单
        Long completedOrders = orderMapper.selectCount(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, 4)
        );
        vo.setCompletedOrders(completedOrders);
        
        return vo;
    }
    
    @Override
    public List<ProductStatisticsVO> getTopSellingProducts(Integer limit) {
        List<Product> products = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getSales)
                .last("LIMIT " + (limit != null ? limit : 10))
        );
        
        return products.stream().map(product -> {
            ProductStatisticsVO vo = new ProductStatisticsVO();
            vo.setProductId(product.getId());
            vo.setProductName(product.getProductName());
            vo.setSalesCount(product.getSales());
            vo.setStock(product.getStock());
            
            // 获取分类名称
            if (product.getCategoryId() != null) {
                ProductCategory category = categoryMapper.selectById(product.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<ProductStatisticsVO> getLowStockProducts(Integer threshold) {
        int stockThreshold = threshold != null ? threshold : 10;
        
        List<Product> products = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .le(Product::getStock, stockThreshold)
                .eq(Product::getStatus, 1)
                .orderByAsc(Product::getStock)
        );
        
        return products.stream().map(product -> {
            ProductStatisticsVO vo = new ProductStatisticsVO();
            vo.setProductId(product.getId());
            vo.setProductName(product.getProductName());
            vo.setSalesCount(product.getSales());
            vo.setStock(product.getStock());
            vo.setCategoryId(product.getCategoryId());
            vo.setIsHot(product.getIsHot());
            vo.setIsNew(product.getIsNew());
            vo.setIsRecommend(product.getIsRecommend());
            
            if (product.getCategoryId() != null) {
                ProductCategory category = categoryMapper.selectById(product.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                }
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public UserStatisticsVO getUserStatistics() {
        UserStatisticsVO vo = new UserStatisticsVO();
        
        // 总用户数
        Long totalUsers = userMapper.selectCount(null);
        vo.setTotalUsers(totalUsers);
        
        // 今日新增
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long todayNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, todayStart)
        );
        vo.setTodayNewUsers(todayNewUsers);
        
        // 本月新增
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        Long monthNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, monthStart)
        );
        vo.setMonthNewUsers(monthNewUsers);
        
        // 活跃用户（本月下单用户）
        List<Order> monthOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .ge(Order::getCreateTime, monthStart)
        );
        long activeUsers = monthOrders.stream()
            .map(Order::getUserId)
            .distinct()
            .count();
        vo.setActiveUsers(activeUsers);
        
        // 会员用户数
        Long memberUsers = memberInfoMapper.selectCount(null);
        vo.setMemberUsers(memberUsers);
        
        return vo;
    }
    
    @Override
    public List<Map<String, Object>> getDailySalesTrend(Integer days) {
        int dayCount = days != null ? days : 7;
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = dayCount - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);
            
            List<Order> dayOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                    .in(Order::getOrderStatus, Arrays.asList(2, 3, 4))
                    .between(Order::getCreateTime, dayStart, dayEnd)
            );
            
            BigDecimal daySales = dayOrders.stream()
                .map(Order::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("sales", daySales);
            dayData.put("orders", dayOrders.size());
            
            result.add(dayData);
        }
        
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getCategorySalesDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有分类
        List<ProductCategory> categories = categoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, 1)
        );
        
        for (ProductCategory category : categories) {
            // 获取该分类下的所有商品
            List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                    .eq(Product::getCategoryId, category.getId())
            );
            
            if (products.isEmpty()) {
                continue;
            }
            
            // 计算销售数量
            int totalSales = products.stream()
                .mapToInt(Product::getSales)
                .sum();
            
            if (totalSales > 0) {
                Map<String, Object> categoryData = new HashMap<>();
                categoryData.put("categoryName", category.getCategoryName());
                categoryData.put("sales", totalSales);
                result.add(categoryData);
            }
        }
        
        return result;
    }

    @Override
    public List<ProductStatisticsVO> getProductProfitAnalysis() {
        List<Product> products = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .gt(Product::getSales, 0)
                .orderByDesc(Product::getSales)
        );

        return products.stream().map(product -> {
            ProductStatisticsVO vo = new ProductStatisticsVO();
            vo.setProductId(product.getId());
            vo.setProductName(product.getProductName());
            vo.setSalesCount(product.getSales());
            vo.setStock(product.getStock());
            vo.setPrice(product.getPrice());
            vo.setCostPrice(product.getCostPrice());

            BigDecimal price = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal cost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            BigDecimal unitProfit = price.subtract(cost);
            BigDecimal totalProfit = unitProfit.multiply(BigDecimal.valueOf(vo.getSalesCount()));

            vo.setUnitProfit(unitProfit);
            vo.setTotalProfit(totalProfit);

            if (product.getCategoryId() != null) {
                ProductCategory category = categoryMapper.selectById(product.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
