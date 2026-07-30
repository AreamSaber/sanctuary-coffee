package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.coffee.vo.MemberBenefitUsageVO;
import com.coffee.vo.MemberBenefitVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberBenefitServiceImplTest {

    @Mock
    private MemberBenefitMapper memberBenefitMapper;

    @Mock
    private MemberBenefitUsageMapper memberBenefitUsageMapper;

    @Mock
    private MemberLevelBenefitMapper memberLevelBenefitMapper;

    @Mock
    private MemberLevelMapper memberLevelMapper;

    @Mock
    private MemberInfoMapper memberInfoMapper;

    @Mock
    private BenefitGrantLogMapper benefitGrantLogMapper;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @InjectMocks
    private MemberBenefitServiceImpl memberBenefitService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(MemberBenefit.class);
        initTableInfo(MemberBenefitUsage.class);
        initTableInfo(MemberLevelBenefit.class);
        initTableInfo(MemberLevel.class);
        initTableInfo(MemberInfo.class);
        initTableInfo(BenefitGrantLog.class);
        initTableInfo(Coupon.class);
        initTableInfo(UserCoupon.class);
    }

    @Test
    void saveLevelBenefitsDeduplicatesAndReplacesBindings() {
        MemberLevel level = new MemberLevel();
        level.setId(3L);
        MemberBenefit pointsBenefit = benefit(10L, "积分加速", 2, new BigDecimal("1.20"));
        MemberBenefit birthdayBenefit = benefit(11L, "生日礼券", 4, new BigDecimal("20.00"));

        when(memberLevelMapper.selectById(3L)).thenReturn(level);
        when(memberBenefitMapper.selectList(anyBenefitQueryWrapper()))
            .thenReturn(List.of(pointsBenefit, birthdayBenefit));

        memberBenefitService.saveLevelBenefits(3L, java.util.Arrays.asList(10L, 10L, 11L, null));

        ArgumentCaptor<MemberLevelBenefit> bindingCaptor = ArgumentCaptor.forClass(MemberLevelBenefit.class);
        verify(memberLevelBenefitMapper).delete(anyBindingQueryWrapper());
        verify(memberLevelBenefitMapper, org.mockito.Mockito.times(2)).insert(bindingCaptor.capture());

        List<MemberLevelBenefit> bindings = bindingCaptor.getAllValues();
        assertEquals(10L, bindings.get(0).getBenefitId());
        assertEquals(1, bindings.get(0).getSortOrder());
        assertEquals(11L, bindings.get(1).getBenefitId());
        assertEquals(2, bindings.get(1).getSortOrder());
    }

    @Test
    void getUserBenefitsReturnsBenefitsByCurrentLevelInBindingOrder() {
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setUserId(100L);
        memberInfo.setLevelId(5L);

        MemberLevelBenefit firstBinding = binding(5L, 11L, 1);
        MemberLevelBenefit secondBinding = binding(5L, 10L, 2);
        MemberBenefit pointsBenefit = benefit(10L, "积分加速", 2, new BigDecimal("1.20"));
        MemberBenefit birthdayBenefit = benefit(11L, "生日礼券", 4, new BigDecimal("20.00"));

        when(memberInfoMapper.selectOne(anyMemberInfoQueryWrapper())).thenReturn(memberInfo);
        when(memberLevelBenefitMapper.selectList(anyBindingQueryWrapper()))
            .thenReturn(List.of(firstBinding, secondBinding));
        when(memberBenefitMapper.selectList(anyBenefitQueryWrapper()))
            .thenReturn(List.of(pointsBenefit, birthdayBenefit));

        List<MemberBenefitVO> result = memberBenefitService.getUserBenefits(100L);

        assertEquals(2, result.size());
        assertEquals("生日礼券", result.get(0).getBenefitName());
        assertEquals("生日券 20元", result.get(0).getValueText());
        assertEquals("积分加速", result.get(1).getBenefitName());
        assertEquals("1.2倍积分", result.get(1).getValueText());
    }

    @Test
    void recordBenefitUsageInsertsSnapshotAndUsageStatus() {
        MemberBenefitVO benefit = benefitVO(3L, "免配送费", 3, null);

        when(memberBenefitUsageMapper.selectCount(anyUsageQueryWrapper())).thenReturn(0L);

        memberBenefitService.recordBenefitUsage(
            100L,
            benefit,
            "PAYMENT_CREATE",
            10L,
            new BigDecimal("8.00"),
            null,
            "会员免配送费抵扣"
        );

        ArgumentCaptor<MemberBenefitUsage> usageCaptor = ArgumentCaptor.forClass(MemberBenefitUsage.class);
        verify(memberBenefitUsageMapper).insert(usageCaptor.capture());

        MemberBenefitUsage usage = usageCaptor.getValue();
        assertEquals(100L, usage.getUserId());
        assertEquals(3L, usage.getBenefitId());
        assertEquals("免配送费", usage.getBenefitName());
        assertEquals(3, usage.getBenefitType());
        assertEquals("PAYMENT_CREATE", usage.getBusinessType());
        assertEquals(10L, usage.getBusinessId());
        assertEquals(new BigDecimal("8.00"), usage.getEffectAmount());
        assertEquals(0, usage.getEffectPoints());
        assertEquals(1, usage.getStatus());
    }

    @Test
    void recordBenefitUsageSkipsDuplicateBusinessRecord() {
        MemberBenefitVO benefit = benefitVO(2L, "积分加速", 2, new BigDecimal("1.50"));

        when(memberBenefitUsageMapper.selectCount(anyUsageQueryWrapper())).thenReturn(1L);

        memberBenefitService.recordBenefitUsage(100L, benefit, "PAYMENT_REWARD", 10L, BigDecimal.ZERO, 99, "duplicate");

        verify(memberBenefitUsageMapper, never()).insert(any(MemberBenefitUsage.class));
    }

    @Test
    void getMyUsagePageMapsDisplayText() {
        MemberBenefitUsage usage = new MemberBenefitUsage();
        usage.setId(1L);
        usage.setUserId(100L);
        usage.setBenefitId(2L);
        usage.setBenefitName("积分加速");
        usage.setBenefitType(2);
        usage.setBenefitValue(new BigDecimal("1.50"));
        usage.setBusinessType("REFUND_ROLLBACK");
        usage.setBusinessId(40L);
        usage.setEffectAmount(BigDecimal.ZERO);
        usage.setEffectPoints(-99);
        usage.setStatus(2);
        usage.setRemark("退款扣回");
        usage.setCreateTime(LocalDateTime.now());
        Page<MemberBenefitUsage> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(usage));

        when(memberBenefitUsageMapper.selectPage(anyUsagePage(), anyUsageQueryWrapper())).thenReturn(page);

        IPage<MemberBenefitUsageVO> result = memberBenefitService.getMyUsagePage(100L, 1, 10);

        assertEquals(1, result.getRecords().size());
        MemberBenefitUsageVO vo = result.getRecords().get(0);
        assertEquals("积分倍率", vo.getBenefitTypeText());
        assertEquals("退款扣回", vo.getBusinessTypeText());
        assertEquals("已回滚", vo.getStatusText());
        assertEquals(-99, vo.getEffectPoints());
    }

    @Test
    void grantBirthdayBenefitCreatesCouponAndGrantLog() {
        MemberBenefit birthdayBenefit = benefit(11L, "生日礼券", 4, new BigDecimal("20.00"));
        Coupon template = birthdayCouponTemplate(101L);

        when(memberBenefitMapper.selectById(11L)).thenReturn(birthdayBenefit);
        when(benefitGrantLogMapper.selectCount(anyGrantLogQueryWrapper())).thenReturn(0L);
        when(couponMapper.selectOne(anyCouponQueryWrapper())).thenReturn(template);

        memberBenefitService.grantBenefit(100L, 11L, null, "生日礼券自动发放", null);

        ArgumentCaptor<UserCoupon> userCouponCaptor = ArgumentCaptor.forClass(UserCoupon.class);
        ArgumentCaptor<BenefitGrantLog> grantLogCaptor = ArgumentCaptor.forClass(BenefitGrantLog.class);
        verify(userCouponMapper).insert(userCouponCaptor.capture());
        verify(couponMapper).updateById(template);
        verify(benefitGrantLogMapper).insert(grantLogCaptor.capture());

        UserCoupon userCoupon = userCouponCaptor.getValue();
        assertEquals(100L, userCoupon.getUserId());
        assertEquals(101L, userCoupon.getCouponId());
        assertEquals(0, userCoupon.getStatus());
        assertEquals(1, template.getReceivedQuantity());

        BenefitGrantLog grantLog = grantLogCaptor.getValue();
        assertEquals(100L, grantLog.getUserId());
        assertEquals(11L, grantLog.getBenefitId());
        assertEquals(4, grantLog.getBenefitType());
        assertEquals(new BigDecimal("20.00"), grantLog.getGrantValue());
        assertEquals("生日礼券自动发放", grantLog.getGrantReason());
        assertEquals(1, grantLog.getStatus());
    }

    @Test
    void grantBirthdayBenefitSkipsDuplicateSameYearGrant() {
        MemberBenefit birthdayBenefit = benefit(11L, "生日礼券", 4, new BigDecimal("20.00"));

        when(memberBenefitMapper.selectById(11L)).thenReturn(birthdayBenefit);
        when(benefitGrantLogMapper.selectCount(anyGrantLogQueryWrapper())).thenReturn(1L);

        memberBenefitService.grantBenefit(100L, 11L, null, "生日礼券自动发放", null);

        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
        verify(couponMapper, never()).insert(any(Coupon.class));
        verify(benefitGrantLogMapper, never()).insert(any(BenefitGrantLog.class));
    }

    private MemberBenefit benefit(Long id, String name, Integer type, BigDecimal value) {
        MemberBenefit benefit = new MemberBenefit();
        benefit.setId(id);
        benefit.setBenefitName(name);
        benefit.setBenefitCode("BENEFIT_" + id);
        benefit.setBenefitType(type);
        benefit.setBenefitValue(value);
        benefit.setStatus(1);
        return benefit;
    }

    private MemberLevelBenefit binding(Long levelId, Long benefitId, Integer sortOrder) {
        MemberLevelBenefit binding = new MemberLevelBenefit();
        binding.setLevelId(levelId);
        binding.setBenefitId(benefitId);
        binding.setSortOrder(sortOrder);
        binding.setStatus(1);
        return binding;
    }

    private MemberBenefitVO benefitVO(Long id, String name, Integer type, BigDecimal value) {
        MemberBenefitVO benefit = new MemberBenefitVO();
        benefit.setId(id);
        benefit.setBenefitName(name);
        benefit.setBenefitType(type);
        benefit.setBenefitValue(value);
        return benefit;
    }

    private Coupon birthdayCouponTemplate(Long id) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCouponName("生日礼券");
        coupon.setCouponType(1);
        coupon.setDiscountType(1);
        coupon.setDiscountValue(new BigDecimal("20.00"));
        coupon.setReceivedQuantity(0);
        coupon.setStatus(1);
        coupon.setStartTime(LocalDateTime.now().minusDays(1));
        coupon.setEndTime(LocalDateTime.now().plusDays(30));
        return coupon;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<MemberBenefit> anyBenefitQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<MemberLevelBenefit> anyBindingQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<MemberInfo> anyMemberInfoQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<MemberBenefitUsage> anyUsageQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<BenefitGrantLog> anyGrantLogQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<Coupon> anyCouponQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private Page<MemberBenefitUsage> anyUsagePage() {
        return any(Page.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
