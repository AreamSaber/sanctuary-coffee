package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.dto.AfterSaleApplyDTO;
import com.coffee.dto.PaymentDTO;
import com.coffee.vo.OrderSettlementVO;
import com.coffee.vo.RefundVO;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 获取订单结算信息
     */
    OrderSettlementVO getOrderSettlement(Long userId, Long orderId);

    /**
     * 创建支付单
     */
    String createPayment(Long userId, PaymentDTO paymentDTO);

    /**
     * 确认支付成功
     */
    void confirmPayment(Long currentUserId, boolean currentUserIsAdmin, String paymentNo);

    /**
     * 申请退款
     */
    void applyRefund(Long userId, Long orderId, String reason);

    /**
     * 提交完整售后申请
     */
    void applyAfterSale(Long userId, AfterSaleApplyDTO applyDTO);

    /**
     * 管理员查询退款分页
     */
    IPage<RefundVO> getAdminRefundPage(
            Integer pageNum,
            Integer pageSize,
            String refundNo,
            String orderNo,
            Long userId,
            Integer refundStatus
    );

    /**
     * 管理员查询退款详情
     */
    RefundVO getAdminRefundDetail(Long refundId);

    /**
     * 管理员审核通过退款。
     */
    void approveRefund(Long refundId, String remark);

    /**
     * 管理员驳回退款。
     */
    void rejectRefund(Long refundId, String remark);

    /**
     * 管理员通过售后单审核退款。
     */
    void approveRefundAfterSale(Long afterSaleId, String remark);

    /**
     * 管理员通过售后单驳回退款。
     */
    void rejectRefundAfterSale(Long afterSaleId, String remark);

    /**
     * 回滚待支付阶段占用的优惠券和积分
     */
    void rollbackPendingBenefits(Long userId, Long orderId);
}
