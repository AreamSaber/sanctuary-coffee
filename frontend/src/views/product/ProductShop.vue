<template>
  <div class="app-page product-shop-page">
    <section class="app-page-header">
      <div>
        <h1 class="app-page-header__title">商品中心</h1>
      </div>
      <div class="app-page-actions">
        <el-button type="primary" @click="handleSearch">刷新货架</el-button>
      </div>
    </section>

    <el-card class="app-panel-card" shadow="never">
      <template #header>
        <div class="product-shop-page__section-head">
          <div>
            <strong>筛选条件</strong>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="app-toolbar">
        <el-form-item>
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索商品名称"
            clearable
            style="width: min(320px, 100%)"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="searchForm.categoryId"
            placeholder="全部分类"
            clearable
            style="width: min(220px, 100%)"
            @change="handleSearch"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <section v-if="activePromotions.length" class="scene-section product-shop-page__spotlight">
      <div class="product-shop-page__section-head">
        <div>
          <h2>当前活动橱窗</h2>
        </div>
        <el-tag effect="plain">{{ activePromotions.length }} 个进行中</el-tag>
      </div>

      <div class="product-shop-page__promo-list">
        <article
          v-for="promotion in activePromotions.slice(0, 3)"
          :key="promotion.id"
          class="product-shop-page__promo-item"
        >
          <el-tag :type="getPromotionTagStyle(promotion.type)" effect="plain" size="small">
            {{ getPromotionTypeName(promotion.type) }}
          </el-tag>
          <strong>{{ promotion.name }}</strong>
          <p>{{ getPromotionSummary(promotion) }}</p>
        </article>
      </div>
    </section>

    <section class="scene-section">
      <div class="product-shop-page__section-head product-shop-page__section-head--catalog">
        <div>
          <h2>编辑化货架</h2>
        </div>
        <span class="product-shop-page__count">{{ total }} 件商品</span>
      </div>

      <div class="products-grid" v-loading="loading">
        <el-empty v-if="products.length === 0 && !loading" description="暂无商品" />

        <div v-for="product in products" :key="product.id" class="product-card">
          <el-card :body-style="{ padding: '0px' }" shadow="hover">
            <div class="product-image" @click="viewProduct(product)">
              <ProductImage
                :src="product.mainImage"
                :name="product.productName"
                fit="cover"
              />

              <div class="product-tags">
                <el-tag v-if="product.isHot === 1" type="danger" size="small" effect="dark">热销</el-tag>
                <el-tag v-if="product.isNew === 1" type="success" size="small" effect="dark">新品</el-tag>
                <el-tag
                  v-if="productPromotionMap[product.id]"
                  :type="getPromotionTagStyle(productPromotionMap[product.id].type)"
                  size="small"
                  effect="dark"
                >
                  {{ getPromotionTypeName(productPromotionMap[product.id].type) }}
                </el-tag>
              </div>

              <div v-if="product.stock === 0" class="out-of-stock-mask">
                <span>已售罄</span>
              </div>
            </div>

            <div class="product-info">
              <h3 class="product-name" @click="viewProduct(product)">{{ product.productName }}</h3>
              <p class="product-desc">{{ product.description || '暂无描述' }}</p>

              <div v-if="productPromotionMap[product.id]" class="promotion-brief">
                <span class="promotion-brief__title">{{ productPromotionMap[product.id].name }}</span>
                <span class="promotion-brief__desc">
                  {{ getPromotionSummary(productPromotionMap[product.id]) }}
                </span>
              </div>

              <div class="product-meta">
                <div class="price-section">
                  <span class="current-price">¥{{ product.price }}</span>
                  <span v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice }}</span>
                </div>
                <div class="sales-info">
                  <span>销量 {{ product.sales || 0 }}</span>
                  <span>库存 {{ product.stock || 0 }}</span>
                </div>
              </div>

              <div class="product-actions">
                <el-button
                  type="primary"
                  :disabled="product.stock === 0"
                  @click="addToCart(product)"
                  style="flex: 1"
                >
                  <el-icon><ShoppingCart /></el-icon>
                  加入购物车
                </el-button>
                <el-button @click="viewReviews(product)">
                  <el-icon><Star /></el-icon>
                  评价
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <div class="app-pagination">
        <el-pagination
          v-if="total > 0"
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadProducts"
          @current-change="loadProducts"
        />
      </div>
    </section>

    <el-dialog
      v-model="detailDialog.visible"
      :title="detailDialog.product?.productName"
      width="min(880px, 92vw)"
    >
      <div v-if="detailDialog.product" class="product-detail">
        <el-row :gutter="24">
          <el-col :xs="24" :md="10">
            <ProductImage
              :src="getDisplayImage(detailDialog.product)"
              :name="detailDialog.product.productName"
              class="product-detail__image"
            />
          </el-col>
          <el-col :xs="24" :md="14">
            <div class="detail-info">
              <div class="detail-tags">
                <el-tag v-if="detailDialog.product.isHot === 1" type="danger" size="small">热销</el-tag>
                <el-tag v-if="detailDialog.product.isNew === 1" type="success" size="small">新品</el-tag>
                <el-tag v-if="detailDialog.product.isRecommend === 1" type="warning" size="small">推荐</el-tag>
                <el-tag
                  v-if="detailDialog.promotion"
                  :type="getPromotionTagStyle(detailDialog.promotion.type)"
                  size="small"
                >
                  {{ getPromotionTypeName(detailDialog.promotion.type) }}
                </el-tag>
              </div>

              <div class="detail-price">
                <span class="current">¥{{ getDisplayPrice(detailDialog.product) }}</span>
                <span v-if="detailDialog.product.originalPrice" class="original">
                  ¥{{ detailDialog.product.originalPrice }}
                </span>
              </div>

              <el-alert
                v-if="detailDialog.promotion"
                type="warning"
                :closable="false"
                show-icon
                class="detail-promotion"
              >
                <template #title>
                  {{ detailDialog.promotion.name }}
                </template>
                <div class="detail-promotion__desc">{{ getPromotionSummary(detailDialog.promotion) }}</div>
                <div v-if="detailDialog.promotion.description" class="detail-promotion__desc">
                  {{ detailDialog.promotion.description }}
                </div>
              </el-alert>

              <el-descriptions :column="1" border>
                <el-descriptions-item label="商品编码">{{ detailDialog.product.productCode }}</el-descriptions-item>
                <el-descriptions-item label="分类">{{ detailDialog.product.categoryName }}</el-descriptions-item>
                <el-descriptions-item label="单位">{{ detailDialog.product.unit }}</el-descriptions-item>
                <el-descriptions-item label="库存">{{ getDisplayStock(detailDialog.product) }}</el-descriptions-item>
                <el-descriptions-item label="销量">{{ detailDialog.product.sales }}</el-descriptions-item>
              </el-descriptions>

              <div v-if="resolveProductSkus(detailDialog.product).length" class="detail-sku">
                <h4>规格选择</h4>
                <el-radio-group
                  v-model="detailDialog.selectedSkuId"
                  class="detail-sku__group"
                  @change="handleSkuChange"
                >
                  <el-radio-button
                    v-for="sku in resolveProductSkus(detailDialog.product)"
                    :key="sku.id"
                    :label="sku.id"
                    :disabled="Number(sku.stock ?? 0) === 0"
                  >
                    {{ sku.skuName || sku.specInfo || sku.skuCode }}
                  </el-radio-button>
                </el-radio-group>
                <div
                  v-if="getSelectedSku(detailDialog.product)?.specInfo"
                  class="detail-sku__info"
                >
                  已选规格：{{ getSelectedSku(detailDialog.product).specInfo }}
                </div>
                <div v-else class="detail-sku__info detail-sku__info--placeholder">
                  请选择规格后加入购物车
                </div>
              </div>

              <div class="detail-desc">
                <h4>商品描述</h4>
                <p>{{ detailDialog.product.description || '暂无描述' }}</p>
              </div>

              <div class="detail-actions">
                <el-input-number
                  v-model="detailDialog.quantity"
                  :min="1"
                  :max="Math.max(getDisplayStock(detailDialog.product), 1)"
                  :disabled="getDisplayStock(detailDialog.product) === 0"
                  style="width: 120px"
                />
                <el-button
                  type="primary"
                  size="large"
                  :disabled="getDisplayStock(detailDialog.product) === 0 || (resolveProductSkus(detailDialog.product).length > 0 && !detailDialog.selectedSkuId)"
                  @click="addToCartWithQuantity"
                >
                  加入购物车
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <el-dialog
      v-model="reviewDialog.visible"
      :title="`${reviewDialog.product?.productName} · 商品评价`"
      width="min(960px, 94vw)"
      destroy-on-close
    >
      <ProductReviews
        v-if="reviewDialog.product"
        :productId="reviewDialog.product.id"
        :showStats="true"
        :showActions="false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Star } from '@element-plus/icons-vue'
