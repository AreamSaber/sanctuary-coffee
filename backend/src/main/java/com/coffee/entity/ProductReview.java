package com.coffee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品评价实体
 * 
 * @author Coffee Shop Team
 */
@Data
@TableName("product_review")
public class ProductReview implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 评价ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 评分 1-5
     */
    private Integer rating;
    
    /**
     * 评价内容
     */
    private String content;
    
    /**
     * 评价图片(JSON)
     */
    private String images;
    
    /**
     * 是否匿名
     */
    private Integer isAnonymous;
    
    /**
     * 状态 0隐藏 1显示
     */
    private Integer status;
    
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
