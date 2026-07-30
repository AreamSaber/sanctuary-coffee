package com.coffee.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后处理日志展示对象。
 */
@Data
public class AfterSaleLogVO {

    private Long id;

    private Long afterSaleId;

    private Long operatorId;

    private String operatorType;

    private String operatorTypeText;

    private String action;

    private String actionText;

    private Integer statusFrom;

    private String statusFromText;

    private Integer statusTo;

    private String statusToText;

    private String remark;

    private LocalDateTime createTime;
}
