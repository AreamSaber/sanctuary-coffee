package com.coffee.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coffee.common.exception.BusinessException;
import com.coffee.common.util.TradeConstants;
import com.coffee.entity.Product;
import com.coffee.entity.ProductSku;
import com.coffee.entity.ProductStockLog;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.ProductSkuMapper;
import com.coffee.mapper.ProductSpecMapper;
import com.coffee.mapper.ProductStockLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeInventoryServiceTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    @Mock
    private ProductSpecMapper productSpecMapper;

    @Mock
    private ProductStockLogMapper stockLogMapper;

    @InjectMocks
    private TradeInventoryService tradeInventoryService;

    @Test
    void deductStockWithSkuWritesOutboundLogAndSyncsProductAggregate() {
        Long productId = 1L;
        Long skuId = 10L;
        Long operatorId = 99L;
        Product product = product(productId, "Latte", "30.00", 15);
        ProductSku sku = sku(productId, skuId, "Large / Iced", "32.00", 10);
        List<ProductSku> syncedSkus = List.of(
            sku(productId, skuId, "Large / Iced", "32.00", 7),
            sku(productId, 11L, "Small / Hot", "28.00", 5)
        );

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
        when(productSkuMapper.deductStock(skuId, 3)).thenReturn(1);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productSkuMapper.selectList(anySkuWrapper())).thenReturn(syncedSkus);

        tradeInventoryService.deductStock(productId, skuId, 3, "paid order", operatorId);

        verify(productSkuMapper).deductStock(skuId, 3);

        ArgumentCaptor<ProductStockLog> logCaptor = ArgumentCaptor.forClass(ProductStockLog.class);
        verify(stockLogMapper).insert(logCaptor.capture());
        ProductStockLog log = logCaptor.getValue();
        assertEquals(productId, log.getProductId());
        assertEquals(skuId, log.getSkuId());
        assertEquals(TradeConstants.STOCK_CHANGE_OUTBOUND, log.getChangeType());
        assertEquals(3, log.getChangeQuantity());
        assertEquals(10, log.getBeforeStock());
        assertEquals(7, log.getAfterStock());
        assertEquals("paid order", log.getRemark());
        assertEquals(operatorId, log.getOperatorId());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(productCaptor.capture());
        Product syncedProduct = productCaptor.getValue();
        assertEquals(12, syncedProduct.getStock());
        assertEquals(new BigDecimal("28.00"), syncedProduct.getPrice());
    }

    @Test
    void deductStockWithSkuThrowsWhenMapperCannotDeductAndDoesNotLog() {
        Long productId = 1L;
        Long skuId = 10L;
        ProductSku sku = sku(productId, skuId, "Large / Iced", "32.00", 2);

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
        when(productSkuMapper.deductStock(skuId, 3)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> tradeInventoryService.deductStock(productId, skuId, 3, "paid order", 99L));

        assertEquals("SKU库存不足", exception.getMessage());
        verify(stockLogMapper, never()).insert(any(ProductStockLog.class));
        verify(productMapper, never()).updateById(any(Product.class));
        verify(productSkuMapper, never()).selectList(anySkuWrapper());
    }

    @Test
    void restoreStockWithSkuWritesReturnLogAndSyncsProductAggregate() {
        Long productId = 1L;
        Long skuId = 10L;
        Long operatorId = 99L;
        Product product = product(productId, "Latte", "30.00", 7);
        ProductSku sku = sku(productId, skuId, "Large / Iced", "32.00", 4);
        List<ProductSku> syncedSkus = List.of(
            sku(productId, skuId, "Large / Iced", "32.00", 10),
            sku(productId, 11L, "Small / Hot", "28.00", 3)
        );

        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
        when(productMapper.selectById(productId)).thenReturn(product);
        when(productSkuMapper.selectList(anySkuWrapper())).thenReturn(syncedSkus);

        tradeInventoryService.restoreStock(productId, skuId, 6, "refund", operatorId);

        verify(productSkuMapper).restoreStock(skuId, 6);

        ArgumentCaptor<ProductStockLog> logCaptor = ArgumentCaptor.forClass(ProductStockLog.class);
        verify(stockLogMapper).insert(logCaptor.capture());
        ProductStockLog log = logCaptor.getValue();
        assertEquals(productId, log.getProductId());
        assertEquals(skuId, log.getSkuId());
        assertEquals(TradeConstants.STOCK_CHANGE_RETURN, log.getChangeType());
        assertEquals(6, log.getChangeQuantity());
        assertEquals(4, log.getBeforeStock());
        assertEquals(10, log.getAfterStock());
        assertEquals("refund", log.getRemark());
        assertEquals(operatorId, log.getOperatorId());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).updateById(productCaptor.capture());
        Product syncedProduct = productCaptor.getValue();
        assertEquals(13, syncedProduct.getStock());
        assertEquals(new BigDecimal("28.00"), syncedProduct.getPrice());
    }

    @Test
    void resolveAndValidateRequireSkuWhenProductHasActiveSku() {
        Long productId = 1L;
        Product product = product(productId, "Latte", "30.00", 15);

        when(productMapper.selectById(productId)).thenReturn(product);
        when(productSkuMapper.selectCount(anySkuWrapper())).thenReturn(1L);

        BusinessException resolveException = assertThrows(BusinessException.class,
            () -> tradeInventoryService.resolveTradeItem(productId, null));
        BusinessException validateException = assertThrows(BusinessException.class,
            () -> tradeInventoryService.validateSaleQuantity(productId, null, 1));

        assertEquals("请选择商品规格", resolveException.getMessage());
        assertEquals("请选择商品规格", validateException.getMessage());
        verify(productSkuMapper, never()).selectById(any(Long.class));
        verify(stockLogMapper, never()).insert(any(ProductStockLog.class));
    }

    @Test
    void validateSaleQuantityThrowsWhenSkuStockIsNotEnough() {
        Long productId = 1L;
        Long skuId = 10L;
        Product product = product(productId, "Latte", "30.00", 15);
        ProductSku sku = sku(productId, skuId, "Large / Iced", "32.00", 2);

        when(productMapper.selectById(productId)).thenReturn(product);
        when(productSkuMapper.selectCount(anySkuWrapper())).thenReturn(1L);
        when(productSkuMapper.selectById(skuId)).thenReturn(sku);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> tradeInventoryService.validateSaleQuantity(productId, skuId, 3));

        assertTrue(exception.getMessage().contains("库存不足"));
    }

    private Product product(Long id, String name, String price, Integer stock) {
        Product product = new Product();
        product.setId(id);
        product.setProductName(name);
        product.setMainImage("product.jpg");
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }

    private ProductSku sku(Long productId, Long skuId, String specInfo, String price, Integer stock) {
        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setProductId(productId);
        sku.setSkuName(specInfo);
        sku.setSpecInfo(specInfo);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        sku.setStatus(1);
        sku.setDeleted(0);
        return sku;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryWrapper<ProductSku> anySkuWrapper() {
        return any(LambdaQueryWrapper.class);
    }
}
