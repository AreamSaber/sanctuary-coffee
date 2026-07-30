package com.coffee.service.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.entity.Order;
import com.coffee.mapper.OrderMapper;
import com.coffee.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelTask {

    private static final int CANCEL_TIMEOUT_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedRate = 60_000)
    public void cancelTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(CANCEL_TIMEOUT_MINUTES);

        List<Order> timeoutOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, 1) // PENDING
                .le(Order::getCreateTime, cutoff)
        );

        if (timeoutOrders.isEmpty()) {
            return;
        }

        log.info("发现 {} 个超时未支付订单，开始自动取消", timeoutOrders.size());

        int success = 0;
        for (Order order : timeoutOrders) {
            try {
                orderService.adminCancelOrder(order.getId(), "超时" + CANCEL_TIMEOUT_MINUTES + "分钟未支付，系统自动取消");
                success++;
            } catch (Exception e) {
                log.error("自动取消订单失败: orderId={}, reason={}", order.getId(), e.getMessage());
            }
        }

        log.info("订单超时自动取消完成: 成功 {}/{}", success, timeoutOrders.size());
    }
}
