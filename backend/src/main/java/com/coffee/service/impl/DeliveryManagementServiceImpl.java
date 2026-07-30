package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.DeliveryMethod;
import com.coffee.entity.DeliveryOrder;
import com.coffee.entity.DeliveryRegion;
import com.coffee.entity.DeliveryStaff;
import com.coffee.mapper.DeliveryMethodMapper;
import com.coffee.mapper.DeliveryOrderMapper;
import com.coffee.mapper.DeliveryRegionMapper;
import com.coffee.mapper.DeliveryStaffMapper;
import com.coffee.service.DeliveryService;
import com.coffee.service.DeliveryManagementService;
import com.coffee.vo.DeliveryRegionTreeVO;
import com.coffee.vo.DeliveryStaffVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryManagementServiceImpl extends ServiceImpl<DeliveryRegionMapper, DeliveryRegion>
    implements DeliveryManagementService {

    private static final int DELIVERY_STATUS_COMPLETED = 4;

    private final DeliveryMethodMapper methodMapper;
    private final DeliveryRegionMapper regionMapper;
    private final DeliveryStaffMapper staffMapper;
    private final DeliveryOrderMapper deliveryOrderMapper;
    private final DeliveryService deliveryService;

    @Override
    public IPage<DeliveryMethod> getMethodPage(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<DeliveryMethod> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeliveryMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(keyword != null && !keyword.isBlank(), query -> query
                .like(DeliveryMethod::getMethodName, keyword)
                .or()
                .like(DeliveryMethod::getDescription, keyword))
            .eq(status != null, DeliveryMethod::getStatus, status)
            .orderByAsc(DeliveryMethod::getId);
        IPage<DeliveryMethod> result = methodMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::normalizeDeliveryMethod);
        return result;
    }

    @Override
    public List<DeliveryMethod> getAllMethods() {
        List<DeliveryMethod> methods = methodMapper.selectList(
            new LambdaQueryWrapper<DeliveryMethod>().orderByAsc(DeliveryMethod::getId)
        );
        methods.forEach(this::normalizeDeliveryMethod);
        return methods;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMethod(DeliveryMethod method) {
        normalizeMethodForSave(method);
        validateMethodName(method.getMethodName(), null);
        methodMapper.insert(method);
        log.info("Delivery method created: id={}, name={}", method.getId(), method.getMethodName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMethod(DeliveryMethod method) {
        DeliveryMethod existing = requireMethod(method.getId());
        normalizeMethodForSave(method);
        validateMethodName(method.getMethodName(), method.getId());

        existing.setMethodName(method.getMethodName());
        existing.setDescription(method.getDescription());
        existing.setFreight(method.getFreight());
        existing.setFreeThreshold(method.getFreeThreshold());
        existing.setStatus(method.getStatus());
        methodMapper.updateById(existing);
        log.info("Delivery method updated: id={}, name={}", existing.getId(), existing.getMethodName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMethod(Long methodId) {
        DeliveryMethod method = requireMethod(methodId);
        methodMapper.deleteById(methodId);
        log.info("Delivery method deleted: id={}, name={}", methodId, method.getMethodName());
    }

    @Override
    public IPage<DeliveryRegion> getRegionPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<DeliveryRegion> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeliveryRegion> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DeliveryRegion::getRegionName, keyword)
                .or()
                .like(DeliveryRegion::getRegionCode, keyword);
        }
        wrapper.orderByAsc(DeliveryRegion::getLevel).orderByAsc(DeliveryRegion::getSortOrder);
        return regionMapper.selectPage(page, wrapper);
    }

    @Override
    public List<DeliveryRegionTreeVO> getRegionTree() {
        List<DeliveryRegion> regions = regionMapper.selectList(
            new LambdaQueryWrapper<DeliveryRegion>()
                .eq(DeliveryRegion::getStatus, 1)
                .orderByAsc(DeliveryRegion::getLevel)
                .orderByAsc(DeliveryRegion::getSortOrder)
                .orderByAsc(DeliveryRegion::getId)
        );
        return buildRegionTree(regions, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRegion(DeliveryRegion region) {
        validateRegionCode(region.getRegionCode(), null);
        if (region.getParentId() == null) {
            region.setParentId(0L);
        }
        if (region.getStatus() == null) {
            region.setStatus(1);
        }
        if (region.getSortOrder() == null) {
            region.setSortOrder(0);
        }
        regionMapper.insert(region);
        log.info("Delivery region created: id={}, name={}", region.getId(), region.getRegionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegion(DeliveryRegion region) {
        DeliveryRegion existing = requireRegion(region.getId());
        validateRegionCode(region.getRegionCode(), region.getId());

        existing.setRegionName(region.getRegionName());
        existing.setRegionCode(region.getRegionCode());
        existing.setParentId(region.getParentId() == null ? 0L : region.getParentId());
        existing.setLevel(region.getLevel());
        existing.setDeliveryFee(region.getDeliveryFee());
        existing.setMinOrderAmount(region.getMinOrderAmount());
        existing.setEstimatedTime(region.getEstimatedTime());
        existing.setLongitude(region.getLongitude());
        existing.setLatitude(region.getLatitude());
        existing.setDeliveryRange(region.getDeliveryRange());
        existing.setStatus(region.getStatus());
        if (region.getSortOrder() != null) {
            existing.setSortOrder(region.getSortOrder());
        }

        regionMapper.updateById(existing);
        log.info("Delivery region updated: id={}, name={}", existing.getId(), existing.getRegionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRegion(Long regionId) {
        DeliveryRegion region = requireRegion(regionId);

        Long childCount = regionMapper.selectCount(
            new LambdaQueryWrapper<DeliveryRegion>().eq(DeliveryRegion::getParentId, regionId)
        );
        if (childCount > 0) {
            throw new BusinessException(ResultCode.DELIVERY_REGION_HAS_CHILDREN);
        }

        Long staffCount = staffMapper.selectCount(
            new LambdaQueryWrapper<DeliveryStaff>().eq(DeliveryStaff::getRegionId, regionId)
        );
        if (staffCount > 0) {
            throw new BusinessException(ResultCode.DELIVERY_REGION_HAS_STAFF);
        }

        regionMapper.deleteById(regionId);
        log.info("Delivery region deleted: id={}, name={}", regionId, region.getRegionName());
    }

    @Override
    public IPage<DeliveryStaffVO> getStaffPage(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<DeliveryStaff> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DeliveryStaff> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(DeliveryStaff::getName, keyword)
                .or()
                .like(DeliveryStaff::getPhone, keyword)
                .or()
                .like(DeliveryStaff::getStaffCode, keyword);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(DeliveryStaff::getStatus, status);
        }
        wrapper.orderByDesc(DeliveryStaff::getCreateTime);

        IPage<DeliveryStaff> staffPage = staffMapper.selectPage(page, wrapper);
        Page<DeliveryStaffVO> result = new Page<>(pageNum, pageSize);
        result.setTotal(staffPage.getTotal());
        result.setRecords(convertToStaffVOList(staffPage.getRecords()));
        return result;
    }

    @Override
    public DeliveryStaffVO getStaffDetail(Long staffId) {
        return convertToStaffVO(requireStaff(staffId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStaff(DeliveryStaff staff) {
        if (staff.getUserId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户ID不能为空");
        }
        staff.setStaffCode(generateStaffCode());
        if (staff.getStatus() == null || staff.getStatus().isBlank()) {
            staff.setStatus("OFFLINE");
        }
        if (staff.getTodayOrders() == null) {
            staff.setTodayOrders(0);
        }
        if (staff.getTotalOrders() == null) {
            staff.setTotalOrders(0);
        }
        if (staff.getRating() == null) {
            staff.setRating(5.0);
        }
        if (staff.getEnabled() == null) {
            staff.setEnabled(1);
        }
        if (staff.getJoinTime() == null) {
            staff.setJoinTime(LocalDateTime.now());
        }
        if (staff.getRegionId() != null) {
            requireRegion(staff.getRegionId());
        }
        staffMapper.insert(staff);
        log.info("Delivery staff created: id={}, name={}", staff.getId(), staff.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStaff(DeliveryStaff staff) {
        DeliveryStaff existing = requireStaff(staff.getId());
        if (staff.getUserId() != null) {
            existing.setUserId(staff.getUserId());
        }
        if (staff.getRegionId() != null) {
            requireRegion(staff.getRegionId());
        }

        existing.setName(staff.getName());
        existing.setPhone(staff.getPhone());
        existing.setIdNumber(staff.getIdNumber());
        existing.setRegionId(staff.getRegionId());
        existing.setStatus(staff.getStatus() == null || staff.getStatus().isBlank() ? existing.getStatus() : staff.getStatus());
        existing.setVehicleType(staff.getVehicleType());
        existing.setVehicleNumber(staff.getVehicleNumber());
        existing.setHealthCertNo(staff.getHealthCertNo());
        existing.setHealthCertExpiry(staff.getHealthCertExpiry());
        if (staff.getEnabled() != null) {
            existing.setEnabled(staff.getEnabled());
        }

        staffMapper.updateById(existing);
        log.info("Delivery staff updated: id={}, name={}", existing.getId(), existing.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStaff(Long staffId) {
        DeliveryStaff staff = requireStaff(staffId);

        Long activeOrders = deliveryOrderMapper.selectCount(
            new LambdaQueryWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getDeliverymanId, staffId)
                .lt(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_COMPLETED)
        );
        if (activeOrders > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配送员存在进行中的订单");
        }

        staffMapper.deleteById(staffId);
        log.info("Delivery staff deleted: id={}, name={}", staffId, staff.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStaffStatus(Long staffId, String status) {
        DeliveryStaff staff = requireStaff(staffId);
        staff.setStatus(status);
        staffMapper.updateById(staff);
        log.info("Delivery staff status updated: id={}, status={}", staffId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignStaffToRegion(Long staffId, Long regionId) {
        DeliveryStaff staff = requireStaff(staffId);
        requireRegion(regionId);
        staff.setRegionId(regionId);
        staffMapper.updateById(staff);
        log.info("Delivery staff assigned: staffId={}, regionId={}", staffId, regionId);
    }

    @Override
    public List<DeliveryStaffVO> getStaffByRegion(Long regionId) {
        return convertToStaffVOList(
            staffMapper.selectList(
                new LambdaQueryWrapper<DeliveryStaff>()
                    .eq(DeliveryStaff::getRegionId, regionId)
                    .eq(DeliveryStaff::getEnabled, 1)
                    .orderByDesc(DeliveryStaff::getCreateTime)
            )
        );
    }

    @Override
    public List<DeliveryStaffVO> getAvailableStaff(Long regionId) {
        return convertToStaffVOList(
            staffMapper.selectList(
                new LambdaQueryWrapper<DeliveryStaff>()
                    .eq(DeliveryStaff::getRegionId, regionId)
                    .eq(DeliveryStaff::getStatus, "IDLE")
                    .eq(DeliveryStaff::getEnabled, 1)
                    .orderByAsc(DeliveryStaff::getTodayOrders)
                    .orderByDesc(DeliveryStaff::getRating)
            )
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoAssignStaff(Long orderId, Long regionId) {
        List<DeliveryStaffVO> availableStaff = getAvailableStaff(regionId);
        if (availableStaff.isEmpty()) {
            throw new BusinessException(ResultCode.NO_AVAILABLE_DELIVERY_STAFF);
        }

        DeliveryStaffVO selectedStaff = availableStaff.stream()
            .min((left, right) -> Integer.compare(defaultInt(left.getTodayOrders()), defaultInt(right.getTodayOrders())))
            .orElse(availableStaff.get(0));

        deliveryService.assignDelivery(orderId, selectedStaff.getId());
        log.info("Delivery staff auto assigned: orderId={}, staffId={}, regionId={}", orderId, selectedStaff.getId(), regionId);
        return selectedStaff.getId();
    }

    private DeliveryRegion requireRegion(Long regionId) {
        DeliveryRegion region = regionMapper.selectById(regionId);
        if (region == null) {
            throw new BusinessException(ResultCode.DELIVERY_REGION_NOT_EXIST);
        }
        return region;
    }

    private DeliveryMethod requireMethod(Long methodId) {
        if (methodId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配送方式ID不能为空");
        }
        DeliveryMethod method = methodMapper.selectById(methodId);
        if (method == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配送方式不存在");
        }
        return method;
    }

    private DeliveryStaff requireStaff(Long staffId) {
        DeliveryStaff staff = staffMapper.selectById(staffId);
        if (staff == null) {
            throw new BusinessException(ResultCode.DELIVERY_STAFF_NOT_EXIST);
        }
        return staff;
    }

    private void validateRegionCode(String regionCode, Long excludeId) {
        LambdaQueryWrapper<DeliveryRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryRegion::getRegionCode, regionCode);
        if (excludeId != null) {
            wrapper.ne(DeliveryRegion::getId, excludeId);
        }
        if (regionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配送区域编码已存在");
        }
    }

    private void validateMethodName(String methodName, Long excludeId) {
        LambdaQueryWrapper<DeliveryMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeliveryMethod::getMethodName, methodName);
        if (excludeId != null) {
            wrapper.ne(DeliveryMethod::getId, excludeId);
        }
        if (methodMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配送方式名称已存在");
        }
    }

    private void normalizeMethodForSave(DeliveryMethod method) {
        if (method.getMethodName() == null || method.getMethodName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "配送方式名称不能为空");
        }
        method.setMethodName(method.getMethodName().trim());
        if (method.getFreight() == null) {
            method.setFreight(java.math.BigDecimal.ZERO);
        }
        if (method.getFreeThreshold() == null) {
            method.setFreeThreshold(java.math.BigDecimal.ZERO);
        }
        if (method.getStatus() == null) {
            method.setStatus(1);
        }
    }

    private void normalizeDeliveryMethod(DeliveryMethod method) {
        if (method.getMethodCode() == null && method.getId() != null) {
            method.setMethodCode("DELIVERY_" + method.getId());
        }
        if (method.getEstimatedTime() == null) {
            method.setEstimatedTime(30);
        }
        if (method.getSortOrder() == null && method.getId() != null) {
            method.setSortOrder(method.getId().intValue());
        }
    }

    private List<DeliveryRegionTreeVO> buildRegionTree(List<DeliveryRegion> regions, Long parentId) {
        List<DeliveryRegionTreeVO> tree = new ArrayList<>();
        for (DeliveryRegion region : regions) {
            if (!isChildOf(region, parentId)) {
                continue;
            }

            DeliveryRegionTreeVO node = new DeliveryRegionTreeVO();
            BeanUtil.copyProperties(region, node);
            node.setStaffCount(Math.toIntExact(
                staffMapper.selectCount(new LambdaQueryWrapper<DeliveryStaff>().eq(DeliveryStaff::getRegionId, region.getId()))
            ));

            List<DeliveryRegionTreeVO> children = buildRegionTree(regions, region.getId());
            if (!children.isEmpty()) {
                node.setChildren(children);
            }
            tree.add(node);
        }
        return tree;
    }

    private boolean isChildOf(DeliveryRegion region, Long parentId) {
        Long currentParent = region.getParentId();
        if (parentId == null) {
            return currentParent == null || currentParent == 0L;
        }
        return Objects.equals(currentParent, parentId);
    }

    private List<DeliveryStaffVO> convertToStaffVOList(List<DeliveryStaff> staffList) {
        if (staffList == null || staffList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> regionIds = staffList.stream()
            .map(DeliveryStaff::getRegionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, String> regionNameMap = new HashMap<>();
        if (!regionIds.isEmpty()) {
            regionNameMap = regionMapper.selectBatchIds(regionIds).stream()
                .collect(Collectors.toMap(DeliveryRegion::getId, DeliveryRegion::getRegionName));
        }

        Map<Long, Long> activeOrderCountMap = new HashMap<>();
        for (DeliveryStaff staff : staffList) {
            Long count = deliveryOrderMapper.selectCount(
                new LambdaQueryWrapper<DeliveryOrder>()
                    .eq(DeliveryOrder::getDeliverymanId, staff.getId())
                    .lt(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_COMPLETED)
            );
            activeOrderCountMap.put(staff.getId(), count);
        }

        List<DeliveryStaffVO> result = new ArrayList<>(staffList.size());
        for (DeliveryStaff staff : staffList) {
            DeliveryStaffVO vo = convertToStaffVO(staff);
            vo.setRegionName(regionNameMap.get(staff.getRegionId()));
            vo.setCurrentOrders(Math.toIntExact(activeOrderCountMap.getOrDefault(staff.getId(), 0L)));
            result.add(vo);
        }
        return result;
    }

    private DeliveryStaffVO convertToStaffVO(DeliveryStaff staff) {
        DeliveryStaffVO vo = new DeliveryStaffVO();
        BeanUtil.copyProperties(staff, vo);
        vo.setStatusDesc(toStatusDesc(staff.getStatus()));
        if (staff.getHealthCertExpiry() != null) {
            vo.setHealthCertExpired(staff.getHealthCertExpiry().isBefore(LocalDateTime.now()));
        } else {
            vo.setHealthCertExpired(false);
        }
        return vo;
    }

    private String toStatusDesc(String status) {
        return switch (status) {
            case "IDLE" -> "空闲";
            case "BUSY" -> "配送中";
            case "OFFLINE" -> "离线";
            case "REST" -> "休息";
            default -> status;
        };
    }

    private String generateStaffCode() {
        return "DS" + System.currentTimeMillis();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
