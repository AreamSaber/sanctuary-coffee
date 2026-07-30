package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.MemberBenefitDTO;
import com.coffee.entity.BenefitGrantLog;
import com.coffee.entity.Coupon;
import com.coffee.entity.MemberBenefit;
import com.coffee.entity.MemberBenefitUsage;
import com.coffee.entity.MemberInfo;
import com.coffee.entity.MemberLevel;
import com.coffee.entity.MemberLevelBenefit;
import com.coffee.entity.UserCoupon;
import com.coffee.mapper.BenefitGrantLogMapper;
import com.coffee.mapper.CouponMapper;
import com.coffee.mapper.MemberBenefitMapper;
import com.coffee.mapper.MemberBenefitUsageMapper;
import com.coffee.mapper.MemberInfoMapper;
import com.coffee.mapper.MemberLevelBenefitMapper;
import com.coffee.mapper.MemberLevelMapper;
import com.coffee.mapper.UserCouponMapper;
import com.coffee.service.MemberBenefitService;
import com.coffee.vo.MemberBenefitUsageVO;
import com.coffee.vo.MemberBenefitVO;
import com.coffee.vo.MemberLevelBenefitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会员权益服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBenefitServiceImpl implements MemberBenefitService {

    private static final int TYPE_DISCOUNT = 1;
    private static final int TYPE_POINTS_MULTIPLIER = 2;
    private static final int TYPE_FREE_SHIPPING = 3;
    private static final int TYPE_BIRTHDAY_COUPON = 4;
    private static final int TYPE_EXCLUSIVE_SERVICE = 5;
    private static final int COUPON_TYPE_FULL_REDUCTION = 1;
    private static final int COUPON_DISCOUNT_TYPE_AMOUNT = 1;
    private static final int COUPON_STATUS_ENABLED = 1;
    private static final int USER_COUPON_STATUS_UNUSED = 0;
    private static final int GRANT_STATUS_GRANTED = 1;
    private static final String BIRTHDAY_COUPON_NAME = "生日礼券";
    private static final String BIRTHDAY_COUPON_CODE_PREFIX = "BD";
    private static final int USAGE_STATUS_USED = 1;
    private static final int USAGE_STATUS_ROLLBACK = 2;

    private final MemberBenefitMapper memberBenefitMapper;
    private final MemberBenefitUsageMapper memberBenefitUsageMapper;
    private final MemberLevelBenefitMapper memberLevelBenefitMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final BenefitGrantLogMapper benefitGrantLogMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public IPage<MemberBenefitVO> getBenefitPage(Integer pageNum, Integer pageSize, String keyword, Integer benefitType, Integer status) {
        Page<MemberBenefit> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MemberBenefit> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), query -> query
                .like(MemberBenefit::getBenefitName, keyword.trim())
                .or()
                .like(MemberBenefit::getBenefitCode, keyword.trim()))
            .eq(benefitType != null, MemberBenefit::getBenefitType, benefitType)
            .eq(status != null, MemberBenefit::getStatus, status)
            .orderByDesc(MemberBenefit::getCreateTime);

        IPage<MemberBenefit> benefitPage = memberBenefitMapper.selectPage(page, wrapper);
        Page<MemberBenefitVO> voPage = new Page<>(benefitPage.getCurrent(), benefitPage.getSize(), benefitPage.getTotal());
        voPage.setRecords(benefitPage.getRecords().stream()
            .map(this::toBenefitVO)
            .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<MemberBenefitVO> getActiveBenefits() {
        return memberBenefitMapper.selectList(new LambdaQueryWrapper<MemberBenefit>()
                .eq(MemberBenefit::getStatus, 1)
                .orderByAsc(MemberBenefit::getBenefitType)
                .orderByAsc(MemberBenefit::getId))
            .stream()
            .map(this::toBenefitVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBenefit(MemberBenefitDTO dto) {
        validateBenefitDTO(null, dto);
        MemberBenefit benefit = new MemberBenefit();
        applyBenefitDTO(benefit, dto);
        memberBenefitMapper.insert(benefit);
        log.info("创建会员权益成功: benefitCode={}", benefit.getBenefitCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBenefit(Long id, MemberBenefitDTO dto) {
        MemberBenefit benefit = requireBenefit(id);
        validateBenefitDTO(id, dto);
        applyBenefitDTO(benefit, dto);
        memberBenefitMapper.updateById(benefit);
        log.info("更新会员权益成功: benefitId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBenefit(Long id) {
        MemberBenefit benefit = requireBenefit(id);
        memberBenefitMapper.deleteById(id);
        memberLevelBenefitMapper.delete(new LambdaQueryWrapper<MemberLevelBenefit>()
            .eq(MemberLevelBenefit::getBenefitId, id));
        log.info("删除会员权益成功: benefitId={}, benefitName={}", id, benefit.getBenefitName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("权益状态无效");
        }
        MemberBenefit benefit = requireBenefit(id);
        benefit.setStatus(status);
        memberBenefitMapper.updateById(benefit);
        log.info("更新会员权益状态: benefitId={}, status={}", id, status);
    }

    @Override
    public List<MemberLevelBenefitVO> getLevelBenefitMatrix() {
        List<MemberLevel> levels = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
            .orderByAsc(MemberLevel::getLevelCode));

        return levels.stream()
            .map(level -> {
                MemberLevelBenefitVO vo = new MemberLevelBenefitVO();
                vo.setLevelId(level.getId());
                vo.setLevelName(level.getLevelName());
                vo.setLevelCode(level.getLevelCode());
                vo.setRequiredPoints(level.getRequiredPoints());
                vo.setDiscountRate(level.getDiscountRate());
                vo.setStatus(level.getStatus());
                vo.setBenefits(getBenefitsByLevelId(level.getId()));
                return vo;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<MemberBenefitVO> getBenefitsByLevelId(Long levelId) {
        if (levelId == null) {
            return List.of();
        }

        List<MemberLevelBenefit> bindings = memberLevelBenefitMapper.selectList(new LambdaQueryWrapper<MemberLevelBenefit>()
            .eq(MemberLevelBenefit::getLevelId, levelId)
            .eq(MemberLevelBenefit::getStatus, 1)
            .orderByAsc(MemberLevelBenefit::getSortOrder)
            .orderByAsc(MemberLevelBenefit::getId));

        List<Long> benefitIds = bindings.stream()
            .map(MemberLevelBenefit::getBenefitId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (benefitIds.isEmpty()) {
            return List.of();
        }

        Map<Long, MemberBenefit> benefitMap = memberBenefitMapper.selectList(new LambdaQueryWrapper<MemberBenefit>()
                .in(MemberBenefit::getId, benefitIds)
                .eq(MemberBenefit::getStatus, 1))
            .stream()
            .collect(Collectors.toMap(MemberBenefit::getId, benefit -> benefit));

        return benefitIds.stream()
            .map(benefitMap::get)
            .filter(Objects::nonNull)
            .map(this::toBenefitVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLevelBenefits(Long levelId, List<Long> benefitIds) {
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (level == null) {
            throw new BusinessException(ResultCode.MEMBER_LEVEL_NOT_EXIST);
        }

        List<Long> normalizedBenefitIds = normalizeBenefitIds(benefitIds);
        validateBenefitIds(normalizedBenefitIds);

        memberLevelBenefitMapper.delete(new LambdaQueryWrapper<MemberLevelBenefit>()
            .eq(MemberLevelBenefit::getLevelId, levelId));

        List<MemberLevelBenefit> bindings = new ArrayList<>();
        for (int index = 0; index < normalizedBenefitIds.size(); index++) {
            MemberLevelBenefit binding = new MemberLevelBenefit();
            binding.setLevelId(levelId);
            binding.setBenefitId(normalizedBenefitIds.get(index));
            binding.setSortOrder(index + 1);
            binding.setStatus(1);
            memberLevelBenefitMapper.insert(binding);
            bindings.add(binding);
        }

        log.info("保存会员等级权益成功: levelId={}, benefitCount={}", levelId, bindings.size());
    }

    @Override
    public List<MemberBenefitVO> getUserBenefits(Long userId) {
        MemberInfo memberInfo = memberInfoMapper.selectOne(new LambdaQueryWrapper<MemberInfo>()
            .eq(MemberInfo::getUserId, userId));
        if (memberInfo == null) {
            return List.of();
        }
        return getBenefitsByLevelId(memberInfo.getLevelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordBenefitUsage(Long userId, MemberBenefitVO benefit, String businessType, Long businessId,
                                   BigDecimal effectAmount, Integer effectPoints, String remark) {
        if (userId == null || benefit == null || benefit.getId() == null || !StringUtils.hasText(businessType)) {
            return;
        }

        String normalizedBusinessType = businessType.trim();
        if (hasExistingUsage(userId, benefit.getId(), normalizedBusinessType, businessId)) {
            log.debug("跳过重复会员权益流水: userId={}, benefitId={}, businessType={}, businessId={}",
                userId, benefit.getId(), normalizedBusinessType, businessId);
            return;
        }

        MemberBenefitUsage usage = new MemberBenefitUsage();
        usage.setUserId(userId);
        usage.setBenefitId(benefit.getId());
        usage.setBenefitName(benefit.getBenefitName());
        usage.setBenefitType(benefit.getBenefitType());
        usage.setBenefitValue(benefit.getBenefitValue());
        usage.setBusinessType(normalizedBusinessType);
        usage.setBusinessId(businessId);
        usage.setEffectAmount(defaultAmount(effectAmount));
        usage.setEffectPoints(effectPoints == null ? 0 : effectPoints);
        usage.setStatus(resolveUsageStatus(normalizedBusinessType, usage.getEffectAmount(), usage.getEffectPoints()));
        usage.setRemark(normalizeOptionalValue(remark));
        memberBenefitUsageMapper.insert(usage);
    }

    @Override
    public IPage<MemberBenefitUsageVO> getMyUsagePage(Long userId, Integer pageNum, Integer pageSize) {
        return getUsagePage(pageNum, pageSize, userId, null, null);
    }

    @Override
    public IPage<MemberBenefitUsageVO> getAdminUsagePage(Integer pageNum, Integer pageSize, Long userId,
                                                         Integer benefitType, String businessType) {
        return getUsagePage(pageNum, pageSize, userId, benefitType, businessType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantBenefit(Long userId, Long benefitId, BigDecimal value, String reason, Long operatorId) {
        MemberBenefit benefit = memberBenefitMapper.selectById(benefitId);
        if (benefit == null || benefit.getStatus() != 1) {
            throw new BusinessException("权益不存在或已禁用");
        }

        BigDecimal grantValue = resolveGrantValue(benefit, value);
        LocalDateTime now = LocalDateTime.now();
        if (isDuplicateBirthdayGrant(userId, benefit, now)) {
            log.info("跳过重复生日礼券发放: userId={}, benefitId={}, year={}", userId, benefitId, now.getYear());
            return;
        }

        BenefitGrantLog grantLog = new BenefitGrantLog();
        grantLog.setUserId(userId);
        grantLog.setBenefitId(benefitId);
        grantLog.setBenefitType(benefit.getBenefitType());
        grantLog.setGrantValue(grantValue);
        grantLog.setGrantReason(reason);
        grantLog.setOperatorId(operatorId);
        grantLog.setGrantTime(now);
        grantLog.setStatus(GRANT_STATUS_GRANTED);

        if (Integer.valueOf(TYPE_BIRTHDAY_COUPON).equals(benefit.getBenefitType())) {
            Coupon coupon = ensureBirthdayCouponTemplate(benefit, grantValue, now);
            UserCoupon userCoupon = createBirthdayUserCoupon(userId, coupon, now);
            grantLog.setExpireTime(userCoupon.getExpireTime());
            grantLog.setOrderId(userCoupon.getId());
        }

        benefitGrantLogMapper.insert(grantLog);

        log.info("发放权益成功: userId={}, benefitId={}, benefitType={}", userId, benefitId, benefit.getBenefitType());
    }

    @Override
    public IPage<BenefitGrantLog> getUserGrantLogs(Long userId, Integer benefitType, int pageNum, int pageSize) {
        Page<BenefitGrantLog> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<BenefitGrantLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, BenefitGrantLog::getUserId, userId)
            .eq(benefitType != null, BenefitGrantLog::getBenefitType, benefitType)
            .orderByDesc(BenefitGrantLog::getGrantTime)
            .orderByDesc(BenefitGrantLog::getId);
        return benefitGrantLogMapper.selectPage(page, wrapper);
    }

    private IPage<MemberBenefitUsageVO> getUsagePage(Integer pageNum, Integer pageSize, Long userId,
                                                     Integer benefitType, String businessType) {
        Page<MemberBenefitUsage> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<MemberBenefitUsage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, MemberBenefitUsage::getUserId, userId)
            .eq(benefitType != null, MemberBenefitUsage::getBenefitType, benefitType)
            .eq(StringUtils.hasText(businessType), MemberBenefitUsage::getBusinessType,
                StringUtils.hasText(businessType) ? businessType.trim() : null)
            .orderByDesc(MemberBenefitUsage::getCreateTime)
            .orderByDesc(MemberBenefitUsage::getId);

        IPage<MemberBenefitUsage> usagePage = memberBenefitUsageMapper.selectPage(page, wrapper);
        Page<MemberBenefitUsageVO> voPage = new Page<>(usagePage.getCurrent(), usagePage.getSize(), usagePage.getTotal());
        voPage.setRecords(usagePage.getRecords().stream()
            .map(this::toUsageVO)
            .collect(Collectors.toList()));
        return voPage;
    }

    private boolean hasExistingUsage(Long userId, Long benefitId, String businessType, Long businessId) {
        LambdaQueryWrapper<MemberBenefitUsage> wrapper = new LambdaQueryWrapper<MemberBenefitUsage>()
            .eq(MemberBenefitUsage::getUserId, userId)
            .eq(MemberBenefitUsage::getBenefitId, benefitId)
            .eq(MemberBenefitUsage::getBusinessType, businessType);
        if (businessId == null) {
            wrapper.isNull(MemberBenefitUsage::getBusinessId);
        } else {
            wrapper.eq(MemberBenefitUsage::getBusinessId, businessId);
        }
        Long count = memberBenefitUsageMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private BigDecimal resolveGrantValue(MemberBenefit benefit, BigDecimal value) {
        BigDecimal configuredValue = defaultAmount(benefit.getBenefitValue());
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        return configuredValue;
    }

    private boolean isDuplicateBirthdayGrant(Long userId, MemberBenefit benefit, LocalDateTime now) {
        if (!Integer.valueOf(TYPE_BIRTHDAY_COUPON).equals(benefit.getBenefitType())) {
            return false;
        }
        LocalDateTime yearStart = LocalDate.of(now.getYear(), 1, 1).atStartOfDay();
        LocalDateTime nextYearStart = yearStart.plusYears(1);
        Long count = benefitGrantLogMapper.selectCount(new LambdaQueryWrapper<BenefitGrantLog>()
            .eq(BenefitGrantLog::getUserId, userId)
            .eq(BenefitGrantLog::getBenefitId, benefit.getId())
            .eq(BenefitGrantLog::getBenefitType, TYPE_BIRTHDAY_COUPON)
            .eq(BenefitGrantLog::getStatus, GRANT_STATUS_GRANTED)
            .ge(BenefitGrantLog::getGrantTime, yearStart)
            .lt(BenefitGrantLog::getGrantTime, nextYearStart));
        return count != null && count > 0;
    }

    private Coupon ensureBirthdayCouponTemplate(MemberBenefit benefit, BigDecimal grantValue, LocalDateTime now) {
        Coupon coupon = couponMapper.selectOne(new LambdaQueryWrapper<Coupon>()
            .eq(Coupon::getCouponName, BIRTHDAY_COUPON_NAME)
            .eq(Coupon::getCouponType, COUPON_TYPE_FULL_REDUCTION)
            .last("LIMIT 1"));
        if (coupon == null) {
            coupon = new Coupon();
            coupon.setCouponName(BIRTHDAY_COUPON_NAME);
            coupon.setCouponType(COUPON_TYPE_FULL_REDUCTION);
            coupon.setDiscountType(COUPON_DISCOUNT_TYPE_AMOUNT);
            coupon.setDiscountValue(grantValue);
            coupon.setMinAmount(BigDecimal.ZERO);
            coupon.setTotalQuantity(999999);
            coupon.setReceivedQuantity(0);
            coupon.setPerLimit(1);
            coupon.setStartTime(LocalDate.of(now.getYear(), 1, 1).atStartOfDay());
            coupon.setEndTime(LocalDate.of(now.getYear(), 12, 31).atTime(LocalTime.MAX));
            coupon.setValidDays(null);
            coupon.setUseScope(1);
            coupon.setDescription(benefit.getDescription());
            coupon.setStatus(COUPON_STATUS_ENABLED);
            couponMapper.insert(coupon);
            return coupon;
        }

        boolean changed = false;
        if (!Integer.valueOf(COUPON_STATUS_ENABLED).equals(coupon.getStatus())) {
            coupon.setStatus(COUPON_STATUS_ENABLED);
            changed = true;
        }
        if (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(grantValue) != 0) {
            coupon.setDiscountValue(grantValue);
            changed = true;
        }
        if (coupon.getStartTime() == null || coupon.getStartTime().isAfter(now)) {
            coupon.setStartTime(LocalDate.of(now.getYear(), 1, 1).atStartOfDay());
            changed = true;
        }
        if (coupon.getEndTime() == null || coupon.getEndTime().isBefore(now)) {
            coupon.setEndTime(LocalDate.of(now.getYear(), 12, 31).atTime(LocalTime.MAX));
            changed = true;
        }
        if (changed) {
            couponMapper.updateById(coupon);
        }
        return coupon;
    }

    private UserCoupon createBirthdayUserCoupon(Long userId, Coupon coupon, LocalDateTime now) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(coupon.getId());
        userCoupon.setCouponCode(generateBirthdayCouponCode(coupon.getId(), userId, now));
        userCoupon.setStatus(USER_COUPON_STATUS_UNUSED);
        userCoupon.setReceiveTime(now);
        userCoupon.setExpireTime(resolveBirthdayCouponExpireTime(coupon, now));
        userCouponMapper.insert(userCoupon);

        coupon.setReceivedQuantity((coupon.getReceivedQuantity() == null ? 0 : coupon.getReceivedQuantity()) + 1);
        couponMapper.updateById(coupon);
        return userCoupon;
    }

    private LocalDateTime resolveBirthdayCouponExpireTime(Coupon coupon, LocalDateTime receiveTime) {
        if (coupon.getValidDays() != null && coupon.getValidDays() > 0) {
            return receiveTime.plusDays(coupon.getValidDays());
        }
        if (coupon.getEndTime() != null) {
            return coupon.getEndTime();
        }
        return LocalDate.of(receiveTime.getYear(), 12, 31).atTime(LocalTime.MAX);
    }

    private String generateBirthdayCouponCode(Long couponId, Long userId, LocalDateTime now) {
        return BIRTHDAY_COUPON_CODE_PREFIX + couponId + userId + now.getYear() + System.nanoTime();
    }

    private void validateBenefitDTO(Long id, MemberBenefitDTO dto) {
        if (dto.getBenefitType() == null || dto.getBenefitType() < TYPE_DISCOUNT || dto.getBenefitType() > TYPE_EXCLUSIVE_SERVICE) {
            throw new BusinessException("不支持的会员权益类型");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BusinessException("权益状态无效");
        }

        BigDecimal value = dto.getBenefitValue();
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("权益值不能为负数");
        }
        if (dto.getBenefitType() == TYPE_DISCOUNT
            && (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessException("专属折扣权益值必须在0到1之间");
        }
        if (dto.getBenefitType() == TYPE_POINTS_MULTIPLIER
            && (value == null || value.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("积分倍率权益值必须大于0");
        }

        Long count = memberBenefitMapper.selectCount(new LambdaQueryWrapper<MemberBenefit>()
            .eq(MemberBenefit::getBenefitCode, dto.getBenefitCode().trim())
            .ne(id != null, MemberBenefit::getId, id));
        if (count > 0) {
            throw new BusinessException("权益编码已存在");
        }
    }

    private void validateBenefitIds(List<Long> benefitIds) {
        if (benefitIds.isEmpty()) {
            return;
        }

        List<MemberBenefit> benefits = memberBenefitMapper.selectList(new LambdaQueryWrapper<MemberBenefit>()
            .in(MemberBenefit::getId, benefitIds));
        if (benefits.size() != benefitIds.size()) {
            throw new BusinessException("存在无效会员权益");
        }
    }

    private void applyBenefitDTO(MemberBenefit benefit, MemberBenefitDTO dto) {
        benefit.setBenefitName(dto.getBenefitName().trim());
        benefit.setBenefitCode(dto.getBenefitCode().trim());
        benefit.setBenefitType(dto.getBenefitType());
        benefit.setBenefitValue(dto.getBenefitValue());
        benefit.setDescription(normalizeOptionalValue(dto.getDescription()));
        benefit.setIcon(normalizeOptionalValue(dto.getIcon()));
        benefit.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private MemberBenefit requireBenefit(Long id) {
        MemberBenefit benefit = memberBenefitMapper.selectById(id);
        if (benefit == null) {
            throw new BusinessException("会员权益不存在");
        }
        return benefit;
    }

    private List<Long> normalizeBenefitIds(List<Long> benefitIds) {
        if (benefitIds == null || benefitIds.isEmpty()) {
            return List.of();
        }
        Set<Long> idSet = benefitIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(idSet);
    }

    private MemberBenefitUsageVO toUsageVO(MemberBenefitUsage usage) {
        MemberBenefitUsageVO vo = new MemberBenefitUsageVO();
        vo.setId(usage.getId());
        vo.setUserId(usage.getUserId());
        vo.setBenefitId(usage.getBenefitId());
        vo.setBenefitName(usage.getBenefitName());
        vo.setBenefitType(usage.getBenefitType());
        vo.setBenefitTypeText(resolveBenefitTypeText(usage.getBenefitType()));
        vo.setBenefitValue(usage.getBenefitValue());
        vo.setBusinessType(usage.getBusinessType());
        vo.setBusinessTypeText(resolveBusinessTypeText(usage.getBusinessType()));
        vo.setBusinessId(usage.getBusinessId());
        vo.setEffectAmount(usage.getEffectAmount());
        vo.setEffectPoints(usage.getEffectPoints());
        vo.setStatus(usage.getStatus());
        vo.setStatusText(resolveUsageStatusText(usage.getStatus()));
        vo.setRemark(usage.getRemark());
        vo.setCreateTime(usage.getCreateTime());
        return vo;
    }

    private MemberBenefitVO toBenefitVO(MemberBenefit benefit) {
        MemberBenefitVO vo = new MemberBenefitVO();
        vo.setId(benefit.getId());
        vo.setBenefitName(benefit.getBenefitName());
        vo.setBenefitCode(benefit.getBenefitCode());
        vo.setBenefitType(benefit.getBenefitType());
        vo.setBenefitTypeText(resolveBenefitTypeText(benefit.getBenefitType()));
        vo.setBenefitValue(benefit.getBenefitValue());
        vo.setValueText(resolveValueText(benefit));
        vo.setDescription(benefit.getDescription());
        vo.setIcon(benefit.getIcon());
        vo.setStatus(benefit.getStatus());
        vo.setCreateTime(benefit.getCreateTime());
        vo.setUpdateTime(benefit.getUpdateTime());
        return vo;
    }

    private String resolveBenefitTypeText(Integer type) {
        if (type == null) {
            return "未知权益";
        }
        return switch (type) {
            case TYPE_DISCOUNT -> "专属折扣";
            case TYPE_POINTS_MULTIPLIER -> "积分倍率";
            case TYPE_FREE_SHIPPING -> "免配送费";
            case TYPE_BIRTHDAY_COUPON -> "生日礼券";
            case TYPE_EXCLUSIVE_SERVICE -> "专属服务";
            default -> "未知权益";
        };
    }

    private String resolveBusinessTypeText(String businessType) {
        if (!StringUtils.hasText(businessType)) {
            return "未知业务";
        }
        return switch (businessType) {
            case "PAYMENT_CREATE" -> "支付创建";
            case "PAYMENT_REWARD" -> "支付奖励";
            case "REFUND_ROLLBACK" -> "退款扣回";
            case "ORDER_SETTLEMENT" -> "订单结算";
            default -> businessType;
        };
    }

    private Integer resolveUsageStatus(String businessType, BigDecimal effectAmount, Integer effectPoints) {
        if (StringUtils.hasText(businessType) && businessType.toUpperCase().contains("ROLLBACK")) {
            return USAGE_STATUS_ROLLBACK;
        }
        if (effectAmount != null && effectAmount.compareTo(BigDecimal.ZERO) < 0) {
            return USAGE_STATUS_ROLLBACK;
        }
        if (effectPoints != null && effectPoints < 0) {
            return USAGE_STATUS_ROLLBACK;
        }
        return USAGE_STATUS_USED;
    }

    private String resolveUsageStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case USAGE_STATUS_USED -> "已使用";
            case USAGE_STATUS_ROLLBACK -> "已回滚";
            default -> "未知";
        };
    }

    private String resolveValueText(MemberBenefit benefit) {
        if (benefit.getBenefitType() == null) {
            return "-";
        }
        BigDecimal value = benefit.getBenefitValue();
        return switch (benefit.getBenefitType()) {
            case TYPE_DISCOUNT -> value == null ? "-" : value.multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString() + "折";
            case TYPE_POINTS_MULTIPLIER -> value == null ? "-" : value.stripTrailingZeros().toPlainString() + "倍积分";
            case TYPE_FREE_SHIPPING -> "免配送费";
            case TYPE_BIRTHDAY_COUPON -> value == null || value.compareTo(BigDecimal.ZERO) <= 0
                ? "生日礼券"
                : "生日券 " + value.stripTrailingZeros().toPlainString() + "元";
            case TYPE_EXCLUSIVE_SERVICE -> "专属服务";
            default -> "-";
        };
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String normalizeOptionalValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
