package com.coffee.service.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.entity.MemberBenefit;
import com.coffee.entity.User;
import com.coffee.mapper.MemberBenefitMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.service.MemberBenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生日礼券自动发放定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayCouponTask {

    private static final int BIRTHDAY_BENEFIT_TYPE = 4;

    private final UserMapper userMapper;
    private final MemberBenefitMapper memberBenefitMapper;
    private final MemberBenefitService memberBenefitService;

    /**
     * 每日凌晨1点执行，检查今日生日用户并发放礼券
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void grantBirthdayCoupons() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        log.info("开始执行生日礼券发放任务: {}-{}", month, day);

        List<MemberBenefit> birthdayBenefits = memberBenefitMapper.selectList(
                new LambdaQueryWrapper<MemberBenefit>()
                        .eq(MemberBenefit::getBenefitType, BIRTHDAY_BENEFIT_TYPE)
                        .eq(MemberBenefit::getStatus, 1)
        );

        if (birthdayBenefits.isEmpty()) {
            log.info("未找到启用的生日礼券权益配置，跳过发放");
            return;
        }

        MemberBenefit birthdayBenefit = birthdayBenefits.get(0);

        List<User> birthdayUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .eq(User::getDeleted, 0)
                        .isNotNull(User::getBirthday)
                        .apply("MONTH(birthday) = {0}", month)
                        .apply("DAY(birthday) = {0}", day)
        );

        if (birthdayUsers.isEmpty()) {
            log.info("今日无生日用户");
            return;
        }

        log.info("今日生日用户数量: {}", birthdayUsers.size());

        int successCount = 0;
        for (User user : birthdayUsers) {
            try {
                memberBenefitService.grantBenefit(
                        user.getId(),
                        birthdayBenefit.getId(),
                        BigDecimal.ZERO,
                        "生日礼券自动发放",
                        null
                );
                successCount++;
                log.info("生日礼券发放成功: userId={}, username={}", user.getId(), user.getUsername());
            } catch (Exception e) {
                log.error("生日礼券发放失败: userId={}, error={}", user.getId(), e.getMessage());
            }
        }

        log.info("生日礼券发放任务完成: 成功 {}/{}", successCount, birthdayUsers.size());
    }
}
