package com.coffee.service.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.coffee.common.exception.BusinessException;
import com.coffee.entity.Product;
import com.coffee.entity.ProductSku;
import com.coffee.entity.ProductStockLog;
import com.coffee.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeInventoryLockTest {

    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private ProductSpecMapper productSpecMapper;
    @Mock private ProductStockLogMapper stockLogMapper;
    @InjectMocks private TradeInventoryService tradeInventoryService;

    @BeforeAll
    static void initMybatisPlus() {
        MybatisConfiguration cfg = new MybatisConfiguration();
        MapperBuilderAssistant asst = new MapperBuilderAssistant(cfg, "");
        TableInfoHelper.initTableInfo(asst, Product.class);
        TableInfoHelper.initTableInfo(asst, ProductSku.class);
        TableInfoHelper.initTableInfo(asst, ProductStockLog.class);
    }

    @Test
    void lockStockReducesAvailableAndIncreasesLocked() {
        ProductSku sku = new ProductSku();
        sku.setId(1L); sku.setProductId(10L); sku.setStatus(1); sku.setStock(100); sku.setDeleted(0);
        when(productSkuMapper.selectById(1L)).thenReturn(sku);
        when(productSkuMapper.lockStock(1L, 3)).thenReturn(1);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        when(productMapper.selectById(10L)).thenReturn(new Product());

        tradeInventoryService.lockStock(10L, 1L, 3, "test", 99L);

        verify(productSkuMapper).lockStock(1L, 3);
        verify(stockLogMapper).insert(any(ProductStockLog.class));
    }

    @Test
    void lockStockThrowsWhenInsufficientStock() {
        ProductSku sku = new ProductSku();
        sku.setId(1L); sku.setProductId(10L); sku.setStatus(1); sku.setStock(2); sku.setDeleted(0);
        when(productSkuMapper.selectById(1L)).thenReturn(sku);
        when(productSkuMapper.lockStock(1L, 5)).thenReturn(0);

        assertThrows(BusinessException.class,
            () -> tradeInventoryService.lockStock(10L, 1L, 5, "test", 99L));
    }

    @Test
    void releaseLockedStockReturnsStockAndDecreasesLocked() {
        ProductSku sku = new ProductSku();
        sku.setId(1L); sku.setProductId(10L); sku.setStatus(1); sku.setStock(97); sku.setDeleted(0);
        when(productSkuMapper.selectById(1L)).thenReturn(sku);
        when(productSkuMapper.releaseLockedStock(1L, 3)).thenReturn(1);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        when(productMapper.selectById(10L)).thenReturn(new Product());

        tradeInventoryService.releaseLockedStock(10L, 1L, 3, "test", 99L);

        verify(productSkuMapper).releaseLockedStock(1L, 3);
        verify(stockLogMapper).insert(any(ProductStockLog.class));
    }

    @Test
    void confirmLockedStockReturnsAffectedRows() {
        ProductSku sku = new ProductSku();
        sku.setId(1L); sku.setProductId(10L); sku.setStatus(1); sku.setStock(97); sku.setDeleted(0);
        when(productSkuMapper.confirmLockedStock(1L, 3)).thenReturn(1);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        when(productMapper.selectById(10L)).thenReturn(new Product());

        int result = tradeInventoryService.confirmLockedStock(10L, 1L, 3, "test", 99L);

        assertEquals(1, result);
        verify(productSkuMapper).confirmLockedStock(1L, 3);
    }

    @Test
    void confirmLockedStockReturnsZeroWhenNoPreLockedInventory() {
        when(productSkuMapper.confirmLockedStock(1L, 3)).thenReturn(0);

        int result = tradeInventoryService.confirmLockedStock(10L, 1L, 3, "test", 99L);

        assertEquals(0, result);
    }
}
