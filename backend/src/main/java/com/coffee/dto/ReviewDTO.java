package com.coffee.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 商品评价DTO
 * 
 * @author Coffee Shop Team
 */
@Data
public class ReviewDTO {
    
    /**
     * 评价ID（更新时使用）
     */
    private Long id;
    
    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    /**
     * 评分 1-5
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为1")
    @Max(value = 5, message = "评分最高为5")
    private Integer rating;
    
    /**
     * 评价内容
     */
    @NotBlank(message = "评价内容不能为空")
    @Size(min = 5, max = 500, message = "评价内容长度在5-500个字符之间")
    private String content;
    
    /**
     * 评价图片(JSON)
     */
    private String images;
    
    /**
     * 是否匿名
     */
    private Boolean isAnonymous = false;
}
