package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.entity.OrderAfterSale;
import com.coffee.vo.AfterSaleVO;

/**
 * 售后服务接口。
 */
public interface AfterSaleService extends IService<OrderAfterSale> {

    /**
     * 用户查询自己的售后分页。
     */
    IPage<AfterSaleVO> getUserAfterSalePage(
            Long userId,
            Integer pageNum,
            Integer pageSize,
            String afterSaleNo,
            String orderNo,
            Integer type,
            Integer status
    );

    /**
     * 用户查询自己的售后详情。
     */
    AfterSaleVO getUserAfterSaleDetail(Long userId, Long afterSaleId);

    /**
     * 管理员查询售后分页。
     */
    IPage<AfterSaleVO> getAdminAfterSalePage(
            Integer pageNum,
            Integer pageSize,
            String afterSaleNo,
            String orderNo,
            Long userId,
            Integer type,
            Integer status
    );

    /**
     * 管理员查询售后详情。
     */
    AfterSaleVO getAdminAfterSaleDetail(Long afterSaleId);
}