import { addToCart as addToCartAPI } from '@/api/cart'
import ProductImage from '@/components/common/ProductImage.vue'
import { getCategoryList, getProductDetail, getProductPage } from '@/api/product'
import { getActivePromotions, getProductPromotion } from '@/api/promotion'
import ProductReviews from '@/views/review/ProductReviews.vue'

const loading = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const activePromotions = ref([])
const productPromotionMap = ref({})

const searchForm = reactive({
  pageNum: 1,
  pageSize: 12,
  keyword: '',
  categoryId: null,
  status: 1
})

const detailDialog = reactive({
  visible: false,
  product: null,
  quantity: 1,
  promotion: null,
  selectedSkuId: null
})

const reviewDialog = reactive({
  visible: false,
  product: null
})

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await getProductPage(searchForm)
    products.value = res.data.records || []
    total.value = res.data.total || 0
    await loadProductPromotions(products.value)
  } catch (error) {
    console.error('加载商品列表失败:', error)
    ElMessage.error('加载商品失败')
  } finally {
    loading.value = false
  }
}

const loadProductPromotions = async (list) => {
  if (!list.length) {
    productPromotionMap.value = {}
    return
  }

  const entries = await Promise.all(
    list.map(async (product) => {
      try {
        const res = await getProductPromotion(product.id)
        return [product.id, res.data || null]
      } catch (error) {
        return [product.id, null]
      }
    })
  )

  productPromotionMap.value = Object.fromEntries(entries)
}

