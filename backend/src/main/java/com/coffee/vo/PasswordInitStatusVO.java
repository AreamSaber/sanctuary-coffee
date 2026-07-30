package com.coffee.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首次密码初始化状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordInitStatusVO {

    private Boolean required;

    private Integer pendingCount;

    private List<PasswordInitUserVO> users;
}
