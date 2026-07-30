<template>
  <div class="app-page refund-manage-page">
    <section class="app-page-header">
      <div>
        <p class="section-eyebrow">Refund Console</p>
        <h1 class="app-page-header__title">退款管理</h1>
        <p class="section-caption">面向管理员统一查看退款申请、订单状态和售后处理信息；当前为毕设演示环境，退款审核通过后执行模拟原路退款。</p>
      </div>
    </section>

    <el-card class="app-panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-header__copy">
            <strong>退款检索</strong>
            <span>支持按退款单号、订单号、用户 ID 和退款状态筛选。</span>
          </div>
          <el-tag type="info">共 {{ total }} 笔退款</el-tag>
        </div>
      </template>

      <div class="app-toolbar refund-manage-page__toolbar">
        <el-input
          v-model.trim="searchForm.refundNo"
          clearable
          placeholder="退款单号"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model.trim="searchForm.orderNo"
          clearable
          placeholder="订单号"
          @keyup.enter="handleSearch"
        />
        <el-input
          v-model.trim="searchForm.userId"
          clearable
          placeholder="用户 ID"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="searchForm.refundStatus" clearable placeholder="退款状态">
          <el-option
            v-for="item in refundStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <div class="app-table-shell">
        <el-table v-loading="loading" :data="tableData" border>
          <el-table-column prop="refundNo" label="退款单号" min-width="180" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="userId" label="用户 ID" width="110" />
          <el-table-column label="退款金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.refundAmount) }}</template>
          </el-table-column>
          <el-table-column label="退款状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getRefundStatusTagType(row.refundStatus)">
                {{ row.refundStatusText || formatRefundStatusText(row.refundStatus) }}
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
          <el-table-column prop="createTime" label="申请时间" min-width="170" />
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="handleViewDetail(row.id)">
                查看详情
              </el-button>
              <el-button
                v-if="isRefundPending(row)"
                link
                type="success"
                :loading="reviewing"
                @click="handleApprove(row)"
              >
                通过
              </el-button>
              <el-button
                v-if="isRefundPending(row)"
                link
                type="danger"
                :loading="reviewing"
                @click="handleReject(row)"
              >
                驳回
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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

    <el-dialog v-model="detailVisible" title="退款详情" width="min(960px, 96vw)">
      <div v-loading="detailLoading">
        <template v-if="currentRefund">
          <el-descriptions :column="2" border class="refund-detail__meta">
            <el-descriptions-item label="退款单号">{{ currentRefund.refundNo }}</el-descriptions-item>
            <el-descriptions-item label="退款状态">
              <el-tag :type="getRefundStatusTagType(currentRefund.refundStatus)">
                {{ currentRefund.refundStatusText || formatRefundStatusText(currentRefund.refundStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单号">{{ currentRefund.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="用户 ID">{{ currentRefund.userId }}</el-descriptions-item>
            <el-descriptions-item label="退款金额">¥{{ formatAmount(currentRefund.refundAmount) }}</el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ currentRefund.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="退款时间">{{ currentRefund.refundTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="getOrderStatusTagType(currentRefund.orderStatus)">
                {{ currentRefund.orderStatusText || formatOrderStatusText(currentRefund.orderStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="收货人">{{ currentRefund.receiverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentRefund.receiverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ currentRefund.receiverAddress || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="退款原因" :span="2">
              {{ currentRefund.refundReason || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="订单备注" :span="2">
              {{ currentRefund.remark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="售后单号">{{ currentRefund.afterSaleNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="售后状态">
              {{ currentRefund.afterSaleStatusText || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="处理时间">{{ currentRefund.handleTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处理备注">
              {{ currentRefund.handleRemark || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-alert
            title="模拟退款说明"
            type="info"
            show-icon
            :closable="false"
            description="审核通过后系统会模拟原路退款，并同步回补库存、更新支付状态和订单状态；不会调用真实第三方支付退款接口。"
            class="refund-demo-alert"
          />

          <el-divider>处理时间线</el-divider>

          <el-timeline class="refund-log-timeline">
            <el-timeline-item
              v-for="log in normalizedLogs(currentRefund)"
              :key="log.id || `${log.action}-${log.createTime}`"
              :timestamp="log.createTime || '-'"
              :type="getLogTimelineType(log)"
              placement="top"
            >
              <div class="refund-log-card">
                <div class="refund-log-card__header">
                  <strong>{{ log.actionText || formatLogActionText(log.action) }}</strong>
                  <el-tag size="small" effect="plain">
                    {{ log.operatorTypeText || formatOperatorTypeText(log.operatorType) }}
                  </el-tag>
                </div>
                <p v-if="formatStatusChange(log)" class="refund-log-card__status">
                  {{ formatStatusChange(log) }}
                </p>
                <p class="refund-log-card__remark">{{ log.remark || '暂无备注' }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>

          <el-divider>商品明细</el-divider>

          <div class="app-table-shell">
            <el-table :data="currentRefund.items || []" border>
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
          v-if="isRefundPending(currentRefund)"
          type="danger"
          :loading="reviewing"
          @click="handleReject(currentRefund)"
        >
          驳回退款
        </el-button>
        <el-button
          v-if="isRefundPending(currentRefund)"
          type="primary"
          :loading="reviewing"
          @click="handleApprove(currentRefund)"
        >
          确认模拟退款
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
  approveAdminRefund,
  getAdminRefundDetail,
  getAdminRefundPage,
  rejectAdminRefund
} from '@/api/payment'

const loading = ref(false)
const detailLoading = ref(false)
const reviewing = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref([])
const currentRefund = ref(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const searchForm = reactive({
  refundNo: '',
  orderNo: '',
  userId: '',
  refundStatus: null
})

const refundStatusOptions = [
  { label: '退款处理中', value: 0 },
  { label: '已退款', value: 1 },
  { label: '已驳回', value: 2 }
]

onMounted(() => {
  loadData()
})

const buildQuery = () => {
  const parsedUserId = Number(searchForm.userId)

  return {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    refundNo: searchForm.refundNo || undefined,
    orderNo: searchForm.orderNo || undefined,
    userId: Number.isFinite(parsedUserId) && parsedUserId > 0 ? parsedUserId : undefined,
    refundStatus: searchForm.refundStatus ?? undefined
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminRefundPage(buildQuery())
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '退款列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.refundNo = ''
  searchForm.orderNo = ''
  searchForm.userId = ''
  searchForm.refundStatus = null
  pagination.pageNum = 1
  loadData()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadData()
}

const handleViewDetail = async (refundId) => {
  detailVisible.value = true
  detailLoading.value = true
  currentRefund.value = null

  try {
    const res = await getAdminRefundDetail(refundId)
    currentRefund.value = res.data || null
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(error.message || '退款详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const isRefundPending = (refund) => {
  return Number(refund?.refundStatus) === 0
}

const handleApprove = async (refund) => {
  if (!refund?.id || reviewing.value) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('可填写审核备注，审核通过后将执行模拟原路退款并回补库存，不会调用真实第三方退款接口。', '确认模拟退款', {
      inputPlaceholder: '例如：审核通过，模拟原路退款完成',
      confirmButtonText: '确认模拟退款',
      cancelButtonText: '取消'
    })
    reviewing.value = true
    await approveAdminRefund(refund.id, { remark: value?.trim() || undefined })
    ElMessage.success('模拟退款已审核通过')
    detailVisible.value = false
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '退款审核失败')
    }
  } finally {
    reviewing.value = false
  }
}

const handleReject = async (refund) => {
  if (!refund?.id || reviewing.value) {
    return
  }

  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因，驳回后订单会恢复到退款前的交易状态。', '驳回退款', {
      inputPlaceholder: '例如：商品已出库，不满足退款条件',
      inputPattern: /\S+/,
      inputErrorMessage: '请输入驳回原因',
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    reviewing.value = true
    await rejectAdminRefund(refund.id, { remark: value.trim() })
    ElMessage.success('退款已驳回')
    detailVisible.value = false
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '退款驳回失败')
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

const formatRefundStatusText = (status) => {
  const map = {
    0: '退款处理中',
    1: '已退款',
    2: '已驳回'
  }
  return map[status] || '未知'
}

const getRefundStatusTagType = (status) => {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger'
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
    2: 'primary',
    3: 'primary',
    4: 'success',
    5: 'info',
    6: 'warning',
    7: 'success'
  }
  return map[status] || 'info'
}

const normalizedLogs = (refund) => {
  if (refund?.logs?.length) {
    return refund.logs
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
      remark: refund?.refundReason || '用户提交退款申请',
      createTime: refund?.createTime
    },
    ...(refund?.handleTime
      ? [{
          id: 'fallback-handle',
          action: Number(refund.refundStatus) === 2 ? 'REJECT' : 'REFUND_COMPLETE',
          actionText: Number(refund.refundStatus) === 2 ? '审核驳回' : '退款完成',
          operatorType: 'ADMIN',
          operatorTypeText: '管理员',
          statusTo: refund.afterSaleStatus,
          statusToText: refund.afterSaleStatusText || '-',
          remark: refund.handleRemark || '平台已处理退款申请',
          createTime: refund.handleTime
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

const formatAfterSaleStatusText = (status) => {
  const map = {
    1: '待处理',
    2: '已同意',
    3: '已驳回',
    4: '处理中',
    5: '已完成'
  }
  return map[status] || '未知'
}

const formatStatusChange = (log) => {
  const fromText = log.statusFromText || (log.statusFrom ? formatAfterSaleStatusText(log.statusFrom) : '')
  const toText = log.statusToText || (log.statusTo ? formatAfterSaleStatusText(log.statusTo) : '')
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
.refund-manage-page__toolbar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto auto;
  gap: var(--spacing-3);
  margin-bottom: var(--spacing-5);
}

.refund-detail__meta {
  margin-bottom: var(--spacing-4);
}

.refund-demo-alert {
  margin-bottom: var(--spacing-4);
  border-radius: 16px;
}

.refund-log-timeline {
  padding: 0 var(--spacing-2);
}

.refund-log-card {
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: 16px;
  border: 1px solid rgba(111, 78, 55, 0.1);
  background: #fffaf4;
}

.refund-log-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
}

.refund-log-card__status,
.refund-log-card__remark {
  margin: 8px 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

@media (max-width: 1080px) {
  .refund-manage-page__toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .refund-manage-page__toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
