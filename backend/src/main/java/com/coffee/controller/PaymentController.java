package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.PaymentDTO;
import com.coffee.dto.RefundReviewDTO;
import com.coffee.service.PaymentService;
import com.coffee.vo.OrderSettlementVO;
import com.coffee.vo.RefundVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付控制器
 */
@Tag(name = "支付管理")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "获取订单结算信息")
    @GetMapping("/settlement/{orderId}")
    public Result<OrderSettlementVO> getOrderSettlement(@PathVariable("orderId") Long orderId) {
        Long userId = getCurrentUserId();
        OrderSettlementVO settlement = paymentService.getOrderSettlement(userId, orderId);
        return Result.success(settlement);
    }

    @Operation(summary = "创建支付单")
    @PostMapping("/create")
    public Result<String> createPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        Long userId = getCurrentUserId();
        String paymentNo = paymentService.createPayment(userId, paymentDTO);
        return Result.success("支付单创建成功", paymentNo);
    }

    @Operation(summary = "确认支付成功")
    @PostMapping("/confirm/{paymentNo}")
    public Result<Void> confirmPayment(@PathVariable("paymentNo") String paymentNo) {
        Long userId = getCurrentUserId();
        paymentService.confirmPayment(userId, SecurityUtils.isAdmin(), paymentNo);
        return Result.success("支付确认成功", null);
    }

    @Operation(summary = "申请退款")
    @PostMapping("/refund/{orderId}")
    public Result<Void> applyRefund(@PathVariable("orderId") Long orderId, @RequestParam("reason") String reason) {
        Long userId = getCurrentUserId();
        paymentService.applyRefund(userId, orderId, reason);
        return Result.success("退款申请已提交", null);
    }

    @Operation(summary = "管理员分页查询退款")
    @GetMapping("/refund/admin/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:refund')")
    public Result<IPage<RefundVO>> getAdminRefundPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String refundNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer refundStatus) {
        IPage<RefundVO> page = paymentService.getAdminRefundPage(pageNum, pageSize, refundNo, orderNo, userId, refundStatus);
        return Result.success(page);
    }

    @Operation(summary = "管理员查询退款详情")
    @GetMapping("/refund/admin/{refundId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:refund')")
    public Result<RefundVO> getAdminRefundDetail(@PathVariable Long refundId) {
        RefundVO detail = paymentService.getAdminRefundDetail(refundId);
        return Result.success(detail);
    }

    @Operation(summary = "管理员审核通过退款")
    @PostMapping("/refund/admin/{refundId}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:refund')")
    public Result<Void> approveRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody(required = false) RefundReviewDTO reviewDTO) {
        paymentService.approveRefund(refundId, reviewDTO == null ? null : reviewDTO.getRemark());
        return Result.success("退款已审核通过", null);
    }

    @Operation(summary = "管理员驳回退款")
    @PostMapping("/refund/admin/{refundId}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:refund')")
    public Result<Void> rejectRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody(required = false) RefundReviewDTO reviewDTO) {
        paymentService.rejectRefund(refundId, reviewDTO == null ? null : reviewDTO.getRemark());
        return Result.success("退款已驳回", null);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
