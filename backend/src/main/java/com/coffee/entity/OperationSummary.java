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
@TableName("operation_summary")
public class OperationSummary implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Integer totalUser;
    private Integer newUser;
    private Integer activeUser;
    private Integer totalOrder;
    private BigDecimal totalAmount;
    private BigDecimal avgOrderAmount;
    private BigDecimal conversionRate;
    private BigDecimal repurchaseRate;
    private Integer productCount;
    private Integer categoryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
