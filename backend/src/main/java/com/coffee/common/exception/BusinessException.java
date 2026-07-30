package com.coffee.common.exception;

import com.coffee.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 
 * @author Coffee Shop Team
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
