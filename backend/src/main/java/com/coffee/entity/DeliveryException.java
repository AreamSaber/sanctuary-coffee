package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配送异常记录实体
 */
@Data
@TableName("delivery_exception")
public class DeliveryException implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deliveryId;

    private Long orderId;

    /**
     * 异常类型 1配送超时 2地址错误 3联系不上 4商品损坏 5其他
     */
    private Integer exceptionType;

    private String exceptionDesc;

    private Long reportedBy;

    private LocalDateTime reportTime;

    /**
     * 处理状态 0待处理 1处理中 2已解决 3已关闭
     */
    private Integer handleStatus;

    private Long handlerId;

    private LocalDateTime handleTime;

    private String handleResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
