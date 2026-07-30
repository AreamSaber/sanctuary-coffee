<template>
  <div class="coupon-manage-container">
    <!-- 核销率统计卡片 -->
    <el-card class="stats-card" v-loading="statsLoading">
      <template #header>
        <div class="card-header">
          <span>优惠券核销统计</span>
          <el-button type="primary" link @click="loadCouponStats">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      <el-table :data="couponStats" style="width: 100%" max-height="300">
        <el-table-column prop="couponName" label="优惠券名称" min-width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.couponType === 1 ? 'danger' : row.couponType === 2 ? 'warning' : 'success'">
              {{ row.couponTypeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalReceived" label="发放数量" width="100" align="center" />
        <el-table-column prop="totalUsed" label="已使用" width="100" align="center" />
        <el-table-column label="核销率" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.redemptionRate >= 50 ? 'success' : row.redemptionRate >= 20 ? 'warning' : 'danger'">
              {{ row.redemptionRate }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="couponStats.length === 0 && !statsLoading" description="暂无统计数据" />
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>优惠券管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            创建优惠券
          </el-button>
        </div>
      </template>
      
      <!-- 搜索筛选 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="优惠券名称">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入优惠券名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        
        <el-form-item label="优惠券类型">
          <el-select
            v-model="searchForm.type"
            placeholder="请选择类型"
            clearable
            style="width: 150px"
          >
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 120px"
          >
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
      
      <!-- 优惠券列表 -->
      <el-table :data="couponList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="couponName" label="优惠券名称" min-width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.couponType === 1 ? 'danger' : 'warning'">
              {{ row.couponType === 1 ? '满减券' : '折扣券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" width="150">
          <template #default="{ row }">
            <span v-if="row.couponType === 1" class="discount-text">
              满{{ row.minAmount }}减{{ row.discountAmount }}
            </span>
            <span v-else class="discount-text">
              {{ (row.discountRate * 10).toFixed(1) }}折
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="发行数量" width="100" />
        <el-table-column label="剩余数量" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.remainCount < 10 }">
              {{ row.remainCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }">
            <div class="date-range">
              <div>{{ formatDate(row.startTime) }}</div>
              <div>{{ formatDate(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button 
              link 
              :type="row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleStatus(row)"
            >
              <el-icon><Switch /></el-icon>
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadCouponList"
        @current-change="loadCouponList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="couponForm" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="优惠券名称" prop="couponName">
          <el-input v-model="couponForm.couponName" placeholder="请输入优惠券名称" />
        </el-form-item>
        
        <el-form-item label="优惠券类型" prop="couponType">
          <el-radio-group v-model="couponForm.couponType">
            <el-radio :label="1">满减券</el-radio>
            <el-radio :label="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="最低消费" prop="minAmount">
          <el-input-number 
            v-model="couponForm.minAmount" 
            :min="0" 
            :precision="2"
            :step="10"
          />
        </el-form-item>
        
        <el-form-item 
          v-if="couponForm.couponType === 1" 
          label="减免金额" 
          prop="discountAmount"
        >
          <el-input-number 
            v-model="couponForm.discountAmount" 
            :min="1" 
            :precision="2"
            :step="5"
          />
        </el-form-item>
        
        <el-form-item 
          v-if="couponForm.couponType === 2" 
          label="折扣率" 
          prop="discountRate"
        >
          <el-input-number 
            v-model="couponForm.discountRate" 
            :min="0.1" 
            :max="0.99"
            :precision="2"
            :step="0.05"
          />
          <span style="margin-left: 10px; color: #909399">
            {{ (couponForm.discountRate * 10).toFixed(1) }}折
          </span>
        </el-form-item>
        
        <el-form-item label="发行数量" prop="totalCount">
          <el-input-number 
            v-model="couponForm.totalCount" 
            :min="1" 
            :max="100000"
            :step="10"
          />
        </el-form-item>
        
        <el-form-item label="有效期" prop="dateRange">
          <el-date-picker
            v-model="couponForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="使用说明" prop="description">
          <el-input
            v-model="couponForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入优惠券使用说明"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RefreshLeft, Edit, Delete, Switch, Refresh } from '@element-plus/icons-vue'
import { createCoupon, updateCoupon, deleteCoupon, getCouponPage, updateCouponStatus, getCouponStats } from '@/api/member'
import dayjs from 'dayjs'

// 数据定义
const loading = ref(false)
const couponList = ref([])
const total = ref(0)
const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const searchForm = reactive({
  name: '',
  type: null,
  status: null
})

const statsLoading = ref(false)
const couponStats = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const couponForm = reactive({
  id: null,
  couponName: '',
  couponType: 1,
  minAmount: 0,
  discountAmount: 0,
  discountRate: 0.9,
  totalCount: 100,
  dateRange: [],
  description: ''
})

const formRules = {
  couponName: [
    { required: true, message: '请输入优惠券名称', trigger: 'blur' }
  ],
  couponType: [
    { required: true, message: '请选择优惠券类型', trigger: 'change' }
  ],
  minAmount: [
    { required: true, message: '请输入最低消费金额', trigger: 'blur' }
  ],
  totalCount: [
    { required: true, message: '请输入发行数量', trigger: 'blur' }
  ],
  dateRange: [
    { required: true, message: '请选择有效期', trigger: 'change' }
  ]
}

// 加载优惠券列表
const loadCouponList = async () => {
  loading.value = true
  try {
    const res = await getCouponPage({
      ...pagination,
      ...searchForm
    })
    couponList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载优惠券列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadCouponList()
}

// 重置
const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    type: null,
    status: null
  })
  handleSearch()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '创建优惠券'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑优惠券'
  Object.assign(couponForm, {
    id: row.id,
    couponName: row.couponName,
    couponType: row.couponType,
    minAmount: row.minAmount,
    discountAmount: row.discountAmount,
    discountRate: row.discountRate,
    totalCount: row.totalCount,
    dateRange: [row.startTime, row.endTime],
    description: row.description
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个优惠券吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteCoupon(row.id)
    ElMessage.success('删除成功')
    loadCouponList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(`确定要${action}这个优惠券吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await updateCouponStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadCouponList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    const data = {
      ...couponForm,
      startTime: couponForm.dateRange[0],
      endTime: couponForm.dateRange[1]
    }
    delete data.dateRange
    
    if (couponForm.id) {
      await updateCoupon(couponForm.id, data)
      ElMessage.success('更新成功')
    } else {
      await createCoupon(data)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadCouponList()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(couponForm, {
    id: null,
    couponName: '',
    couponType: 1,
    minAmount: 0,
    discountAmount: 0,
    discountRate: 0.9,
    totalCount: 100,
    dateRange: [],
    description: ''
  })
  formRef.value?.clearValidate()
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

// 加载核销统计
const loadCouponStats = async () => {
  statsLoading.value = true
  try {
    const res = await getCouponStats()
    couponStats.value = res.data || []
  } catch (error) {
    console.error('加载核销统计失败:', error)
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  loadCouponList()
  loadCouponStats()
})
</script>

<style scoped lang="scss">
.coupon-manage-container {
  padding: 20px;

  .stats-card {
    margin-bottom: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 18px;
  }
  
  .search-form {
    margin-bottom: 20px;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 4px;
  }
  
  .discount-text {
    font-weight: bold;
    color: #f56c6c;
  }
  
  .text-danger {
    color: #f56c6c;
    font-weight: bold;
  }
  
  .date-range {
    div {
      font-size: 12px;
      
      &:first-child {
        margin-bottom: 4px;
      }
    }
  }
}
</style>
