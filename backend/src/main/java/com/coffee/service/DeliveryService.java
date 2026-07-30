package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.entity.DeliveryException;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryTracking;
import com.coffee.vo.DeliveryOrderVO;
import com.coffee.vo.DeliveryTaskVO;

import java.util.List;

/**
 * 配送服务接口
 */
public interface DeliveryService {

    /**
     * 获取可用配送方式列表
     */
    List<DeliveryMethod> getAvailableMethods();

    /**
     * 创建配送订单（支付完成后调用）
     */
    void createDeliveryOrder(Long orderId);

    /**
     * 分配配送员
     */
    void assignDelivery(Long orderId, Long staffId);

    /**
     * 配送员接单
     */
    void acceptDelivery(Long deliverymanId, Long deliveryOrderId);

    /**
     * 按订单由当前配送员接单
     */
    void acceptDeliveryByOrderId(Long currentUserId, Long orderId);

    /**
     * 开始配送
     */
    void startDelivery(Long deliveryOrderId);

    /**
     * 按订单开始配送
     */
    void startDeliveryByOrderId(Long currentUserId, boolean currentUserIsAdmin, Long orderId);

    /**
     * 完成配送
     */
    void completeDelivery(Long deliveryOrderId);

    /**
     * 按订单完成配送
     */
    void completeDeliveryByOrderId(Long currentUserId, boolean currentUserIsAdmin, Long orderId);

    /**
     * 获取配送详情
     */
    DeliveryOrderVO getDeliveryDetail(Long userId, Long orderId);

    /**
     * 获取当前账号的配送任务
     */
    List<DeliveryTaskVO> getDeliveryTasks(Long currentUserId, boolean currentUserIsAdmin, Integer deliveryStatus);

    /**
     * 获取配送轨迹
     */
    List<DeliveryTracking> getDeliveryTracking(Long userId, Long orderId);

    /**
     * 校验当前账号是否可访问订单配送信息
     */
    void assertDeliveryAccess(Long userId, Long orderId);

    /**
     * 上报配送异常
     */
    void reportException(Long deliveryId, Long reporterId, Integer exceptionType, String exceptionDesc);

    /**
     * 处理配送异常
     */
    void handleException(Long exceptionId, Long handlerId, String handleResult);

    /**
     * 分页查询配送异常记录
     */
    IPage<DeliveryException> getExceptionPage(Integer handleStatus, int pageNum, int pageSize);
}
