package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.SecurityUtils;
import com.coffee.entity.DeliveryException;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryOrder;
import com.coffee.entity.DeliveryStaff;
import com.coffee.entity.DeliveryTracking;
import com.coffee.entity.Order;
import com.coffee.mapper.DeliveryExceptionMapper;
import com.coffee.mapper.DeliveryMethodMapper;
import com.coffee.mapper.DeliveryOrderMapper;
import com.coffee.mapper.DeliveryStaffMapper;
import com.coffee.mapper.DeliveryTrackingMapper;
import com.coffee.mapper.OrderMapper;
import com.coffee.service.DeliveryService;
import com.coffee.vo.DeliveryOrderVO;
import com.coffee.vo.DeliveryTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 配送服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private static final int ORDER_STATUS_PAID = 2;
    private static final int ORDER_STATUS_DELIVERING = 3;
    private static final int DELIVERY_STATUS_PENDING = 1;
    private static final int DELIVERY_STATUS_ASSIGNED = 2;
    private static final int DELIVERY_STATUS_DELIVERING = 3;
    private static final int DELIVERY_STATUS_DELIVERED = 4;

    private final DeliveryMethodMapper deliveryMethodMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DeliveryTrackingMapper trackingMapper;
    private final DeliveryStaffMapper staffMapper;
    private final OrderMapper orderMapper;
    private final DeliveryExceptionMapper exceptionMapper;

    @Override
    public List<DeliveryMethod> getAvailableMethods() {
        List<DeliveryMethod> methods = deliveryMethodMapper.selectList(
                new LambdaQueryWrapper<DeliveryMethod>()
                        .eq(DeliveryMethod::getStatus, 1)
                        .orderByAsc(DeliveryMethod::getId)
        );
        methods.forEach(this::normalizeDeliveryMethod);
        return methods;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDeliveryOrder(Long orderId) {
        Order order = requireOrder(orderId);
        DeliveryOrder existing = getDeliveryOrderByOrderId(orderId);
        if (existing != null) {
            log.info("Delivery order already exists, skip duplicate create: deliveryOrderId={}, orderId={}", existing.getId(), orderId);
            return;
        }

        if (!Integer.valueOf(ORDER_STATUS_PAID).equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不合法");
        }

        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setDeliveryNo(generateDeliveryNo());
        deliveryOrder.setOrderId(order.getId());
        deliveryOrder.setDeliveryStatus(DELIVERY_STATUS_PENDING);
        deliveryOrderMapper.insert(deliveryOrder);

        addTracking(deliveryOrder.getId(), "配送单已创建，等待分配配送员");

        log.info("Delivery order created: deliveryNo={}, orderId={}", deliveryOrder.getDeliveryNo(), orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDelivery(Long orderId, Long staffId) {
        DeliveryOrder deliveryOrder = getDeliveryOrderByOrderId(orderId);
        if (deliveryOrder == null) {
            createDeliveryOrder(orderId);
            deliveryOrder = getDeliveryOrderByOrderId(orderId);
        }
        if (deliveryOrder == null) {
            throw new BusinessException("配送单不存在");
        }
        if (!Integer.valueOf(DELIVERY_STATUS_PENDING).equals(deliveryOrder.getDeliveryStatus())) {
            throw new BusinessException("配送单状态不合法");
        }

        DeliveryStaff staff = staffId == null ? findAvailableStaff() : requireStaff(staffId);
        if (staff == null) {
            throw new BusinessException(ResultCode.NO_AVAILABLE_DELIVERY_STAFF);
        }
        if (!Integer.valueOf(1).equals(staff.getEnabled())) {
            throw new BusinessException("配送员已禁用");
        }

        deliveryOrder.setDeliverymanId(staff.getId());
        deliveryOrder.setDeliveryStatus(DELIVERY_STATUS_ASSIGNED);
        deliveryOrderMapper.updateById(deliveryOrder);

        staff.setStatus("BUSY");
        staffMapper.updateById(staff);

        addTracking(deliveryOrder.getId(), "配送员已分配：" + staff.getName());
        log.info("Delivery assigned: orderId={}, deliveryOrderId={}, staffId={}", orderId, deliveryOrder.getId(), staff.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptDelivery(Long deliverymanId, Long deliveryOrderId) {
        DeliveryOrder deliveryOrder = requireDeliveryOrder(deliveryOrderId);
        if (!List.of(DELIVERY_STATUS_PENDING, DELIVERY_STATUS_ASSIGNED).contains(deliveryOrder.getDeliveryStatus())) {
            throw new BusinessException("配送单状态不合法");
        }
        DeliveryStaff staff = requireStaff(deliverymanId);
        if (!Integer.valueOf(1).equals(staff.getEnabled())) {
            throw new BusinessException("配送员已禁用");
        }
        if (deliveryOrder.getDeliverymanId() != null && !deliveryOrder.getDeliverymanId().equals(staff.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (deliveryOrder.getAcceptTime() != null) {
            throw new BusinessException("配送单已接单");
        }

        deliveryOrder.setDeliverymanId(staff.getId());
        deliveryOrder.setDeliveryStatus(DELIVERY_STATUS_ASSIGNED);
        deliveryOrder.setAcceptTime(LocalDateTime.now());
        deliveryOrderMapper.updateById(deliveryOrder);

        staff.setStatus("BUSY");
        staffMapper.updateById(staff);

        addTracking(deliveryOrder.getId(), "配送员已接单：" + staff.getName());
        log.info("Delivery accepted: deliverymanId={}, deliveryOrderId={}", deliverymanId, deliveryOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptDeliveryByOrderId(Long currentUserId, Long orderId) {
        DeliveryOrder deliveryOrder = requireDeliveryOrderByOrderId(orderId);
        DeliveryStaff staff = requireStaffByUserId(currentUserId);
        if (!Integer.valueOf(DELIVERY_STATUS_ASSIGNED).equals(deliveryOrder.getDeliveryStatus())
            || deliveryOrder.getDeliverymanId() == null
            || !deliveryOrder.getDeliverymanId().equals(staff.getId())) {
            throw new BusinessException("当前配送单未分配给你");
        }
        acceptDelivery(staff.getId(), deliveryOrder.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startDelivery(Long deliveryOrderId) {
        DeliveryOrder deliveryOrder = requireDeliveryOrder(deliveryOrderId);
        if (!Integer.valueOf(DELIVERY_STATUS_ASSIGNED).equals(deliveryOrder.getDeliveryStatus())) {
            throw new BusinessException("配送单状态不合法");
        }
        if (deliveryOrder.getAcceptTime() == null) {
            throw new BusinessException("配送员尚未接单");
        }

        deliveryOrder.setDeliveryStatus(DELIVERY_STATUS_DELIVERING);
        deliveryOrder.setPickupTime(LocalDateTime.now());
        deliveryOrderMapper.updateById(deliveryOrder);

        Order order = requireOrder(deliveryOrder.getOrderId());
        if (!Integer.valueOf(ORDER_STATUS_PAID).equals(order.getOrderStatus())
            && !Integer.valueOf(ORDER_STATUS_DELIVERING).equals(order.getOrderStatus())) {
            throw new BusinessException("订单状态不合法");
        }
        order.setOrderStatus(ORDER_STATUS_DELIVERING);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(order);

        addTracking(deliveryOrder.getId(), "商品已取货，正在配送中");
        log.info("Delivery started: deliveryOrderId={}", deliveryOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startDeliveryByOrderId(Long currentUserId, boolean currentUserIsAdmin, Long orderId) {
        DeliveryOrder deliveryOrder = requireDeliveryOrderByOrderId(orderId);
        verifyDeliveryOperator(deliveryOrder, currentUserId, currentUserIsAdmin);
        startDelivery(deliveryOrder.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDelivery(Long deliveryOrderId) {
        DeliveryOrder deliveryOrder = requireDeliveryOrder(deliveryOrderId);
        if (!Integer.valueOf(DELIVERY_STATUS_DELIVERING).equals(deliveryOrder.getDeliveryStatus())) {
            throw new BusinessException("配送单状态不合法");
        }

        deliveryOrder.setDeliveryStatus(DELIVERY_STATUS_DELIVERED);
        deliveryOrder.setDeliveredTime(LocalDateTime.now());
        deliveryOrderMapper.updateById(deliveryOrder);

        addTracking(deliveryOrder.getId(), "订单已送达，配送完成");

        if (deliveryOrder.getDeliverymanId() != null) {
            DeliveryStaff staff = staffMapper.selectById(deliveryOrder.getDeliverymanId());
            if (staff != null) {
                staff.setStatus("IDLE");
                staff.setTodayOrders(defaultInt(staff.getTodayOrders()) + 1);
                staff.setTotalOrders(defaultInt(staff.getTotalOrders()) + 1);
                staffMapper.updateById(staff);
            }
        }

        log.info("Delivery completed: deliveryOrderId={}", deliveryOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDeliveryByOrderId(Long currentUserId, boolean currentUserIsAdmin, Long orderId) {
        DeliveryOrder deliveryOrder = requireDeliveryOrderByOrderId(orderId);
        verifyDeliveryOperator(deliveryOrder, currentUserId, currentUserIsAdmin);
        completeDelivery(deliveryOrder.getId());
    }

    @Override
    public DeliveryOrderVO getDeliveryDetail(Long userId, Long orderId) {
        Order order = validateOrderAccess(userId, orderId);
        DeliveryOrder deliveryOrder = getDeliveryOrderByOrderId(orderId);
        if (deliveryOrder == null) {
            return null;
        }

        DeliveryOrderVO vo = BeanUtil.copyProperties(deliveryOrder, DeliveryOrderVO.class);
        vo.setOrderNo(order.getOrderNo());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setDeliveryStatusText(getDeliveryStatusText(deliveryOrder.getDeliveryStatus()));
        return vo;
    }

    @Override
    public List<DeliveryTaskVO> getDeliveryTasks(Long currentUserId, boolean currentUserIsAdmin, Integer deliveryStatus) {
        LambdaQueryWrapper<DeliveryOrder> wrapper = new LambdaQueryWrapper<>();

        if (currentUserIsAdmin) {
            applyTaskStatusFilter(wrapper, deliveryStatus, true);
        } else {
            DeliveryStaff staff = requireStaffByUserId(currentUserId);
            wrapper.eq(DeliveryOrder::getDeliverymanId, staff.getId());
            applyTaskStatusFilter(wrapper, deliveryStatus, false);
        }

        wrapper.orderByDesc(DeliveryOrder::getId);

        List<DeliveryOrder> deliveryOrders = deliveryOrderMapper.selectList(wrapper);
        if (deliveryOrders.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> orderIds = new LinkedHashSet<>();
        LinkedHashSet<Long> staffIds = new LinkedHashSet<>();
        for (DeliveryOrder deliveryOrder : deliveryOrders) {
            if (deliveryOrder.getOrderId() != null) {
                orderIds.add(deliveryOrder.getOrderId());
            }
            if (deliveryOrder.getDeliverymanId() != null) {
                staffIds.add(deliveryOrder.getDeliverymanId());
            }
        }

        Map<Long, Order> orderMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<Order> orders = orderMapper.selectBatchIds(new ArrayList<>(orderIds));
            for (Order order : orders) {
                orderMap.put(order.getId(), order);
            }
        }

        Map<Long, DeliveryStaff> staffMap = new HashMap<>();
        if (!staffIds.isEmpty()) {
            List<DeliveryStaff> staffs = staffMapper.selectBatchIds(new ArrayList<>(staffIds));
            for (DeliveryStaff staff : staffs) {
                staffMap.put(staff.getId(), staff);
            }
        }

        List<DeliveryTaskVO> tasks = new ArrayList<>(deliveryOrders.size());
        for (DeliveryOrder deliveryOrder : deliveryOrders) {
            DeliveryTaskVO task = BeanUtil.copyProperties(deliveryOrder, DeliveryTaskVO.class);
            task.setDeliveryStatusText(getDeliveryTaskStatusText(deliveryOrder));

            Order order = orderMap.get(deliveryOrder.getOrderId());
            if (order != null) {
                task.setOrderNo(order.getOrderNo());
                task.setReceiverName(order.getReceiverName());
                task.setReceiverPhone(order.getReceiverPhone());
                task.setReceiverAddress(order.getReceiverAddress());
            }

            if (deliveryOrder.getDeliverymanId() != null) {
                DeliveryStaff staff = staffMap.get(deliveryOrder.getDeliverymanId());
                if (staff != null) {
                    task.setDeliverymanName(staff.getName());
                    task.setDeliverymanPhone(staff.getPhone());
                }
            }

            tasks.add(task);
        }

        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportException(Long deliveryId, Long reporterId, Integer exceptionType, String exceptionDesc) {
        DeliveryOrder deliveryOrder = requireDeliveryOrder(deliveryId);
        DeliveryException exception = new DeliveryException();
        exception.setDeliveryId(deliveryId);
        exception.setOrderId(deliveryOrder.getOrderId());
        exception.setExceptionType(exceptionType);
        exception.setExceptionDesc(exceptionDesc);
        exception.setReportedBy(reporterId);
        exception.setReportTime(LocalDateTime.now());
        exception.setHandleStatus(0);
        exceptionMapper.insert(exception);

        deliveryOrder.setHasException(1);
        deliveryOrderMapper.updateById(deliveryOrder);

        addTracking(deliveryId, "配送异常已上报：" + exceptionDesc);
        log.info("Delivery exception reported: deliveryId={}, type={}", deliveryId, exceptionType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleException(Long exceptionId, Long handlerId, String handleResult) {
        DeliveryException exception = exceptionMapper.selectById(exceptionId);
        if (exception == null) {
            throw new BusinessException("异常记录不存在");
        }
        exception.setHandlerId(handlerId);
        exception.setHandleTime(LocalDateTime.now());
        exception.setHandleResult(handleResult);
        exception.setHandleStatus(2);
        exceptionMapper.updateById(exception);

        DeliveryOrder deliveryOrder = deliveryOrderMapper.selectById(exception.getDeliveryId());
        if (deliveryOrder != null) {
            boolean hasUnhandled = exceptionMapper.selectCount(
                    new LambdaQueryWrapper<DeliveryException>()
                            .eq(DeliveryException::getDeliveryId, deliveryOrder.getId())
                            .ne(DeliveryException::getId, exceptionId)
                            .in(DeliveryException::getHandleStatus, 0, 1)
            ) > 0;
            if (!hasUnhandled) {
                deliveryOrder.setHasException(0);
                deliveryOrderMapper.updateById(deliveryOrder);
            }
        }

        addTracking(exception.getDeliveryId(), "配送异常已处理：" + handleResult);
        log.info("Delivery exception handled: exceptionId={}", exceptionId);
    }

    @Override
    public IPage<DeliveryException> getExceptionPage(Integer handleStatus, int pageNum, int pageSize) {
        LambdaQueryWrapper<DeliveryException> wrapper = new LambdaQueryWrapper<>();
        if (handleStatus != null) {
            wrapper.eq(DeliveryException::getHandleStatus, handleStatus);
        }
        wrapper.orderByDesc(DeliveryException::getCreateTime);
        return exceptionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<DeliveryTracking> getDeliveryTracking(Long userId, Long orderId) {
        validateOrderAccess(userId, orderId);
        DeliveryOrder deliveryOrder = getDeliveryOrderByOrderId(orderId);
        if (deliveryOrder == null) {
            return List.of();
        }

        List<DeliveryTracking> trackingList = trackingMapper.selectList(
                new LambdaQueryWrapper<DeliveryTracking>()
                        .eq(DeliveryTracking::getDeliveryOrderId, deliveryOrder.getId())
                        .orderByAsc(DeliveryTracking::getCreateTime)
        );
        trackingList.forEach(this::fillTrackingLocation);
        return trackingList;
    }

    @Override
    public void assertDeliveryAccess(Long userId, Long orderId) {
        validateOrderAccess(userId, orderId);
    }

    private void addTracking(Long deliveryOrderId, String desc) {
        DeliveryTracking tracking = new DeliveryTracking();
        tracking.setDeliveryOrderId(deliveryOrderId);
        tracking.setTrackDesc(desc);
        trackingMapper.insert(tracking);
    }

    private void fillTrackingLocation(DeliveryTracking tracking) {
        if (tracking.getLongitude() == null || tracking.getLatitude() == null) {
            return;
        }
        tracking.setLocation(formatCoordinate(tracking.getLongitude()) + ", " + formatCoordinate(tracking.getLatitude()));
    }

    private void normalizeDeliveryMethod(DeliveryMethod method) {
        if (method.getMethodCode() == null && method.getId() != null) {
            method.setMethodCode("DELIVERY_" + method.getId());
        }
        if (method.getEstimatedTime() == null) {
            method.setEstimatedTime(30);
        }
        if (method.getSortOrder() == null && method.getId() != null) {
            method.setSortOrder(method.getId().intValue());
        }
    }

    private String formatCoordinate(BigDecimal coordinate) {
        return coordinate.stripTrailingZeros().toPlainString();
    }

    private DeliveryOrder getDeliveryOrderByOrderId(Long orderId) {
        return deliveryOrderMapper.selectOne(
                new LambdaQueryWrapper<DeliveryOrder>()
                        .eq(DeliveryOrder::getOrderId, orderId)
        );
    }

    private DeliveryOrder requireDeliveryOrderByOrderId(Long orderId) {
        DeliveryOrder deliveryOrder = getDeliveryOrderByOrderId(orderId);
        if (deliveryOrder == null) {
            throw new BusinessException("配送单不存在");
        }
        return deliveryOrder;
    }

    private DeliveryOrder requireDeliveryOrder(Long deliveryOrderId) {
        DeliveryOrder deliveryOrder = deliveryOrderMapper.selectById(deliveryOrderId);
        if (deliveryOrder == null) {
            throw new BusinessException("配送单不存在");
        }
        return deliveryOrder;
    }

    private DeliveryStaff requireStaff(Long staffId) {
        DeliveryStaff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(ResultCode.DELIVERY_STAFF_NOT_EXIST);
        }
        return staff;
    }

    private DeliveryStaff findAvailableStaff() {
        List<DeliveryStaff> idleStaff = staffMapper.selectList(
                new LambdaQueryWrapper<DeliveryStaff>()
                        .eq(DeliveryStaff::getEnabled, 1)
                        .eq(DeliveryStaff::getStatus, "IDLE")
                        .orderByAsc(DeliveryStaff::getTodayOrders)
                        .orderByDesc(DeliveryStaff::getRating)
                        .last("LIMIT 1")
        );
        if (!idleStaff.isEmpty()) {
            return idleStaff.get(0);
        }
        return null;
    }

    private Order requireOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private String generateDeliveryNo() {
        return "DEL" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss")
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    private String getDeliveryStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case DELIVERY_STATUS_PENDING -> "待分配配送员";
            case DELIVERY_STATUS_ASSIGNED -> "已分配/待取货";
            case DELIVERY_STATUS_DELIVERING -> "配送中";
            case DELIVERY_STATUS_DELIVERED -> "已送达";
            case 5 -> "配送异常";
            default -> "未知";
        };
    }

    private String getDeliveryTaskStatusText(DeliveryOrder deliveryOrder) {
        if (deliveryOrder == null) {
            return "未知";
        }

        if (Integer.valueOf(DELIVERY_STATUS_ASSIGNED).equals(deliveryOrder.getDeliveryStatus())) {
            return deliveryOrder.getAcceptTime() == null ? "待接单" : "待取货";
        }

        return getDeliveryStatusText(deliveryOrder.getDeliveryStatus());
    }

    private void applyTaskStatusFilter(
            LambdaQueryWrapper<DeliveryOrder> wrapper,
            Integer deliveryStatus,
            boolean includePendingTasks
    ) {
        if (deliveryStatus != null) {
            wrapper.eq(DeliveryOrder::getDeliveryStatus, deliveryStatus);
            return;
        }

        if (includePendingTasks) {
            wrapper.in(
                DeliveryOrder::getDeliveryStatus,
                DELIVERY_STATUS_PENDING,
                DELIVERY_STATUS_ASSIGNED,
                DELIVERY_STATUS_DELIVERING,
                DELIVERY_STATUS_DELIVERED
            );
            return;
        }

        wrapper.in(
            DeliveryOrder::getDeliveryStatus,
            DELIVERY_STATUS_ASSIGNED,
            DELIVERY_STATUS_DELIVERING,
            DELIVERY_STATUS_DELIVERED
        );
    }

    private Order validateOrderAccess(Long userId, Long orderId) {
        Order order = requireOrder(orderId);
        if (SecurityUtils.isAdmin() || (userId != null && userId.equals(order.getUserId()))) {
            return order;
        }

        DeliveryOrder deliveryOrder = getDeliveryOrderByOrderId(orderId);
        DeliveryStaff staff = getStaffByUserId(userId);
        if (deliveryOrder != null
            && staff != null
            && deliveryOrder.getDeliverymanId() != null
            && deliveryOrder.getDeliverymanId().equals(staff.getId())) {
            return order;
        }

        throw new BusinessException("无权限查看该订单配送信息");
    }

    private DeliveryStaff getStaffByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return staffMapper.selectOne(
            new LambdaQueryWrapper<DeliveryStaff>()
                .eq(DeliveryStaff::getUserId, userId)
                .last("LIMIT 1")
        );
    }

    private DeliveryStaff requireStaffByUserId(Long userId) {
        DeliveryStaff staff = getStaffByUserId(userId);
        if (staff == null) {
            throw new BusinessException("当前账号未绑定配送员");
        }
        if (!Integer.valueOf(1).equals(staff.getEnabled())) {
            throw new BusinessException("配送员已禁用");
        }
        return staff;
    }

    private void verifyDeliveryOperator(DeliveryOrder deliveryOrder, Long currentUserId, boolean currentUserIsAdmin) {
        if (currentUserIsAdmin) {
            return;
        }

        DeliveryStaff staff = requireStaffByUserId(currentUserId);
        if (deliveryOrder.getDeliverymanId() == null || !deliveryOrder.getDeliverymanId().equals(staff.getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
