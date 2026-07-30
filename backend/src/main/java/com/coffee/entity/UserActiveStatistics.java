package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_active_statistics")
public class UserActiveStatistics implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate statDate;
    private Integer loginCount;
    private Integer browseCount;
    private Integer orderCount;
    private Integer payCount;
    private Integer onlineTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
