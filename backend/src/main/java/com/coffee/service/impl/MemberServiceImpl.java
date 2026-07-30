package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.MemberUpdateDTO;
import com.coffee.entity.MemberInfo;
import com.coffee.entity.MemberLevel;
import com.coffee.entity.User;
import com.coffee.mapper.MemberInfoMapper;
import com.coffee.mapper.MemberLevelMapper;
import com.coffee.mapper.UserMapper;
import com.coffee.service.MemberService;
import com.coffee.service.PointsService;
import com.coffee.vo.MemberInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberInfoMapper memberInfoMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final UserMapper userMapper;
    private final PointsService pointsService;

    @Override
    public MemberInfoVO getMemberInfo(Long userId) {
        MemberInfo memberInfo = memberInfoMapper.selectOne(
                new LambdaQueryWrapper<MemberInfo>()
                        .eq(MemberInfo::getUserId, userId)
        );

        if (memberInfo == null) {
            initMemberInfo(userId);
            memberInfo = memberInfoMapper.selectOne(
                    new LambdaQueryWrapper<MemberInfo>()
                            .eq(MemberInfo::getUserId, userId)
            );
        }

        MemberInfoVO vo = BeanUtil.copyProperties(memberInfo, MemberInfoVO.class);

        MemberLevel level = memberLevelMapper.selectById(memberInfo.getLevelId());
        if (level != null) {
            vo.setLevelName(level.getLevelName());
            vo.setLevelIcon(level.getIcon());
            vo.setDiscountRate(level.getDiscountRate());

            MemberLevel nextLevel = memberLevelMapper.selectOne(
                    new LambdaQueryWrapper<MemberLevel>()
                            .eq(MemberLevel::getStatus, 1)
                            .gt(MemberLevel::getLevelCode, level.getLevelCode())
                            .orderByAsc(MemberLevel::getLevelCode)
                            .last("LIMIT 1")
            );
            if (nextLevel != null) {
                vo.setNextLevelGrowth(nextLevel.getRequiredPoints() - memberInfo.getGrowthValue());
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initMemberInfo(Long userId) {
        MemberLevel level = memberLevelMapper.selectOne(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getLevelCode, 1)
                        .eq(MemberLevel::getStatus, 1)
        );

        if (level == null) {
            log.warn("未找到初始会员等级配置");
            return;
        }

        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setUserId(userId);
        memberInfo.setLevelId(level.getId());
        memberInfo.setGrowthValue(0);
        memberInfo.setPoints(0);
        memberInfo.setTotalConsumption(BigDecimal.ZERO);

        memberInfoMapper.insert(memberInfo);
        log.info("初始化会员信息成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGrowthValue(Long userId, Integer growth) {
        MemberInfo memberInfo = memberInfoMapper.selectOne(
                new LambdaQueryWrapper<MemberInfo>()
                        .eq(MemberInfo::getUserId, userId)
        );

        if (memberInfo == null) {
            return;
        }

        memberInfo.setGrowthValue(memberInfo.getGrowthValue() + growth);
        memberInfoMapper.updateById(memberInfo);

        checkAndUpgradeLevel(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndUpgradeLevel(Long userId) {
        MemberInfo memberInfo = memberInfoMapper.selectOne(
                new LambdaQueryWrapper<MemberInfo>()
                        .eq(MemberInfo::getUserId, userId)
        );

        if (memberInfo == null) {
            return;
        }

        List<MemberLevel> levels = memberLevelMapper.selectList(
                new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getStatus, 1)
                        .le(MemberLevel::getRequiredPoints, memberInfo.getGrowthValue())
                        .orderByDesc(MemberLevel::getLevelCode)
        );

        if (!levels.isEmpty()) {
            MemberLevel targetLevel = levels.get(0);
            if (!targetLevel.getId().equals(memberInfo.getLevelId())) {
                memberInfo.setLevelId(targetLevel.getId());
                memberInfoMapper.updateById(memberInfo);
                log.info("会员等级升级: userId={}, newLevel={}", userId, targetLevel.getLevelName());
            }
        }
    }

    @Override
    public IPage<MemberInfoVO> getMemberList(Integer pageNum, Integer pageSize, Long levelId, String username) {
        Page<MemberInfo> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<MemberInfo> wrapper = new LambdaQueryWrapper<>();
        if (levelId != null) {
            wrapper.eq(MemberInfo::getLevelId, levelId);
        }

        IPage<MemberInfo> memberPage = memberInfoMapper.selectPage(page, wrapper);

        List<Long> userIds = memberPage.getRecords().stream()
                .map(MemberInfo::getUserId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getId, userIds);
        if (username != null && !username.trim().isEmpty()) {
            String keyword = username.trim();
            userWrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }

        List<User> users = userMapper.selectList(userWrapper);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<MemberLevel> levels = memberLevelMapper.selectList(null);
        Map<Long, MemberLevel> levelMap = levels.stream()
                .collect(Collectors.toMap(MemberLevel::getId, level -> level));

        List<MemberInfoVO> voList = memberPage.getRecords().stream()
                .filter(memberInfo -> userMap.containsKey(memberInfo.getUserId()))
                .map(memberInfo -> {
                    MemberInfoVO vo = BeanUtil.copyProperties(memberInfo, MemberInfoVO.class);
                    User user = userMap.get(memberInfo.getUserId());
                    MemberLevel level = levelMap.get(memberInfo.getLevelId());

                    vo.setUserId(user.getId());
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                    vo.setEmail(user.getEmail());
                    vo.setPhone(user.getPhone());
                    vo.setAvatar(user.getAvatar());
                    vo.setCreateTime(user.getCreateTime());

                    if (level != null) {
                        vo.setLevelName(level.getLevelName());
                        vo.setLevelIcon(level.getIcon());
                        vo.setDiscountRate(level.getDiscountRate());
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        Page<MemberInfoVO> result = new Page<>(pageNum, pageSize);
        result.setRecords(voList);
        result.setTotal(memberPage.getTotal());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(Long userId, MemberUpdateDTO updateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        MemberInfo memberInfo = memberInfoMapper.selectOne(
                new LambdaQueryWrapper<MemberInfo>()
                        .eq(MemberInfo::getUserId, userId)
        );
        if (memberInfo == null) {
            initMemberInfo(userId);
            memberInfo = memberInfoMapper.selectOne(
                    new LambdaQueryWrapper<MemberInfo>()
                            .eq(MemberInfo::getUserId, userId)
            );
        }
        if (memberInfo == null) {
            throw new BusinessException(ResultCode.MEMBER_INFO_NOT_EXIST);
        }

        MemberLevel level = memberLevelMapper.selectById(updateDTO.getLevelId());
        if (level == null) {
            throw new BusinessException(ResultCode.MEMBER_LEVEL_NOT_EXIST);
        }

        user.setNickname(normalizeOptionalValue(updateDTO.getNickname()));
        user.setEmail(normalizeOptionalValue(updateDTO.getEmail()));
        user.setPhone(normalizeOptionalValue(updateDTO.getPhone()));
        userMapper.updateById(user);

        memberInfo.setLevelId(updateDTO.getLevelId());
        memberInfoMapper.updateById(memberInfo);

        log.info("管理员更新会员资料成功: userId={}, levelId={}", userId, updateDTO.getLevelId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long userId, Integer type, Integer points, String remark) {
        if (type == 1) {
            pointsService.addPoints(userId, points, 99, null, remark);
        } else {
            pointsService.deductPoints(userId, points, 99, null, remark);
        }
        log.info("管理员调整积分: userId={}, type={}, points={}", userId, type, points);
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
