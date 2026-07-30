<template>
  <div class="app-page add-review-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">发表评论</h1>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/order')">返回订单</el-button>
      </div>
    </section>

    <section v-if="!hasReviewContext" class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never">
        <div class="stack-card__body">
          <el-empty description="请先从已完成订单中选择商品后再评价">
            <div class="add-review-page__empty-actions">
              <el-button type="primary" @click="router.push('/order')">去订单中心</el-button>
              <el-button @click="router.push('/review/my')">查看我的评价</el-button>
            </div>
          </el-empty>
        </div>
      </el-card>
    </section>

    <section v-else class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never" v-loading="loading">
        <div class="stack-card__body">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="商品信息">
              <div class="product-info">
                <ProductImage
                  :src="productInfo.image"
                  :name="productInfo.name"
                  fit="cover"
                  class="product-info__image"
                />
                <div class="product-info__copy">
                  <span class="product-info__name">{{ productInfo.name }}</span>
                  <span v-if="productInfo.specInfo" class="product-info__spec">{{ productInfo.specInfo }}</span>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="商品评分" prop="rating">
              <el-rate
                v-model="form.rating"
                :texts="['非常差', '差', '一般', '好', '非常好']"
                show-text
                :colors="['#99a9bf', '#f7ba2a', '#ff9900']"
              />
            </el-form-item>

            <el-form-item label="评价内容" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="5"
                placeholder="请分享您的购买体验，对其他买家很有帮助哦~"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="上传图片">
              <el-upload
                v-model:file-list="fileList"
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :limit="5"
                accept="image/*"
                :before-upload="beforeReviewImageUpload"
                @preview="handlePreview"
                @remove="handleRemove"
              >
                <el-icon><Plus /></el-icon>
                <template #tip>
                  <div class="upload-tip">最多上传 5 张图片，单张不超过 5MB</div>
                </template>
              </el-upload>
            </el-form-item>

            <el-form-item label="匿名评价">
              <el-switch v-model="form.isAnonymous" />
              <span class="anonymous-tip">开启后将隐藏您的昵称和头像</span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSubmit">提交评价</el-button>
              <el-button @click="router.push('/order')">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </section>

    <el-dialog v-model="previewVisible">
      <img :src="previewUrl" alt="Preview" style="width: 100%" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ProductImage from '@/components/common/ProductImage.vue'
import { uploadImages as uploadReviewImageFiles } from '@/api/file'
import { addReview } from '@/api/review'
import { getProductDetail } from '@/api/product'
import { getOrderDetail } from '@/api/order'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const formRef = ref()
const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')
const orderId = ref(null)
const productId = ref(null)
const itemId = ref(null)

const productInfo = ref({
  id: null,
  name: '',
  image: '',
  specInfo: '',
  skuId: null
})

const form = reactive({
  productId: null,
  orderId: null,
  rating: 5,
  content: '',
  images: '',
  isAnonymous: false
})

const rules = {
  rating: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入评价内容', trigger: 'blur' },
    { min: 5, max: 500, message: '评价内容长度在 5-500 个字符之间', trigger: 'blur' }
  ]
}

const hasReviewContext = computed(() => {
  return Number.isFinite(orderId.value) && orderId.value > 0 && Number.isFinite(productId.value) && productId.value > 0
})

const resetPageState = () => {
  loading.value = false
  orderId.value = null
  productId.value = null
  itemId.value = null
  productInfo.value = {
    id: null,
    name: '',
    image: '',
    specInfo: '',
    skuId: null
  }
  Object.assign(form, {
    productId: null,
    orderId: null,
    rating: 5,
    content: '',
    images: '',
    isAnonymous: false
  })
  fileList.value = []
  previewVisible.value = false
  previewUrl.value = ''
  formRef.value?.clearValidate()
}

const clearReviewContext = () => {
  orderId.value = null
  productId.value = null
  itemId.value = null
  form.productId = null
  form.orderId = null
  productInfo.value = {
    id: null,
    name: '',
    image: '',
    specInfo: '',
    skuId: null
  }
}

