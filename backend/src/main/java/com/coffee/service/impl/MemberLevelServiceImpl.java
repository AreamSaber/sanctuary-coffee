package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.MemberInfo;
import com.coffee.entity.MemberLevel;
import com.coffee.mapper.MemberInfoMapper;
import com.coffee.mapper.MemberLevelMapper;
import com.coffee.service.MemberLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会员等级服务实现类
 * 
 * @author Coffee Shop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelMapper, MemberLevel> implements MemberLevelService {
    
    private final MemberLevelMapper memberLevelMapper;

    private final MemberInfoMapper memberInfoMapper;
    
    @Override
    public IPage<MemberLevel> getMemberLevelPage(Integer pageNum, Integer pageSize) {
        Page<MemberLevel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(MemberLevel::getLevelCode);
        return memberLevelMapper.selectPage(page, wrapper);
    }
    
    @Override
    public List<MemberLevel> getActiveLevels() {
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberLevel::getStatus, 1)
               .orderByAsc(MemberLevel::getLevelCode);
        return memberLevelMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createLevel(MemberLevel memberLevel) {
        // 检查等级编号是否重复
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberLevel::getLevelCode, memberLevel.getLevelCode());
        Long count = memberLevelMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "等级编号已存在");
        }
        
        memberLevelMapper.insert(memberLevel);
        log.info("创建会员等级成功: {}", memberLevel.getLevelName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLevel(MemberLevel memberLevel) {
        MemberLevel existLevel = memberLevelMapper.selectById(memberLevel.getId());
        if (existLevel == null) {
            throw new BusinessException(ResultCode.MEMBER_LEVEL_NOT_EXIST);
        }
        
        // 如果修改了等级编号，检查是否重复
        if (!existLevel.getLevelCode().equals(memberLevel.getLevelCode())) {
            LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MemberLevel::getLevelCode, memberLevel.getLevelCode())
                   .ne(MemberLevel::getId, memberLevel.getId());
            Long count = memberLevelMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "等级编号已存在");
            }
        }
        
        memberLevelMapper.updateById(memberLevel);
        log.info("更新会员等级成功: {}", memberLevel.getLevelName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLevel(Long id) {
        MemberLevel level = memberLevelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException(ResultCode.MEMBER_LEVEL_NOT_EXIST);
        }
        
        Long memberCount = memberInfoMapper.selectCount(
            new LambdaQueryWrapper<MemberInfo>()
                .eq(MemberInfo::getLevelId, id)
        );
        if (memberCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该会员等级下存在会员，无法删除");
        }

        memberLevelMapper.deleteById(id);
        log.info("删除会员等级成功: {}", level.getLevelName());
    }
    
    @Override
    public void updateStatus(Long id, Integer status) {
        MemberLevel level = memberLevelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException(ResultCode.MEMBER_LEVEL_NOT_EXIST);
        }
        
        level.setStatus(status);
        memberLevelMapper.updateById(level);
        log.info("更新会员等级状态: {} -> {}", level.getLevelName(), status == 1 ? "启用" : "禁用");
    }
    
    @Override
    public MemberLevel getLevelByGrowthValue(Integer growthValue) {
        // 查询所有启用的等级，按所需积分降序排列
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberLevel::getStatus, 1)
               .le(MemberLevel::getRequiredPoints, growthValue)
               .orderByDesc(MemberLevel::getRequiredPoints)
               .last("LIMIT 1");
        
        MemberLevel level = memberLevelMapper.selectOne(wrapper);
        
        // 如果没有找到，返回最低等级
        if (level == null) {
            wrapper.clear();
            wrapper.eq(MemberLevel::getStatus, 1)
                   .orderByAsc(MemberLevel::getRequiredPoints)
                   .last("LIMIT 1");
            level = memberLevelMapper.selectOne(wrapper);
        }
        
        return level;
    }
}
