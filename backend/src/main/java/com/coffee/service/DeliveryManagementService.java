package com.coffee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryRegion;
import com.coffee.entity.DeliveryStaff;
import com.coffee.vo.DeliveryRegionTreeVO;
import com.coffee.vo.DeliveryStaffVO;

import java.util.List;

public interface DeliveryManagementService extends IService<DeliveryRegion> {

    IPage<DeliveryMethod> getMethodPage(Integer pageNum, Integer pageSize, String keyword, Integer status);

    List<DeliveryMethod> getAllMethods();

    void createMethod(DeliveryMethod method);

    void updateMethod(DeliveryMethod method);

    void deleteMethod(Long methodId);

    IPage<DeliveryRegion> getRegionPage(Integer pageNum, Integer pageSize, String keyword);

    List<DeliveryRegionTreeVO> getRegionTree();

    void createRegion(DeliveryRegion region);

    void updateRegion(DeliveryRegion region);

    void deleteRegion(Long regionId);

    IPage<DeliveryStaffVO> getStaffPage(Integer pageNum, Integer pageSize, String keyword, String status);

    DeliveryStaffVO getStaffDetail(Long staffId);

    void createStaff(DeliveryStaff staff);

    void updateStaff(DeliveryStaff staff);

    void deleteStaff(Long staffId);

    void updateStaffStatus(Long staffId, String status);

    void assignStaffToRegion(Long staffId, Long regionId);

    List<DeliveryStaffVO> getStaffByRegion(Long regionId);

    List<DeliveryStaffVO> getAvailableStaff(Long regionId);

    Long autoAssignStaff(Long orderId, Long regionId);
}