const initializePage = async (rawOrderId, rawProductId, rawItemId) => {
  resetPageState()

  const parsedOrderId = Number(rawOrderId)
  const parsedProductId = Number(rawProductId)
  const parsedItemId = Number(rawItemId)
  if (!Number.isFinite(parsedOrderId) || parsedOrderId <= 0 || !Number.isFinite(parsedProductId) || parsedProductId <= 0) {
    return
  }

  orderId.value = parsedOrderId
  productId.value = parsedProductId
  itemId.value = Number.isFinite(parsedItemId) && parsedItemId > 0 ? parsedItemId : null
  form.orderId = parsedOrderId
  form.productId = parsedProductId

  await loadReviewProductInfo(parsedOrderId, parsedProductId, itemId.value)
}

const beforeReviewImageUpload = (file) => {
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return false
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('单张图片不能超过 5MB')
    return false
  }

  return true
}

const loadReviewProductInfo = async (targetOrderId, targetProductId, targetItemId) => {
  loading.value = true
  try {
    const res = await getOrderDetail(targetOrderId)
    const order = res.data || {}
    const items = order.items || []
    const orderItem = targetItemId
      ? items.find((item) => String(item.id) === String(targetItemId))
      : items.find((item) => String(item.productId) === String(targetProductId))

    if (orderItem) {
      productInfo.value = {
        id: orderItem.productId,
        name: orderItem.productName,
        image: orderItem.productImage,
        specInfo: orderItem.specInfo || '',
        skuId: orderItem.skuId || null
      }
      return
    }

    await loadProductInfo(targetProductId, false)
  } catch (error) {
    await loadProductInfo(targetProductId, false)
  } finally {
    loading.value = false
  }
}

const loadProductInfo = async (targetProductId, manageLoading = true) => {
  if (manageLoading) {
    loading.value = true
  }

  try {
    const res = await getProductDetail(targetProductId)
    const product = res.data
    productInfo.value = {
      id: product.id,
      name: product.productName,
      image: product.mainImage,
      specInfo: '',
      skuId: null
    }
  } catch (error) {
    clearReviewContext()
    ElMessage.error(error.message || '加载商品信息失败')
  } finally {
    if (manageLoading) {
      loading.value = false
    }
  }
}

const handlePreview = (file) => {
  previewUrl.value = file.url || file.response?.url || file.response?.data?.url || (file.raw ? URL.createObjectURL(file.raw) : '')
  previewVisible.value = true
}

const handleRemove = (file) => {
  const index = fileList.value.indexOf(file)
  if (index > -1) {
    fileList.value.splice(index, 1)
  }
}

const uploadReviewImages = async () => {
  const rawFiles = fileList.value
    .map((file) => file.raw)
    .filter(Boolean)

  if (rawFiles.length === 0) {
    return ''
  }

  const res = await uploadReviewImageFiles(rawFiles, 'review')
  const imageUrls = (res.data || []).map((item) => item.url)
  return JSON.stringify(imageUrls)
}

const handleSubmit = async () => {
  if (!hasReviewContext.value) {
    ElMessage.warning('请先从订单中选择要评价的商品')
    return
  }

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (fileList.value.length > 0) {
        form.images = await uploadReviewImages()
      } else {
        form.images = ''
      }

      await addReview(form)
      ElMessage.success('评价成功')
      router.push('/order')
    } catch (error) {
      ElMessage.error(error.message || '评价失败')
    }
  })
}

watch(
  () => [route.query.orderId, route.query.productId, route.query.itemId],
  async ([rawOrderId, rawProductId, rawItemId]) => {
    await initializePage(rawOrderId, rawProductId, rawItemId)
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.product-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.product-info__image {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  overflow: hidden;
  flex-shrink: 0;
}

.product-info__name {
  font-size: 14px;
  color: #303133;
}

.product-info__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-info__spec {
  color: #909399;
  font-size: 12px;
}

.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.anonymous-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.add-review-page__empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--spacing-3);
}
</style>