const loadActivePromotionList = async () => {
  try {
    const res = await getActivePromotions()
    activePromotions.value = res.data || []
  } catch (error) {
    activePromotions.value = []
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const handleSearch = () => {
  searchForm.pageNum = 1
  loadProducts()
}

const getErrorMessage = (error, fallback) => {
  return error?.response?.data?.message || error?.message || error?.msg || fallback
}

const isSkuSelectionRequired = (message) => {
  return String(message || '').includes('请选择商品规格')
}

const resolveProductSkus = (product) => {
  const skuList = product?.skuList || product?.skus || product?.productSkuList || []
  return Array.isArray(skuList) ? skuList.filter(Boolean) : []
}

const getSelectedSku = (product = detailDialog.product) => {
  if (!product || !detailDialog.selectedSkuId) {
    return null
  }
  return resolveProductSkus(product).find((sku) => String(sku.id) === String(detailDialog.selectedSkuId)) || null
}

const getDisplayPrice = (product = detailDialog.product) => {
  return getSelectedSku(product)?.price ?? product?.price ?? 0
}

const getDisplayStock = (product = detailDialog.product) => {
  return Number(getSelectedSku(product)?.stock ?? product?.stock ?? 0)
}

const getDisplayImage = (product = detailDialog.product) => {
  return getSelectedSku(product)?.image || product?.mainImage
}

const syncDetailQuantity = () => {
  const stock = getDisplayStock()
  if (stock <= 0) {
    detailDialog.quantity = 1
    return
  }
  detailDialog.quantity = Math.min(Math.max(detailDialog.quantity, 1), stock)
}

const initDetailSelection = (product) => {
  const skuList = resolveProductSkus(product)
  detailDialog.selectedSkuId = skuList.length === 1 ? skuList[0].id : null
  detailDialog.quantity = 1
  syncDetailQuantity()
}

const viewProduct = async (product) => {
  detailDialog.product = product
  detailDialog.promotion = productPromotionMap.value[product.id] || null
  detailDialog.visible = true
  const targetProductId = product.id

  try {
    const res = await getProductDetail(product.id)
    const detailProduct = res.data || product
    if (detailDialog.product?.id !== targetProductId) {
      return
    }
    detailDialog.product = detailProduct

    const productIndex = products.value.findIndex((item) => item.id === product.id)
    if (productIndex > -1) {
      products.value[productIndex] = {
        ...products.value[productIndex],
        ...detailProduct
      }
    }
  } catch (error) {
    if (detailDialog.product?.id !== targetProductId) {
      return
    }
    console.error('加载商品详情失败:', error)
    ElMessage.error(getErrorMessage(error, '加载商品详情失败'))
    detailDialog.product = product
  } finally {
    if (detailDialog.product?.id === targetProductId) {
      initDetailSelection(detailDialog.product)
    }
  }
}

const viewReviews = (product) => {
  reviewDialog.product = product
  reviewDialog.visible = true
}

const submitAddToCart = async (product, quantity) => {
  const skuList = resolveProductSkus(product)
  const selectedSku = getSelectedSku(product)

  if (skuList.length > 0 && !selectedSku) {
    ElMessage.warning('请选择规格后再加入购物车')
    return false
  }

  const stock = selectedSku?.stock ?? product.stock
  if (Number(stock) === 0) {
    ElMessage.warning('该商品已售罄')
    return false
  }

  await addToCartAPI({
    productId: product.id,
    skuId: selectedSku?.id,
    quantity
  })
  return true
}

const addToCart = async (product) => {
  if (product.stock === 0) {
    ElMessage.warning('该商品已售罄')
    return
  }

  if (resolveProductSkus(product).length > 0) {
    viewProduct(product)
    return
  }

  try {
    await submitAddToCart(product, 1)
    ElMessage.success('已加入购物车')
  } catch (error) {
    console.error('加入购物车失败:', error)
    const message = getErrorMessage(error, '加入购物车失败')
    if (isSkuSelectionRequired(message)) {
      await viewProduct(product)
      return
    }
    ElMessage.error(message)
  }
}

const handleSkuChange = () => {
  syncDetailQuantity()
}

const addToCartWithQuantity = async () => {
  const product = detailDialog.product
  if (!product || getDisplayStock(product) === 0) {
    ElMessage.warning('该商品已售罄')
    return
  }

  try {
    const success = await submitAddToCart(product, detailDialog.quantity)
    if (!success) {
      return
    }
    ElMessage.success(`已加入 ${detailDialog.quantity} 件到购物车`)
    detailDialog.visible = false
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error(getErrorMessage(error, '加入购物车失败'))
  }
}

onMounted(() => {
  loadProducts()
  loadCategories()
  loadActivePromotionList()
})

const normalizePromotionType = (type) => {
  const value = String(type ?? '').toUpperCase()
  const typeMap = {
    1: 'DISCOUNT',
    2: 'REDUCTION',
    3: 'FLASH_SALE',
    DISCOUNT: 'DISCOUNT',
    REDUCTION: 'REDUCTION',
    GIFT: 'GIFT',
    FLASH_SALE: 'FLASH_SALE'
  }
  return typeMap[value] || value
}

const getPromotionTypeName = (type) => {
  const typeMap = {
    DISCOUNT: '折扣',
    REDUCTION: '满减',
    GIFT: '赠品',
    FLASH_SALE: '秒杀'
  }
  return typeMap[normalizePromotionType(type)] || '活动'
}

const getPromotionTagStyle = (type) => {
  const styleMap = {
    DISCOUNT: 'success',
    REDUCTION: 'warning',
    GIFT: 'danger',
    FLASH_SALE: 'info'
  }
  return styleMap[normalizePromotionType(type)] || 'warning'
}

const getPromotionSummary = (promotion) => {
  if (!promotion) {
    return ''
  }

  if (normalizePromotionType(promotion.type) === 'FLASH_SALE' && promotion.flashPrice) {
    return `秒杀价 ¥${promotion.flashPrice}，活动库存 ${promotion.stock ?? '-'}`
  }

  if (promotion.description) {
    return promotion.description
  }

  return '活动进行中，下单前可在详情页查看完整优惠信息。'
}
</script>

<style lang="scss" scoped>
.product-shop-page__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-4);

  strong,
  h2 {
    margin: 0;
    color: var(--color-text);
  }

  span,
  p {
    margin-top: 6px;
    color: var(--color-text-muted);
  }
}

