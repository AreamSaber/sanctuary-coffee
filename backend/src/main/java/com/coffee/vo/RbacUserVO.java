package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户角色分配 VO
 */
@Data
public class RbacUserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createTime;
    private List<Long> roleIds;
    private List<String> roleCodes;
    private List<String> roleNames;
}
