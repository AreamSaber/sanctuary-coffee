<template>
  <div class="app-page delivery-task-page">
    <section class="app-page-header">
      <div>
        <h1 class="app-page-header__title">{{ pageTitle }}</h1>
        <p class="compact-note">{{ pageDescription }}</p>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/home')">返回首页</el-button>
        <el-button type="primary" :loading="loading" @click="loadTasks">刷新任务</el-button>
      </div>
    </section>

    <el-card class="app-panel-card">
      <template #header>
        <div class="card-header">
          <strong>{{ panelTitle }}</strong>
          <el-tag effect="plain">{{ filteredTaskList.length }} 条</el-tag>
        </div>
      </template>

      <div v-loading="loading">
        <el-tabs v-model="activeTab" class="delivery-task-tabs">
          <el-tab-pane
            v-for="tab in taskTabs"
            :key="tab.name"
            :label="`${tab.label} (${tab.count})`"
            :name="tab.name"
          />
        </el-tabs>

        <el-empty v-if="filteredTaskList.length === 0" :description="emptyDescription">
          <el-button type="primary" @click="router.push('/home')">返回工作台</el-button>
        </el-empty>

        <div v-else class="delivery-task-grid">
          <article v-for="task in filteredTaskList" :key="task.id" class="delivery-task-card">
            <header class="delivery-task-card__header">
              <div class="delivery-task-card__main">
                <strong>{{ task.orderNo || `订单 #${task.orderId}` }}</strong>
                <span>配送单号：{{ task.deliveryNo || '-' }}</span>
              </div>
              <el-tag :type="getStatusType(task.deliveryStatus)">
                {{ task.deliveryStatusText || '未知状态' }}
              </el-tag>
            </header>

            <div class="delivery-task-card__meta">
              <div class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">收货人</span>
                <strong>{{ task.receiverName || '-' }}</strong>
              </div>
              <div class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">联系电话</span>
                <strong>{{ task.receiverPhone || '-' }}</strong>
              </div>
              <div class="delivery-task-card__meta-item delivery-task-card__meta-item--wide">
                <span class="delivery-task-card__label">收货地址</span>
                <strong>{{ task.receiverAddress || '-' }}</strong>
              </div>
              <div v-if="showDeliverymanInfo" class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">配送员</span>
                <strong>{{ task.deliverymanName || '待分配' }}</strong>
              </div>
              <div v-if="showDeliverymanInfo" class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">骑手电话</span>
                <strong>{{ task.deliverymanPhone || '-' }}</strong>
              </div>
              <div class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">分配时间</span>
                <strong>{{ formatDateTime(task.assignTime) }}</strong>
              </div>
              <div class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">接单时间</span>
                <strong>{{ formatDateTime(task.acceptTime) }}</strong>
              </div>
              <div class="delivery-task-card__meta-item">
                <span class="delivery-task-card__label">取货时间</span>
                <strong>{{ formatDateTime(task.pickupTime) }}</strong>
              </div>
            </div>

            <div class="delivery-task-card__actions">
              <el-button size="small" @click="handleViewTracking(task)">查看轨迹</el-button>
              <el-button
                v-if="canReportException(task)"
                size="small"
                type="danger"
                :loading="actionLoadingId === task.id"
                @click="openExceptionDialog(task)"
              >
                上报异常
              </el-button>
              <el-button
                v-if="canAssignTask(task)"
                size="small"
                type="primary"
                :loading="actionLoadingId === task.id"
                @click="handleAssignTask(task)"
              >
                自动分配
              </el-button>
              <el-button
                v-if="canAcceptTask(task)"
                size="small"
                type="primary"
                :loading="actionLoadingId === task.id"
                @click="handleAcceptTask(task)"
              >
                确认接单
              </el-button>
              <el-button
                v-if="canStartTask(task)"
                size="small"
                type="primary"
                :loading="actionLoadingId === task.id"
                @click="handleStartTask(task)"
              >
                开始配送
              </el-button>
              <el-button
                v-if="canCompleteTask(task)"
                size="small"
                type="success"
                :loading="actionLoadingId === task.id"
                @click="handleCompleteTask(task)"
              >
                确认送达
              </el-button>
            </div>
          </article>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="exceptionDialogVisible" title="上报配送异常" width="480px">
      <el-form :model="exceptionForm" label-width="80px">
        <el-form-item label="异常类型">
          <el-select v-model="exceptionForm.exceptionType" placeholder="请选择异常类型" style="width: 100%">
            <el-option label="配送超时" :value="1" />
            <el-option label="地址错误" :value="2" />
            <el-option label="联系不上" :value="3" />
            <el-option label="商品损坏" :value="4" />
            <el-option label="其他" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常描述">
          <el-input v-model="exceptionForm.exceptionDesc" type="textarea" :rows="3" placeholder="请描述异常情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exceptionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="exceptionSubmitting" @click="submitException">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { acceptDelivery, assignDelivery, completeDelivery, getDeliveryTasks, reportDeliveryException, startDelivery } from '@/api/delivery'
