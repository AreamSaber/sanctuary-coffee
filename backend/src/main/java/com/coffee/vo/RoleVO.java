package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理 VO
 */
@Data
public class RoleVO {

    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Long> permissionIds;
    private List<String> permissionCodes;
}
