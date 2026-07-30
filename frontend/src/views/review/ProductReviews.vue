<template>
  <div class="reviews-container">
    <!-- 评价统计卡片 -->
    <el-card v-if="showStats" class="review-stats" shadow="hover">
      <div class="stats-wrapper">
        <div class="rating-overview">
          <div class="average-rating">
            <div class="rating-value">{{ stats.averageRating || 0 }}</div>
            <el-rate
              v-model="averageRatingDisplay"
              disabled
              allow-half
              :colors="['#99a9bf', '#f7ba2a', '#ff9900']"
            />
            <div class="total-reviews">{{ stats.totalReviews || 0 }} 条评价</div>
          </div>
        </div>
        
        <div class="rating-distribution">
          <div v-for="i in 5" :key="i" class="rating-bar">
            <span class="star-label">{{ 6 - i }}星</span>
            <el-progress
              :percentage="stats.ratingPercentage?.[6 - i] || 0"
              :stroke-width="10"
              :color="getProgressColor(6 - i)"
            />
            <span class="count-label">{{ stats.ratingDistribution?.[6 - i] || 0 }}</span>
          </div>
        </div>
        
        <div class="stats-summary">
          <div class="summary-item">
            <div class="summary-value">{{ stats.positiveRate || 0 }}%</div>
            <div class="summary-label">好评率</div>
          </div>
          <div class="summary-item">
            <div class="summary-value">{{ stats.withImagesCount || 0 }}</div>
            <div class="summary-label">有图评价</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-radio-group v-model="filterType" @change="handleFilterChange">
        <el-radio-button label="all">全部({{ stats.totalReviews || 0 }})</el-radio-button>
        <el-radio-button label="positive">好评({{ positiveCount }})</el-radio-button>
        <el-radio-button label="medium">中评({{ mediumCount }})</el-radio-button>
        <el-radio-button label="negative">差评({{ negativeCount }})</el-radio-button>
        <el-radio-button label="withImages">有图({{ stats.withImagesCount || 0 }})</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 评价列表 -->
    <div class="reviews-list">
      <el-empty v-if="reviews.length === 0" description="暂无评价" />
      
      <div v-for="review in reviews" :key="review.id" class="review-item">
        <div class="reviewer-info">
          <el-avatar :src="review.avatar" :size="40">
            {{ review.nickname?.substring(0, 1) }}
          </el-avatar>
          <div class="reviewer-detail">
            <div class="reviewer-name">{{ review.nickname || '匿名用户' }}</div>
            <div class="review-time">{{ formatTime(review.createTime) }}</div>
          </div>
        </div>
        
        <div class="review-content">
          <el-rate
            v-model="review.rating"
            disabled
            :colors="['#99a9bf', '#f7ba2a', '#ff9900']"
          />
          <div v-if="review.specInfo" class="review-spec">{{ review.specInfo }}</div>
          <p class="review-text">{{ review.content }}</p>
          
          <div v-if="review.imageList?.length > 0" class="review-images">
            <el-image
              v-for="(img, index) in review.imageList"
              :key="index"
              :src="img"
              :preview-src-list="review.imageList"
              :initial-index="index"
              fit="cover"
              class="review-image"
            />
          </div>

          <div v-if="review.replyContent" class="merchant-reply">
            <div class="merchant-reply__label">商家回复</div>
            <p>{{ review.replyContent }}</p>
            <span>{{ formatTime(review.replyTime) }}</span>
          </div>
        </div>
        
        <div v-if="showActions && review.userId === currentUserId" class="review-actions">
          <el-button type="danger" size="small" text @click="handleDelete(review.id)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="handlePageChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getProductReviews, getReviewStats, deleteReview } from '@/api/review'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const props = defineProps({
  productId: {
    type: Number,
    required: true
  },
  showStats: {
    type: Boolean,
    default: true
  },
  showActions: {
    type: Boolean,
    default: false
  }
})

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)

// 评价统计
const stats = ref({})
const averageRatingDisplay = computed(() => Number(stats.value.averageRating) || 0)

// 评价列表
const reviews = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 筛选
const filterType = ref('all')
const positiveCount = computed(() => {
  return (stats.value.ratingDistribution?.[5] || 0) + (stats.value.ratingDistribution?.[4] || 0)
})
const mediumCount = computed(() => stats.value.ratingDistribution?.[3] || 0)
const negativeCount = computed(() => {
  return (stats.value.ratingDistribution?.[2] || 0) + (stats.value.ratingDistribution?.[1] || 0)
})

