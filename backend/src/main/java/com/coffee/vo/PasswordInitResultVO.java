package com.coffee.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首次密码初始化结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordInitResultVO {

    private Integer updatedCount;

    private List<String> usernames;
}
