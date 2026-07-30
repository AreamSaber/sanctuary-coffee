<template>
  <div class="app-page review-manage-page">
    <section class="app-page-header">
      <div>
        <p class="app-page-header__eyebrow">Reviews</p>
        <h1 class="app-page-header__title">评价总览</h1>
        <p class="app-page-header__desc">
          面向运营和后台统一查看全站商品评价，按商品和评分快速筛选，确认前端已接通管理员评价分页接口。
        </p>
      </div>
    </section>

    <el-card class="app-panel-card">
      <div class="app-toolbar review-toolbar">
        <el-select v-model="searchForm.productId" clearable filterable placeholder="筛选商品">
          <el-option
            v-for="product in products"
            :key="product.id"
            :label="product.productName"
            :value="product.id"
          />
        </el-select>
        <el-select v-model="searchForm.rating" clearable placeholder="评分">
          <el-option v-for="item in ratingOptions" :key="item" :label="`${item} 星`" :value="item" />
        </el-select>
        <el-select v-model="searchForm.status" clearable placeholder="展示状态">
          <el-option label="显示中" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </div>

      <div class="app-table-shell">
        <el-table :data="reviews" border v-loading="loading">
          <el-table-column prop="id" label="评价ID" width="90" />
          <el-table-column label="商品" min-width="240">
            <template #default="{ row }">
              <div class="product-cell">
                <el-image
                  :src="row.productImage"
                  fit="cover"
                  class="product-cell__image"
                  :preview-src-list="row.productImage ? [row.productImage] : []"
                />
                <div class="product-cell__copy">
                  <strong>{{ row.productName }}</strong>
                  <span v-if="row.specInfo">{{ row.specInfo }}</span>
                  <span>#{{ row.productId }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="用户" width="180">
            <template #default="{ row }">
              <div class="user-cell">
                <strong>{{ row.nickname }}</strong>
                <div class="user-cell__meta">
                  <span>ID {{ row.userId }}</span>
                  <el-tag v-if="row.isAnonymous" size="small" type="info" effect="plain">匿名</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="160">
            <template #default="{ row }">
              <div class="rating-cell">
                <el-rate v-model="row.rating" disabled />
                <span>{{ row.rating }} 星</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'info' : 'success'" effect="plain">
                {{ row.status === 0 ? '已隐藏' : '显示中' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="320" show-overflow-tooltip />
          <el-table-column label="商家回复" min-width="280">
            <template #default="{ row }">
              <div v-if="row.replyContent" class="reply-cell">
                <p>{{ row.replyContent }}</p>
                <span>{{ formatTime(row.replyTime) }}</span>
              </div>
              <span v-else class="empty-copy">未回复</span>
            </template>
          </el-table-column>
          <el-table-column label="配图" width="180">
            <template #default="{ row }">
              <div v-if="row.imageList?.length" class="image-grid">
                <el-image
                  v-for="(image, index) in row.imageList.slice(0, 3)"
                  :key="`${row.id}-${index}`"
                  :src="image"
                  fit="cover"
                  class="image-grid__item"
                  :preview-src-list="row.imageList"
                  :initial-index="index"
                />
              </div>
              <span v-else class="empty-copy">无配图</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                :loading="replyingId === row.id"
                @click="handleReplyReview(row)"
              >
                {{ row.replyContent ? '修改回复' : '回复' }}
              </el-button>
              <el-button
                v-if="row.status !== 0"
                type="warning"
                link
                @click="handleHideReview(row)"
              >
                隐藏
              </el-button>
              <el-button
                v-else
                type="success"
                link
                @click="handleRestoreReview(row)"
              >
                恢复
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="app-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadReviews"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getProductPage } from '@/api/product'
import { getAllReviews, hideReview, replyReview, restoreReview } from '@/api/review'

const loading = ref(false)
const replyingId = ref(null)
const reviews = ref([])
const products = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const ratingOptions = [5, 4, 3, 2, 1]

const searchForm = reactive({
  productId: null,
  rating: null,
  status: null
})

const loadProducts = async () => {
  try {
    const res = await getProductPage({
      pageNum: 1,
      pageSize: 1000
    })
    products.value = res.data.records || []
  } catch (error) {
    ElMessage.error('商品列表加载失败')
  }
}

const loadReviews = async () => {
  loading.value = true
  try {
    const res = await getAllReviews({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      productId: searchForm.productId || undefined,
      rating: searchForm.rating || undefined,
      status: searchForm.status ?? undefined
    })
    reviews.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '评价列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await loadReviews()
}

const handleReset = async () => {
  Object.assign(searchForm, {
    productId: null,
    rating: null,
    status: null
  })
  await handleSearch()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await loadReviews()
}

const handleReplyReview = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入商家回复内容，回复后会展示给用户查看。',
      row.replyContent ? '修改商家回复' : '回复评价',
      {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValue: row.replyContent || '',
        inputPattern: /\S+/,
        inputErrorMessage: '请输入回复内容',
        inputPlaceholder: '请输入回复内容，最多 500 个字符'
      }
    )
    const content = (value || '').trim()
    if (content.length > 500) {
      ElMessage.error('回复内容不能超过500个字符')
      return
    }

    replyingId.value = row.id
    await replyReview(row.id, { content })
    ElMessage.success(row.replyContent ? '回复已更新' : '回复成功')
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '回复评价失败')
    }
  } finally {
    replyingId.value = null
  }
}

const handleHideReview = async (row) => {
  try {
    await ElMessageBox.confirm(
      '隐藏后，该评价将不再出现在前台商品评价列表和评价统计中，是否继续？',
      '隐藏评价',
      { type: 'warning' }
    )
    await hideReview(row.id)
    ElMessage.success('评价已隐藏')
    await loadReviews()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '隐藏评价失败')
    }
  }
}

const handleRestoreReview = async (row) => {
  try {
    await restoreReview(row.id)
    ElMessage.success('评价已恢复展示')
    await loadReviews()
  } catch (error) {
    ElMessage.error(error.message || '恢复评价失败')
  }
}

const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm')

onMounted(async () => {
  await loadProducts()
  await loadReviews()
})
</script>

<style scoped>
.review-toolbar {
  margin-bottom: var(--spacing-4);
}

.review-toolbar :deep(.el-select) {
  min-width: 200px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-cell__image {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  flex-shrink: 0;
}

.product-cell__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.product-cell__copy strong {
  color: var(--color-text);
}

.product-cell__copy span,
.empty-copy {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.user-cell__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.rating-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reply-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reply-cell p {
  margin: 0;
  color: var(--color-text);
  line-height: 1.5;
}

.reply-cell span {
  color: var(--color-text-muted);
  font-size: var(--text-xs);
}

.image-grid {
  display: flex;
  gap: 8px;
}

.image-grid__item {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  overflow: hidden;
}

@media (max-width: 768px) {
  .review-toolbar :deep(.el-select),
  .review-toolbar :deep(.el-button) {
    width: 100%;
  }
}
</style>
