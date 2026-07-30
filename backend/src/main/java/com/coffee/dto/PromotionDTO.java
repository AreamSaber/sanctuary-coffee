package com.coffee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Promotion DTO.
 */
@Data
public class PromotionDTO {

    private Long id;

    @NotBlank(message = "Promotion name is required")
    @Size(max = 100, message = "Promotion name must be 100 characters or fewer")
    private String name;

    @Size(max = 500, message = "Promotion description must be 500 characters or fewer")
    private String description;

    @NotBlank(message = "Promotion type is required")
    private String type;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String banner;

    private Integer status = 1;

    private List<Long> productIds;

    private BigDecimal discountRate;

    private BigDecimal conditionAmount;

    private BigDecimal reductionAmount;

    private Long giftProductId;

    private BigDecimal flashPrice;

    private Integer stock;

    private Integer limitPerUser;

    private Integer priority;
}
