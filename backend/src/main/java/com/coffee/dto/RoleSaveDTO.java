package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 角色保存 DTO
 */
@Data
public class RoleSaveDTO {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z0-9_:]+$", message = "角色编码仅支持大写字母、数字、冒号和下划线")
    private String roleCode;

    private String description;

    private Integer status;
}
