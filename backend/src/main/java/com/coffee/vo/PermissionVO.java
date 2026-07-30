package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限树节点 VO
 */
@Data
public class PermissionVO {

    private Long id;
    private Long parentId;
    private String permissionName;
    private String permissionCode;
    private Integer permissionType;
    private String path;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<PermissionVO> children = new ArrayList<>();
}
