package com.coffee.common;

import lombok.Getter;

/**
 * 响应码枚举
 * 
 * @author Coffee Shop Team
 */
@Getter
public enum ResultCode {
    
    /* 成功状态码 */
    SUCCESS(200, "操作成功"),
    
    /* 客户端错误 4xx */
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    /* 服务器错误 5xx */
    ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    /* 业务错误 1xxx */
    PARAM_ERROR(1000, "参数错误"),
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    TOKEN_INVALID(1005, "Token无效"),
    TOKEN_EXPIRED(1006, "Token已过期"),
    
    PRODUCT_NOT_EXIST(1101, "商品不存在"),
    PRODUCT_STOCK_NOT_ENOUGH(1102, "商品库存不足"),
    
    ORDER_NOT_EXIST(1201, "订单不存在"),
    ORDER_STATUS_ERROR(1202, "订单状态错误"),
    
    CART_EMPTY(1301, "购物车为空"),
    
    ADDRESS_NOT_EXIST(1401, "地址不存在"),
    
    COUPON_NOT_EXIST(1501, "优惠券不存在"),
    COUPON_EXPIRED(1502, "优惠券已过期"),
    COUPON_USED(1503, "优惠券已使用"),
    
    PAYMENT_FAILED(1601, "支付失败"),
    REFUND_FAILED(1602, "退款失败"),
    
    REVIEW_NOT_ALLOWED(1701, "无法评价该商品"),
    REVIEW_NOT_EXIST(1702, "评价不存在"),
    REVIEW_ALREADY_EXIST(1703, "已经评价过该商品"),
    
    PROMOTION_NOT_EXIST(1801, "促销活动不存在"),
    PROMOTION_EXPIRED(1802, "促销活动已过期"),
    PROMOTION_NOT_STARTED(1803, "促销活动未开始"),
    
    MEMBER_LEVEL_NOT_EXIST(1901, "会员等级不存在"),
    MEMBER_INFO_NOT_EXIST(1902, "会员信息不存在"),
    
    INVOICE_NOT_EXIST(2001, "发票不存在"),
    INVOICE_ALREADY_EXIST(2002, "发票已申请"),
    INVOICE_NOT_ISSUED(2003, "发票未开具"),
    
    DELIVERY_REGION_NOT_EXIST(2101, "配送区域不存在"),
    DELIVERY_REGION_HAS_CHILDREN(2102, "配送区域存在子区域"),
    DELIVERY_REGION_HAS_STAFF(2103, "配送区域存在配送员"),
    DELIVERY_STAFF_NOT_EXIST(2104, "配送员不存在"),
    NO_AVAILABLE_DELIVERY_STAFF(2105, "暂无可用配送员");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
