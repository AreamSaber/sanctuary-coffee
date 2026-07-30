package com.coffee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coffee.common.ResultCode;
import com.coffee.common.exception.BusinessException;
import com.coffee.dto.ProductDTO;
import com.coffee.dto.ProductSkuDTO;
import com.coffee.dto.ProductSpecDTO;
import com.coffee.entity.Product;
import com.coffee.entity.ProductSku;
import com.coffee.entity.ProductSpec;
import com.coffee.mapper.ProductMapper;
import com.coffee.mapper.ProductSkuMapper;
import com.coffee.mapper.ProductSpecMapper;
import com.coffee.service.ProductService;
import com.coffee.service.support.TradeInventoryService;
import com.coffee.vo.ProductSkuVO;
import com.coffee.vo.ProductSpecVO;
import com.coffee.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSpecMapper productSpecMapper;
    private final TradeInventoryService tradeInventoryService;

    @Override
    public IPage<ProductVO> getProductPage(Integer pageNum, Integer pageSize, String keyword, Long categoryId, Integer status) {
        Page<ProductVO> page = new Page<>(pageNum, pageSize);
        IPage<ProductVO> result = productMapper.selectProductPage(page, keyword, categoryId, status);
        fillTradeMeta(result.getRecords());
        return result;
    }

    @Override
    public ProductVO getProductDetail(Long id) {
        ProductVO productVO = productMapper.selectProductVOById(id);
        if (productVO == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }
        fillTradeMeta(Collections.singletonList(productVO));
        return productVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProduct(ProductDTO productDTO) {
        Product existProduct = productMapper.selectOne(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, productDTO.getProductCode())
        );
        if (existProduct != null) {
            throw new BusinessException("商品编码已存在");
        }

        Product product = BeanUtil.copyProperties(productDTO, Product.class);
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getSales() == null) {
            product.setSales(0);
        }
        if (product.getIsHot() == null) {
            product.setIsHot(0);
        }
        if (product.getIsNew() == null) {
            product.setIsNew(0);
        }
        if (product.getIsRecommend() == null) {
            product.setIsRecommend(0);
        }
        if (product.getUnit() == null || product.getUnit().isBlank()) {
            product.setUnit("item");
        }

        productMapper.insert(product);
        syncSpecs(product.getId(), productDTO.getSpecList());
        syncSkus(product.getId(), productDTO.getSkuList());
        if (tradeInventoryService.hasActiveSku(product.getId())) {
            tradeInventoryService.syncProductAggregate(product.getId());
        }
        log.info("添加商品成功: {}", product.getProductName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductDTO productDTO) {
        Product product = productMapper.selectById(productDTO.getId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }

        if (!product.getProductCode().equals(productDTO.getProductCode())) {
            Product existProduct = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                    .eq(Product::getProductCode, productDTO.getProductCode())
                    .ne(Product::getId, productDTO.getId())
            );
            if (existProduct != null) {
                throw new BusinessException("商品编码已存在");
            }
        }

        BeanUtil.copyProperties(productDTO, product, "id", "sales", "createTime");
        productMapper.updateById(product);
        syncSpecs(product.getId(), productDTO.getSpecList());
        syncSkus(product.getId(), productDTO.getSkuList());
        if (tradeInventoryService.hasActiveSku(product.getId())) {
            tradeInventoryService.syncProductAggregate(product.getId());
        }
        log.info("更新商品成功: {}", product.getProductName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }

        productMapper.deleteById(id);
        ProductSku deletedSku = new ProductSku();
        deletedSku.setDeleted(1);
        productSkuMapper.update(
            deletedSku,
            new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, id)
                .eq(ProductSku::getDeleted, 0)
        );
        productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, id));
        log.info("删除商品成功: {}", product.getProductName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onShelf(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }

        product.setStatus(1);
        productMapper.updateById(product);
        log.info("商品上架成功: {}", product.getProductName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXIST);
        }

        product.setStatus(0);
        productMapper.updateById(product);
        log.info("商品下架成功: {}", product.getProductName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long id, Long skuId, Integer stock, Long operatorId) {
        tradeInventoryService.adjustStock(id, skuId, stock, "库存调整", operatorId);
        log.info("商品库存更新成功: productId={}, skuId={}, stock={}", id, skuId, stock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restock(Long id, Long skuId, Integer quantity, String remark, Long operatorId) {
        tradeInventoryService.restock(id, skuId, quantity, remark, operatorId);
        log.info("商品补货成功: productId={}, skuId={}, quantity={}", id, skuId, quantity);
    }

    private void fillTradeMeta(List<ProductVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> productIds = records.stream().map(ProductVO::getId).distinct().toList();
        List<ProductSku> skuList = productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSku>()
                .in(ProductSku::getProductId, productIds)
                .eq(ProductSku::getDeleted, 0)
                .orderByAsc(ProductSku::getId)
        );
        List<ProductSpec> specList = productSpecMapper.selectList(
            new LambdaQueryWrapper<ProductSpec>()
                .in(ProductSpec::getProductId, productIds)
                .orderByAsc(ProductSpec::getId)
        );

        Map<Long, List<ProductSkuVO>> skuMap = skuList.stream().collect(Collectors.groupingBy(
            ProductSku::getProductId,
            Collectors.mapping(item -> BeanUtil.copyProperties(item, ProductSkuVO.class), Collectors.toList())
        ));
        Map<Long, List<ProductSpecVO>> specMap = specList.stream().collect(Collectors.groupingBy(
            ProductSpec::getProductId,
            Collectors.mapping(item -> BeanUtil.copyProperties(item, ProductSpecVO.class), Collectors.toList())
        ));

        records.forEach(item -> {
            List<ProductSkuVO> skuVOList = skuMap.getOrDefault(item.getId(), List.of());
            item.setSkuList(skuVOList);
            item.setSpecList(specMap.getOrDefault(item.getId(), List.of()));
            item.setHasSku(!skuVOList.isEmpty());
        });
    }

    private void syncSpecs(Long productId, List<ProductSpecDTO> specList) {
        if (specList == null) {
            return;
        }

        productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, productId));
        for (ProductSpecDTO item : specList) {
            ProductSpec spec = new ProductSpec();
            spec.setProductId(productId);
            spec.setSpecName(item.getSpecName());
            spec.setSpecValues(item.getSpecValues());
            productSpecMapper.insert(spec);
        }
    }

    private void syncSkus(Long productId, List<ProductSkuDTO> skuList) {
        if (skuList == null) {
            return;
        }

        List<ProductSku> existingSkuList = productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId)
        );
        Map<Long, ProductSku> existingMap = existingSkuList.stream()
            .filter(item -> item.getId() != null)
            .collect(Collectors.toMap(ProductSku::getId, item -> item));

        Set<Long> keepIds = new HashSet<>();
        Set<String> requestSkuCodes = new HashSet<>();

        for (ProductSkuDTO item : skuList) {
            String normalizedCode = item.getSkuCode().trim();
            if (!requestSkuCodes.add(normalizedCode)) {
                throw new BusinessException("SKU编码重复: " + normalizedCode);
            }

            ProductSku duplicatedSku = productSkuMapper.selectOne(
                new LambdaQueryWrapper<ProductSku>()
                    .eq(ProductSku::getSkuCode, normalizedCode)
                    .eq(ProductSku::getDeleted, 0)
                    .ne(item.getId() != null, ProductSku::getId, item.getId())
            );
            if (duplicatedSku != null) {
                throw new BusinessException("SKU编码已存在: " + normalizedCode);
            }

            ProductSku target;
            if (item.getId() != null) {
                target = existingMap.get(item.getId());
                if (target == null || !productId.equals(target.getProductId())) {
                    throw new BusinessException("SKU不存在或不属于当前商品");
                }
            } else {
                target = new ProductSku();
                target.setProductId(productId);
                target.setDeleted(0);
            }

            BeanUtil.copyProperties(item, target, "id");
            target.setProductId(productId);
            target.setSkuCode(normalizedCode);
            if (target.getStatus() == null) {
                target.setStatus(1);
            }
            if (item.getId() == null) {
                productSkuMapper.insert(target);
            } else {
                productSkuMapper.updateById(target);
            }
            keepIds.add(target.getId());
        }

        existingSkuList.stream()
            .filter(item -> item.getId() != null)
            .filter(item -> !keepIds.contains(item.getId()))
            .filter(item -> !Integer.valueOf(1).equals(item.getDeleted()))
            .forEach(item -> {
                item.setDeleted(1);
                productSkuMapper.updateById(item);
            });
    }
}