import { useUserStore } from '@/stores/user'
import { ROLE_ADMIN, ROLE_DELIVERY } from '@/utils/permission'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const actionLoadingId = ref(null)
const taskList = ref([])
const activeTab = ref('all')
const exceptionDialogVisible = ref(false)
const exceptionSubmitting = ref(false)
const exceptionForm = ref({ exceptionType: null, exceptionDesc: '' })
const exceptionTaskId = ref(null)

const userRoles = computed(() => userStore.roles || [])
const isAdmin = computed(() => userRoles.value.includes(ROLE_ADMIN))
const isDelivery = computed(() => userRoles.value.includes(ROLE_DELIVERY))
const showDeliverymanInfo = computed(() => isAdmin.value)
const pageTitle = computed(() => (isAdmin.value ? '配送履约任务' : '我的配送任务'))
const pageDescription = computed(() => (
  isAdmin.value
    ? '集中处理待分配、待接单、待取货、配送中和历史履约任务。'
    : '集中处理待接单、待取货、配送中和已送达历史任务。'
))
const panelTitle = computed(() => (
  activeTab.value === 'delivered'
    ? '已送达任务'
    : '配送任务面板'
))
const taskTabs = computed(() => {
  const tabs = isAdmin.value
    ? [
        { name: 'all', label: '全部任务' },
        { name: 'pendingAssign', label: '待分配' },
        { name: 'waitAccept', label: '待接单' },
        { name: 'waitPickup', label: '待取货' },
        { name: 'delivering', label: '配送中' },
        { name: 'delivered', label: '已送达' }
      ]
    : [
        { name: 'all', label: '全部任务' },
        { name: 'waitAccept', label: '待接单' },
        { name: 'waitPickup', label: '待取货' },
        { name: 'delivering', label: '配送中' },
        { name: 'delivered', label: '已送达' }
      ]

  return tabs.map((tab) => ({
    ...tab,
    count: countTasksByTab(tab.name)
  }))
})
const filteredTaskList = computed(() => taskList.value.filter((task) => matchesTab(task, activeTab.value)))
const emptyDescription = computed(() => (
  resolveEmptyDescription(activeTab.value)
))

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await getDeliveryTasks()
    taskList.value = res.data || []
  } catch (error) {
    taskList.value = []
    ElMessage.error(error.message || '配送任务加载失败')
  } finally {
    loading.value = false
  }
}

const matchesTab = (task, tabName) => {
  const status = Number(task.deliveryStatus)
  const isAccepted = Boolean(task.acceptTime)

  switch (tabName) {
    case 'pendingAssign':
      return status === 1
    case 'waitAccept':
      return status === 2 && !isAccepted
    case 'waitPickup':
      return status === 2 && isAccepted
    case 'delivering':
      return status === 3
    case 'delivered':
      return status === 4
    case 'all':
    default:
      return true
  }
}

const countTasksByTab = (tabName) => taskList.value.filter((task) => matchesTab(task, tabName)).length

