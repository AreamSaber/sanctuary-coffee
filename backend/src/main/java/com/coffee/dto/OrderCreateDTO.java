package com.coffee.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 订单创建DTO
 * 
 * @author Coffee Shop Team
 */
@Data
public class OrderCreateDTO {
    
    /**
     * 收货地址ID
     */
    @NotNull(message = "收货地址不能为空")
    private Long addressId;
    
    /**
     * 购物车项ID列表
     */
    @NotEmpty(message = "购物车不能为空")
    private List<Long> cartIds;
    
    /**
     * 订单备注
     */
    private String remark;

    /**
     * 配送方式ID
     */
    private Long deliveryMethodId;

    /**
     * 优惠券ID
     */
    private Long couponId;
}
