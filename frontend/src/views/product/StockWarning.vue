<template>
  <div class="stock-warning-container">
    <div class="page-header">
      <h2 class="page-title">库存预警管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="exportData" :icon="Download">
          导出报表
        </el-button>
        <el-button @click="refreshData" :icon="Refresh">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 预警设置卡片 -->
    <el-card class="settings-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>预警设置</span>
        </div>
      </template>
      <el-form :model="warningSettings" label-width="120px" inline>
        <el-form-item label="预警阈值">
          <el-input-number 
            v-model="warningSettings.threshold" 
            :min="1" 
            :max="1000"
            @change="handleThresholdChange"
          />
          <span class="threshold-hint">库存低于此值将触发预警</span>
        </el-form-item>
        <el-form-item label="紧急阈值">
          <el-input-number 
            v-model="warningSettings.criticalThreshold" 
            :min="0" 
            :max="100"
            @change="handleThresholdChange"
          />
          <span class="threshold-hint">库存低于此值标记为紧急</span>
        </el-form-item>
        <el-form-item label="自动提醒">
          <el-switch 
            v-model="warningSettings.autoAlert"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计概览 -->
    <el-row :gutter="20" class="stats-overview">
      <el-col :span="6">
        <el-card class="stat-card normal">
          <div class="stat-icon">
            <el-icon><Box /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stockStats.total }}</div>
            <div class="stat-label">商品总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card warning">
          <div class="stat-icon">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stockStats.warning }}</div>
            <div class="stat-label">预警库存项</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card danger">
          <div class="stat-icon">
            <el-icon><CircleCloseFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stockStats.critical }}</div>
            <div class="stat-label">紧急库存项</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card out-of-stock">
          <div class="stat-icon">
            <el-icon><SoldOut /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stockStats.outOfStock }}</div>
            <div class="stat-label">缺货库存项</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警列表 -->
    <el-card class="warning-list">
      <template #header>
        <div class="card-header">
          <span>库存预警列表</span>
          <div class="filter-controls">
            <el-select v-model="filterLevel" placeholder="库存层级" @change="loadLowStockProducts">
              <el-option label="全部层级" value="all" />
              <el-option label="商品汇总" value="product" />
              <el-option label="SKU 明细" value="sku" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="筛选状态" @change="loadLowStockProducts">
              <el-option label="全部" value="" />
              <el-option label="预警" value="warning" />
              <el-option label="紧急" value="critical" />
              <el-option label="缺货" value="out" />
            </el-select>
            <el-select v-model="filterCategory" placeholder="筛选分类" @change="loadLowStockProducts">
              <el-option label="全部分类" :value="null" />
              <el-option 
                v-for="category in categories" 
                :key="category.id"
                :label="category.categoryName" 
                :value="category.id" 
              />
            </el-select>
          </div>
        </div>
      </template>

      <el-table 
        :data="lowStockProducts" 
        style="width: 100%"
        row-key="rowKey"
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="index" width="60" label="序号" />
        <el-table-column prop="productName" label="库存项" min-width="240">
          <template #default="{ row }">
            <div class="product-info">
              <el-tag :type="row.itemType === 'sku' ? 'warning' : 'info'" size="small">
                {{ row.itemTypeText }}
              </el-tag>
              <div>
                <div class="product-name">{{ row.productName }}</div>
                <div v-if="row.itemType === 'sku'" class="sku-name">
                  {{ row.itemName }}
                </div>
              </div>
              <el-tag v-if="row.isNew" type="success" size="small">新品</el-tag>
              <el-tag v-if="row.isHot" type="danger" size="small">热销</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="商品分类" width="120" />
        <el-table-column prop="specInfo" label="规格" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.specInfo || '商品汇总' }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="当前库存" width="120" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="getStockTagType(row.stock)"
              effect="dark"
            >
              {{ row.stock || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salesCount" label="月销量" width="100" align="center" />
        <el-table-column prop="dailySales" label="日均销量" width="100" align="center">
          <template #default="{ row }">
            {{ (row.salesCount / 30).toFixed(1) }}
          </template>
        </el-table-column>
        <el-table-column label="预计可售天数" width="120" align="center">
          <template #default="{ row }">
            <span :class="getDaysClass(row)">
              {{ calculateDaysRemaining(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusType(row.stock)"
              effect="plain"
            >
              {{ getStatusText(row.stock) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="quickRestock(row)">
              快速补货
            </el-button>
            <el-button size="small" @click="viewHistory(row)">
              历史记录
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 快速补货对话框 -->
    <el-dialog 
      v-model="restockDialog.visible" 
      title="快速补货" 
      width="500px"
    >
      <el-form :model="restockForm" label-width="100px">
        <el-form-item label="商品名称">
          <el-input v-model="restockForm.productName" disabled />
        </el-form-item>
        <el-form-item v-if="restockForm.skuId" label="SKU">
          <el-input v-model="restockForm.itemName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input v-model="restockForm.currentStock" disabled />
        </el-form-item>
        <el-form-item label="补货数量" required>
          <el-input-number 
            v-model="restockForm.quantity" 
            :min="1" 
            :max="9999"
            :step="10"
          />
        </el-form-item>
        <el-form-item label="补货后库存">
          <el-input 
            :value="restockForm.currentStock + restockForm.quantity" 
            disabled 
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="restockForm.remark" 
            type="textarea" 
            :rows="3"
            placeholder="请输入补货原因或说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="restockDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmRestock">确认补货</el-button>
      </template>
    </el-dialog>

    <!-- 库存历史记录对话框 -->
    <el-dialog 
      v-model="historyDialog.visible" 
      :title="'库存变更记录 - ' + historyDialog.productName" 
      width="800px"
    >
      <el-table 
        :data="stockLogs" 
        v-loading="historyLoading"
        style="width: 100%"
        max-height="400"
      >
        <el-table-column type="index" width="60" label="序号" />
        <el-table-column label="变更类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getChangeTypeTag(row.changeType).type" size="small">
              {{ getChangeTypeTag(row.changeType).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeQuantity" label="变更数量" width="100" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.changeType === 1 || row.changeType === 3 ? '#67c23a' : '#f56c6c' }">
              {{ row.changeType === 1 || row.changeType === 3 ? '+' : '-' }}{{ row.changeQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="beforeStock" label="变更前" width="80" align="center" />
        <el-table-column prop="afterStock" label="变更后" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
      <div v-if="stockLogs.length === 0 && !historyLoading" class="empty-tip">
        暂无库存变更记录
      </div>
      <template #footer>
        <el-button @click="historyDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download,
  Refresh,
  Setting,
  Box,
  WarningFilled,
  CircleCloseFilled,
  SoldOut
} from '@element-plus/icons-vue'
import { getProductPage, getCategoryList, restockProduct, getStockLog } from '@/api/product'

// 预警设置
const warningSettings = ref({
  threshold: 10,
  criticalThreshold: 5,
  autoAlert: true
})

// 统计数据
const stockStats = ref({
  total: 0,
  warning: 0,
  critical: 0,
  outOfStock: 0
})

// 筛选条件
const filterStatus = ref('')
const filterCategory = ref(null)
const filterLevel = ref('all')
const categories = ref([])

// 商品列表
const lowStockProducts = ref([])

// 补货对话框
const restockDialog = ref({
  visible: false
})

const restockForm = ref({
  productId: null,
  skuId: null,
  productName: '',
  itemName: '',
  currentStock: 0,
  quantity: 50,
  remark: ''
})

// 历史记录对话框
const historyDialog = ref({
  visible: false,
  productId: null,
  skuId: null,
  productName: ''
})

const stockLogs = ref([])
const historyLoading = ref(false)
const totalProductCount = ref(0)

const resolveProductSkus = (product) => {
  const skuList = product?.skuList || product?.skus || product?.productSkuList || []
  return Array.isArray(skuList) ? skuList.filter(Boolean) : []
}

const buildInventoryRows = (products) => {
  return products.flatMap((product) => {
    const skuList = resolveProductSkus(product).filter((sku) => Number(sku.status ?? 1) === 1)
    const baseRow = {
      rowKey: `product-${product.id}`,
      itemType: 'product',
      itemTypeText: skuList.length ? '汇总' : '商品',
      productId: product.id,
      skuId: null,
      productName: product.productName,
      itemName: product.productName,
      specInfo: null,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      stock: Number(product.stock || 0),
      salesCount: Number(product.sales || product.salesCount || 0),
      isHot: product.isHot,
      isNew: product.isNew,
      isRecommend: product.isRecommend
    }

    const skuRows = skuList.map((sku) => ({
      rowKey: `sku-${sku.id}`,
      itemType: 'sku',
      itemTypeText: 'SKU',
      productId: product.id,
      skuId: sku.id,
      productName: product.productName,
      itemName: sku.skuName || sku.specInfo || sku.skuCode,
      specInfo: sku.specInfo || sku.skuName || sku.skuCode,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      stock: Number(sku.stock || 0),
      salesCount: Number(product.sales || product.salesCount || 0),
      isHot: product.isHot,
      isNew: product.isNew,
      isRecommend: product.isRecommend
    }))

    return [baseRow, ...skuRows]
  })
}

const filterWarningRows = (rows) => {
  return rows
    .filter((item) => item.stock <= warningSettings.value.threshold)
    .filter((item) => {
      if (filterLevel.value === 'product') return item.itemType === 'product'
      if (filterLevel.value === 'sku') return item.itemType === 'sku'
      return true
    })
    .filter((item) => {
      if (!filterStatus.value) return true
      if (filterStatus.value === 'out') return item.stock === 0
      if (filterStatus.value === 'critical') return item.stock > 0 && item.stock <= warningSettings.value.criticalThreshold
      if (filterStatus.value === 'warning') return item.stock > warningSettings.value.criticalThreshold && item.stock <= warningSettings.value.threshold
      return true
    })
}

// 加载低库存商品
const loadLowStockProducts = async () => {
  try {
    const productRes = await getProductPage({
      pageNum: 1,
      pageSize: 1000,
      categoryId: filterCategory.value || undefined,
      status: 1
    })
    const products = productRes.data?.records || []
    totalProductCount.value = Number(productRes.data?.total || 0)
    lowStockProducts.value = filterWarningRows(buildInventoryRows(products))
    
    // 更新统计数据
    updateStatistics()
  } catch (error) {
    totalProductCount.value = 0
    ElMessage.error('加载库存数据失败')
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data || []
  } catch (error) {
    ElMessage.error('加载分类失败')
  }
}

// 更新统计数据
const updateStatistics = () => {
  const products = lowStockProducts.value
  stockStats.value = {
    total: totalProductCount.value,
    warning: products.filter(p => p.stock > warningSettings.value.criticalThreshold && p.stock <= warningSettings.value.threshold).length,
    critical: products.filter(p => p.stock > 0 && p.stock <= warningSettings.value.criticalThreshold).length,
    outOfStock: products.filter(p => p.stock === 0).length
  }
}

// 获取库存标签类型
const getStockTagType = (stock) => {
  if (stock === 0) return 'danger'
  if (stock <= warningSettings.value.criticalThreshold) return 'warning'
  if (stock <= warningSettings.value.threshold) return 'warning'
  return 'success'
}

// 获取状态类型
const getStatusType = (stock) => {
  if (stock === 0) return 'danger'
  if (stock <= warningSettings.value.criticalThreshold) return 'danger'
  if (stock <= warningSettings.value.threshold) return 'warning'
  return 'success'
}

// 获取状态文本
const getStatusText = (stock) => {
  if (stock === 0) return '缺货'
  if (stock <= warningSettings.value.criticalThreshold) return '紧急'
  if (stock <= warningSettings.value.threshold) return '预警'
  return '正常'
}

// 计算可售天数
const calculateDaysRemaining = (row) => {
  if (!row.salesCount || row.salesCount === 0) return '∞'
  const dailySales = row.salesCount / 30
  if (dailySales === 0) return '∞'
  const days = Math.floor(row.stock / dailySales)
  return days > 999 ? '999+' : days + '天'
}

// 获取天数样式
const getDaysClass = (row) => {
  if (!row.salesCount || row.salesCount === 0) return 'days-normal'
  const dailySales = row.salesCount / 30
  if (dailySales === 0) return 'days-normal'
  const days = Math.floor(row.stock / dailySales)
  if (days <= 3) return 'days-danger'
  if (days <= 7) return 'days-warning'
  return 'days-normal'
}

// 表格行样式
const tableRowClassName = ({ row }) => {
  if (row.stock === 0) return 'out-of-stock-row'
  if (row.stock <= warningSettings.value.criticalThreshold) return 'critical-row'
  if (row.stock <= warningSettings.value.threshold) return 'warning-row'
  return ''
}

// 阈值改变处理
const handleThresholdChange = () => {
  loadLowStockProducts()
}

// 快速补货
const quickRestock = (row) => {
  restockForm.value = {
    productId: row.productId,
    skuId: row.skuId,
    productName: row.productName,
    itemName: row.itemType === 'sku' ? row.itemName : '',
    currentStock: row.stock,
    quantity: 50,
    remark: row.itemType === 'sku' ? `SKU补货：${row.itemName}` : ''
  }
  restockDialog.value.visible = true
}

// 确认补货
const confirmRestock = async () => {
  try {
    await restockProduct(
      restockForm.value.productId, 
      restockForm.value.quantity, 
      restockForm.value.remark,
      restockForm.value.skuId
    )
    
    ElMessage.success('补货成功')
    restockDialog.value.visible = false
    loadLowStockProducts()
  } catch (error) {
    ElMessage.error('补货失败: ' + (error.message || '未知错误'))
  }
}

// 查看历史
const viewHistory = async (row) => {
  historyDialog.value = {
    visible: true,
    productId: row.productId,
    skuId: row.skuId,
    productName: row.itemType === 'sku' ? `${row.productName} - ${row.itemName}` : row.productName
  }
  await loadStockHistory(row.productId, row.skuId)
}

// 加载库存历史记录
const loadStockHistory = async (productId, skuId) => {
  historyLoading.value = true
  try {
    const res = await getStockLog({ productId, skuId, pageSize: 50 })
    stockLogs.value = res.data?.records || []
  } catch (error) {
    ElMessage.error('加载历史记录失败')
    stockLogs.value = []
  } finally {
    historyLoading.value = false
  }
}

// 获取变更类型标签
const getChangeTypeTag = (type) => {
  const types = {
    1: { text: '入库', type: 'success' },
    2: { text: '出库', type: 'danger' },
    3: { text: '退货', type: 'warning' },
    4: { text: '调整', type: 'info' }
  }
  return types[type] || { text: '未知', type: 'info' }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 刷新数据
const refreshData = () => {
  loadLowStockProducts()
  ElMessage.success('数据已刷新')
}

// 导出数据
const escapeCsvCell = (value) => {
  const text = value == null ? '' : String(value)
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const getExportStatusLabel = (stock) => {
  if (stock === 0) return '缺货'
  if (stock <= warningSettings.value.criticalThreshold) return '紧急'
  if (stock <= warningSettings.value.threshold) return '预警'
  return '正常'
}

const getExportDays = (row) => {
  if (!row.salesCount || row.salesCount === 0) return '—'
  const dailySales = row.salesCount / 30
  if (dailySales === 0) return '—'
  const days = Math.floor((row.stock || 0) / dailySales)
  return days > 999 ? '999+' : String(days)
}

const buildExportFilename = () => {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  return `stock-warning-${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}.csv`
}

const exportData = () => {
  if (!lowStockProducts.value.length) {
    ElMessage.warning('暂无可导出的库存预警数据')
    return
  }

  const headers = ['层级', '商品名称', 'SKU/库存项', '商品分类', '规格', '当前库存', '月销量', '日均销量', '预计可售天数', '状态']
  const rows = lowStockProducts.value.map((row) => [
    row.itemTypeText || '',
    row.productName || '',
    row.itemName || '',
    row.categoryName || '',
    row.specInfo || '',
    row.stock ?? 0,
    row.salesCount ?? 0,
    Number(row.salesCount || 0) > 0 ? (Number(row.salesCount) / 30).toFixed(1) : '0.0',
    getExportDays(row),
    getExportStatusLabel(row.stock ?? 0)
  ])

  const csvContent = ['\uFEFF' + headers.map(escapeCsvCell).join(','), ...rows.map((row) => row.map(escapeCsvCell).join(','))].join('\r\n')
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = buildExportFilename()
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  ElMessage.success('库存预警数据导出成功')
}

onMounted(() => {
  loadCategories()
  loadLowStockProducts()
})
</script>

<style lang="scss" scoped>
.stock-warning-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .page-title {
      font-size: 24px;
      font-weight: 500;
      color: #303133;
    }
  }
  
  .settings-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .threshold-hint {
      margin-left: 10px;
      color: #909399;
      font-size: 12px;
    }
  }
  
  .stats-overview {
    margin-bottom: 20px;
    
    .stat-card {
      position: relative;
      overflow: hidden;
      
      &.normal .stat-icon {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
      
      &.warning .stat-icon {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }
      
      &.danger .stat-icon {
        background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
      }
      
      &.out-of-stock .stat-icon {
        background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
      }
      
      :deep(.el-card__body) {
        display: flex;
        align-items: center;
        padding: 20px;
      }
      
      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 15px;
        
        .el-icon {
          font-size: 28px;
          color: white;
        }
      }
      
      .stat-info {
        flex: 1;
        
        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: #303133;
        }
        
        .stat-label {
          font-size: 14px;
          color: #909399;
          margin-top: 5px;
        }
      }
    }
  }
  
  .warning-list {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .filter-controls {
        display: flex;
        gap: 10px;
      }
    }
    
    .product-info {
      display: flex;
      align-items: center;
      gap: 5px;
      
      .product-name {
        font-weight: 500;
      }

      .sku-name {
        color: #909399;
        font-size: 12px;
        margin-top: 2px;
      }
    }
    
    .days-normal {
      color: #67c23a;
    }
    
    .days-warning {
      color: #e6a23c;
      font-weight: 600;
    }
    
    .days-danger {
      color: #f56c6c;
      font-weight: 600;
    }
    
    :deep(.warning-row) {
      background-color: #fef0f0;
    }
    
    :deep(.critical-row) {
      background-color: #fdf6ec;
    }
    
    :deep(.out-of-stock-row) {
      background-color: #f4f4f5;
    }
  }
  
  .empty-tip {
    text-align: center;
    padding: 40px 0;
    color: #909399;
    font-size: 14px;
  }
}
</style>
