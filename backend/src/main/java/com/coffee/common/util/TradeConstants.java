package com.coffee.common.util;

import com.coffee.entity.Order;

/**
 * 交易域统一常量与状态映射。
 */
public final class TradeConstants {

    private TradeConstants() {
    }

    public static final int ORDER_STATUS_PENDING = 1;
    public static final int ORDER_STATUS_PAID = 2;
    public static final int ORDER_STATUS_DELIVERING = 3;
    public static final int ORDER_STATUS_COMPLETED = 4;
    public static final int ORDER_STATUS_CANCELLED = 5;
    public static final int ORDER_STATUS_REFUNDING = 6;
    public static final int ORDER_STATUS_REFUNDED = 7;

    public static final int PAY_STATUS_UNPAID = 0;
    public static final int PAY_STATUS_SUCCESS = 1;
    public static final int PAY_STATUS_FAILED = 2;
    public static final int PAY_STATUS_REFUNDED = 3;

    public static final int PAY_TYPE_ALIPAY = 1;
    public static final int PAY_TYPE_WECHAT = 2;
    public static final int PAY_TYPE_BALANCE = 3;
    public static final int PAY_TYPE_MOCK = 4;

    public static final int STOCK_CHANGE_INBOUND = 1;
    public static final int STOCK_CHANGE_OUTBOUND = 2;
    public static final int STOCK_CHANGE_RETURN = 3;
    public static final int STOCK_CHANGE_ADJUST = 4;

    public static final int REFUND_STATUS_APPLYING = 0;
    public static final int REFUND_STATUS_SUCCESS = 1;
    public static final int REFUND_STATUS_FAILED = 2;

    public static final int AFTER_SALE_TYPE_REFUND = 1;
    public static final int AFTER_SALE_TYPE_DELIVERY = 2;
    public static final int AFTER_SALE_TYPE_RETURN_REFUND = 3;

    public static final int AFTER_SALE_STATUS_PENDING = 1;
    public static final int AFTER_SALE_STATUS_APPROVED = 2;
    public static final int AFTER_SALE_STATUS_REJECTED = 3;
    public static final int AFTER_SALE_STATUS_PROCESSING = 4;
    public static final int AFTER_SALE_STATUS_COMPLETED = 5;

    public static final String AFTER_SALE_LOG_ACTION_APPLY = "APPLY";
    public static final String AFTER_SALE_LOG_ACTION_APPROVE = "APPROVE";
    public static final String AFTER_SALE_LOG_ACTION_REJECT = "REJECT";
    public static final String AFTER_SALE_LOG_ACTION_REFUND_COMPLETE = "REFUND_COMPLETE";

    public static final String AFTER_SALE_OPERATOR_USER = "USER";
    public static final String AFTER_SALE_OPERATOR_ADMIN = "ADMIN";

    public static void syncOrderStatus(Order order, Integer orderStatus) {
        order.setOrderStatus(orderStatus);
        order.setStatus(resolveOrderStatusCode(orderStatus));
    }

    public static void syncOrderPayment(Order order, Integer payType, Integer payStatus) {
        order.setPayType(payType);
        order.setPayStatus(payStatus);
        order.setPaymentMethod(resolvePaymentMethod(payType));
    }

    public static String resolveOrderStatusCode(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case ORDER_STATUS_PENDING -> "PENDING";
            case ORDER_STATUS_PAID -> "PAID";
            case ORDER_STATUS_DELIVERING -> "SHIPPED";
            case ORDER_STATUS_COMPLETED -> "COMPLETED";
            case ORDER_STATUS_CANCELLED -> "CANCELLED";
            case ORDER_STATUS_REFUNDING -> "REFUNDING";
            case ORDER_STATUS_REFUNDED -> "REFUNDED";
            default -> "UNKNOWN";
        };
    }

    public static String resolveOrderStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case ORDER_STATUS_PENDING -> "待付款";
            case ORDER_STATUS_PAID -> "待发货";
            case ORDER_STATUS_DELIVERING -> "待收货";
            case ORDER_STATUS_COMPLETED -> "已完成";
            case ORDER_STATUS_CANCELLED -> "已取消";
            case ORDER_STATUS_REFUNDING -> "退款中";
            case ORDER_STATUS_REFUNDED -> "已退款";
            default -> "未知";
        };
    }

    public static Integer resolvePayType(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return PAY_TYPE_ALIPAY;
        }
        return switch (paymentMethod.trim().toUpperCase()) {
            case "WECHAT", "WECHAT_PAY" -> PAY_TYPE_WECHAT;
            case "BALANCE", "CASH", "CARD", "OFFLINE" -> PAY_TYPE_BALANCE;
            case "MOCK", "MOCK_PAY" -> PAY_TYPE_MOCK;
            case "ALIPAY" -> PAY_TYPE_ALIPAY;
            default -> PAY_TYPE_ALIPAY;
        };
    }

    public static String resolvePaymentMethod(Integer payType) {
        if (payType == null) {
            return null;
        }
        return switch (payType) {
            case PAY_TYPE_ALIPAY -> "ALIPAY";
            case PAY_TYPE_WECHAT -> "WECHAT";
            case PAY_TYPE_BALANCE -> "BALANCE";
            case PAY_TYPE_MOCK -> "MOCK_PAY";
            default -> "UNKNOWN";
        };
    }
}
