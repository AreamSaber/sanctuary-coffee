package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.MemberInfo;
import com.coffee.entity.MemberLevel;
import com.coffee.entity.PointsRecord;
import com.coffee.mapper.MemberInfoMapper;
import com.coffee.mapper.PointsRecordMapper;
import com.coffee.service.MemberLevelService;
import com.coffee.service.PointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 积分服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements PointsService {

    private static final String UNKNOWN_SOURCE_TYPE = "UNKNOWN";

    private final PointsRecordMapper pointsRecordMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final MemberLevelService memberLevelService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer points, Integer bizType, Long bizId, String description) {
        MemberInfo memberInfo = getMemberInfo(userId);
        int beforeBalance = getSafePoints(memberInfo);
        int afterBalance = beforeBalance + points;

        memberInfo.setPoints(afterBalance);
        memberInfoMapper.updateById(memberInfo);

        PointsRecord record = buildRecord(userId, 1, points, bizType, bizId, description, beforeBalance, afterBalance);
        pointsRecordMapper.insert(record);

        log.info("Points added: userId={}, points={}", userId, points);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, Integer points, Integer bizType, Long bizId, String description) {
        MemberInfo memberInfo = getMemberInfo(userId);
        int beforeBalance = getSafePoints(memberInfo);
        if (beforeBalance < points) {
            throw new BusinessException("积分不足");
        }

        int afterBalance = beforeBalance - points;
        memberInfo.setPoints(afterBalance);
        memberInfoMapper.updateById(memberInfo);

        PointsRecord record = buildRecord(userId, 2, points, bizType, bizId, description, beforeBalance, afterBalance);
        pointsRecordMapper.insert(record);

        log.info("Points deducted: userId={}, points={}", userId, points);
    }

    @Override
    public IPage<PointsRecord> getPointsRecordPage(Long userId, Integer pageNum, Integer pageSize) {
        Page<PointsRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId)
            .orderByDesc(PointsRecord::getCreateTime);

        IPage<PointsRecord> result = pointsRecordMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::normalizeRecord);
        return result;
    }

    @Override
    public Integer getPointsBalance(Long userId) {
        return getSafePoints(getMemberInfo(userId));
    }

    private PointsRecord buildRecord(Long userId, Integer type, Integer points, Integer bizType, Long bizId,
                                     String description, Integer beforeBalance, Integer afterBalance) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setPoints(points);
        record.setBizType(bizType);
        record.setSourceType(toSourceType(bizType));
        record.setBizId(bizId);
        record.setDescription(description);
        record.setBeforeBalance(beforeBalance);
        record.setAfterBalance(afterBalance);
        return record;
    }

    private void normalizeRecord(PointsRecord record) {
        if (record.getBizType() == null) {
            record.setBizType(toBizType(record.getSourceType()));
        }
        if (record.getSourceType() == null) {
            record.setSourceType(toSourceType(record.getBizType()));
        }
    }

    private String toSourceType(Integer bizType) {
        return bizType == null ? UNKNOWN_SOURCE_TYPE : String.valueOf(bizType);
    }

    private Integer toBizType(String sourceType) {
        if (sourceType == null || sourceType.isBlank() || UNKNOWN_SOURCE_TYPE.equals(sourceType)) {
            return null;
        }

        try {
            return Integer.valueOf(sourceType);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private int getSafePoints(MemberInfo memberInfo) {
        return memberInfo.getPoints() == null ? 0 : memberInfo.getPoints();
    }

    private MemberInfo getMemberInfo(Long userId) {
        MemberInfo memberInfo = memberInfoMapper.selectOne(
            new LambdaQueryWrapper<MemberInfo>()
                .eq(MemberInfo::getUserId, userId)
        );

        if (memberInfo == null) {
            memberInfo = initMemberInfo(userId);
        }

        if (memberInfo == null) {
            throw new BusinessException("会员信息不存在");
        }

        return memberInfo;
    }

    private MemberInfo initMemberInfo(Long userId) {
        MemberLevel level = memberLevelService.getLevelByGrowthValue(0);
        if (level == null) {
            return null;
        }

        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setUserId(userId);
        memberInfo.setLevelId(level.getId());
        memberInfo.setGrowthValue(0);
        memberInfo.setPoints(0);
        memberInfo.setTotalConsumption(BigDecimal.ZERO);
        memberInfoMapper.insert(memberInfo);
        return memberInfo;
    }
}
