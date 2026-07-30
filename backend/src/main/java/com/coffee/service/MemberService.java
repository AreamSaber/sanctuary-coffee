package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.dto.MemberUpdateDTO;
import com.coffee.vo.MemberInfoVO;

/**
 * 会员服务接口
 */
public interface MemberService {

    /**
     * 获取会员信息
     */
    MemberInfoVO getMemberInfo(Long userId);

    /**
     * 初始化会员信息
     */
    void initMemberInfo(Long userId);

    /**
     * 更新成长值（下单后）
     */
    void updateGrowthValue(Long userId, Integer growth);

    /**
     * 检查并升级会员等级
     */
    void checkAndUpgradeLevel(Long userId);

    /**
     * 获取会员列表（管理员）
     */
    IPage<MemberInfoVO> getMemberList(Integer pageNum, Integer pageSize, Long levelId, String username);

    /**
     * 管理员编辑会员资料
     */
    void updateMember(Long userId, MemberUpdateDTO updateDTO);

    /**
     * 调整会员积分（管理员）
     */
    void adjustPoints(Long userId, Integer type, Integer points, String remark);
}
