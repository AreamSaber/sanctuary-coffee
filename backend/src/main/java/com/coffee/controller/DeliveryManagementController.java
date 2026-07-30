package com.coffee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.common.Result;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryRegion;
import com.coffee.entity.DeliveryStaff;
import com.coffee.service.DeliveryManagementService;
import com.coffee.vo.DeliveryRegionTreeVO;
import com.coffee.vo.DeliveryStaffVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Delivery Management", description = "Delivery regions and staff management APIs")
@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'delivery:manage')")
public class DeliveryManagementController {

    private final DeliveryManagementService deliveryManagementService;

    @Operation(summary = "Get delivery method page")
    @GetMapping("/method/page")
    public Result<IPage<DeliveryMethod>> getMethodPage(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status
    ) {
        return Result.success(deliveryManagementService.getMethodPage(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "Get all delivery methods")
    @GetMapping("/method/list")
    public Result<List<DeliveryMethod>> getAllMethods() {
        return Result.success(deliveryManagementService.getAllMethods());
    }

    @Operation(summary = "Create delivery method")
    @PostMapping("/method")
    public Result<Void> createMethod(@Valid @RequestBody DeliveryMethod method) {
        deliveryManagementService.createMethod(method);
        return Result.success("Created successfully", null);
    }

    @Operation(summary = "Update delivery method")
    @PutMapping("/method")
    public Result<Void> updateMethod(@Valid @RequestBody DeliveryMethod method) {
        deliveryManagementService.updateMethod(method);
        return Result.success("Updated successfully", null);
    }

    @Operation(summary = "Delete delivery method")
    @DeleteMapping("/method/{id}")
    public Result<Void> deleteMethod(@PathVariable Long id) {
        deliveryManagementService.deleteMethod(id);
        return Result.success("Deleted successfully", null);
    }

    @Operation(summary = "Get delivery region page")
    @GetMapping("/region/page")
    public Result<IPage<DeliveryRegion>> getRegionPage(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword
    ) {
        return Result.success(deliveryManagementService.getRegionPage(pageNum, pageSize, keyword));
    }

    @Operation(summary = "Get delivery region tree")
    @GetMapping("/region/tree")
    public Result<List<DeliveryRegionTreeVO>> getRegionTree() {
        return Result.success(deliveryManagementService.getRegionTree());
    }

    @Operation(summary = "Create delivery region")
    @PostMapping("/region")
    public Result<Void> createRegion(@Valid @RequestBody DeliveryRegion region) {
        deliveryManagementService.createRegion(region);
        return Result.success("Created successfully", null);
    }

    @Operation(summary = "Update delivery region")
    @PutMapping("/region")
    public Result<Void> updateRegion(@Valid @RequestBody DeliveryRegion region) {
        deliveryManagementService.updateRegion(region);
        return Result.success("Updated successfully", null);
    }

    @Operation(summary = "Delete delivery region")
    @DeleteMapping("/region/{id}")
    public Result<Void> deleteRegion(@PathVariable Long id) {
        deliveryManagementService.deleteRegion(id);
        return Result.success("Deleted successfully", null);
    }

    @Operation(summary = "Get delivery staff page")
    @GetMapping("/staff/page")
    public Result<IPage<DeliveryStaffVO>> getStaffPage(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status
    ) {
        return Result.success(deliveryManagementService.getStaffPage(pageNum, pageSize, keyword, status));
    }

    @Operation(summary = "Get delivery staff detail")
    @GetMapping("/staff/{id}")
    public Result<DeliveryStaffVO> getStaffDetail(@PathVariable Long id) {
        return Result.success(deliveryManagementService.getStaffDetail(id));
    }

    @Operation(summary = "Create delivery staff")
    @PostMapping("/staff")
    public Result<Void> createStaff(@Valid @RequestBody DeliveryStaff staff) {
        deliveryManagementService.createStaff(staff);
        return Result.success("Created successfully", null);
    }

    @Operation(summary = "Update delivery staff")
    @PutMapping("/staff")
    public Result<Void> updateStaff(@Valid @RequestBody DeliveryStaff staff) {
        deliveryManagementService.updateStaff(staff);
        return Result.success("Updated successfully", null);
    }

    @Operation(summary = "Delete delivery staff")
    @DeleteMapping("/staff/{id}")
    public Result<Void> deleteStaff(@PathVariable Long id) {
        deliveryManagementService.deleteStaff(id);
        return Result.success("Deleted successfully", null);
    }

    @Operation(summary = "Update delivery staff status")
    @PutMapping("/staff/{id}/status")
    public Result<Void> updateStaffStatus(@PathVariable Long id, @RequestParam String status) {
        deliveryManagementService.updateStaffStatus(id, status);
        return Result.success("Status updated successfully", null);
    }

    @Operation(summary = "Assign delivery staff to region")
    @PutMapping("/staff/{staffId}/region/{regionId}")
    public Result<Void> assignStaffToRegion(@PathVariable Long staffId, @PathVariable Long regionId) {
        deliveryManagementService.assignStaffToRegion(staffId, regionId);
        return Result.success("Assigned successfully", null);
    }

    @Operation(summary = "Get staff by region")
    @GetMapping("/staff/region/{regionId}")
    public Result<List<DeliveryStaffVO>> getStaffByRegion(@PathVariable Long regionId) {
        return Result.success(deliveryManagementService.getStaffByRegion(regionId));
    }

    @Operation(summary = "Get available staff by region")
    @GetMapping("/staff/available/{regionId}")
    public Result<List<DeliveryStaffVO>> getAvailableStaff(@PathVariable Long regionId) {
        return Result.success(deliveryManagementService.getAvailableStaff(regionId));
    }

    @Operation(summary = "Auto assign staff")
    @PostMapping("/staff/auto-assign")
    public Result<Long> autoAssignStaff(@RequestParam Long orderId, @RequestParam Long regionId) {
        return Result.success("Assigned successfully", deliveryManagementService.autoAssignStaff(orderId, regionId));
    }
}
