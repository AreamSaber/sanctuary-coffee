package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.MemberInfo;
import com.coffee.entity.MemberLevel;
import com.coffee.mapper.MemberInfoMapper;
import com.coffee.mapper.MemberLevelMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberLevelServiceImplTest {

    @Mock
    private MemberLevelMapper memberLevelMapper;

    @Mock
    private MemberInfoMapper memberInfoMapper;

    @InjectMocks
    private MemberLevelServiceImpl memberLevelService;

    @BeforeAll
    static void initMybatisPlusLambdaMetadata() {
        initTableInfo(MemberLevel.class);
        initTableInfo(MemberInfo.class);
    }

    @Test
    void deleteLevelRejectsLevelUsedByMembers() {
        MemberLevel level = memberLevel(2L, "黄金会员");
        when(memberLevelMapper.selectById(2L)).thenReturn(level);
        when(memberInfoMapper.selectCount(anyMemberInfoQueryWrapper())).thenReturn(3L);

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> memberLevelService.deleteLevel(2L)
        );

        assertEquals(ResultCode.PARAM_ERROR.getCode(), exception.getCode());
        assertEquals("该会员等级下存在会员，无法删除", exception.getMessage());
        verify(memberLevelMapper, never()).deleteById(2L);
    }

    @Test
    void deleteLevelAllowsUnusedLevel() {
        MemberLevel level = memberLevel(2L, "黄金会员");
        when(memberLevelMapper.selectById(2L)).thenReturn(level);
        when(memberInfoMapper.selectCount(anyMemberInfoQueryWrapper())).thenReturn(0L);

        memberLevelService.deleteLevel(2L);

        verify(memberLevelMapper).deleteById(2L);
    }

    private MemberLevel memberLevel(Long id, String name) {
        MemberLevel level = new MemberLevel();
        level.setId(id);
        level.setLevelName(name);
        level.setLevelCode(2);
        level.setStatus(1);
        return level;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<MemberInfo> anyMemberInfoQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }

    private static void initTableInfo(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }
}