.product-shop-page__section-head--catalog {
  margin-bottom: var(--spacing-5);
}

.product-shop-page__count {
  align-self: center;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.product-shop-page__spotlight {
  display: grid;
  gap: var(--spacing-5);
}

.product-shop-page__promo-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--spacing-4);
}

.product-shop-page__promo-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: var(--spacing-5);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(107, 101, 91, 0.08);

  strong {
    font-size: var(--text-lg);
    color: var(--color-text);
  }

  p {
    margin: 0;
    color: var(--color-text-secondary);
  }
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--spacing-5);
  min-height: 400px;
}

.product-card {
  transition: transform var(--transition-base);
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-card :deep(.el-card) {
  height: 100%;
}

.product-image {
  position: relative;
  height: 220px;
  overflow: hidden;
  cursor: pointer;
}

.image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: rgba(107, 101, 91, 0.08);
  color: var(--color-text-muted);
  font-size: 48px;
}

.product-tags {
  position: absolute;
  top: 14px;
  left: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.out-of-stock-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 13, 11, 0.56);

  span {
    font-size: var(--text-2xl);
    font-weight: var(--font-semibold);
    color: var(--color-text-inverse);
  }
}

.product-info {
  padding: 18px;
}

.product-name {
  margin: 0 0 8px;
  font-family: var(--font-serif);
  font-size: var(--text-xl);
  color: var(--color-text);
  cursor: pointer;
}

