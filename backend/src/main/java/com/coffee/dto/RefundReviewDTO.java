package com.coffee.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退款审核请求。
 */
@Data
public class RefundReviewDTO {

    @Size(max = 255, message = "处理备注不能超过255个字符")
    private String remark;
}
