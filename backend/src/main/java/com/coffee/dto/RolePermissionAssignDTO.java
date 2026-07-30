package com.coffee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色权限分配 DTO
 */
@Data
public class RolePermissionAssignDTO {

    @NotNull(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
