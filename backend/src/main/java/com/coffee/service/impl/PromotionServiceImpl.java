package com.coffee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.PromotionDTO;
import com.coffee.entity.Product;
import com.coffee.entity.Promotion;
import com.coffee.entity.PromotionProduct;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.PromotionMapper;
import com.coffee.mapper.PromotionProductMapper;
import com.coffee.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Promotion service implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl extends ServiceImpl<PromotionMapper, Promotion> implements PromotionService {

    private static final int TYPE_DISCOUNT = 1;
    private static final int TYPE_REDUCTION = 2;
    private static final int TYPE_FLASH_SALE = 3;

    private final PromotionMapper promotionMapper;
    private final PromotionProductMapper promotionProductMapper;
    private final ProductMapper productMapper;

    @Override
    public IPage<Promotion> getPromotionPage(Integer pageNum, Integer pageSize, String keyword, String type, Integer status) {
        Page<Promotion> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Promotion> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(item -> item.like(Promotion::getName, keyword).or().like(Promotion::getDescription, keyword));
        }

        Integer typeCode = parseTypeCode(type, false);
        if (typeCode != null) {
            wrapper.eq(Promotion::getType, typeCode);
        }
        if (status != null) {
            wrapper.eq(Promotion::getStatus, status);
        }
        wrapper.orderByDesc(Promotion::getCreateTime);
        IPage<Promotion> result = promotionMapper.selectPage(page, wrapper);
        enrichPromotions(result.getRecords());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPromotion(PromotionDTO promotionDTO) {
        validatePromotionDTO(promotionDTO);

        Promotion promotion = new Promotion();
        applyPromotionDTO(promotionDTO, promotion);
        promotionMapper.insert(promotion);

        replacePromotionProducts(promotion.getId(), promotionDTO);
        log.info("Promotion created: promotionId={}, name={}", promotion.getId(), promotion.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePromotion(PromotionDTO promotionDTO) {
        if (promotionDTO.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "促销活动ID不能为空");
        }

        Promotion promotion = promotionMapper.selectById(promotionDTO.getId());
        if (promotion == null) {
            throw new BusinessException(ResultCode.PROMOTION_NOT_EXIST);
        }

        validatePromotionDTO(promotionDTO);
        applyPromotionDTO(promotionDTO, promotion);
        promotionMapper.updateById(promotion);

        replacePromotionProducts(promotion.getId(), promotionDTO);
        log.info("Promotion updated: promotionId={}", promotion.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePromotion(Long id) {
        Promotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.PROMOTION_NOT_EXIST);
        }

        promotionMapper.deleteById(id);
        promotionProductMapper.delete(
            new LambdaQueryWrapper<PromotionProduct>()
                .eq(PromotionProduct::getPromotionId, id)
        );
        log.info("Promotion deleted: promotionId={}", id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Promotion promotion = promotionMapper.selectById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.PROMOTION_NOT_EXIST);
        }

        promotion.setStatus(status);
        promotionMapper.updateById(promotion);
        log.info("Promotion status updated: promotionId={}, status={}", id, status);
    }

    @Override
    public List<Promotion> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionMapper.selectList(
            new LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, 1)
                .le(Promotion::getStartTime, now)
                .ge(Promotion::getEndTime, now)
                .orderByDesc(Promotion::getCreateTime)
        );
        enrichPromotions(promotions);
        return promotions;
    }

    @Override
    public Promotion getProductPromotion(Long productId) {
        List<PromotionProduct> relations = promotionProductMapper.selectList(
            new LambdaQueryWrapper<PromotionProduct>()
                .eq(PromotionProduct::getProductId, productId)
        );
        if (relations.isEmpty()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        for (PromotionProduct relation : relations) {
            Promotion promotion = promotionMapper.selectById(relation.getPromotionId());
            if (promotion != null
                && Integer.valueOf(1).equals(promotion.getStatus())
                && (promotion.getStartTime() == null || !promotion.getStartTime().isAfter(now))
                && (promotion.getEndTime() == null || !promotion.getEndTime().isBefore(now))) {
                promotion.setProductIds(new ArrayList<>(Collections.singletonList(productId)));
                promotion.setFlashPrice(relation.getPromotionPrice());
                promotion.setStock(relation.getStockLimit());
                return promotion;
            }
        }
        return null;
    }

    private void validatePromotionDTO(PromotionDTO promotionDTO) {
        if (promotionDTO.getStartTime() == null || promotionDTO.getEndTime() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "促销活动时间范围不能为空");
        }
        if (!promotionDTO.getEndTime().isAfter(promotionDTO.getStartTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (promotionDTO.getProductIds() == null || promotionDTO.getProductIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "至少选择一个活动商品");
        }
        parseTypeCode(promotionDTO.getType(), true);
    }

    private void applyPromotionDTO(PromotionDTO promotionDTO, Promotion promotion) {
        promotion.setName(promotionDTO.getName().trim());
        promotion.setDescription(promotionDTO.getDescription());
        promotion.setType(parseTypeCode(promotionDTO.getType(), true));
        promotion.setStartTime(promotionDTO.getStartTime());
        promotion.setEndTime(promotionDTO.getEndTime());
        promotion.setBanner(promotionDTO.getBanner());
        promotion.setStatus(promotionDTO.getStatus() == null ? 1 : promotionDTO.getStatus());
    }

    private void replacePromotionProducts(Long promotionId, PromotionDTO promotionDTO) {
        promotionProductMapper.delete(
            new LambdaQueryWrapper<PromotionProduct>()
                .eq(PromotionProduct::getPromotionId, promotionId)
        );

        if (promotionDTO.getProductIds() == null || promotionDTO.getProductIds().isEmpty()) {
            return;
        }

        for (Long productId : promotionDTO.getProductIds()) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
            }

            PromotionProduct relation = new PromotionProduct();
            relation.setPromotionId(promotionId);
            relation.setProductId(productId);
            relation.setPromotionPrice(resolvePromotionPrice(product, promotionDTO));
            relation.setStockLimit(resolveStockLimit(promotionDTO));
            promotionProductMapper.insert(relation);
        }
    }

    private BigDecimal resolvePromotionPrice(Product product, PromotionDTO promotionDTO) {
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        String type = normalizeTypeKey(promotionDTO.getType());

        if ("FLASH_SALE".equals(type) || TYPE_FLASH_SALE == parseTypeCode(promotionDTO.getType(), true)) {
            if (promotionDTO.getFlashPrice() != null) {
                return promotionDTO.getFlashPrice();
            }
            return price;
        }

        if ("DISCOUNT".equals(type)) {
            if (promotionDTO.getDiscountRate() == null || promotionDTO.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0) {
                return price;
            }
            return price.multiply(promotionDTO.getDiscountRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        if ("REDUCTION".equals(type)) {
            BigDecimal reduction = promotionDTO.getReductionAmount() == null ? BigDecimal.ZERO : promotionDTO.getReductionAmount();
            BigDecimal reducedPrice = price.subtract(reduction);
            return reducedPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : reducedPrice;
        }

        if ("GIFT".equals(type)) {
            return BigDecimal.ZERO;
        }

        return price;
    }

    private Integer resolveStockLimit(PromotionDTO promotionDTO) {
        if (promotionDTO.getStock() != null && promotionDTO.getStock() > 0) {
            return promotionDTO.getStock();
        }
        if (promotionDTO.getLimitPerUser() != null && promotionDTO.getLimitPerUser() > 0) {
            return promotionDTO.getLimitPerUser();
        }
        return null;
    }

    private Integer parseTypeCode(String rawType, boolean strict) {
        if (rawType == null || rawType.isBlank()) {
            if (strict) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "促销类型不能为空");
            }
            return null;
        }

        String type = normalizeTypeKey(rawType);
        return switch (type) {
            case "1", "DISCOUNT" -> TYPE_DISCOUNT;
            case "2", "REDUCTION" -> TYPE_REDUCTION;
            case "3", "FLASH_SALE", "GIFT" -> TYPE_FLASH_SALE;
            default -> {
                if (strict) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的促销类型");
                }
                yield null;
            }
        };
    }

    private String normalizeTypeKey(String rawType) {
        return rawType.trim().toUpperCase(Locale.ROOT);
    }

    private void enrichPromotions(List<Promotion> promotions) {
        if (promotions == null || promotions.isEmpty()) {
            return;
        }

        List<Long> promotionIds = promotions.stream()
            .map(Promotion::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
        if (promotionIds.isEmpty()) {
            return;
        }

        List<PromotionProduct> relations = promotionProductMapper.selectList(
            new LambdaQueryWrapper<PromotionProduct>()
                .in(PromotionProduct::getPromotionId, promotionIds)
        );
        Map<Long, List<PromotionProduct>> relationMap = relations.stream()
            .collect(Collectors.groupingBy(PromotionProduct::getPromotionId));

        promotions.forEach(promotion -> {
            List<PromotionProduct> currentRelations = relationMap.getOrDefault(promotion.getId(), List.of());
            promotion.setProductIds(currentRelations.stream()
                .map(PromotionProduct::getProductId)
                .distinct()
                .collect(Collectors.toList()));

            if (!currentRelations.isEmpty()) {
                PromotionProduct firstRelation = currentRelations.get(0);
                promotion.setFlashPrice(firstRelation.getPromotionPrice());
                promotion.setStock(firstRelation.getStockLimit());
            }
        });
    }
}