.product-name:hover {
  color: var(--color-primary);
}

.product-desc {
  display: -webkit-box;
  margin: 0 0 14px;
  min-height: 42px;
  overflow: hidden;
  color: var(--color-text-muted);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.promotion-brief {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(210, 170, 125, 0.12);
  border: 1px solid rgba(210, 170, 125, 0.18);
}

.promotion-brief__title {
  font-weight: var(--font-semibold);
  color: #936739;
}

.promotion-brief__desc {
  font-size: var(--text-sm);
  color: #8a6641;
}

.product-meta {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
}

.current-price {
  margin-right: 8px;
  font-family: var(--font-serif);
  font-size: 1.8rem;
  color: #b85a52;
}

.original-price {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.sales-info {
  display: flex;
  gap: 16px;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.product-actions {
  display: flex;
  gap: 8px;
}

.product-detail__image {
  width: 100%;
  border-radius: 24px;
}

.detail-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-price .current {
  margin-right: 12px;
  font-family: var(--font-serif);
  font-size: 2.4rem;
  color: #b85a52;
}

.detail-price .original {
  color: var(--color-text-muted);
  text-decoration: line-through;
}

.detail-promotion__desc {
  line-height: 1.7;
}

.detail-sku {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-sku h4 {
  margin: 0;
  font-size: var(--text-lg);
}

.detail-sku__group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-sku__info {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.detail-sku__info--placeholder {
  color: var(--color-text-muted);
}

.detail-desc h4 {
  margin-bottom: 10px;
  font-size: var(--text-lg);
}

.detail-desc p {
  margin: 0;
  color: var(--color-text-secondary);
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 768px) {
  .product-shop-page__section-head,
  .product-actions,
  .detail-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .products-grid {
    grid-template-columns: 1fr;
  }
}
</style>
