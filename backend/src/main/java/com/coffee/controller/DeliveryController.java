package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.entity.DeliveryException;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryTracking;
import com.coffee.service.DeliveryService;
import com.coffee.service.support.DeliveryPositionSimulator;
import com.coffee.vo.DeliveryOrderVO;
import com.coffee.vo.DeliveryTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 配送控制器
 */
@Tag(name = "配送管理")
@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryPositionSimulator positionSimulator;

    @Operation(summary = "获取可用配送方式")
    @GetMapping("/methods")
    public Result<List<DeliveryMethod>> getAvailableMethods() {
        List<DeliveryMethod> methods = deliveryService.getAvailableMethods();
        return Result.success(methods);
    }

    @Operation(summary = "获取配送详情")
    @GetMapping("/detail/{orderId}")
    public Result<DeliveryOrderVO> getDeliveryDetail(@PathVariable Long orderId) {
        DeliveryOrderVO detail = deliveryService.getDeliveryDetail(getCurrentUserId(), orderId);
        return Result.success(detail);
    }

    @Operation(summary = "获取当前账号配送任务")
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DELIVERY')")
    public Result<List<DeliveryTaskVO>> getDeliveryTasks(@RequestParam(required = false) Integer deliveryStatus) {
        List<DeliveryTaskVO> tasks = deliveryService.getDeliveryTasks(getCurrentUserId(), SecurityUtils.isAdmin(), deliveryStatus);
        return Result.success(tasks);
    }

    @Operation(summary = "获取配送轨迹")
    @GetMapping("/tracking/{orderId}")
    public Result<List<DeliveryTracking>> getDeliveryTracking(@PathVariable Long orderId) {
        List<DeliveryTracking> tracking = deliveryService.getDeliveryTracking(getCurrentUserId(), orderId);
        return Result.success(tracking);
    }

    @Operation(summary = "分配配送员")
    @PostMapping("/{orderId}/assign")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'delivery:manage')")
    public Result<Void> assignDelivery(
            @PathVariable Long orderId,
            @RequestParam(required = false) Long staffId) {
        deliveryService.assignDelivery(orderId, staffId);
        return Result.success("配送员分配成功", null);
    }

    @Operation(summary = "配送员接单")
    @PostMapping("/{orderId}/accept")
    @PreAuthorize("hasAuthority('ROLE_DELIVERY')")
    public Result<Void> acceptDelivery(@PathVariable Long orderId) {
        deliveryService.acceptDeliveryByOrderId(getCurrentUserId(), orderId);
        return Result.success("配送员已接单", null);
    }

    @Operation(summary = "开始配送")
    @PostMapping("/{orderId}/start")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DELIVERY')")
    public Result<Void> startDelivery(@PathVariable Long orderId) {
        deliveryService.startDeliveryByOrderId(getCurrentUserId(), SecurityUtils.isAdmin(), orderId);
        return Result.success("配送已开始", null);
    }

    @Operation(summary = "确认送达")
    @PostMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DELIVERY')")
    public Result<Void> completeDelivery(@PathVariable Long orderId) {
        deliveryService.completeDeliveryByOrderId(getCurrentUserId(), SecurityUtils.isAdmin(), orderId);
        return Result.success("配送已送达", null);
    }

    @Operation(summary = "上报配送异常")
    @PostMapping("/exception/report")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DELIVERY')")
    public Result<Void> reportException(
            @RequestParam Long deliveryId,
            @RequestParam Integer exceptionType,
            @RequestParam(required = false) String exceptionDesc) {
        deliveryService.reportException(deliveryId, getCurrentUserId(), exceptionType, exceptionDesc);
        return Result.success("异常上报成功", null);
    }

    @Operation(summary = "处理配送异常")
    @PutMapping("/exception/{exceptionId}/handle")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'delivery:manage')")
    public Result<Void> handleException(
            @PathVariable Long exceptionId,
            @RequestParam String handleResult) {
        deliveryService.handleException(exceptionId, getCurrentUserId(), handleResult);
        return Result.success("异常处理成功", null);
    }

    @Operation(summary = "分页查询配送异常记录")
    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'delivery:manage')")
    public Result<IPage<DeliveryException>> getExceptionPage(
            @RequestParam(required = false) Integer handleStatus,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<DeliveryException> page = deliveryService.getExceptionPage(handleStatus, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取配送实时位置（含模拟GPS坐标）")
    @GetMapping("/position/{orderId}")
    public Result<Map<String, Object>> getDeliveryPosition(@PathVariable Long orderId) {
        deliveryService.assertDeliveryAccess(getCurrentUserId(), orderId);
        return Result.success(positionSimulator.getCurrentPosition(orderId));
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
