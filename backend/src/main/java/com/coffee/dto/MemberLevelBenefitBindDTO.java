package com.coffee.dto;

import lombok.Data;

import java.util.List;

/**
 * 会员等级权益绑定参数
 */
@Data
public class MemberLevelBenefitBindDTO {

    private List<Long> benefitIds;
}
