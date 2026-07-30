package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配送异常视图对象
 */
@Data
public class DeliveryExceptionVO {

    private Long id;

    private Long deliveryId;

    private Long orderId;

    private Integer exceptionType;

    private String exceptionTypeText;

    private String exceptionDesc;

    private Long reportedBy;

    private String reporterName;

    private LocalDateTime reportTime;

    private Integer handleStatus;

    private String handleStatusText;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handleTime;

    private String handleResult;

    private LocalDateTime createTime;
}
