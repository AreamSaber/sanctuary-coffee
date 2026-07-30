package com.coffee.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class ReviewVO {
    
    /**
     * 评价ID
     */
    private Long id;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 商品图片
     */
    private String productImage;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 订单项规格快照
     */
    private String specInfo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String avatar;
    
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
     * 评价图片列表
     */
    private List<String> imageList;
    
    /**
     * 是否匿名
     */
    private Boolean isAnonymous;

    /**
     * 状态 0隐藏 1显示
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否已有商家回复
     */
    private Boolean replied;

    /**
     * 商家回复ID
     */
    private Long replyId;

    /**
     * 回复人ID
     */
    private Long replyUserId;

    /**
     * 商家回复内容
     */
    private String replyContent;

    /**
     * 商家回复时间
     */
    private LocalDateTime replyTime;
}
