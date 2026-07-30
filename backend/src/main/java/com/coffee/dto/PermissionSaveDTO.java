package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 权限保存 DTO
 */
@Data
public class PermissionSaveDTO {

    private Long parentId;

    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @NotBlank(message = "权限码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9:_-]+$", message = "权限码仅支持字母、数字、冒号、下划线和横线")
    private String permissionCode;

    @NotNull(message = "权限类型不能为空")
    private Integer permissionType;

    private String path;

    private String icon;

    private Integer sortOrder;

    private Integer status;
}
