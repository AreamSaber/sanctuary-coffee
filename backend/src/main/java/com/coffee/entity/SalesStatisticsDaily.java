package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sales_statistics_daily")
public class SalesStatisticsDaily implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer payCount;
    private BigDecimal payAmount;
    private Integer refundCount;
    private BigDecimal refundAmount;
    private Integer newUserCount;
    private Integer activeUserCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
