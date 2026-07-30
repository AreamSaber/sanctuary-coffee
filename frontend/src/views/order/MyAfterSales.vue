<template>
  <div class="app-page my-after-sales-page">
    <section class="app-page-header">
      <div>
        <p class="section-eyebrow">After-Sales Center</p>
        <h1 class="app-page-header__title">我的售后</h1>
        <p class="section-caption">查看退款申请、审核结果和处理进度，退款中的咖啡也得有个明明白白的旅程。</p>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/order')">返回订单</el-button>
      </div>
    </section>

    <el-card class="app-panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-header__copy">
            <strong>售后进度</strong>
            <span>支持按售后单号、订单号、类型和状态筛选。</span>
          </div>
          <el-tag type="info">共 {{ total }} 条售后</el-tag>
        </div>
      </template>

      <div class="app-toolbar my-after-sales-page__toolbar">
        <el-input
          v-model.trim="searchForm.afterSaleNo"
          clearable
          placeholder="售后单号"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model.trim="searchForm.orderNo"
          clearable
          placeholder="订单号"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="searchForm.type" clearable placeholder="售后类型">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="searchForm.status" clearable placeholder="售后状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <div v-loading="loading" class="after-sale-list">
        <el-empty v-if="tableData.length === 0" description="暂无售后记录" />

        <template v-else>
          <article v-for="item in tableData" :key="item.id" class="after-sale-card">
            <header class="after-sale-card__header">
              <div>
                <strong>售后单号：{{ item.afterSaleNo }}</strong>
                <span>订单号：{{ item.orderNo || '-' }}</span>
              </div>
              <el-tag :type="getStatusTagType(item.status)">
                {{ item.statusText || formatStatusText(item.status) }}
              </el-tag>
            </header>

            <div class="after-sale-card__body">
              <div class="after-sale-card__summary">
                <el-tag :type="getTypeTagType(item.type)" effect="plain">
                  {{ item.typeText || formatTypeText(item.type) }}
                </el-tag>
                <span>申请时间：{{ item.createTime || '-' }}</span>
                <span>退款金额：¥{{ formatAmount(item.refundAmount) }}</span>
                <span>处理时间：{{ item.handleTime || '-' }}</span>
              </div>

              <div class="after-sale-card__goods">
                <div v-for="goods in previewItems(item.items)" :key="goods.id || goods.productId" class="goods-item">
                  <ProductImage :src="goods.productImage" :name="goods.productName" fit="cover" class="goods-image" />
                  <div>
                    <div class="goods-name">{{ goods.productName }}</div>
                    <div v-if="goods.specInfo" class="goods-spec">{{ goods.specInfo }}</div>
                    <div class="goods-meta">¥{{ formatAmount(goods.price) }} × {{ goods.quantity }}</div>
                  </div>
                </div>
                <span v-if="(item.items || []).length > 2" class="goods-more">
                  另有 {{ item.items.length - 2 }} 件商品
                </span>
              </div>

              <div class="after-sale-card__actions">
                <el-button type="primary" plain @click="handleViewDetail(item.id)">查看进度</el-button>
              </div>
            </div>
          </article>
        </template>
      </div>

      <div class="app-pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="售后进度详情" width="min(920px, 96vw)">
      <div v-loading="detailLoading">
        <template v-if="currentAfterSale">
          <div class="progress-strip">
            <div class="progress-node progress-node--done">
              <strong>提交申请</strong>
              <span>{{ currentAfterSale.createTime || '-' }}</span>
            </div>
            <div
              class="progress-node"
              :class="{
                'progress-node--done': Number(currentAfterSale.status) !== 1,
                'progress-node--active': Number(currentAfterSale.status) === 1
              }"
            >
              <strong>平台审核</strong>
              <span>{{ currentAfterSale.handleTime || '等待处理' }}</span>
            </div>
            <div class="progress-node" :class="getResultNodeClass(currentAfterSale.status)">
              <strong>{{ getResultTitle(currentAfterSale.status) }}</strong>
              <span>{{ currentAfterSale.statusText || formatStatusText(currentAfterSale.status) }}</span>
            </div>
          </div>

          <el-descriptions :column="2" border class="after-sale-detail__meta">
            <el-descriptions-item label="售后单号">{{ currentAfterSale.afterSaleNo }}</el-descriptions-item>
            <el-descriptions-item label="售后状态">
              <el-tag :type="getStatusTagType(currentAfterSale.status)">
                {{ currentAfterSale.statusText || formatStatusText(currentAfterSale.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单号">{{ currentAfterSale.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="售后类型">
              {{ currentAfterSale.typeText || formatTypeText(currentAfterSale.type) }}
            </el-descriptions-item>
            <el-descriptions-item label="退款金额">¥{{ formatAmount(currentAfterSale.refundAmount) }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              {{ currentAfterSale.orderStatusText || formatOrderStatusText(currentAfterSale.orderStatus) }}
            </el-descriptions-item>
            <el-descriptions-item label="售后原因" :span="2">
              {{ currentAfterSale.reason || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="售后说明" :span="2">
              {{ currentAfterSale.description || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="处理备注" :span="2">
              {{ currentAfterSale.handleRemark || '平台还没有填写处理备注' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider>处理时间线</el-divider>

          <el-timeline class="after-sale-log-timeline">
            <el-timeline-item
              v-for="log in normalizedLogs(currentAfterSale)"
              :key="log.id || `${log.action}-${log.createTime}`"
              :timestamp="log.createTime || '-'"
              :type="getLogTimelineType(log)"
              placement="top"
            >
              <div class="after-sale-log-card">
                <div class="after-sale-log-card__header">
                  <strong>{{ log.actionText || formatLogActionText(log.action) }}</strong>
                  <el-tag size="small" effect="plain">
                    {{ log.operatorTypeText || formatOperatorTypeText(log.operatorType) }}
                  </el-tag>
                </div>
                <p v-if="formatStatusChange(log)" class="after-sale-log-card__status">
                  {{ formatStatusChange(log) }}
                </p>
                <p class="after-sale-log-card__remark">{{ log.remark || '暂无备注' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>

          <el-divider>商品明细</el-divider>

          <div class="app-table-shell">
            <el-table :data="currentAfterSale.items || []" border>
              <el-table-column prop="productName" label="商品名称" min-width="180" />
              <el-table-column prop="specInfo" label="规格" min-width="140" />
              <el-table-column prop="price" label="单价" width="120">
                <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="90" />
              <el-table-column prop="totalAmount" label="小计" width="120">
                <template #default="{ row }">¥{{ formatAmount(row.totalAmount) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="router.push('/order')">查看订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import ProductImage from '@/components/common/ProductImage.vue'
import { getMyAfterSaleDetail, getMyAfterSalePage } from '@/api/afterSale'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref([])
const currentAfterSale = ref(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const searchForm = reactive({
  afterSaleNo: '',
  orderNo: typeof route.query.orderNo === 'string' ? route.query.orderNo : '',
  type: null,
  status: null
})

const typeOptions = [
  { label: '仅退款', value: 1 },
  { label: '配送问题', value: 2 },
  { label: '退货退款', value: 3 }
]

const statusOptions = [
  { label: '待处理', value: 1 },
  { label: '已同意', value: 2 },
  { label: '已驳回', value: 3 },
  { label: '处理中', value: 4 },
  { label: '已完成', value: 5 }
]

onMounted(() => {
  loadData()
})

const buildQuery = () => ({
  pageNum: pagination.pageNum,
  pageSize: pagination.pageSize,
  afterSaleNo: searchForm.afterSaleNo || undefined,
  orderNo: searchForm.orderNo || undefined,
  type: searchForm.type ?? undefined,
  status: searchForm.status ?? undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyAfterSalePage(buildQuery())
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '售后记录加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.afterSaleNo = ''
  searchForm.orderNo = ''
  searchForm.type = null
  searchForm.status = null
  pagination.pageNum = 1
  loadData()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadData()
}

const handleViewDetail = async (afterSaleId) => {
  detailVisible.value = true
  detailLoading.value = true
  currentAfterSale.value = null

  try {
    const res = await getMyAfterSaleDetail(afterSaleId)
    currentAfterSale.value = res.data || null
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(error.message || '售后详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const previewItems = (items = []) => {
  return items.slice(0, 2)
}

const formatAmount = (amount) => {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const numeric = Number(amount)
  return Number.isFinite(numeric) ? numeric.toFixed(2) : amount
}

const formatTypeText = (type) => {
  const map = {
    1: '仅退款',
    2: '配送问题',
    3: '退货退款'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type) => {
  const map = {
    1: 'warning',
    2: 'info',
    3: 'danger'
  }
  return map[type] || 'info'
}

const formatStatusText = (status) => {
  const map = {
    1: '待处理',
    2: '已同意',
    3: '已驳回',
    4: '处理中',
    5: '已完成'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status) => {
  const map = {
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info',
    5: 'success'
  }
  return map[status] || 'info'
}

const formatOrderStatusText = (status) => {
  const map = {
    1: '待付款',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消',
    6: '退款中',
    7: '已退款'
  }
  return map[status] || '未知'
}

const getResultTitle = (status) => {
  if (Number(status) === 3) {
    return '申请驳回'
  }
  if (Number(status) === 5 || Number(status) === 2) {
    return '处理完成'
  }
  return '等待结果'
}

const getResultNodeClass = (status) => {
  if (Number(status) === 3) {
    return 'progress-node--rejected'
  }
  if ([2, 5].includes(Number(status))) {
    return 'progress-node--done'
  }
  return 'progress-node--pending'
}

const normalizedLogs = (afterSale) => {
  if (afterSale?.logs?.length) {
    return afterSale.logs
  }

  return [
    {
      id: 'fallback-apply',
      action: 'APPLY',
      actionText: '提交申请',
      operatorType: 'USER',
      operatorTypeText: '用户',
      statusTo: 1,
      statusToText: '待处理',
      remark: afterSale?.reason || '用户提交售后申请',
      createTime: afterSale?.createTime
    },
    ...(afterSale?.handleTime
      ? [{
          id: 'fallback-handle',
          action: Number(afterSale.status) === 3 ? 'REJECT' : 'REFUND_COMPLETE',
          actionText: Number(afterSale.status) === 3 ? '审核驳回' : '退款完成',
          operatorType: 'ADMIN',
          operatorTypeText: '管理员',
          statusTo: afterSale.status,
          statusToText: afterSale.statusText || formatStatusText(afterSale.status),
          remark: afterSale.handleRemark || '平台已处理售后申请',
          createTime: afterSale.handleTime
        }]
      : [])
  ]
}

const formatLogActionText = (action) => {
  const map = {
    APPLY: '提交申请',
    APPROVE: '审核通过',
    REJECT: '审核驳回',
    REFUND_COMPLETE: '退款完成'
  }
  return map[action] || '处理记录'
}

const formatOperatorTypeText = (operatorType) => {
  const map = {
    USER: '用户',
    ADMIN: '管理员'
  }
  return map[operatorType] || '系统'
}

const formatStatusChange = (log) => {
  const fromText = log.statusFromText || (log.statusFrom ? formatStatusText(log.statusFrom) : '')
  const toText = log.statusToText || (log.statusTo ? formatStatusText(log.statusTo) : '')
  if (fromText && toText) {
    return `${fromText} -> ${toText}`
  }
  return toText ? `状态：${toText}` : ''
}

const getLogTimelineType = (log) => {
  const action = log.action || ''
  if (action === 'REJECT') {
    return 'danger'
  }
  if (action === 'REFUND_COMPLETE' || action === 'APPROVE') {
    return 'success'
  }
  return 'primary'
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-4);
}

.card-header__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header__copy span {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.my-after-sales-page__toolbar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto auto;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
}

.after-sale-list {
  min-height: 320px;
}

.after-sale-card {
  border: 1px solid rgba(111, 78, 55, 0.1);
  border-radius: 24px;
  margin-bottom: var(--spacing-4);
  overflow: hidden;
  background: linear-gradient(180deg, #ffffff 0%, #fffaf4 100%);
}

.after-sale-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-4);
  padding: var(--spacing-4) var(--spacing-5);
  border-bottom: 1px solid rgba(111, 78, 55, 0.08);
  background: rgba(111, 78, 55, 0.04);
}

.after-sale-card__header div {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
  align-items: center;
}

.after-sale-card__header span,
.after-sale-card__summary span {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.after-sale-card__body {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr) 120px;
  gap: var(--spacing-5);
  padding: var(--spacing-5);
  align-items: center;
}

.after-sale-card__summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.after-sale-card__goods {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.goods-item {
  display: flex;
  gap: var(--spacing-3);
  align-items: center;
}

.goods-image {
  width: 58px;
  height: 58px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
}

.goods-name {
  font-weight: var(--font-bold);
  color: var(--color-text);
}

.goods-spec,
.goods-meta,
.goods-more {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  margin-top: 4px;
}

.after-sale-card__actions {
  display: flex;
  justify-content: flex-end;
}

.progress-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
}

.progress-node {
  border-radius: 18px;
  border: 1px solid rgba(111, 78, 55, 0.1);
  padding: var(--spacing-4);
  background: #fffaf4;
}

.progress-node strong,
.progress-node span {
  display: block;
}

.progress-node span {
  margin-top: 6px;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.progress-node--active {
  border-color: rgba(230, 162, 60, 0.45);
  background: #fff7e6;
}

.progress-node--done {
  border-color: rgba(103, 194, 58, 0.35);
  background: #f0f9eb;
}

.progress-node--rejected {
  border-color: rgba(245, 108, 108, 0.35);
  background: #fef0f0;
}

.after-sale-detail__meta {
  margin-bottom: var(--spacing-4);
}

.after-sale-log-timeline {
  padding: 0 var(--spacing-2);
}

.after-sale-log-card {
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: 16px;
  border: 1px solid rgba(111, 78, 55, 0.1);
  background: #fffaf4;
}

.after-sale-log-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
}

.after-sale-log-card__status,
.after-sale-log-card__remark {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

@media (max-width: 1080px) {
  .my-after-sales-page__toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .after-sale-card__body {
    grid-template-columns: 1fr;
  }

  .after-sale-card__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .my-after-sales-page__toolbar,
  .progress-strip {
    grid-template-columns: 1fr;
  }

  .after-sale-card__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
