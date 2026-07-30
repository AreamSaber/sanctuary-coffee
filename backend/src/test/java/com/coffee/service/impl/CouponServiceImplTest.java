package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.Coupon;
import com.coffee.entity.UserCoupon;
import com.coffee.mapper.CouponMapper;
import com.coffee.mapper.UserCouponMapper;
import com.coffee.vo.CouponVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(Coupon.class);
        initTableInfo(UserCoupon.class);
    }

    @Test
    void receiveCouponUsesAtomicStockGuardBeforeCreatingUserCoupon() {
        Coupon coupon = activeCoupon(10L);
        when(couponMapper.selectById(10L)).thenReturn(coupon);
        when(userCouponMapper.selectCount(anyUserCouponQueryWrapper())).thenReturn(0L);
        when(couponMapper.increaseReceivedQuantityIfAvailable(10L)).thenReturn(1);

        couponService.receiveCoupon(100L, 10L);

        ArgumentCaptor<UserCoupon> userCouponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(couponMapper).increaseReceivedQuantityIfAvailable(10L);
        verify(userCouponMapper).insert(userCouponCaptor.capture());

        UserCoupon userCoupon = userCouponCaptor.getValue();
        assertEquals(100L, userCoupon.getUserId());
        assertEquals(10L, userCoupon.getCouponId());
        assertEquals(0, userCoupon.getStatus());
        assertNotNull(userCoupon.getCouponCode());
        assertNotNull(userCoupon.getReceiveTime());
        assertNotNull(userCoupon.getExpireTime());
    }

    @Test
    void receiveCouponDoesNotCreateUserCouponWhenAtomicStockGuardFails() {
        Coupon coupon = activeCoupon(10L);
        when(couponMapper.selectById(10L)).thenReturn(coupon);
        when(userCouponMapper.selectCount(anyUserCouponQueryWrapper())).thenReturn(0L);
        when(couponMapper.increaseReceivedQuantityIfAvailable(10L)).thenReturn(0);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> couponService.receiveCoupon(100L, 10L)
        );
        assertEquals("优惠券已领完", exception.getMessage());

        verify(couponMapper).increaseReceivedQuantityIfAvailable(10L);
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    void deleteCouponRejectsCouponWithUserReceiveRecords() {
        Coupon coupon = activeCoupon(10L);
        when(couponMapper.selectById(10L)).thenReturn(coupon);
        when(userCouponMapper.selectCount(anyUserCouponQueryWrapper())).thenReturn(1L);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> couponService.deleteCoupon(10L)
        );

        assertEquals(500, exception.getCode());
        assertEquals("该优惠券已有用户领取记录，无法删除", exception.getMessage());
        verify(couponMapper, never()).deleteById(10L);
    }

    @Test
    void deleteCouponAllowsCouponWithoutUserReceiveRecords() {
        Coupon coupon = activeCoupon(10L);
        when(couponMapper.selectById(10L)).thenReturn(coupon);
        when(userCouponMapper.selectCount(anyUserCouponQueryWrapper())).thenReturn(0L);

        couponService.deleteCoupon(10L);

        verify(couponMapper).deleteById(10L);
    }

    @Test
    void getUserCouponsRefreshesExpiredUnusedCouponsBeforeListing() {
        UserCoupon userCoupon = userCoupon(20L, 10L, 100L, 2, LocalDateTime.now().minusDays(1));
        when(userCouponMapper.update(isNull(), anyUserCouponUpdateWrapper())).thenReturn(1);
        when(userCouponMapper.selectList(anyUserCouponQueryWrapper())).thenReturn(List.of(userCoupon));
        when(couponMapper.selectById(10L)).thenReturn(activeCoupon(10L));

        List<CouponVO> coupons = couponService.getUserCoupons(100L, 2);

        assertEquals(1, coupons.size());
        assertEquals(20L, coupons.get(0).getId());
        assertEquals(10L, coupons.get(0).getCouponId());
        assertEquals(2, coupons.get(0).getStatus());
        assertEquals(userCoupon.getExpireTime(), coupons.get(0).getExpireTime());
        verify(userCouponMapper).update(isNull(), anyUserCouponUpdateWrapper());
    }

    private Coupon activeCoupon(Long id) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCouponName("测试优惠券");
        coupon.setCouponType(1);
        coupon.setDiscountType(1);
        coupon.setDiscountValue(new BigDecimal("10.00"));
        coupon.setMinAmount(BigDecimal.ZERO);
        coupon.setTotalQuantity(100);
        coupon.setReceivedQuantity(0);
        coupon.setPerLimit(1);
        coupon.setStartTime(LocalDateTime.now().minusDays(1));
        coupon.setEndTime(LocalDateTime.now().plusDays(7));
        coupon.setValidDays(7);
        coupon.setStatus(1);
        return coupon;
    }

    private UserCoupon userCoupon(Long id, Long couponId, Long userId, Integer status, LocalDateTime expireTime) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(id);
        userCoupon.setCouponId(couponId);
        userCoupon.setUserId(userId);
        userCoupon.setCouponCode("UC" + id);
        userCoupon.setStatus(status);
        userCoupon.setReceiveTime(LocalDateTime.now().minusDays(2));
        userCoupon.setExpireTime(expireTime);
        return userCoupon;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<UserCoupon> anyUserCouponQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<UserCoupon> anyUserCouponUpdateWrapper() {
        return any(LambdaUpdateWrapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
