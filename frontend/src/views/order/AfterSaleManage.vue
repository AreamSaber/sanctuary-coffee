<template>
  <div class="app-page after-sale-manage-page">
    <section class="app-page-header">
      <div>
        <p class="section-eyebrow">After-Sales Console</p>
        <h1 class="app-page-header__title">售后管理</h1>
        <p class="section-caption">面向管理员统一查看退款类与配送类售后单，联动订单状态、支付状态和商品明细；退款通过后执行模拟原路退款。</p>
      </div>
    </section>

    <el-card class="app-panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-header__copy">
            <strong>售后检索</strong>
            <span>支持按售后单号、订单号、用户 ID、售后类型和状态筛选。</span>
          </div>
          <el-tag type="info">共 {{ total }} 条售后</el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="page-search-form">
        <el-form-item label="售后单号">
          <el-input v-model="searchForm.afterSaleNo" placeholder="请输入售后单号" clearable />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户 ID" clearable />
        </el-form-item>
        <el-form-item label="售后类型">
          <el-select v-model="searchForm.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="售后状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="app-table-shell">
        <el-table v-loading="loading" :data="tableData" border>
          <el-table-column prop="afterSaleNo" label="售后单号" min-width="180" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="userId" label="用户 ID" width="110" />
          <el-table-column label="售后类型" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)">
                {{ row.typeText || formatTypeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="退款金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column label="售后状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ row.statusText || formatStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="订单状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusTagType(row.orderStatus)">
                {{ row.orderStatusText || formatOrderStatusText(row.orderStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" min-width="168" />
          <el-table-column label="操作" fixed="right" width="220" align="center">
            <template #default="{ row }">
              <el-button type="primary" link :icon="View" @click="handleViewDetail(row.id)">详情</el-button>
              <el-button
                v-if="isPendingRefundAfterSale(row)"
                type="success"
                link
                :loading="reviewing"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button
                v-if="isPendingRefundAfterSale(row)"
                type="danger"
                link
                :loading="reviewing"
                @click="handleReject(row)"
              >
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="售后详情" width="min(960px, 96vw)">
      <div v-loading="detailLoading">
        <template v-if="currentAfterSale">
          <el-descriptions :column="2" border class="after-sale-detail__meta">
            <el-descriptions-item label="售后单号">{{ currentAfterSale.afterSaleNo }}</el-descriptions-item>
            <el-descriptions-item label="售后状态">
              <el-tag :type="getStatusTagType(currentAfterSale.status)">
                {{ currentAfterSale.statusText || formatStatusText(currentAfterSale.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单号">{{ currentAfterSale.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户 ID">{{ currentAfterSale.userId }}</el-descriptions-item>
            <el-descriptions-item label="售后类型">
              <el-tag :type="getTypeTagType(currentAfterSale.type)">
                {{ currentAfterSale.typeText || formatTypeText(currentAfterSale.type) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="退款金额">¥{{ formatAmount(currentAfterSale.refundAmount) }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ currentAfterSale.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处理时间">{{ currentAfterSale.handleTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="getOrderStatusTagType(currentAfterSale.orderStatus)">
                {{ currentAfterSale.orderStatusText || formatOrderStatusText(currentAfterSale.orderStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="getPayStatusTagType(currentAfterSale.payStatus)">
                {{ currentAfterSale.payStatusText || formatPayStatusText(currentAfterSale.payStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="收货人">{{ currentAfterSale.receiverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentAfterSale.receiverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ currentAfterSale.receiverAddress || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="售后原因" :span="2">
              {{ currentAfterSale.reason || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="售后说明" :span="2">
              {{ currentAfterSale.description || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="订单备注" :span="2">
              {{ currentAfterSale.remark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="处理备注" :span="2">
              {{ currentAfterSale.handleRemark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="审核人">
              {{ currentAfterSale.reviewerName || currentAfterSale.reviewerId || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="审核时间">
              {{ currentAfterSale.reviewTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="审核备注" :span="2">
              {{ currentAfterSale.reviewRemark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="凭证图片" :span="2">
              <div v-if="getAfterSaleImages(currentAfterSale.images).length" class="after-sale-image-list">
                <el-image
                  v-for="image in getAfterSaleImages(currentAfterSale.images)"
                  :key="image"
                  :src="image"
                  fit="cover"
                  :preview-src-list="getAfterSaleImages(currentAfterSale.images)"
                  preview-teleported
                  class="after-sale-image"
                />
              </div>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            v-if="isRefundRelatedAfterSale(currentAfterSale)"
            title="模拟退款说明"
            type="info"
            show-icon
            :closable="false"
            description="退款类售后审核通过后，系统会模拟原路退款并同步库存、支付、订单和发票状态；不会调用真实第三方退款接口。"
            class="after-sale-demo-alert"
          />

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
        <el-button
          v-if="isPendingRefundAfterSale(currentAfterSale)"
          type="danger"
          :loading="reviewing"
          @click="handleReject(currentAfterSale)"
        >
          驳回售后
        </el-button>
        <el-button
          v-if="isPendingRefundAfterSale(currentAfterSale)"
          type="primary"
          :loading="reviewing"
          @click="handleApprove(currentAfterSale)"
        >
          审核通过
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Search, View } from '@element-plus/icons-vue'
import {
  approveAdminAfterSale,
  getAdminAfterSaleDetail,
  getAdminAfterSalePage,
  rejectAdminAfterSale
} from '@/api/afterSale'

const loading = ref(false)
const detailLoading = ref(false)
const reviewing = ref(false)
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
  orderNo: '',
  userId: '',
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

const buildQuery = () => {
  const parsedUserId = Number(searchForm.userId)

  return {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    afterSaleNo: searchForm.afterSaleNo || undefined,
    orderNo: searchForm.orderNo || undefined,
    userId: Number.isFinite(parsedUserId) && parsedUserId > 0 ? parsedUserId : undefined,
    type: searchForm.type ?? undefined,
    status: searchForm.status ?? undefined
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminAfterSalePage(buildQuery())
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '售后列表加载失败')
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
  searchForm.userId = ''
  searchForm.type = null
  searchForm.status = null
  pagination.pageNum = 1
  loadData()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

const handleViewDetail = async (afterSaleId) => {
  detailVisible.value = true
  detailLoading.value = true
  currentAfterSale.value = null

  try {
    const res = await getAdminAfterSaleDetail(afterSaleId)
    currentAfterSale.value = res.data || null
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(error.message || '售后详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const isPendingRefundAfterSale = (afterSale) => {
  return isRefundRelatedAfterSale(afterSale) && Number(afterSale?.status) === 1
}

const isRefundRelatedAfterSale = (afterSale) => {
  return [1, 3].includes(Number(afterSale?.type))
}

const handleApprove = async (afterSale) => {
  if (!afterSale?.id || reviewing.value) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('可填写处理备注，通过后会执行模拟原路退款，并联动库存、支付和发票状态。', '审核通过售后', {
      inputPlaceholder: '例如：审核通过，模拟原路退款完成',
      confirmButtonText: '通过售后',
      cancelButtonText: '取消'
    })
    reviewing.value = true
    await approveAdminAfterSale(afterSale.id, { remark: value?.trim() || undefined })
    ElMessage.success('售后已审核通过')
    detailVisible.value = false
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '售后审核失败')
    }
  } finally {
    reviewing.value = false
  }
}

const handleReject = async (afterSale) => {
  if (!afterSale?.id || reviewing.value) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因，驳回后订单会恢复到退款前的交易状态。', '驳回售后', {
      inputPlaceholder: '例如：不满足退款条件',
      inputPattern: /\S+/,
      inputErrorMessage: '请输入驳回原因',
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    reviewing.value = true
    await rejectAdminAfterSale(afterSale.id, { remark: value.trim() })
    ElMessage.success('售后已驳回')
    detailVisible.value = false
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '售后驳回失败')
    }
  } finally {
    reviewing.value = false
  }
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

const getOrderStatusTagType = (status) => {
  const map = {
    1: 'warning',
    2: 'info',
    3: 'info',
    4: 'success',
    5: 'danger',
    6: 'warning',
    7: 'success'
  }
  return map[status] || 'info'
}

const formatPayStatusText = (status) => {
  const map = {
    0: '待支付',
    1: '已支付',
    2: '支付失败',
    3: '已退款'
  }
  return map[status] || '未知'
}

const getPayStatusTagType = (status) => {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: 'info'
  }
  return map[status] || 'info'
}

const getAfterSaleImages = (images) => {
  if (!images) {
    return []
  }
  if (Array.isArray(images)) {
    return images.filter(Boolean)
  }
  try {
    const parsed = JSON.parse(images)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    return []
  }
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
  gap: 16px;
}

.card-header__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header__copy span {
  color: var(--color-text-muted);
  font-size: 0.92rem;
}

.page-search-form {
  margin-bottom: 20px;
}

.page-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.after-sale-detail__meta {
  margin-bottom: 20px;
}

.after-sale-image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.after-sale-image {
  width: 88px;
  height: 88px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(111, 78, 55, 0.12);
}

.after-sale-demo-alert {
  margin-bottom: var(--spacing-4);
  border-radius: 16px;
}

.after-sale-log-timeline {
  padding: 0 8px;
}

.after-sale-log-card {
  padding: 12px 16px;
  border-radius: 16px;
  border: 1px solid rgba(111, 78, 55, 0.1);
  background: #fffaf4;
}

.after-sale-log-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.after-sale-log-card__status,
.after-sale-log-card__remark {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
</style>
