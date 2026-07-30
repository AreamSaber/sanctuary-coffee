package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.CouponDTO;
import com.coffee.entity.Coupon;
import com.coffee.entity.UserCoupon;
import com.coffee.mapper.CouponMapper;
import com.coffee.mapper.UserCouponMapper;
import com.coffee.service.CouponService;
import com.coffee.vo.CouponStatsVO;
import com.coffee.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Coupon service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final int COUPON_TYPE_FULL_REDUCTION = 1;
    private static final int COUPON_TYPE_DISCOUNT = 2;
    private static final int COUPON_TYPE_FREE_SHIPPING = 3;

    private static final int DISCOUNT_TYPE_AMOUNT = 1;
    private static final int DISCOUNT_TYPE_RATE = 2;
    private static final int USER_COUPON_STATUS_UNUSED = 0;
    private static final int USER_COUPON_STATUS_USED = 1;
    private static final int USER_COUPON_STATUS_EXPIRED = 2;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public IPage<CouponVO> getAllCoupons(Integer pageNum, Integer pageSize, String name, Integer type, Integer status) {
        Page<Coupon> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            wrapper.like(Coupon::getCouponName, name);
        }
        if (type != null) {
            wrapper.eq(Coupon::getCouponType, type);
        }
        if (status != null) {
            wrapper.eq(Coupon::getStatus, status);
        }
        wrapper.orderByDesc(Coupon::getCreateTime);

        IPage<Coupon> couponPage = couponMapper.selectPage(page, wrapper);

        IPage<CouponVO> voPage = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        voPage.setRecords(couponPage.getRecords().stream()
            .map(this::convertToSimpleVO)
            .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public IPage<CouponVO> getCouponPage(Long userId, Integer pageNum, Integer pageSize) {
        Page<Coupon> page = new Page<>(pageNum, pageSize);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getStatus, 1)
            .le(Coupon::getStartTime, now)
            .ge(Coupon::getEndTime, now)
            .orderByDesc(Coupon::getCreateTime);

        IPage<Coupon> couponPage = couponMapper.selectPage(page, wrapper);

        IPage<CouponVO> voPage = new Page<>(couponPage.getCurrent(), couponPage.getSize(), couponPage.getTotal());
        voPage.setRecords(couponPage.getRecords().stream()
            .map(coupon -> convertToAvailableVO(coupon, userId))
            .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long userId, Long couponId) {
        Coupon coupon = requireCoupon(couponId);
        LocalDateTime now = LocalDateTime.now();

        if (!isCouponActive(coupon, now)) {
            throw new BusinessException("优惠券未生效");
        }

        long userReceivedCount = userCouponMapper.selectCount(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId)
        );
        if (coupon.getPerLimit() != null && userReceivedCount >= coupon.getPerLimit()) {
            throw new BusinessException("已达到该优惠券领取上限");
        }

        int updated = couponMapper.increaseReceivedQuantityIfAvailable(couponId);
        if (updated == 0) {
            throw new BusinessException("优惠券已领完");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCouponCode(generateCouponCode(couponId, userId));
        userCoupon.setStatus(USER_COUPON_STATUS_UNUSED);
        userCoupon.setReceiveTime(now);
        userCoupon.setExpireTime(resolveExpireTime(coupon, now));
        userCouponMapper.insert(userCoupon);

        log.info("Coupon received: userId={}, couponId={}, userCouponId={}", userId, couponId, userCoupon.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CouponVO> getUserCoupons(Long userId, Integer status) {
        refreshExpiredUserCoupons(userId, LocalDateTime.now());

        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
            .eq(status != null, UserCoupon::getStatus, status)
            .orderByDesc(UserCoupon::getReceiveTime);

        return userCouponMapper.selectList(wrapper).stream()
            .map(this::convertToUserCouponVO)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCoupon(CouponDTO couponDTO) {
        Coupon coupon = new Coupon();
        applyCouponDTO(couponDTO, coupon);
        coupon.setReceivedQuantity(0);
        coupon.setStatus(1);

        couponMapper.insert(coupon);
        log.info("Coupon created: couponId={}, couponName={}", coupon.getId(), coupon.getCouponName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoupon(Long id, CouponDTO couponDTO) {
        Coupon coupon = requireCoupon(id);
        applyCouponDTO(couponDTO, coupon);
        couponMapper.updateById(coupon);

        log.info("Coupon updated: couponId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoupon(Long id) {
        Coupon coupon = requireCoupon(id);
        Long userCouponCount = userCouponMapper.selectCount(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCouponId, id)
        );
        if (userCouponCount > 0) {
            throw new BusinessException("该优惠券已有用户领取记录，无法删除");
        }

        couponMapper.deleteById(id);
        log.info("Coupon deleted: couponId={}, couponName={}", id, coupon.getCouponName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Coupon coupon = requireCoupon(id);
        coupon.setStatus(status);
        couponMapper.updateById(coupon);
        log.info("Coupon status updated: couponId={}, status={}", id, status);
    }

    @Override
    public List<CouponStatsVO> getCouponStats(Integer couponType, String startTime, String endTime) {
        LambdaQueryWrapper<Coupon> couponWrapper = new LambdaQueryWrapper<>();
        couponWrapper.eq(couponType != null, Coupon::getCouponType, couponType);
        couponWrapper.orderByDesc(Coupon::getCreateTime);

        List<Coupon> coupons = couponMapper.selectList(couponWrapper);
        if (coupons.isEmpty()) {
            return List.of();
        }

        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LambdaQueryWrapper<UserCoupon> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.in(UserCoupon::getCouponId, couponIds);
        if (startTime != null && !startTime.isBlank()) {
            ucWrapper.ge(UserCoupon::getReceiveTime, LocalDateTime.parse(startTime, formatter));
        }
        if (endTime != null && !endTime.isBlank()) {
            ucWrapper.le(UserCoupon::getReceiveTime, LocalDateTime.parse(endTime, formatter));
        }

        List<UserCoupon> allUserCoupons = userCouponMapper.selectList(ucWrapper);

        Map<Long, Long> receivedMap = allUserCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        Map<Long, Long> usedMap = allUserCoupons.stream()
                .filter(uc -> Integer.valueOf(USER_COUPON_STATUS_USED).equals(uc.getStatus()))
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));

        List<CouponStatsVO> stats = new ArrayList<>();
        for (Coupon coupon : coupons) {
            CouponStatsVO vo = new CouponStatsVO();
            vo.setCouponId(coupon.getId());
            vo.setCouponName(coupon.getCouponName());
            vo.setCouponType(coupon.getCouponType());
            vo.setCouponTypeText(getCouponTypeText(coupon.getCouponType()));

            long received = receivedMap.getOrDefault(coupon.getId(), 0L);
            long used = usedMap.getOrDefault(coupon.getId(), 0L);

            vo.setTotalReceived((int) received);
            vo.setTotalUsed((int) used);

            if (received > 0) {
                vo.setRedemptionRate(BigDecimal.valueOf(used)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(received), 2, RoundingMode.HALF_UP));
            } else {
                vo.setRedemptionRate(BigDecimal.ZERO);
            }

            stats.add(vo);
        }

        return stats;
    }

    private CouponVO convertToAvailableVO(Coupon coupon, Long userId) {
        CouponVO vo = convertToSimpleVO(coupon);
        if (userId != null) {
            long count = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getCouponId, coupon.getId())
            );
            vo.setReceived(count > 0);
            vo.setReceivedNum((int) count);
        }
        return vo;
    }

    private CouponVO convertToUserCouponVO(UserCoupon userCoupon) {
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            return null;
        }

        CouponVO vo = convertToSimpleVO(coupon);
        vo.setId(userCoupon.getId());
        vo.setCouponId(coupon.getId());
        vo.setCouponCode(userCoupon.getCouponCode());
        vo.setStatus(userCoupon.getStatus());
        vo.setExpireTime(userCoupon.getExpireTime());
        vo.setReceived(true);
        vo.setReceivedNum(1);
        return vo;
    }

    private CouponVO convertToSimpleVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setCouponId(coupon.getId());
        vo.setStatus(coupon.getStatus());
        vo.setCouponName(coupon.getCouponName());
        vo.setCouponType(coupon.getCouponType());
        vo.setCouponTypeText(getCouponTypeText(coupon.getCouponType()));
        vo.setMinAmount(coupon.getMinAmount());
        vo.setRemainCount(getSafeTotalQuantity(coupon) - getSafeReceivedQuantity(coupon));
        vo.setLimitPerUser(coupon.getPerLimit());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        vo.setValidDays(coupon.getValidDays());

        if (coupon.getDiscountType() == DISCOUNT_TYPE_AMOUNT) {
            vo.setDiscountAmount(coupon.getDiscountValue());
        } else if (coupon.getDiscountType() == DISCOUNT_TYPE_RATE) {
            vo.setDiscountRate(coupon.getDiscountValue());
        }
        return vo;
    }

    private void applyCouponDTO(CouponDTO couponDTO, Coupon coupon) {
        validateCouponDTO(couponDTO);

        coupon.setCouponName(couponDTO.getCouponName().trim());
        coupon.setCouponType(couponDTO.getCouponType());
        coupon.setMinAmount(defaultBigDecimal(couponDTO.getMinAmount()));
        coupon.setTotalQuantity(couponDTO.getTotalCount());
        coupon.setPerLimit(couponDTO.getLimitPerUser() == null ? 1 : couponDTO.getLimitPerUser());
        coupon.setStartTime(couponDTO.getStartTime());
        coupon.setEndTime(couponDTO.getEndTime());
        coupon.setValidDays(couponDTO.getValidDays());
        coupon.setUseScope(couponDTO.getUseScope() == null ? 1 : couponDTO.getUseScope());
        coupon.setScopeIds(couponDTO.getScopeIds());
        coupon.setDescription(couponDTO.getDescription());

        if (couponDTO.getCouponType() == COUPON_TYPE_DISCOUNT) {
            coupon.setDiscountType(DISCOUNT_TYPE_RATE);
            coupon.setDiscountValue(couponDTO.getDiscountRate());
        } else if (couponDTO.getCouponType() == COUPON_TYPE_FREE_SHIPPING) {
            coupon.setDiscountType(DISCOUNT_TYPE_AMOUNT);
            coupon.setDiscountValue(defaultBigDecimal(couponDTO.getDiscountAmount()));
        } else {
            coupon.setDiscountType(DISCOUNT_TYPE_AMOUNT);
            coupon.setDiscountValue(couponDTO.getDiscountAmount());
        }
    }

    private void validateCouponDTO(CouponDTO couponDTO) {
        if (couponDTO.getTotalCount() == null || couponDTO.getTotalCount() <= 0) {
            throw new BusinessException("优惠券总量设置无效");
        }
        if (couponDTO.getLimitPerUser() != null && couponDTO.getLimitPerUser() <= 0) {
            throw new BusinessException("每人限领数量设置无效");
        }
        if (!couponDTO.getStartTime().isBefore(couponDTO.getEndTime())) {
            throw new BusinessException("优惠券有效期设置无效");
        }

        switch (couponDTO.getCouponType()) {
            case COUPON_TYPE_FULL_REDUCTION -> {
                if (couponDTO.getDiscountAmount() == null || couponDTO.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请设置优惠金额");
                }
            }
            case COUPON_TYPE_DISCOUNT -> {
                if (couponDTO.getDiscountRate() == null
                    || couponDTO.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                    || couponDTO.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessException("折扣率必须在0到1之间");
                }
            }
            case COUPON_TYPE_FREE_SHIPPING -> {
                if (couponDTO.getDiscountAmount() != null && couponDTO.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("免邮券金额不能为负数");
                }
            }
            default -> throw new BusinessException("不支持的优惠券类型");
        }
    }

    private Coupon requireCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        return coupon;
    }

    private boolean isCouponActive(Coupon coupon, LocalDateTime now) {
        return coupon.getStatus() != null
            && coupon.getStatus() == 1
            && (coupon.getStartTime() == null || !coupon.getStartTime().isAfter(now))
            && (coupon.getEndTime() == null || !coupon.getEndTime().isBefore(now));
    }

    private LocalDateTime resolveExpireTime(Coupon coupon, LocalDateTime receiveTime) {
        if (coupon.getValidDays() != null && coupon.getValidDays() > 0) {
            return receiveTime.plusDays(coupon.getValidDays());
        }
        return coupon.getEndTime();
    }

    private int getSafeReceivedQuantity(Coupon coupon) {
        return coupon.getReceivedQuantity() == null ? 0 : coupon.getReceivedQuantity();
    }

    private int getSafeTotalQuantity(Coupon coupon) {
        return coupon.getTotalQuantity() == null ? 0 : coupon.getTotalQuantity();
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void refreshExpiredUserCoupons(Long userId, LocalDateTime now) {
        userCouponMapper.update(null,
            new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_UNUSED)
                .lt(UserCoupon::getExpireTime, now)
                .set(UserCoupon::getStatus, USER_COUPON_STATUS_EXPIRED)
        );
    }

    private String generateCouponCode(Long couponId, Long userId) {
        return "UC" + couponId + userId + System.currentTimeMillis();
    }

    private String getCouponTypeText(Integer type) {
        return switch (type) {
            case COUPON_TYPE_FULL_REDUCTION -> "Full Reduction";
            case COUPON_TYPE_DISCOUNT -> "Discount";
            case COUPON_TYPE_FREE_SHIPPING -> "Free Shipping";
            default -> "Unknown";
        };
    }
}
