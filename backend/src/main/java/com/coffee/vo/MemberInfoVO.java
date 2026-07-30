package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员信息VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class MemberInfoVO {
    
    /**
     * 会员ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 注册时间
     */
    private LocalDateTime createTime;
    
    /**
     * 会员等级ID
     */
    private Long levelId;
    
    /**
     * 会员等级名称
     */
    private String levelName;
    
    /**
     * 等级图标
     */
    private String levelIcon;
    
    /**
     * 折扣率
     */
    private BigDecimal discountRate;
    
    /**
     * 成长值
     */
    private Integer growthValue;
    
    /**
     * 下一等级所需成长值
     */
    private Integer nextLevelGrowth;
    
    /**
     * 积分余额
     */
    private Integer points;
    
    /**
     * 累计消费金额
     */
    private BigDecimal totalConsumption;
}
