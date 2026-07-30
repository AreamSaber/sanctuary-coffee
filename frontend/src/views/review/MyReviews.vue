<template>
  <div class="my-reviews-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>管理评价</span>
          <el-tag type="info">{{ total }} 条评价</el-tag>
        </div>
      </template>
      
      <!-- 筛选栏 -->
      <el-form :inline="true" :model="searchForm" class="filter-form">
        <el-form-item label="商品">
          <el-select
            v-model="searchForm.productId"
            placeholder="请选择商品"
            clearable
            filterable
            style="width: 250px"
            @change="handleSearch"
          >
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="product.productName"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="评分">
          <el-select
            v-model="searchForm.rating"
            placeholder="请选择评分"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option label="⭐⭐⭐⭐⭐ 5星" :value="5" />
            <el-option label="⭐⭐⭐⭐ 4星" :value="4" />
            <el-option label="⭐⭐⭐ 3星" :value="3" />
            <el-option label="⭐⭐ 2星" :value="2" />
            <el-option label="⭐ 1星" :value="1" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 150px"
            @change="handleSearch"
          >
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-empty v-if="reviews.length === 0" description="暂无评价数据" />
      
      <div v-else class="reviews-list">
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <!-- 商品信息 -->
          <div class="product-info">
            <el-image :src="review.productImage" fit="cover" class="product-image" />
            <div class="product-detail">
              <div class="product-name">{{ review.productName }}</div>
              <div v-if="review.specInfo" class="product-spec">{{ review.specInfo }}</div>
              <div class="review-time">评价时间：{{ formatTime(review.createTime) }}</div>
            </div>
          </div>
          
          <!-- 评价内容 -->
          <div class="review-content">
            <el-rate
              v-model="review.rating"
              disabled
              :colors="['#99a9bf', '#f7ba2a', '#ff9900']"
            />
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
            
            <div class="review-meta">
              <el-tag v-if="review.isAnonymous" type="info" size="small">匿名评价</el-tag>
              <span class="order-info">订单号：{{ review.orderId }}</span>
            </div>
          </div>
          
          <!-- 操作按钮 -->
          <div class="review-actions">
            <el-button type="danger" size="small" @click="handleDelete(review.id)">
              删除评价
            </el-button>
            <el-button size="small" @click="viewProduct(review.productId)">
              查看商品
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
        @current-change="loadReviews"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { getMyReviews, deleteReview } from '@/api/review'
import { getProductPage } from '@/api/product'
import dayjs from 'dayjs'

const router = useRouter()

// 评价列表
const reviews = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 商品列表
const products = ref([])

// 搜索表单
const searchForm = reactive({
  productId: null,
  rating: null,
  status: null
})

// 加载商品列表
const loadProducts = async () => {
  try {
    const res = await getProductPage({
      pageNum: 1,
      pageSize: 1000,
      status: 1  // 只获取上架商品
    })
    products.value = res.data.records || []
  } catch (error) {
    console.error('加载商品列表失败:', error)
  }
}

// 加载评价列表
const loadReviews = async () => {
  try {
    const res = await getMyReviews({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      productId: searchForm.productId,
      rating: searchForm.rating,
      status: searchForm.status
    })
    reviews.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载评价列表失败')
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadReviews()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    productId: null,
    rating: null,
    status: null
  })
  handleSearch()
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
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 查看商品
const viewProduct = (productId) => {
  router.push('/shop')
}

// 格式化时间
const formatTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

onMounted(() => {
  loadProducts()
  loadReviews()
})
</script>

<style lang="scss" scoped>
.my-reviews-container {
  padding: 20px;
  
  .filter-form {
    margin-bottom: 20px;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 4px;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 18px;
  }
  
  .reviews-list {
    .review-item {
      padding: 20px;
      border-bottom: 1px solid #e4e7ed;
      
      &:last-child {
        border-bottom: none;
      }
      
      .product-info {
        display: flex;
        margin-bottom: 15px;
        
        .product-image {
          width: 80px;
          height: 80px;
          border-radius: 4px;
          margin-right: 15px;
        }
        
        .product-detail {
          flex: 1;
          
          .product-name {
            font-size: 16px;
            font-weight: 500;
            color: #303133;
            margin-bottom: 8px;
          }

          .product-spec {
            color: #909399;
            font-size: 12px;
            margin-bottom: 8px;
          }
          
          .review-time {
            font-size: 12px;
            color: #909399;
          }
        }
      }
      
      .review-content {
        margin-left: 95px;
        
        .review-text {
          margin: 10px 0;
          line-height: 1.6;
          color: #606266;
        }
        
        .review-images {
          display: flex;
          gap: 10px;
          margin: 10px 0;
          
          .review-image {
            width: 100px;
            height: 100px;
            border-radius: 4px;
            cursor: pointer;
          }
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
        
        .review-meta {
          margin-top: 10px;
          display: flex;
          align-items: center;
          gap: 10px;
          
          .order-info {
            font-size: 12px;
            color: #909399;
          }
        }
      }
      
      .review-actions {
        margin-left: 95px;
        margin-top: 15px;
      }
    }
  }
  
  .el-pagination {
    margin-top: 20px;
    text-align: center;
  }
}
</style>
