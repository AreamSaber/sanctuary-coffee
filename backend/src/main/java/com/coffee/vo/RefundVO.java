package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款管理视图对象。
 */
@Data
public class RefundVO {

    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal refundAmount;

    private String refundReason;

    private Integer refundStatus;

    private String refundStatusText;

    private LocalDateTime refundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer orderStatus;

    private String orderStatusText;

    private Integer payStatus;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private String afterSaleNo;

    private Integer afterSaleStatus;

    private String afterSaleStatusText;

    private LocalDateTime handleTime;

    private String handleRemark;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核人名称
     */
    private String reviewerName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 审核备注
     */
    private String reviewRemark;

    private List<OrderItemVO> items;

    private List<AfterSaleLogVO> logs;
}