const resolveEmptyDescription = (tabName) => {
  const adminCopy = {
    all: '当前没有可查看的配送任务',
    pendingAssign: '当前没有待分配的配送单',
    waitAccept: '当前没有等待配送员接单的任务',
    waitPickup: '当前没有待取货任务',
    delivering: '当前没有配送中的任务',
    delivered: '当前没有已送达任务'
  }
  const deliveryCopy = {
    all: '当前没有分配给你的配送任务',
    waitAccept: '当前没有待你接单的任务',
    waitPickup: '当前没有待你取货的任务',
    delivering: '当前没有配送中的任务',
    delivered: '当前还没有已送达历史任务'
  }

  const copyMap = isAdmin.value ? adminCopy : deliveryCopy
  return copyMap[tabName] || copyMap.all
}

const runTaskAction = async (task, action, successMessage) => {
  actionLoadingId.value = task.id
  try {
    await action(task.orderId)
    ElMessage.success(successMessage)
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '配送操作失败')
  } finally {
    actionLoadingId.value = null
  }
}

const handleViewTracking = (task) => {
  router.push(`/delivery/tracking?orderId=${task.orderId}`)
}

const handleAssignTask = async (task) => {
  await runTaskAction(task, assignDelivery, '配送员已自动分配')
}

const handleAcceptTask = async (task) => {
  await runTaskAction(task, acceptDelivery, '配送员已接单')
}

const handleStartTask = async (task) => {
  await runTaskAction(task, startDelivery, '配送已开始')
}

const handleCompleteTask = async (task) => {
  await runTaskAction(task, completeDelivery, '订单已送达')
}

const canAssignTask = (task) => (
  isAdmin.value
  && Number(task.deliveryStatus) === 1
)

const canAcceptTask = (task) => (
  isDelivery.value
  && Number(task.deliveryStatus) === 2
  && !task.acceptTime
)

const canStartTask = (task) => (
  (isAdmin.value || isDelivery.value)
  && Number(task.deliveryStatus) === 2
  && Boolean(task.acceptTime)
)

const canCompleteTask = (task) => (
  (isAdmin.value || isDelivery.value)
  && Number(task.deliveryStatus) === 3
)

const canReportException = (task) => {
  const status = Number(task.deliveryStatus)
  return (isAdmin.value || isDelivery.value) && (status === 2 || status === 3) && !task.hasException
}

const openExceptionDialog = (task) => {
  exceptionTaskId.value = task.id
  exceptionForm.value = { exceptionType: null, exceptionDesc: '' }
  exceptionDialogVisible.value = true
}

const submitException = async () => {
  if (!exceptionForm.value.exceptionType) {
    ElMessage.warning('请选择异常类型')
    return
  }
  exceptionSubmitting.value = true
  try {
    await reportDeliveryException(exceptionTaskId.value, exceptionForm.value.exceptionType, exceptionForm.value.exceptionDesc)
    ElMessage.success('异常上报成功')
    exceptionDialogVisible.value = false
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '异常上报失败')
  } finally {
    exceptionSubmitting.value = false
  }
}

const getStatusType = (status) => {
  const map = {
    1: 'info',
    2: 'warning',
    3: 'primary',
    4: 'success',
    5: 'danger'
  }
  return map[Number(status)] || 'info'
}

const formatDateTime = (value) => value || '-'

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.delivery-task-tabs {
  margin-bottom: var(--spacing-5);
}

.delivery-task-grid {
  display: grid;
  gap: var(--spacing-5);
}

.delivery-task-card {
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.92);
  padding: clamp(18px, 2vw, 24px);
  box-shadow: 0 18px 40px rgba(25, 34, 42, 0.08);
}

.delivery-task-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-4);
}

.delivery-task-card__main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.delivery-task-card__main strong {
  font-size: var(--text-lg);
}

.delivery-task-card__main span {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.delivery-task-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--spacing-4);
  margin-top: var(--spacing-5);
}

.delivery-task-card__meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.delivery-task-card__meta-item--wide {
  grid-column: 1 / -1;
}

.delivery-task-card__label {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.delivery-task-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
  margin-top: var(--spacing-5);
}

@media (max-width: 768px) {
  .delivery-task-card__header {
    flex-direction: column;
  }

  .delivery-task-card__meta {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
