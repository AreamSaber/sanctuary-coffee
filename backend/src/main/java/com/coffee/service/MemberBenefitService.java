package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.coffee.dto.MemberBenefitDTO;
import com.coffee.entity.BenefitGrantLog;
import com.coffee.vo.MemberBenefitUsageVO;
import com.coffee.vo.MemberBenefitVO;
import com.coffee.vo.MemberLevelBenefitVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员权益服务
 */
public interface MemberBenefitService {

    IPage<MemberBenefitVO> getBenefitPage(Integer pageNum, Integer pageSize, String keyword, Integer benefitType, Integer status);

    List<MemberBenefitVO> getActiveBenefits();

    void createBenefit(MemberBenefitDTO dto);

    void updateBenefit(Long id, MemberBenefitDTO dto);

    void deleteBenefit(Long id);

    void updateStatus(Long id, Integer status);

    List<MemberLevelBenefitVO> getLevelBenefitMatrix();

    List<MemberBenefitVO> getBenefitsByLevelId(Long levelId);

    void saveLevelBenefits(Long levelId, List<Long> benefitIds);

    List<MemberBenefitVO> getUserBenefits(Long userId);

    void recordBenefitUsage(Long userId, MemberBenefitVO benefit, String businessType, Long businessId,
                            BigDecimal effectAmount, Integer effectPoints, String remark);

    IPage<MemberBenefitUsageVO> getMyUsagePage(Long userId, Integer pageNum, Integer pageSize);

    IPage<MemberBenefitUsageVO> getAdminUsagePage(Integer pageNum, Integer pageSize, Long userId,
                                                  Integer benefitType, String businessType);

    /**
     * 发放权益给用户
     */
    void grantBenefit(Long userId, Long benefitId, BigDecimal value, String reason, Long operatorId);

    /**
     * 查询用户权益发放记录
     */
    IPage<BenefitGrantLog> getUserGrantLogs(Long userId, Integer benefitType, int pageNum, int pageSize);
}
