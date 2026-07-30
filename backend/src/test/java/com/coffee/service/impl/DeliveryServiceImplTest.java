package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.*;
import com.coffee.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock private DeliveryMethodMapper deliveryMethodMapper;
    @Mock private DeliveryOrderMapper deliveryOrderMapper;
    @Mock private DeliveryTrackingMapper trackingMapper;
    @Mock private DeliveryStaffMapper staffMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private DeliveryExceptionMapper exceptionMapper;
    @InjectMocks private DeliveryServiceImpl deliveryService;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisConfiguration cfg = new MybatisConfiguration();
        MapperBuilderAssistant asst = new MapperBuilderAssistant(cfg, "");
        TableInfoHelper.initTableInfo(asst, DeliveryOrder.class);
        TableInfoHelper.initTableInfo(asst, DeliveryStaff.class);
        TableInfoHelper.initTableInfo(asst, DeliveryTracking.class);
        TableInfoHelper.initTableInfo(asst, Order.class);
    }

    @Test
    void createDeliveryOrderSkipsWhenAlreadyExists() {
        Order order = paidOrder(10L);
        DeliveryOrder existing = new DeliveryOrder();
        existing.setId(1L); existing.setOrderId(10L);

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(deliveryOrderMapper.selectOne(any())).thenReturn(existing);

        deliveryService.createDeliveryOrder(10L);

        verify(deliveryOrderMapper, never()).insert(any());
    }

    @Test
    void assignDeliveryAssignsIdleStaff() {
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(1L); deliveryOrder.setOrderId(10L); deliveryOrder.setDeliveryStatus(1);
        DeliveryStaff staff = new DeliveryStaff();
        staff.setId(1L); staff.setName("zhang"); staff.setEnabled(1); staff.setStatus("IDLE");

        when(deliveryOrderMapper.selectOne(any())).thenReturn(deliveryOrder);
        when(staffMapper.selectList(any())).thenReturn(List.of(staff));

        deliveryService.assignDelivery(10L, null);

        verify(deliveryOrderMapper).updateById(deliveryOrder);
        assertEquals(1L, deliveryOrder.getDeliverymanId());
        assertEquals(2, deliveryOrder.getDeliveryStatus());
        verify(staffMapper).updateById(staff);
        assertEquals("BUSY", staff.getStatus());
        verify(trackingMapper).insert(any(DeliveryTracking.class));
    }

    @Test
    void acceptDeliveryRejectsIfAlreadyAccepted() {
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(1L); deliveryOrder.setOrderId(10L);
        deliveryOrder.setDeliveryStatus(2); deliveryOrder.setAcceptTime(LocalDateTime.now());
        DeliveryStaff staff = new DeliveryStaff();
        staff.setId(1L); staff.setEnabled(1);

        when(deliveryOrderMapper.selectById(1L)).thenReturn(deliveryOrder);
        when(staffMapper.selectById(1L)).thenReturn(staff);

        assertThrows(BusinessException.class,
            () -> deliveryService.acceptDelivery(1L, 1L));
    }

    @Test
    void completeDeliveryUpdatesStaffStats() {
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(1L); deliveryOrder.setOrderId(10L);
        deliveryOrder.setDeliveryStatus(3); deliveryOrder.setDeliverymanId(1L);
        DeliveryStaff staff = new DeliveryStaff();
        staff.setId(1L); staff.setStatus("BUSY"); staff.setTodayOrders(5); staff.setTotalOrders(100);

        when(deliveryOrderMapper.selectById(1L)).thenReturn(deliveryOrder);
        when(staffMapper.selectById(1L)).thenReturn(staff);

        deliveryService.completeDelivery(1L);

        assertEquals(4, deliveryOrder.getDeliveryStatus());
        assertNotNull(deliveryOrder.getDeliveredTime());
        assertEquals("IDLE", staff.getStatus());
        assertEquals(6, staff.getTodayOrders());
        assertEquals(101, staff.getTotalOrders());
        verify(staffMapper).updateById(staff);
        verify(trackingMapper).insert(any(DeliveryTracking.class));
    }

    @Test
    void reportExceptionCreatesExceptionRecord() {
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(1L); deliveryOrder.setOrderId(10L);

        when(deliveryOrderMapper.selectById(1L)).thenReturn(deliveryOrder);

        deliveryService.reportException(1L, 99L, 1, "address error");

        verify(exceptionMapper).insert(any(DeliveryException.class));
        verify(deliveryOrderMapper).updateById(deliveryOrder);
        assertEquals(1, deliveryOrder.getHasException());
    }

    @Test
    void handleExceptionClearsExceptionFlagWhenAllHandled() {
        DeliveryException exception = new DeliveryException();
        exception.setId(1L); exception.setDeliveryId(1L); exception.setHandleStatus(0);
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(1L); deliveryOrder.setHasException(1);

        when(exceptionMapper.selectById(1L)).thenReturn(exception);
        when(deliveryOrderMapper.selectById(1L)).thenReturn(deliveryOrder);
        when(exceptionMapper.selectCount(any())).thenReturn(0L);

        deliveryService.handleException(1L, 99L, "resolved");

        assertEquals(2, exception.getHandleStatus());
        assertNotNull(exception.getHandleTime());
        assertEquals("resolved", exception.getHandleResult());
        assertEquals(0, deliveryOrder.getHasException());
        verify(trackingMapper).insert(any(DeliveryTracking.class));
    }

    @Test
    void assertDeliveryAccessAllowsOrderOwner() {
        Order order = paidOrder(10L);
        order.setUserId(100L);

        when(orderMapper.selectById(10L)).thenReturn(order);

        assertDoesNotThrow(() -> deliveryService.assertDeliveryAccess(100L, 10L));
        verify(deliveryOrderMapper, never()).selectOne(any());
        verify(staffMapper, never()).selectOne(any());
    }

    @Test
    void assertDeliveryAccessRejectsUnrelatedUser() {
        Order order = paidOrder(10L);
        order.setUserId(100L);

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(deliveryOrderMapper.selectOne(any())).thenReturn(null);
        when(staffMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> deliveryService.assertDeliveryAccess(200L, 10L)
        );
        assertEquals("无权限查看该订单配送信息", exception.getMessage());
    }

    @Test
    void assertDeliveryAccessAllowsAssignedDeliveryStaff() {
        Order order = paidOrder(10L);
        order.setUserId(100L);
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setId(1L);
        deliveryOrder.setOrderId(10L);
        deliveryOrder.setDeliverymanId(2L);
        DeliveryStaff staff = new DeliveryStaff();
        staff.setId(2L);
        staff.setUserId(200L);

        when(orderMapper.selectById(10L)).thenReturn(order);
        when(deliveryOrderMapper.selectOne(any())).thenReturn(deliveryOrder);
        when(staffMapper.selectOne(any())).thenReturn(staff);

        assertDoesNotThrow(() -> deliveryService.assertDeliveryAccess(200L, 10L));
    }

    private Order paidOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("ORD" + id);
        order.setOrderStatus(2);
        return order;
    }
}
