package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.dto.OrderCreateDTO;
import com.coffee.service.OrderService;
import com.coffee.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单控制器
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping({"", "/create"})
    public Result<Long> createOrder(@Valid @RequestBody OrderCreateDTO orderCreateDTO) {
        Long orderId = orderService.createOrder(getCurrentUserId(), orderCreateDTO);
        return Result.success("订单创建成功", orderId);
    }

    @Operation(summary = "分页查询我的订单")
    @GetMapping("/page")
    public Result<IPage<OrderVO>> getOrderPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer orderStatus) {
        IPage<OrderVO> page = orderService.getOrderPage(getCurrentUserId(), pageNum, pageSize, orderStatus);
        return Result.success(page);
    }

    @Operation(summary = "查询订单详情")
    @GetMapping({"/detail/{orderId}", "/{orderId}"})
    public Result<OrderVO> getOrderDetail(@PathVariable Long orderId) {
        OrderVO orderVO = orderService.getOrderDetail(getCurrentUserId(), orderId);
        return Result.success(orderVO);
    }

    @Operation(summary = "取消订单")
    @PostMapping({"/cancel/{orderId}", "/{orderId}/cancel"})
    public Result<Void> cancelOrder(@PathVariable Long orderId, @RequestParam String reason) {
        orderService.cancelOrder(getCurrentUserId(), orderId, reason);
        return Result.success("订单取消成功", null);
    }

    @Operation(summary = "兼容旧支付接口")
    @PostMapping({"/pay/{orderId}", "/{orderId}/pay"})
    public Result<Void> payOrder(@PathVariable Long orderId, @RequestParam(defaultValue = "ALIPAY") String paymentMethod) {
        orderService.payOrder(getCurrentUserId(), orderId, paymentMethod);
        return Result.success("订单支付成功", null);
    }

    @Operation(summary = "确认收货")
    @PostMapping({"/confirm/{orderId}", "/{orderId}/confirm"})
    public Result<Void> confirmReceipt(@PathVariable Long orderId) {
        orderService.confirmReceipt(getCurrentUserId(), orderId);
        return Result.success("确认收货成功", null);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping({"/{orderId}", "/delete/{orderId}"})
    public Result<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(getCurrentUserId(), orderId);
        return Result.success("订单删除成功", null);
    }

    @Operation(summary = "管理员分页查询订单")
    @GetMapping("/admin/page")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:manage')")
    public Result<IPage<OrderVO>> getAdminOrderPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer orderStatus,
            @RequestParam(required = false) Integer payStatus) {
        IPage<OrderVO> page = orderService.getAdminOrderPage(pageNum, pageSize, orderNo, userId, orderStatus, payStatus);
        return Result.success(page);
    }

    @Operation(summary = "管理员查询订单详情")
    @GetMapping("/admin/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:manage')")
    public Result<OrderVO> getAdminOrderDetail(@PathVariable Long orderId) {
        OrderVO orderVO = orderService.getAdminOrderDetail(orderId);
        return Result.success(orderVO);
    }

    @Operation(summary = "管理员取消待付款订单")
    @PostMapping("/admin/{orderId}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'order:manage')")
    public Result<Void> adminCancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason) {
        orderService.adminCancelOrder(orderId, reason);
        return Result.success("订单取消成功", null);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
