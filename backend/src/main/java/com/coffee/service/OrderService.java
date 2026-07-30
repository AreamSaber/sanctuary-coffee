package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.dto.OrderCreateDTO;
import com.coffee.entity.Order;
import com.coffee.vo.OrderVO;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    Long createOrder(Long userId, OrderCreateDTO orderCreateDTO);

    /**
     * 查询当前用户订单分页
     */
    IPage<OrderVO> getOrderPage(Long userId, Integer pageNum, Integer pageSize, Integer orderStatus);

    /**
     * 查询当前用户订单详情
     */
    OrderVO getOrderDetail(Long userId, Long orderId);

    /**
     * 管理员查询订单分页
     */
    IPage<OrderVO> getAdminOrderPage(
            Integer pageNum,
            Integer pageSize,
            String orderNo,
            Long userId,
            Integer orderStatus,
            Integer payStatus
    );

    /**
     * 管理员查询订单详情
     */
    OrderVO getAdminOrderDetail(Long orderId);

    /**
     * 取消订单
     */
    void cancelOrder(Long userId, Long orderId, String reason);

    /**
     * 旧支付兼容接口
     */
    void payOrder(Long userId, Long orderId, String paymentMethod);

    /**
     * 确认收货
     */
    void confirmReceipt(Long userId, Long orderId);

    /**
     * 管理员取消待付款订单
     */
    void adminCancelOrder(Long orderId, String reason);

    /**
     * 删除订单
     */
    void deleteOrder(Long userId, Long orderId);
}
