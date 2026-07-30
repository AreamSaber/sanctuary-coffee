package com.coffee.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 配送员VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class DeliveryStaffVO {
    
    /**
     * 配送员ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 配送员姓名
     */
    private String name;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 配送员编号
     */
    private String staffCode;
    
    /**
     * 负责区域ID
     */
    private Long regionId;
    
    /**
     * 负责区域名称
     */
    private String regionName;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 今日配送单数
     */
    private Integer todayOrders;
    
    /**
     * 总配送单数
     */
    private Integer totalOrders;
    
    /**
     * 评分
     */
    private Double rating;
    
    /**
     * 车辆类型
     */
    private String vehicleType;
    
    /**
     * 车牌号
     */
    private String vehicleNumber;
    
    /**
     * 健康证有效期
     */
    private LocalDateTime healthCertExpiry;
    
    /**
     * 健康证是否过期
     */
    private Boolean healthCertExpired;
    
    /**
     * 入职时间
     */
    private LocalDateTime joinTime;
    
    /**
     * 是否启用
     */
    private Integer enabled;
    
    /**
     * 当前配送订单数
     */
    private Integer currentOrders;
    
    /**
     * 最后上线时间
     */
    private LocalDateTime lastOnlineTime;
}
