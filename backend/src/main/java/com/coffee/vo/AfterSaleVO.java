package com.coffee.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后后台展示对象。
 */
@Data
public class AfterSaleVO {

    private Long id;

    private Long orderId;

    private Long userId;

    private String afterSaleNo;

    private String orderNo;

    private Integer type;

    private String typeText;

    private String reason;

    private String description;

    private String images;

    private BigDecimal refundAmount;

    private Integer status;

    private String statusText;

    private LocalDateTime handleTime;

    private String handleRemark;

    private Long reviewerId;

    private LocalDateTime reviewTime;

    private String reviewRemark;

    private BigDecimal totalAmount;

    private BigDecimal payAmount;

    private Integer orderStatus;

    private String orderStatusText;

    private Integer payStatus;

    private String payStatusText;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<OrderItemVO> items;

    private List<AfterSaleLogVO> logs;
}
