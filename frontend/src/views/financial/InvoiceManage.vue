<template>
  <div class="app-page invoice-manage-page">
    <section class="app-page-header">
      <div>
        <h1 class="app-page-header__title">发票中心</h1>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/financial/report')">返回报表</el-button>
      </div>
    </section>

    <el-card class="app-panel-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的发票" name="my" />
        <el-tab-pane v-if="isAdmin" label="全部发票" name="admin" />
      </el-tabs>

      <div class="app-toolbar invoice-toolbar">
        <el-select v-model="statusFilter" clearable placeholder="按状态筛选" @change="reloadCurrentTab">
          <el-option label="待开具" value="PENDING" />
          <el-option label="已开具" value="ISSUED" />
          <el-option label="已作废" value="VOID" />
        </el-select>
        <el-button @click="reloadCurrentTab">刷新</el-button>
      </div>

      <div class="app-table-shell">
        <el-table :data="currentRecords" v-loading="loading" border>
          <el-table-column prop="invoiceNo" label="发票号" min-width="180" />
          <el-table-column prop="orderId" label="订单ID" width="100" />
          <el-table-column prop="title" label="抬头" min-width="180" />
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="statusText" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ formatStatusText(row.statusText) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" />
          <el-table-column prop="issueTime" label="开具时间" width="180">
            <template #default="{ row }">
              {{ row.issueTime || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleViewDetail(row.id)">详情</el-button>
              <el-button
                v-if="isAdmin && row.status === 1"
                size="small"
                type="primary"
                @click="handleIssue(row.id)"
              >
                开具
              </el-button>
              <el-button
                v-if="row.status === 2"
                size="small"
                type="success"
                @click="handleResend(row.id)"
              >
                重发
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
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @current-change="reloadCurrentTab"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="发票详情" width="min(720px, 92vw)">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="发票号">{{ detail.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="抬头">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(detail.status)">
            {{ formatStatusText(detail.statusText) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发票类型">{{ formatInvoiceType(detail.invoiceType) }}</el-descriptions-item>
        <el-descriptions-item label="抬头类型">{{ formatTitleType(detail.titleType) }}</el-descriptions-item>
        <el-descriptions-item label="税号">{{ detail.taxNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开具时间">{{ detail.issueTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getAllInvoices,
  getInvoiceDetail,
  getMyInvoices,
  issueInvoice,
  resendInvoice
} from '@/api/financial'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('my')
const statusFilter = ref('')
const loading = ref(false)
const myRecords = ref([])
const adminRecords = ref([])
const detailVisible = ref(false)
const detail = ref(null)
const pagination = ref({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const isAdmin = computed(() => {
  const roles = userStore.userInfo?.roles || []
  return roles.includes('ROLE_ADMIN')
})

const currentRecords = computed(() => {
  return activeTab.value === 'admin' ? adminRecords.value : myRecords.value
})

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.getUserInfo()
  }
  await reloadCurrentTab()
})

const handleTabChange = async () => {
  pagination.value.pageNum = 1
  pagination.value.total = 0
  await reloadCurrentTab()
}

const handlePageSizeChange = async () => {
  pagination.value.pageNum = 1
  await reloadCurrentTab()
}

const reloadCurrentTab = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'admin' && isAdmin.value) {
      const res = await getAllInvoices(buildQuery())
      adminRecords.value = res.data.records || []
      pagination.value.total = res.data.total || 0
      return
    }

    const res = await getMyInvoices(buildQuery())
    myRecords.value = res.data.records || []
    pagination.value.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('发票列表加载失败')
  } finally {
    loading.value = false
  }
}

const handleViewDetail = async (invoiceId) => {
  try {
    const res = await getInvoiceDetail(invoiceId)
    detail.value = res.data
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('发票详情加载失败')
  }
}

const handleIssue = async (invoiceId) => {
  try {
    await ElMessageBox.confirm('确认开具这张发票吗？', '提示', {
      type: 'warning'
    })
    await issueInvoice(invoiceId)
    ElMessage.success('发票已开具')
    await reloadCurrentTab()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('开具发票失败')
    }
  }
}

const handleResend = async (invoiceId) => {
  try {
    await resendInvoice(invoiceId)
    ElMessage.success('发票已重新发送')
    await reloadCurrentTab()
  } catch (error) {
    ElMessage.error('重发发票失败')
  }
}

const buildQuery = () => {
  const query = {
    pageNum: pagination.value.pageNum,
    pageSize: pagination.value.pageSize
  }

  if (statusFilter.value) {
    query.status = statusFilter.value
  }

  return query
}

const getStatusTagType = (status) => {
  const map = {
    1: 'warning',
    2: 'success',
    3: 'info'
  }
  return map[status] || 'info'
}

const formatStatusText = (statusText) => {
  const map = {
    PENDING: '待开具',
    ISSUED: '已开具',
    VOID: '已作废'
  }
  return map[statusText] || statusText || '-'
}

const formatInvoiceType = (invoiceType) => {
  const map = {
    1: '普通发票',
    2: '增值税发票'
  }
  return map[invoiceType] || invoiceType || '-'
}

const formatTitleType = (titleType) => {
  const map = {
    1: '个人',
    2: '企业'
  }
  return map[titleType] || titleType || '-'
}
</script>

<style scoped>
.invoice-toolbar {
  margin-bottom: var(--spacing-4);
}

@media (max-width: 768px) {
  .invoice-toolbar :deep(.el-select) {
    width: 100%;
  }
}
</style>