// 加载评价统计
const loadStats = async () => {
  if (!props.showStats) return
  
  try {
    const res = await getReviewStats(props.productId)
    stats.value = res.data || {}
  } catch (error) {
    console.error('加载评价统计失败:', error)
  }
}

// 加载评价列表
const loadReviews = async () => {
  try {
    const res = await getProductReviews(props.productId, {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...buildFilterParams()
    })
    reviews.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载评价列表失败')
  }
}

// 删除评价
const handleDelete = async (reviewId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评价吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteReview(reviewId)
    ElMessage.success('删除成功')
    loadReviews()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 处理筛选
const handleFilterChange = () => {
  currentPage.value = 1
  loadReviews()
}

// 处理分页
const handlePageChange = () => {
  loadReviews()
}

// 获取进度条颜色
const getProgressColor = (rating) => {
  if (rating >= 4) return '#67c23a'
  if (rating === 3) return '#e6a23c'
  return '#f56c6c'
}

// 格式化时间
const formatTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const buildFilterParams = () => {
  const filterMap = {
    positive: { minRating: 4, maxRating: 5 },
    medium: { minRating: 3, maxRating: 3 },
    negative: { minRating: 1, maxRating: 2 },
    withImages: { hasImages: true }
  }
  return filterMap[filterType.value] || {}
}

// 监听商品ID变化
watch(() => props.productId, () => {
  currentPage.value = 1
  filterType.value = 'all'
  loadReviews()
  loadStats()
})

onMounted(() => {
  loadReviews()
  loadStats()
})
</script>

<style lang="scss" scoped>
.reviews-container {
  .review-stats {
    margin-bottom: 20px;
    
    .stats-wrapper {
      display: flex;
      gap: 40px;
      
      .rating-overview {
        flex: 0 0 200px;
        text-align: center;
        
        .average-rating {
          .rating-value {
            font-size: 48px;
            font-weight: 600;
            color: #ff9900;
            margin-bottom: 10px;
          }
          
          .total-reviews {
            margin-top: 10px;
            color: #909399;
            font-size: 14px;
          }
        }
      }
      
      .rating-distribution {
        flex: 1;
        
        .rating-bar {
          display: flex;
          align-items: center;
          margin-bottom: 8px;
          
          .star-label {
            width: 40px;
            font-size: 14px;
            color: #606266;
          }
          
          :deep(.el-progress) {
            flex: 1;
            margin: 0 10px;
          }
          
          .count-label {
            width: 40px;
            text-align: right;
            font-size: 14px;
            color: #909399;
          }
        }
      }
      
      .stats-summary {
        flex: 0 0 200px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        gap: 20px;
        
        .summary-item {
          text-align: center;
          
          .summary-value {
            font-size: 24px;
            font-weight: 600;
            color: #303133;
          }
          
          .summary-label {
            margin-top: 5px;
            font-size: 14px;
            color: #909399;
          }
        }
      }
    }
  }
  
  .filter-bar {
    margin-bottom: 20px;
  }
  
  .reviews-list {
    .review-item {
      padding: 20px;
      border-bottom: 1px solid #e4e7ed;
      
      &:last-child {
        border-bottom: none;
      }
      
      .reviewer-info {
        display: flex;
        align-items: center;
        margin-bottom: 15px;
        
        .reviewer-detail {
          margin-left: 12px;
          
          .reviewer-name {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
          }
          
          .review-time {
            font-size: 12px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
      
      .review-content {
        margin-left: 52px;
        
        .review-text {
          margin: 10px 0;
          line-height: 1.6;
          color: #606266;
        }

        .review-spec {
          display: inline-flex;
          margin-top: 8px;
          color: #909399;
          font-size: 12px;
        }

        .merchant-reply {
          margin-top: 14px;
          padding: 12px 14px;
          border-left: 3px solid #409eff;
          background: #f5f9ff;
          border-radius: 4px;

          .merchant-reply__label {
            font-size: 12px;
            font-weight: 600;
            color: #337ecc;
            margin-bottom: 6px;
          }

          p {
            margin: 0;
            color: #606266;
            line-height: 1.6;
          }

          span {
            display: inline-flex;
            margin-top: 6px;
            color: #909399;
            font-size: 12px;
          }
        }
        
        .review-images {
          display: flex;
          gap: 10px;
          margin-top: 10px;
          
          .review-image {
            width: 100px;
            height: 100px;
            border-radius: 4px;
            cursor: pointer;
          }
        }
      }
      
      .review-actions {
        margin-left: 52px;
        margin-top: 10px;
      }
    }
  }
  
  .el-pagination {
    margin-top: 20px;
    text-align: center;
  }
}
</style>
