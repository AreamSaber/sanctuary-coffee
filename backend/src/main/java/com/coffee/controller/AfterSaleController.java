package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.AfterSaleApplyDTO;
import com.coffee.dto.RefundReviewDTO;
import com.coffee.service.AfterSaleService;
import com.coffee.service.PaymentService;
import com.coffee.vo.AfterSaleVO;
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
 * 售后控制器。
 */
@Tag(name = "售后管理")
@RestController
@RequestMapping("/after-sale")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;
    private final PaymentService paymentService;

    @Operation(summary = "分页查询我的售后")
    @GetMapping("/page")
    public Result<IPage<AfterSaleVO>> getUserAfterSalePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String afterSaleNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        IPage<AfterSaleVO> page = afterSaleService.getUserAfterSalePage(
                getCurrentUserId(),
                pageNum,
                pageSize,
                afterSaleNo,
                orderNo,
                type,
                status
        );
        return Result.success(page);
    }

    @Operation(summary = "查询我的售后详情")
    @GetMapping("/{afterSaleId}")
    public Result<AfterSaleVO> getUserAfterSaleDetail(@PathVariable Long afterSaleId) {
        return Result.success(afterSaleService.getUserAfterSaleDetail(getCurrentUserId(), afterSaleId));
    }

    @Operation(summary = "提交售后申请")
    @PostMapping("/apply")
    public Result<Void> applyAfterSale(@Valid @RequestBody AfterSaleApplyDTO applyDTO) {
        paymentService.applyAfterSale(getCurrentUserId(), applyDTO);
        return Result.success("售后申请已提交", null);
    }

    @Operation(summary = "管理员分页查询售后")
    @GetMapping("/admin/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:after-sale')")
    public Result<IPage<AfterSaleVO>> getAdminAfterSalePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String afterSaleNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status) {
        IPage<AfterSaleVO> page = afterSaleService.getAdminAfterSalePage(
                pageNum,
                pageSize,
                afterSaleNo,
                orderNo,
                userId,
                type,
                status
        );
        return Result.success(page);
    }

    @Operation(summary = "管理员查询售后详情")
    @GetMapping("/admin/{afterSaleId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:after-sale')")
    public Result<AfterSaleVO> getAdminAfterSaleDetail(@PathVariable Long afterSaleId) {
        return Result.success(afterSaleService.getAdminAfterSaleDetail(afterSaleId));
    }

    @Operation(summary = "管理员审核通过退款售后")
    @PostMapping("/admin/{afterSaleId}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:after-sale')")
    public Result<Void> approveAfterSale(
            @PathVariable Long afterSaleId,
            @Valid @RequestBody(required = false) RefundReviewDTO reviewDTO) {
        paymentService.approveRefundAfterSale(afterSaleId, reviewDTO == null ? null : reviewDTO.getRemark());
        return Result.success("售后已审核通过", null);
    }

    @Operation(summary = "管理员驳回退款售后")
    @PostMapping("/admin/{afterSaleId}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:after-sale')")
    public Result<Void> rejectAfterSale(
            @PathVariable Long afterSaleId,
            @Valid @RequestBody(required = false) RefundReviewDTO reviewDTO) {
        paymentService.rejectRefundAfterSale(afterSaleId, reviewDTO == null ? null : reviewDTO.getRemark());
        return Result.success("售后已驳回", null);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
