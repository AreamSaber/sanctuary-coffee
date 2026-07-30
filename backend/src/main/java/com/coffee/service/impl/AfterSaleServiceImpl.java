package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.TradeConstants;
import com.coffee.entity.Order;
import com.coffee.entity.OrderAfterSale;
import com.coffee.entity.OrderAfterSaleLog;
import com.coffee.entity.OrderItem;
import com.coffee.mapper.OrderAfterSaleMapper;
import com.coffee.mapper.OrderAfterSaleLogMapper;
import com.coffee.mapper.OrderItemMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.service.AfterSaleService;
import com.coffee.vo.AfterSaleLogVO;
import com.coffee.vo.AfterSaleVO;
import com.coffee.vo.OrderItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 售后服务实现类。
 */
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl extends ServiceImpl<OrderAfterSaleMapper, OrderAfterSale> implements AfterSaleService {

    private final OrderAfterSaleMapper orderAfterSaleMapper;
    private final OrderAfterSaleLogMapper orderAfterSaleLogMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public IPage<AfterSaleVO> getUserAfterSalePage(
            Long userId,
            Integer pageNum,
            Integer pageSize,
            String afterSaleNo,
            String orderNo,
            Integer type,
            Integer status) {
        Page<OrderAfterSale> page = new Page<>(pageNum, pageSize);
        List<Long> matchedOrderIds = hasText(orderNo) ? findMatchedOrderIds(orderNo, userId) : List.of();
        if (hasText(orderNo) && matchedOrderIds.isEmpty()) {
            Page<AfterSaleVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        LambdaQueryWrapper<OrderAfterSale> wrapper = new LambdaQueryWrapper<OrderAfterSale>()
                .eq(OrderAfterSale::getUserId, userId)
                .like(hasText(afterSaleNo), OrderAfterSale::getAfterSaleNo, afterSaleNo)
                .eq(type != null, OrderAfterSale::getType, type)
                .eq(status != null, OrderAfterSale::getStatus, status)
                .in(hasText(orderNo), OrderAfterSale::getOrderId, matchedOrderIds)
                .orderByDesc(OrderAfterSale::getCreateTime);

        IPage<OrderAfterSale> afterSalePage = orderAfterSaleMapper.selectPage(page, wrapper);
        return buildAfterSalePage(afterSalePage);
    }

    @Override
    public AfterSaleVO getUserAfterSaleDetail(Long userId, Long afterSaleId) {
        OrderAfterSale afterSale = orderAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null || !userId.equals(afterSale.getUserId())) {
            throw new BusinessException("售后记录不存在");
        }

        List<Long> orderIds = afterSale.getOrderId() == null ? List.of() : List.of(afterSale.getOrderId());
        return buildAfterSaleVO(afterSale, getOrderMap(orderIds), getOrderItemVOMap(orderIds), getLogVOMap(List.of(afterSaleId)));
    }

    @Override
    public IPage<AfterSaleVO> getAdminAfterSalePage(
            Integer pageNum,
            Integer pageSize,
            String afterSaleNo,
            String orderNo,
            Long userId,
            Integer type,
            Integer status) {
        Page<OrderAfterSale> page = new Page<>(pageNum, pageSize);
        List<Long> matchedOrderIds = hasText(orderNo) ? findMatchedOrderIds(orderNo, null) : List.of();
        if (hasText(orderNo) && matchedOrderIds.isEmpty()) {
            Page<AfterSaleVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setRecords(List.of());
            return emptyPage;
        }

        LambdaQueryWrapper<OrderAfterSale> wrapper = new LambdaQueryWrapper<OrderAfterSale>()
                .like(hasText(afterSaleNo), OrderAfterSale::getAfterSaleNo, afterSaleNo)
                .eq(userId != null, OrderAfterSale::getUserId, userId)
                .eq(type != null, OrderAfterSale::getType, type)
                .eq(status != null, OrderAfterSale::getStatus, status)
                .in(hasText(orderNo), OrderAfterSale::getOrderId, matchedOrderIds)
                .orderByDesc(OrderAfterSale::getCreateTime);

        IPage<OrderAfterSale> afterSalePage = orderAfterSaleMapper.selectPage(page, wrapper);
        return buildAfterSalePage(afterSalePage);
    }

    @Override
    public AfterSaleVO getAdminAfterSaleDetail(Long afterSaleId) {
        OrderAfterSale afterSale = orderAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new BusinessException("售后记录不存在");
        }

        List<Long> orderIds = afterSale.getOrderId() == null ? List.of() : List.of(afterSale.getOrderId());
        return buildAfterSaleVO(afterSale, getOrderMap(orderIds), getOrderItemVOMap(orderIds), getLogVOMap(List.of(afterSaleId)));
    }

    private Page<AfterSaleVO> buildAfterSalePage(IPage<OrderAfterSale> afterSalePage) {
        Page<AfterSaleVO> voPage = new Page<>(afterSalePage.getCurrent(), afterSalePage.getSize(), afterSalePage.getTotal());

        List<OrderAfterSale> records = afterSalePage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }

        List<Long> orderIds = records.stream()
                .map(OrderAfterSale::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> afterSaleIds = records.stream()
                .map(OrderAfterSale::getId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Order> orderMap = getOrderMap(orderIds);
        Map<Long, List<OrderItemVO>> orderItemMap = getOrderItemVOMap(orderIds);
        Map<Long, List<AfterSaleLogVO>> logMap = getLogVOMap(afterSaleIds);

        voPage.setRecords(records.stream()
                .map(afterSale -> buildAfterSaleVO(afterSale, orderMap, orderItemMap, logMap))
                .collect(Collectors.toList()));
        return voPage;
    }

    private List<Long> findMatchedOrderIds(String orderNo, Long userId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .like(Order::getOrderNo, orderNo)
                        .eq(userId != null, Order::getUserId, userId)
        ).stream().map(Order::getId).collect(Collectors.toList());
    }

    private Map<Long, Order> getOrderMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .in(Order::getId, orderIds)
        ).stream().collect(Collectors.toMap(Order::getId, item -> item));
    }

    private Map<Long, List<OrderItemVO>> getOrderItemVOMap(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, orderIds)
        ).stream().collect(Collectors.groupingBy(
                OrderItem::getOrderId,
                Collectors.mapping(item -> BeanUtil.copyProperties(item, OrderItemVO.class), Collectors.toList())
        ));
    }

    private Map<Long, List<AfterSaleLogVO>> getLogVOMap(List<Long> afterSaleIds) {
        if (afterSaleIds == null || afterSaleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<OrderAfterSaleLog> logs = orderAfterSaleLogMapper.selectList(
                new LambdaQueryWrapper<OrderAfterSaleLog>()
                        .in(OrderAfterSaleLog::getAfterSaleId, afterSaleIds)
                        .orderByAsc(OrderAfterSaleLog::getCreateTime)
                        .orderByAsc(OrderAfterSaleLog::getId)
        );
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyMap();
        }

        return logs.stream()
                .map(this::buildAfterSaleLogVO)
                .collect(Collectors.groupingBy(
                        AfterSaleLogVO::getAfterSaleId,
                        Collectors.collectingAndThen(Collectors.toList(), groupedLogs -> groupedLogs.stream()
                                .sorted(Comparator.comparing(AfterSaleLogVO::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(AfterSaleLogVO::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                                .collect(Collectors.toList()))
                ));
    }

    private AfterSaleVO buildAfterSaleVO(
            OrderAfterSale afterSale,
            Map<Long, Order> orderMap,
            Map<Long, List<OrderItemVO>> orderItemMap,
            Map<Long, List<AfterSaleLogVO>> logMap) {
        AfterSaleVO vo = BeanUtil.copyProperties(afterSale, AfterSaleVO.class);
        vo.setTypeText(resolveAfterSaleTypeText(afterSale.getType()));
        vo.setStatusText(resolveAfterSaleStatusText(afterSale.getStatus()));
        if (afterSale.getHandleTime() != null) {
            vo.setReviewTime(afterSale.getHandleTime());
        }
        if (afterSale.getHandleRemark() != null) {
            vo.setReviewRemark(afterSale.getHandleRemark());
        }
        vo.setItems(orderItemMap.getOrDefault(afterSale.getOrderId(), List.of()));
        vo.setLogs(logMap.getOrDefault(afterSale.getId(), List.of()));

        Order order = orderMap.get(afterSale.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setPayAmount(order.getPayAmount());
            vo.setOrderStatus(order.getOrderStatus());
            vo.setOrderStatusText(TradeConstants.resolveOrderStatusText(order.getOrderStatus()));
            vo.setPayStatus(order.getPayStatus());
            vo.setPayStatusText(resolvePayStatusText(order.getPayStatus()));
            vo.setReceiverName(order.getReceiverName());
            vo.setReceiverPhone(order.getReceiverPhone());
            vo.setReceiverAddress(order.getReceiverAddress());
            vo.setRemark(order.getRemark());
        }
        return vo;
    }

    private AfterSaleLogVO buildAfterSaleLogVO(OrderAfterSaleLog log) {
        AfterSaleLogVO vo = BeanUtil.copyProperties(log, AfterSaleLogVO.class);
        vo.setActionText(resolveAfterSaleLogActionText(log.getAction()));
        vo.setOperatorTypeText(resolveOperatorTypeText(log.getOperatorType()));
        vo.setStatusFromText(resolveAfterSaleStatusText(log.getStatusFrom()));
        vo.setStatusToText(resolveAfterSaleStatusText(log.getStatusTo()));
        return vo;
    }

    private String resolveAfterSaleTypeText(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case TradeConstants.AFTER_SALE_TYPE_REFUND -> "仅退款";
            case TradeConstants.AFTER_SALE_TYPE_DELIVERY -> "配送问题";
            case TradeConstants.AFTER_SALE_TYPE_RETURN_REFUND -> "退货退款";
            default -> "未知";
        };
    }

    private String resolveAfterSaleStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case TradeConstants.AFTER_SALE_STATUS_PENDING -> "待处理";
            case TradeConstants.AFTER_SALE_STATUS_APPROVED -> "已同意";
            case TradeConstants.AFTER_SALE_STATUS_REJECTED -> "已驳回";
            case TradeConstants.AFTER_SALE_STATUS_PROCESSING -> "处理中";
            case TradeConstants.AFTER_SALE_STATUS_COMPLETED -> "已完成";
            default -> "未知";
        };
    }

    private String resolvePayStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case TradeConstants.PAY_STATUS_UNPAID -> "待支付";
            case TradeConstants.PAY_STATUS_SUCCESS -> "已支付";
            case TradeConstants.PAY_STATUS_FAILED -> "支付失败";
            case TradeConstants.PAY_STATUS_REFUNDED -> "已退款";
            default -> "未知";
        };
    }

    private String resolveAfterSaleLogActionText(String action) {
        if (action == null || action.isBlank()) {
            return "处理记录";
        }
        return switch (action) {
            case TradeConstants.AFTER_SALE_LOG_ACTION_APPLY -> "提交申请";
            case TradeConstants.AFTER_SALE_LOG_ACTION_APPROVE -> "审核通过";
            case TradeConstants.AFTER_SALE_LOG_ACTION_REJECT -> "审核驳回";
            case TradeConstants.AFTER_SALE_LOG_ACTION_REFUND_COMPLETE -> "退款完成";
            default -> "处理记录";
        };
    }

    private String resolveOperatorTypeText(String operatorType) {
        if (TradeConstants.AFTER_SALE_OPERATOR_USER.equals(operatorType)) {
            return "用户";
        }
        if (TradeConstants.AFTER_SALE_OPERATOR_ADMIN.equals(operatorType)) {
            return "管理员";
        }
        return "系统";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
