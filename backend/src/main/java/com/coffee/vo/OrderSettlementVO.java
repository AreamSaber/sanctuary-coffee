package com.coffee.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 订单结算VO
 * 
 * @author Coffee Shop Team
 */
@Data
public class OrderSettlementVO {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 商品总额
     */
    private BigDecimal totalAmount;
    
    /**
     * 运费
     */
    private BigDecimal freightAmount;

    /**
     * 促销活动优惠
     */
    private BigDecimal promotionDiscount;

    /**
     * 会员免配送费权益抵扣
     */
    private BigDecimal freightDiscount;
    
    /**
     * 优惠券优惠
     */
    private BigDecimal couponDiscount;
    
    /**
     * 积分抵扣
     */
    private BigDecimal pointsDiscount;
    
    /**
     * 会员折扣
     */
    private BigDecimal memberDiscount;

    /**
     * 会员折扣率
     */
    private BigDecimal memberDiscountRate;

    /**
     * 积分奖励倍率
     */
    private BigDecimal pointRewardMultiplier;

    /**
     * 当前摘要预计奖励积分
     */
    private Integer estimatedRewardPoints;
    
    /**
     * 实付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 可用积分
     */
    private Integer availablePoints;
    
    /**
     * 积分抵扣比例（多少积分抵1元）
     */
    private Integer pointsRate;
    
    /**
     * 订单商品列表
     */
    private List<OrderItemVO> items;

    /**
     * 当前会员等级可用权益
     */
    private List<MemberBenefitVO> memberBenefits;
}
