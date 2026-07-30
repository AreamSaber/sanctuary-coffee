package com.coffee.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.TradeConstants;
import com.coffee.entity.Product;
import com.coffee.entity.ProductSku;
import com.coffee.entity.ProductSpec;
import com.coffee.entity.ProductStockLog;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.ProductSkuMapper;
import com.coffee.mapper.ProductSpecMapper;
import com.coffee.mapper.ProductStockLogMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易库存辅助服务，统一处理 SKU / 商品库存与日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeInventoryService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductStockLogMapper stockLogMapper;

    public Product requireProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || Integer.valueOf(1).equals(product.getDeleted())) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }

    public List<ProductSku> listActiveSkus(Long productId) {
        return productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getDeleted, 0)
                .eq(ProductSku::getStatus, 1)
                .orderByAsc(ProductSku::getId)
        );
    }

    public List<ProductSpec> listProductSpecs(Long productId) {
        return productSpecMapper.selectList(
            new LambdaQueryWrapper<ProductSpec>()
                .eq(ProductSpec::getProductId, productId)
                .orderByAsc(ProductSpec::getId)
        );
    }

    public boolean hasActiveSku(Long productId) {
        return productSkuMapper.selectCount(
            new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
                .eq(ProductSku::getDeleted, 0)
                .eq(ProductSku::getStatus, 1)
        ) > 0;
    }

    public ProductSku requireActiveSku(Long productId, Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || Integer.valueOf(1).equals(sku.getDeleted()) || !productId.equals(sku.getProductId())) {
            throw new BusinessException("SKU不存在");
        }
        if (!Integer.valueOf(1).equals(sku.getStatus())) {
            throw new BusinessException("SKU已禁用");
        }
        return sku;
    }

    public ResolvedTradeItem resolveTradeItem(Long productId, Long skuId) {
        Product product = requireProduct(productId);
        if (!Integer.valueOf(1).equals(product.getStatus())) {
            throw new BusinessException("商品已下架");
        }

        boolean hasActiveSku = hasActiveSku(productId);
        if (skuId == null) {
            if (hasActiveSku) {
                throw new BusinessException("请选择商品规格");
            }
            return new ResolvedTradeItem(
                product.getId(),
                null,
                product.getProductName(),
                product.getMainImage(),
                null,
                defaultAmount(product.getPrice()),
                safeInt(product.getStock())
            );
        }

        ProductSku sku = requireActiveSku(productId, skuId);
        return new ResolvedTradeItem(
            product.getId(),
            sku.getId(),
            product.getProductName(),
            sku.getImage() != null && !sku.getImage().isBlank() ? sku.getImage() : product.getMainImage(),
            sku.getSpecInfo() != null && !sku.getSpecInfo().isBlank() ? sku.getSpecInfo() : sku.getSkuName(),
            defaultAmount(sku.getPrice()),
            safeInt(sku.getStock())
        );
    }

    public void validateSaleQuantity(Long productId, Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("购买数量必须大于0");
        }
        ResolvedTradeItem item = resolveTradeItem(productId, skuId);
        if (item.getStock() < quantity) {
            throw new BusinessException(item.getProductName() + " 库存不足");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long productId, Long skuId, Integer quantity, String remark, Long operatorId) {
        if (skuId != null) {
            ProductSku sku = requireActiveSku(productId, skuId);
            int beforeStock = safeInt(sku.getStock());
            int affectedRows = productSkuMapper.deductStock(skuId, quantity);
            if (affectedRows == 0) {
                throw new BusinessException("SKU库存不足");
            }
            logStock(productId, skuId, TradeConstants.STOCK_CHANGE_OUTBOUND, quantity,
                beforeStock, beforeStock - quantity, remark, operatorId);
            syncProductAggregate(productId);
            return;
        }

        Product product = requireProduct(productId);
        int beforeStock = safeInt(product.getStock());
        int affectedRows = productMapper.deductStock(productId, quantity);
        if (affectedRows == 0) {
            throw new BusinessException(product.getProductName() + " 库存不足或商品已下架");
        }
        logStock(productId, null, TradeConstants.STOCK_CHANGE_OUTBOUND, quantity,
            beforeStock, beforeStock - quantity, remark, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(Long productId, Long skuId, Integer quantity, String remark, Long operatorId) {
        if (skuId != null) {
            ProductSku sku = requireActiveSku(productId, skuId);
            int beforeStock = safeInt(sku.getStock());
            productSkuMapper.restoreStock(skuId, quantity);
            logStock(productId, skuId, TradeConstants.STOCK_CHANGE_RETURN, quantity,
                beforeStock, beforeStock + quantity, remark, operatorId);
            syncProductAggregate(productId);
            return;
        }

        Product product = requireProduct(productId);
        int beforeStock = safeInt(product.getStock());
        productMapper.restoreStock(productId, quantity);
        logStock(productId, null, TradeConstants.STOCK_CHANGE_RETURN, quantity,
            beforeStock, beforeStock + quantity, remark, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long productId, Long skuId, Integer stock, String remark, Long operatorId) {
        if (stock == null || stock < 0) {
            throw new BusinessException("库存不能小于0");
        }
        if (skuId != null) {
            ProductSku sku = requireActiveSku(productId, skuId);
            int beforeStock = safeInt(sku.getStock());
            sku.setStock(stock);
            productSkuMapper.updateById(sku);
            logStock(productId, skuId, TradeConstants.STOCK_CHANGE_ADJUST, stock - beforeStock,
                beforeStock, stock, defaultRemark(remark, "SKU库存调整"), operatorId);
            syncProductAggregate(productId);
            return;
        }

        if (hasActiveSku(productId)) {
            throw new BusinessException("当前商品存在多个SKU，请指定skuId后再调整库存");
        }

        Product product = requireProduct(productId);
        int beforeStock = safeInt(product.getStock());
        product.setStock(stock);
        productMapper.updateById(product);
        logStock(productId, null, TradeConstants.STOCK_CHANGE_ADJUST, stock - beforeStock,
            beforeStock, stock, defaultRemark(remark, "库存调整"), operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restock(Long productId, Long skuId, Integer quantity, String remark, Long operatorId) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("补货数量无效");
        }
        if (skuId != null) {
            ProductSku sku = requireActiveSku(productId, skuId);
            int beforeStock = safeInt(sku.getStock());
            productSkuMapper.restoreStock(skuId, quantity);
            logStock(productId, skuId, TradeConstants.STOCK_CHANGE_INBOUND, quantity,
                beforeStock, beforeStock + quantity, defaultRemark(remark, "SKU快速补货"), operatorId);
            syncProductAggregate(productId);
            return;
        }

        if (hasActiveSku(productId)) {
            throw new BusinessException("当前商品存在多个SKU，请指定skuId后再补货");
        }

        Product product = requireProduct(productId);
        int beforeStock = safeInt(product.getStock());
        productMapper.restoreStock(productId, quantity);
        logStock(productId, null, TradeConstants.STOCK_CHANGE_INBOUND, quantity,
            beforeStock, beforeStock + quantity, defaultRemark(remark, "快速补货"), operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncProductAggregate(Long productId) {
        Product product = requireProduct(productId);
        List<ProductSku> skuList = listActiveSkus(productId);
        if (skuList.isEmpty()) {
            return;
        }

        int totalStock = skuList.stream().mapToInt(item -> safeInt(item.getStock())).sum();
        BigDecimal minPrice = skuList.stream()
            .map(ProductSku::getPrice)
            .filter(price -> price != null)
            .min(BigDecimal::compareTo)
            .orElse(product.getPrice());

        product.setStock(totalStock);
        product.setPrice(minPrice);
        productMapper.updateById(product);
    }

    @Transactional(rollbackFor = Exception.class)
    public void lockStock(Long productId, Long skuId, Integer quantity, String remark, Long operatorId) {
        if (skuId != null) {
            ProductSku sku = requireActiveSku(productId, skuId);
            int beforeStock = safeInt(sku.getStock());
            int affected = productSkuMapper.lockStock(skuId, quantity);
            if (affected == 0) {
                throw new BusinessException(sku.getSkuName() + " 库存不足");
            }
            logStock(productId, skuId, TradeConstants.STOCK_CHANGE_OUTBOUND, quantity,
                beforeStock, beforeStock - quantity, remark, operatorId);
            syncProductAggregate(productId);
            return;
        }
        Product product = requireProduct(productId);
        int beforeStock = safeInt(product.getStock());
        int affected = productMapper.lockStock(productId, quantity);
        if (affected == 0) {
            throw new BusinessException(product.getProductName() + " 库存不足");
        }
        logStock(productId, null, TradeConstants.STOCK_CHANGE_OUTBOUND, quantity,
            beforeStock, beforeStock - quantity, remark, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseLockedStock(Long productId, Long skuId, Integer quantity, String remark, Long operatorId) {
        if (skuId != null) {
            ProductSku sku = requireActiveSku(productId, skuId);
            int beforeStock = safeInt(sku.getStock());
            productSkuMapper.releaseLockedStock(skuId, quantity);
            logStock(productId, skuId, TradeConstants.STOCK_CHANGE_RETURN, quantity,
                beforeStock, beforeStock + quantity, remark, operatorId);
            syncProductAggregate(productId);
            return;
        }
        Product product = requireProduct(productId);
        int beforeStock = safeInt(product.getStock());
        productMapper.releaseLockedStock(productId, quantity);
        logStock(productId, null, TradeConstants.STOCK_CHANGE_RETURN, quantity,
            beforeStock, beforeStock + quantity, remark, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int confirmLockedStock(Long productId, Long skuId, Integer quantity, String remark, Long operatorId) {
        if (skuId != null) {
            int affected = productSkuMapper.confirmLockedStock(skuId, quantity);
            if (affected > 0) {
                syncProductAggregate(productId);
            }
            return affected;
        }
        return productMapper.confirmLockedStock(productId, quantity);
    }

    private void logStock(Long productId, Long skuId, Integer changeType, Integer changeQuantity,
                          Integer beforeStock, Integer afterStock, String remark, Long operatorId) {
        ProductStockLog stockLog = new ProductStockLog();
        stockLog.setProductId(productId);
        stockLog.setSkuId(skuId);
        stockLog.setChangeType(changeType);
        stockLog.setChangeQuantity(changeQuantity);
        stockLog.setBeforeStock(beforeStock);
        stockLog.setAfterStock(afterStock);
        stockLog.setRemark(remark);
        stockLog.setOperatorId(operatorId);
        stockLogMapper.insert(stockLog);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String defaultRemark(String remark, String defaultRemark) {
        return remark == null || remark.isBlank() ? defaultRemark : remark;
    }

    @Getter
    @AllArgsConstructor
    public static class ResolvedTradeItem {
        private Long productId;
        private Long skuId;
        private String productName;
        private String productImage;
        private String specInfo;
        private BigDecimal price;
        private Integer stock;
    }
}
