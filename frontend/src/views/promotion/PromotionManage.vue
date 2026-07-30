<template>
  <div class="promotion-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>促销活动管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            创建活动
          </el-button>
        </div>
      </template>
      
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索活动名称或描述"
          clearable
          style="width: 300px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="searchForm.type" placeholder="活动类型" clearable style="width: 150px">
          <el-option label="折扣优惠" value="DISCOUNT" />
          <el-option label="满减活动" value="REDUCTION" />
          <el-option label="赠品活动" value="GIFT" />
          <el-option label="限时秒杀" value="FLASH_SALE" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      
      <!-- 活动列表 -->
      <el-table :data="promotionList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="活动名称" min-width="150" />
        <el-table-column prop="type" label="活动类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagStyle(row.type)">
              {{ getTypeName(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动详情" min-width="200">
          <template #default="{ row }">
            <div class="promotion-detail">
              <template v-if="normalizePromotionType(row.type) === 'DISCOUNT'">
                <span>{{ row.discountRate }}% 折扣</span>
              </template>
              <template v-else-if="normalizePromotionType(row.type) === 'REDUCTION'">
                <span>满 ¥{{ row.conditionAmount }} 减 ¥{{ row.reductionAmount }}</span>
              </template>
              <template v-else-if="normalizePromotionType(row.type) === 'GIFT'">
                <span>赠送商品ID: {{ row.giftProductId }}</span>
              </template>
              <template v-else-if="normalizePromotionType(row.type) === 'FLASH_SALE'">
                <span>秒杀价: ¥{{ row.flashPrice }} (库存: {{ row.stock }})</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="180">
          <template #default="{ row }">
            <div class="time-info">
              <div>{{ formatTime(row.startTime) }}</div>
              <div>至</div>
              <div>{{ formatTime(row.endTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="活动状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getActivityStatus(row).type">
              {{ getActivityStatus(row).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadPromotions"
        @current-change="loadPromotions"
      />
    </el-card>
    
    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        
        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入活动描述"
          />
        </el-form-item>
        
        <el-form-item label="活动类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择活动类型" @change="handleTypeChange">
            <el-option label="折扣优惠" value="DISCOUNT" />
            <el-option label="满减活动" value="REDUCTION" />
            <el-option label="赠品活动" value="GIFT" />
            <el-option label="限时秒杀" value="FLASH_SALE" />
          </el-select>
        </el-form-item>

        <el-form-item label="适用商品" prop="productIds">
          <el-select
            v-model="form.productIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择商品"
            style="width: 100%"
          >
            <el-option
              v-for="product in productOptions"
              :key="product.id"
              :label="`${product.productName} (#${product.id})`"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        
        <!-- 根据活动类型显示不同的配置项 -->
        <template v-if="form.type === 'DISCOUNT'">
          <el-form-item label="折扣比例" prop="discountRate">
            <el-input-number
              v-model="form.discountRate"
              :min="1"
              :max="99"
              :precision="0"
            />
            <span class="input-suffix">%</span>
          </el-form-item>
        </template>
        
        <template v-if="form.type === 'REDUCTION'">
          <el-form-item label="满减条件" prop="conditionAmount">
            <el-input-number
              v-model="form.conditionAmount"
              :min="0"
              :precision="2"
            />
            <span class="input-suffix">元</span>
          </el-form-item>
          <el-form-item label="优惠金额" prop="reductionAmount">
            <el-input-number
              v-model="form.reductionAmount"
              :min="0"
              :precision="2"
            />
            <span class="input-suffix">元</span>
          </el-form-item>
        </template>
        
        <template v-if="form.type === 'GIFT'">
          <el-form-item label="赠品ID" prop="giftProductId">
            <el-input-number v-model="form.giftProductId" :min="1" />
          </el-form-item>
        </template>
        
        <template v-if="form.type === 'FLASH_SALE'">
          <el-form-item label="秒杀价格" prop="flashPrice">
            <el-input-number
              v-model="form.flashPrice"
              :min="0"
              :precision="2"
            />
            <span class="input-suffix">元</span>
          </el-form-item>
          <el-form-item label="活动库存" prop="stock">
            <el-input-number v-model="form.stock" :min="1" />
          </el-form-item>
          <el-form-item label="限购数量" prop="limitPerUser">
            <el-input-number v-model="form.limitPerUser" :min="1" />
            <span class="input-suffix">件/人</span>
          </el-form-item>
        </template>
        
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="999" />
          <span class="input-hint">数字越大优先级越高</span>
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
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
import { Plus } from '@element-plus/icons-vue'
import { getPromotionPage, createPromotion, updatePromotion, deletePromotion, updatePromotionStatus } from '@/api/promotion'
import { getProductPage } from '@/api/product'
import dayjs from 'dayjs'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  type: '',
  status: null
})

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 活动列表
const promotionList = ref([])
const productOptions = ref([])

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('创建活动')
const formRef = ref()

// 表单数据
const form = reactive({
  id: null,
  name: '',
  description: '',
  type: '',
  discountRate: null,
  conditionAmount: null,
  reductionAmount: null,
  giftProductId: null,
  flashPrice: null,
  productIds: [],
  stock: null,
  limitPerUser: 1,
  timeRange: [],
  priority: 0,
  status: 1
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入活动名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择活动类型', trigger: 'change' }
  ],
  productIds: [
    { type: 'array', required: true, min: 1, message: '请至少选择一个商品', trigger: 'change' }
  ],
  timeRange: [
    { required: true, message: '请选择活动时间', trigger: 'change' }
  ]
}

// 加载促销活动列表
const loadPromotions = async () => {
  loading.value = true
  try {
    const res = await getPromotionPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchForm
    })
    promotionList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载活动列表失败')
  } finally {
    loading.value = false
  }
}

const loadProductOptions = async () => {
  try {
    const res = await getProductPage({
      pageNum: 1,
      pageSize: 200,
      status: 1
    })
    productOptions.value = res.data.records || []
  } catch (error) {
    ElMessage.error('加载商品列表失败')
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadPromotions()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.type = ''
  searchForm.status = null
  handleSearch()
}

// 添加活动
const handleAdd = () => {
  dialogTitle.value = '创建活动'
  resetForm()
  dialogVisible.value = true
}

// 编辑活动
const handleEdit = (row) => {
  dialogTitle.value = '编辑活动'
  Object.assign(form, row, {
    productIds: Array.isArray(row.productIds) ? [...row.productIds] : []
  })
  form.type = normalizePromotionType(row.type)
  // 处理时间范围
  if (row.startTime && row.endTime) {
    form.timeRange = [row.startTime, row.endTime]
  }
  dialogVisible.value = true
}

// 删除活动
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除活动"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deletePromotion(row.id)
    ElMessage.success('删除成功')
    loadPromotions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 更新状态
const handleStatusChange = async (row) => {
  try {
    await updatePromotionStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

// 活动类型改变
const handleTypeChange = () => {
  // 清空特定类型的字段
  form.discountRate = null
  form.conditionAmount = null
  form.reductionAmount = null
  form.giftProductId = null
  form.flashPrice = null
  form.stock = null
  form.limitPerUser = 1
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      const submitData = {
        ...form,
        startTime: form.timeRange[0],
        endTime: form.timeRange[1]
      }
      delete submitData.timeRange
      
      if (form.id) {
        await updatePromotion(submitData)
        ElMessage.success('更新成功')
      } else {
        await createPromotion(submitData)
        ElMessage.success('创建成功')
      }
      
      dialogVisible.value = false
      loadPromotions()
    } catch (error) {
      ElMessage.error(form.id ? '更新失败' : '创建失败')
    }
  })
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.name = ''
  form.description = ''
  form.type = ''
  form.discountRate = null
  form.conditionAmount = null
  form.reductionAmount = null
  form.giftProductId = null
  form.flashPrice = null
  form.productIds = []
  form.stock = null
  form.limitPerUser = 1
  form.timeRange = []
  form.priority = 0
  form.status = 1
  formRef.value?.clearValidate()
}

// 获取活动类型名称
const getTypeName = (type) => {
  const normalizedType = normalizePromotionType(type)
  const typeMap = {
    DISCOUNT: '折扣优惠',
    REDUCTION: '满减活动',
    GIFT: '赠品活动',
    FLASH_SALE: '限时秒杀'
  }
  return typeMap[normalizedType] || normalizedType
}

// 获取类型标签样式
const getTypeTagStyle = (type) => {
  const normalizedType = normalizePromotionType(type)
  const styleMap = {
    DISCOUNT: 'success',
    REDUCTION: 'warning',
    GIFT: 'danger',
    FLASH_SALE: 'info'
  }
  return styleMap[normalizedType] || ''
}

// 获取活动状态
const getActivityStatus = (row) => {
  const now = dayjs()
  const start = dayjs(row.startTime)
  const end = dayjs(row.endTime)
  
  if (row.status === 0) {
    return { text: '已禁用', type: 'info' }
  }
  if (now.isBefore(start)) {
    return { text: '未开始', type: 'warning' }
  }
  if (now.isAfter(end)) {
    return { text: '已结束', type: 'info' }
  }
  return { text: '进行中', type: 'success' }
}

// 格式化时间
const formatTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const normalizePromotionType = (type) => {
  const value = String(type ?? '').toUpperCase()
  const typeMap = {
    '1': 'DISCOUNT',
    '2': 'REDUCTION',
    '3': 'FLASH_SALE',
    DISCOUNT: 'DISCOUNT',
    REDUCTION: 'REDUCTION',
    GIFT: 'GIFT',
    FLASH_SALE: 'FLASH_SALE'
  }
  return typeMap[value] || value
}

onMounted(() => {
  loadPromotions()
  loadProductOptions()
})
</script>

<style lang="scss" scoped>
.promotion-container {
  padding: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-bar {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
  }
  
  .promotion-detail {
    color: #409eff;
    font-weight: 500;
  }
  
  .time-info {
    font-size: 12px;
    line-height: 1.5;
    color: #606266;
  }
  
  .input-suffix {
    margin-left: 10px;
    color: #909399;
  }
  
  .input-hint {
    margin-left: 10px;
    color: #909399;
    font-size: 12px;
  }
  
  .el-pagination {
    margin-top: 20px;
    text-align: right;
  }
}
</style>
