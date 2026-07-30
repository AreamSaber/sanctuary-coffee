package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配送员实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("delivery_staff")
public class DeliveryStaff implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 配送员ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID（关联用户表）
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
     * 身份证号
     */
    private String idNumber;
    
    /**
     * 配送员编号
     */
    private String staffCode;
    
    /**
     * 负责区域ID
     */
    private Long regionId;
    
    /**
     * 状态：IDLE-空闲，BUSY-配送中，OFFLINE-离线，REST-休息
     */
    private String status;
    
    /**
     * 今日配送单数
     */
    private Integer todayOrders;
    
    /**
     * 总配送单数
     */
    private Integer totalOrders;
    
    /**
     * 评分（1-5）
     */
    private Double rating;
    
    /**
     * 车辆类型：BIKE-自行车，EBIKE-电动车，MOTORCYCLE-摩托车
     */
    private String vehicleType;
    
    /**
     * 车牌号
     */
    private String vehicleNumber;
    
    /**
     * 健康证号
     */
    private String healthCertNo;
    
    /**
     * 健康证有效期
     */
    private LocalDateTime healthCertExpiry;
    
    /**
     * 入职时间
     */
    private LocalDateTime joinTime;
    
    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer enabled;
    
    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
