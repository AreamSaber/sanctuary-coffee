package com.coffee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户角色分配 DTO
 */
@Data
public class UserRoleAssignDTO {

    @NotNull(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
