<template>
  <div class="app-page order-manage-page">
    <section class="app-page-header">
      <div>
        <p class="section-eyebrow">Trade Console</p>
        <h1 class="app-page-header__title">订单管理</h1>
        <p class="section-caption">面向管理员统一查看订单状态、支付状态和配送入口。</p>
      </div>
    </section>

    <el-card class="app-panel-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="card-header__copy">
            <strong>订单检索</strong>
            <span>支持按订单号、用户 ID、订单状态和支付状态筛选。</span>
          </div>
          <el-tag type="info">共 {{ total }} 笔订单</el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="订单号">
          <el-input
            v-model="searchForm.orderNo"
            clearable
            placeholder="请输入订单号"
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户 ID">
          <el-input
            v-model="searchForm.userId"
            clearable
            placeholder="请输入用户 ID"
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select
            v-model="searchForm.orderStatus"
            clearable
            placeholder="全部状态"
            style="width: 160px"
          >
            <el-option
              v-for="item in orderStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select
            v-model="searchForm.payStatus"
            clearable
            placeholder="全部状态"
            style="width: 160px"
          >
            <el-option
              v-for="item in payStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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

      <div class="app-table-shell">
        <el-table :data="tableData" v-loading="loading" size="default">
          <el-table-column prop="id" label="订单 ID" width="96" />
          <el-table-column prop="orderNo" label="订单号" min-width="210" />
          <el-table-column label="用户 ID" width="110">
            <template #default="{ row }">
              {{ row.userId ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="收货人" min-width="160">
            <template #default="{ row }">
              <div class="receiver-block">
                <strong>{{ row.receiverName || '-' }}</strong>
                <span>{{ row.receiverPhone || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="payAmount" label="实付金额" width="110">
            <template #default="{ row }">
              <span class="price">¥{{ row.payAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="订单状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusTagType(row.orderStatus)">
                {{ row.orderStatusText || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="支付状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getPayStatusTagType(row.payStatus)">
                {{ formatPayStatusText(row.payStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="180" />
          <el-table-column prop="payTime" label="支付时间" width="180" />
          <el-table-column label="操作" fixed="right" width="260">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleViewDetail(row.id)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-button
                v-if="canCancelOrder(row)"
                link
                type="danger"
                @click="handleAdminCancel(row)"
              >
                取消
              </el-button>
              <el-button
                v-if="canManageDelivery(row)"
                link
                type="success"
                @click="handleManageDelivery(row.id)"
              >
                配送
              </el-button>
              <el-button link type="primary" @click="handleViewTracking(row.id)">
                <el-icon><Location /></el-icon>
                物流
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="920px">
      <div v-loading="detailLoading">
        <el-empty v-if="!currentOrder" description="暂无订单详情" />

        <template v-else>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单 ID">{{ currentOrder.id }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ currentOrder.orderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户 ID">{{ currentOrder.userId ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="getOrderStatusTagType(currentOrder.orderStatus)">
                {{ currentOrder.orderStatusText || '-' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="支付状态">
              <el-tag :type="getPayStatusTagType(currentOrder.payStatus)">
                {{ formatPayStatusText(currentOrder.payStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="实付金额">¥{{ currentOrder.payAmount }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ currentOrder.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="支付时间">{{ currentOrder.payTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ currentOrder.receiverAddress || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="订单备注" :span="2">
              {{ currentOrder.remark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item
              v-if="currentOrder.cancelReason"
              label="取消原因"
              :span="2"
            >
              {{ currentOrder.cancelReason }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider>商品明细</el-divider>

          <div class="app-table-shell">
            <el-table :data="currentOrder.items || []" border>
              <el-table-column prop="productName" label="商品名称" min-width="180" />
              <el-table-column prop="specInfo" label="规格" min-width="140" />
              <el-table-column prop="price" label="单价" width="120">
                <template #default="{ row }">¥{{ row.price }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="90" />
              <el-table-column prop="totalAmount" label="小计" width="120">
                <template #default="{ row }">¥{{ row.totalAmount }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import { adminCancelOrder, getAdminOrderDetail, getAdminOrderPage } from '@/api/order'

const router = useRouter()

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref([])
const currentOrder = ref(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const searchForm = reactive({
  orderNo: '',
  userId: '',
  orderStatus: null,
  payStatus: null
})

const orderStatusOptions = [
  { label: '待付款', value: 1 },
  { label: '待发货', value: 2 },
  { label: '待收货', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 },
  { label: '退款中', value: 6 },
  { label: '已退款', value: 7 }
]

const payStatusOptions = [
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '支付失败', value: 2 },
  { label: '已退款', value: 3 }
]

const buildQuery = () => {
  const query = {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize
  }

  const trimmedOrderNo = searchForm.orderNo.trim()
  if (trimmedOrderNo) {
    query.orderNo = trimmedOrderNo
  }

  const trimmedUserId = searchForm.userId.trim()
  if (trimmedUserId) {
    const parsedUserId = Number(trimmedUserId)
    if (Number.isFinite(parsedUserId) && parsedUserId > 0) {
      query.userId = parsedUserId
    }
  }

  if (searchForm.orderStatus !== null) {
    query.orderStatus = searchForm.orderStatus
  }

  if (searchForm.payStatus !== null) {
    query.payStatus = searchForm.payStatus
  }

  return query
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminOrderPage(buildQuery())
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error(error.message || '订单列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.orderNo = ''
  searchForm.userId = ''
  searchForm.orderStatus = null
  searchForm.payStatus = null
  pagination.pageNum = 1
  loadData()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadData()
}

const handleViewDetail = async (orderId) => {
  detailVisible.value = true
  detailLoading.value = true
  currentOrder.value = null

  try {
    const res = await getAdminOrderDetail(orderId)
    currentOrder.value = res.data || null
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(error.message || '订单详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

const handleViewTracking = (orderId) => {
  router.push(`/delivery/tracking?orderId=${orderId}`)
}

const canCancelOrder = (order) => Number(order.orderStatus) === 1
const canManageDelivery = (order) => [2, 3].includes(Number(order.orderStatus))

const handleAdminCancel = async (order) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `确定要取消订单 ${order.orderNo || order.id} 吗？`,
      '取消订单',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        inputValue: '管理员后台取消订单',
        inputPlaceholder: '请输入取消原因',
        inputValidator: (value) => Boolean(value && value.trim()),
        inputErrorMessage: '请输入取消原因',
        type: 'warning'
      }
    )
    await adminCancelOrder(order.id, value)
    ElMessage.success('订单已取消')
    await loadData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

const handleManageDelivery = (orderId) => {
  router.push(`/delivery/tracking?orderId=${orderId}`)
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

const getPayStatusTagType = (status) => {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: 'info'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.section-eyebrow {
  margin: 0 0 8px;
  font-size: 0.78rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.section-caption {
  margin: 12px 0 0;
  max-width: 640px;
  color: var(--color-text-muted);
}

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
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(111, 78, 55, 0.05) 0%, rgba(111, 78, 55, 0.02) 100%);
}

.receiver-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.receiver-block span {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.price {
  color: #d9485f;
  font-weight: var(--font-bold);
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
