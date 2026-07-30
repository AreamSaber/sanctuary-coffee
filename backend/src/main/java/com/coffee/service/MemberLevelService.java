package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.entity.MemberLevel;
import java.util.List;

/**
 * 会员等级服务接口
 * 
 * @author Coffee Shop Team
 */
public interface MemberLevelService extends IService<MemberLevel> {
    
    /**
     * 分页查询会员等级
     */
    IPage<MemberLevel> getMemberLevelPage(Integer pageNum, Integer pageSize);
    
    /**
     * 获取所有启用的会员等级
     */
    List<MemberLevel> getActiveLevels();
    
    /**
     * 创建会员等级
     */
    void createLevel(MemberLevel memberLevel);
    
    /**
     * 更新会员等级
     */
    void updateLevel(MemberLevel memberLevel);
    
    /**
     * 删除会员等级
     */
    void deleteLevel(Long id);
    
    /**
     * 更新会员等级状态
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 根据成长值获取对应的会员等级
     */
    MemberLevel getLevelByGrowthValue(Integer growthValue);
}
