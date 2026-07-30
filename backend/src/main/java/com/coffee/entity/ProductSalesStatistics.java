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
@TableName("product_sales_statistics")
public class ProductSalesStatistics implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private LocalDate statDate;
    private Integer salesCount;
    private BigDecimal salesAmount;
    private Integer viewCount;
    private Integer collectCount;
    private Integer cartCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
