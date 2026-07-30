package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分记录实体
 */
@Data
@TableName("points_record")
public class PointsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("change_type")
    private Integer type;

    @TableField("change_points")
    private Integer points;

    @TableField("before_points")
    private Integer beforeBalance;

    @TableField("after_points")
    private Integer afterBalance;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long bizId;

    @TableField("remark")
    private String description;

    @TableField(exist = false)
    private Integer bizType;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
