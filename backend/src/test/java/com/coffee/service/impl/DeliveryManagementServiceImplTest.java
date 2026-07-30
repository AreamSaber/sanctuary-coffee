package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryOrder;
import com.coffee.entity.DeliveryRegion;
import com.coffee.entity.DeliveryStaff;
import com.coffee.mapper.DeliveryMethodMapper;
import com.coffee.mapper.DeliveryOrderMapper;
import com.coffee.mapper.DeliveryRegionMapper;
import com.coffee.mapper.DeliveryStaffMapper;
import com.coffee.service.DeliveryService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryManagementServiceImplTest {

    @Mock private DeliveryMethodMapper methodMapper;
    @Mock private DeliveryRegionMapper regionMapper;
    @Mock private DeliveryStaffMapper staffMapper;
    @Mock private DeliveryOrderMapper deliveryOrderMapper;
    @Mock private DeliveryService deliveryService;

    private DeliveryManagementServiceImpl deliveryManagementService;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisConfiguration cfg = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
        initTableInfo(assistant, DeliveryMethod.class);
        initTableInfo(assistant, DeliveryRegion.class);
        initTableInfo(assistant, DeliveryStaff.class);
        initTableInfo(assistant, DeliveryOrder.class);
    }

    private static void initTableInfo(MapperBuilderAssistant assistant, Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) == null) {
            TableInfoHelper.initTableInfo(assistant, entityClass);
        }
    }

    @BeforeEach
    void setUp() {
        deliveryManagementService = new DeliveryManagementServiceImpl(
            methodMapper,
            regionMapper,
            staffMapper,
            deliveryOrderMapper,
            deliveryService
        );
    }

    @Test
    void autoAssignStaffBindsSelectedStaffToDeliveryOrder() {
        DeliveryStaff busyCandidate = staff(1L, 5L, "IDLE", 6);
        DeliveryStaff selected = staff(2L, 5L, "IDLE", 1);
        when(staffMapper.selectList(any())).thenReturn(List.of(busyCandidate, selected));
        when(regionMapper.selectBatchIds(List.of(5L))).thenReturn(List.of(region(5L, "主城区")));
        when(deliveryOrderMapper.selectCount(any())).thenReturn(0L, 0L);

        Long staffId = deliveryManagementService.autoAssignStaff(100L, 5L);

        assertEquals(2L, staffId);
        verify(deliveryService).assignDelivery(100L, 2L);
        verify(staffMapper, never()).updateById(any(DeliveryStaff.class));
    }

    @Test
    void autoAssignStaffThrowsWhenNoAvailableStaff() {
        when(staffMapper.selectList(any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> deliveryManagementService.autoAssignStaff(100L, 5L));

        verify(deliveryService, never()).assignDelivery(any(), any());
    }

    private DeliveryStaff staff(Long id, Long regionId, String status, Integer todayOrders) {
        DeliveryStaff staff = new DeliveryStaff();
        staff.setId(id);
        staff.setUserId(1000L + id);
        staff.setRegionId(regionId);
        staff.setName("staff-" + id);
        staff.setStatus(status);
        staff.setEnabled(1);
        staff.setTodayOrders(todayOrders);
        staff.setRating(5.0);
        return staff;
    }

    private DeliveryRegion region(Long id, String name) {
        DeliveryRegion region = new DeliveryRegion();
        region.setId(id);
        region.setRegionName(name);
        return region;
    }
}
