<template>
  <div class="app-page delivery-tracking-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">物流追踪</h1>
      </div>
      <div class="app-page-actions">
        <el-tag v-if="hasOrderId" effect="plain">订单ID：{{ orderId }}</el-tag>
        <el-button @click="goBack">{{ backButtonLabel }}</el-button>
      </div>
    </section>

    <section v-if="showEmptyState" class="page-grid page-grid--content">
      <el-card class="stack-card" shadow="never">
        <div class="stack-card__body">
          <el-empty :description="emptyDescription">
            <div class="delivery-tracking-page__empty-actions">
              <el-button
                v-if="canManageOrderDelivery && hasOrderId"
                type="primary"
                :loading="deliveryActionLoading"
                @click="handleAssignDelivery"
              >
                创建并分配配送
              </el-button>
              <el-button type="primary" @click="goBack">{{ backActionLabel }}</el-button>
              <el-button @click="router.push('/home')">返回首页</el-button>
            </div>
          </el-empty>
        </div>
      </el-card>
    </section>

    <section v-else class="page-grid page-grid--content">
      <el-card v-if="hasActiveDelivery" class="stack-card" shadow="never">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">实时位置</h2>
            <p class="compact-note">{{ position?.label || '获取位置中...' }}</p>
          </div>
        </div>
        <div class="stack-card__body">
          <div ref="mapContainer" class="delivery-map"></div>
        </div>
      </el-card>

      <el-card class="stack-card" shadow="never" v-loading="loading">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">配送轨迹</h2>
            <p class="compact-note">最新节点会显示在最上方。</p>
          </div>
        </div>

        <div class="stack-card__body">
          <el-timeline v-if="trackingList.length > 0">
            <el-timeline-item
              v-for="(track, index) in trackingList"
              :key="track.id"
              :timestamp="track.createTime"
              :type="index === 0 ? 'primary' : 'info'"
              :size="index === 0 ? 'large' : 'normal'"
            >
              <div class="track-content">
                <strong>{{ track.trackDesc }}</strong>
                <div v-if="track.location" class="track-location">
                  <el-icon><Location /></el-icon>
                  <span>{{ track.location }}</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>

          <el-empty v-else description="暂无物流信息" />

          <div v-if="showDeliveryActions" class="delivery-actions">
            <p class="compact-note">{{ deliveryActionTip }}</p>
            <el-button
              v-if="canAssignDeliveryAction"
              type="primary"
              :loading="deliveryActionLoading"
              @click="handleAssignDelivery"
            >
              分配配送员
            </el-button>
            <el-button
              v-if="canAcceptDeliveryAction"
              type="primary"
              :loading="deliveryActionLoading"
              @click="handleAcceptDelivery"
            >
              确认接单
            </el-button>
            <el-button
              v-if="canStartDeliveryAction"
              type="primary"
              :loading="deliveryActionLoading"
              @click="handleStartDelivery"
            >
              开始配送
            </el-button>
            <el-button
              v-if="canCompleteDeliveryAction"
              type="success"
              :loading="deliveryActionLoading"
              @click="handleCompleteDelivery"
            >
              确认送达
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card class="stack-card" shadow="never">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">配送信息</h2>
          </div>
        </div>
        <div class="stack-card__body">
          <el-descriptions v-if="deliveryInfo" :column="1" border>
            <el-descriptions-item label="配送单号">{{ deliveryInfo.deliveryNo }}</el-descriptions-item>
            <el-descriptions-item label="订单号">{{ deliveryInfo.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="配送状态">
              <el-tag :type="getStatusType(deliveryInfo.deliveryStatus)">
                {{ deliveryInfo.deliveryStatusText }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ deliveryInfo.createTime }}</el-descriptions-item>
            <el-descriptions-item label="收货人">{{ deliveryInfo.receiverName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ deliveryInfo.receiverPhone }}</el-descriptions-item>
            <el-descriptions-item label="收货地址">{{ deliveryInfo.receiverAddress }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="该订单暂未生成配送单" />
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { acceptDelivery, assignDelivery, completeDelivery, getDeliveryDetail, getDeliveryTracking, startDelivery } from '@/api/delivery'
import { useUserStore } from '@/stores/user'
import { ROLE_ADMIN, ROLE_DELIVERY } from '@/utils/permission'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const deliveryActionLoading = ref(false)
const orderId = ref(null)
const deliveryInfo = ref(null)
const trackingList = ref([])
const loadFailed = ref(false)
const position = ref(null)
const mapContainer = ref(null)
let mapInstance = null
let positionTimer = null
let mapMarkers = []
const isAdmin = computed(() => (userStore.roles || []).includes(ROLE_ADMIN))
const isDelivery = computed(() => (userStore.roles || []).includes(ROLE_DELIVERY))
const canManageOrderDelivery = computed(() => isAdmin.value)
const deliveryStatus = computed(() => Number(deliveryInfo.value?.deliveryStatus || 0))
const isAccepted = computed(() => Boolean(deliveryInfo.value?.acceptTime))
const canAssignDeliveryAction = computed(() => (
  isAdmin.value
  && Boolean(deliveryInfo.value)
  && deliveryStatus.value === 1
))
const canAcceptDeliveryAction = computed(() => (
  isDelivery.value
  && Boolean(deliveryInfo.value)
  && deliveryStatus.value === 2
  && !isAccepted.value
))
const canStartDeliveryAction = computed(() => (
  (isAdmin.value || isDelivery.value)
  && Boolean(deliveryInfo.value)
  && deliveryStatus.value === 2
  && isAccepted.value
))
const canCompleteDeliveryAction = computed(() => (
  (isAdmin.value || isDelivery.value)
  && Boolean(deliveryInfo.value)
  && deliveryStatus.value === 3
))
const showDeliveryActions = computed(() => (
  canAssignDeliveryAction.value
  || canAcceptDeliveryAction.value
  || canStartDeliveryAction.value
  || canCompleteDeliveryAction.value
))
const hasOrderId = computed(() => Number.isFinite(orderId.value) && orderId.value > 0)
const hasDeliveryData = computed(() => Boolean(deliveryInfo.value) || trackingList.value.length > 0)
const backButtonLabel = computed(() => (isDelivery.value && !isAdmin.value ? '返回任务' : '返回订单'))
const backActionLabel = computed(() => (isDelivery.value && !isAdmin.value ? '回任务列表' : '去订单中心'))

const showEmptyState = computed(() => {
  if (!hasOrderId.value) {
    return true
  }
  if (loading.value) {
    return false
  }
  return loadFailed.value || !hasDeliveryData.value
})

const emptyDescription = computed(() => {
  if (!hasOrderId.value) {
    if (isDelivery.value) {
      return '请先从配送任务列表中选择具体订单'
    }
    return '请先从订单中选择要查看的物流'
  }
  if (loadFailed.value) {
    if (isDelivery.value) {
      return '当前订单暂时不可查看，可能还未分配给你或已被其他配送员处理'
    }
    return '当前订单暂时无法查看物流；如订单已支付，管理员可尝试创建并分配配送'
  }
  return '该订单还没有生成物流信息'
})

const deliveryActionTip = computed(() => {
  if (deliveryStatus.value === 1) {
    return isAdmin.value ? '配送单已创建，下一步需要分配可用配送员。' : '配送单尚未分配，请等待调度。'
  }
  if (deliveryStatus.value === 2 && !isAccepted.value) {
    return isAdmin.value ? '配送员已分配，等待其确认接单。' : '任务已分配给你，请先确认接单。'
  }
  if (deliveryStatus.value === 2) {
    return '配送员已接单，下一步确认取货并开始配送。'
  }
  if (deliveryStatus.value === 3) {
    return '订单正在配送中，送达后请确认完成。'
  }
  return ''
})

const resetPageState = () => {
  orderId.value = null
  deliveryInfo.value = null
  trackingList.value = []
  loadFailed.value = false
  loading.value = false
  deliveryActionLoading.value = false
}

const initializePage = async (rawOrderId) => {
  resetPageState()

  const parsedOrderId = Number(rawOrderId)
  if (!Number.isFinite(parsedOrderId) || parsedOrderId <= 0) {
    return
  }

  orderId.value = parsedOrderId
  await loadData()
  await nextTick()
  if (hasActiveDelivery.value && mapContainer.value) {
    initMap()
    fetchPosition()
  }
  startPositionPolling()
}

const loadData = async () => {
  loading.value = true
  loadFailed.value = false
  try {
    const detailRes = await getDeliveryDetail(orderId.value)
    deliveryInfo.value = detailRes.data || null

    const trackingRes = await getDeliveryTracking(orderId.value)
    trackingList.value = (trackingRes.data || []).reverse()
  } catch (error) {
    deliveryInfo.value = null
    trackingList.value = []
    loadFailed.value = true
    ElMessage.error(error.message || '加载配送信息失败')
  } finally {
    loading.value = false
  }
}

const runDeliveryAction = async (action, successMessage) => {
  if (!hasOrderId.value) {
    return
  }

  deliveryActionLoading.value = true
  try {
    await action(orderId.value)
    ElMessage.success(successMessage)
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '配送操作失败')
  } finally {
    deliveryActionLoading.value = false
  }
}

const handleAssignDelivery = async () => {
  await runDeliveryAction(assignDelivery, '配送员已分配')
}

const handleAcceptDelivery = async () => {
  await runDeliveryAction(acceptDelivery, '配送员已接单')
}

const handleStartDelivery = async () => {
  await runDeliveryAction(startDelivery, '配送已开始')
}

const handleCompleteDelivery = async () => {
  await runDeliveryAction(completeDelivery, '订单已送达')
}

const getStatusType = (status) => {
  const typeMap = {
    1: 'info',
    2: 'warning',
    3: 'primary',
    4: 'success',
    5: 'danger'
  }
  return typeMap[status] || 'info'
}

const goBack = () => {
  router.push(isDelivery.value && !isAdmin.value ? '/delivery/tasks' : '/order')
}

const hasActiveDelivery = computed(() => {
  if (!deliveryInfo.value) return false
  const s = deliveryInfo.value.deliveryStatus
  return s === 2 || s === 3
})

const initMap = () => {
  if (!mapContainer.value || mapInstance) return
  mapInstance = L.map(mapContainer.value, { zoomControl: true }).setView([39.916527, 116.397128], 13)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 19
  }).addTo(mapInstance)
}

const clearMarkers = () => {
  mapMarkers.forEach(m => mapInstance?.removeLayer(m))
  mapMarkers = []
}

const renderPosition = (pos) => {
  if (!mapInstance || !pos) return
  clearMarkers()

  const pLng = pos.pickupLongitude || pos.destLongitude
  const pLat = pos.pickupLatitude || pos.destLatitude
  if (pLng && pLat) {
    const pickupIcon = L.divIcon({ className: 'map-marker-pickup', html: '<div style="background:#409EFF;width:14px;height:14px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>', iconSize: [20, 20], iconAnchor: [10, 10] })
    const m = L.marker([pLat, pLng], { icon: pickupIcon }).bindPopup('取货点').addTo(mapInstance)
    mapMarkers.push(m)
  }

  const dLng = pos.destLongitude
  const dLat = pos.destLatitude
  if (dLng && dLat) {
    const destIcon = L.divIcon({ className: 'map-marker-dest', html: '<div style="background:#F56C6C;width:14px;height:14px;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>', iconSize: [20, 20], iconAnchor: [10, 10] })
    const m = L.marker([dLat, dLng], { icon: destIcon }).bindPopup('收货地址').addTo(mapInstance)
    mapMarkers.push(m)
  }

  const cLng = pos.longitude
  const cLat = pos.latitude
  if (cLng && cLat) {
    const currentIcon = L.divIcon({ className: 'map-marker-current', html: '<div style="background:#67C23A;width:18px;height:18px;border-radius:50%;border:3px solid #fff;box-shadow:0 3px 10px rgba(103,194,58,0.5);animation:pulse 1.5s infinite"></div>', iconSize: [24, 24], iconAnchor: [12, 12] })
    const m = L.marker([cLat, cLng], { icon: currentIcon }).bindPopup(pos.label || '当前位置').addTo(mapInstance)
    mapMarkers.push(m)
  }

  if (pLng && pLat && dLng && dLat) {
    const line = L.polyline([[pLat, pLng], [dLat, dLng]], { color: '#909399', weight: 2, dashArray: '6,6' }).addTo(mapInstance)
    mapMarkers.push(line)
  }

  const bounds = []
  if (pLat && pLng) bounds.push([pLat, pLng])
  if (dLat && dLng) bounds.push([dLat, dLng])
  if (cLat && cLng) bounds.push([cLat, cLng])
  if (bounds.length > 0) {
    mapInstance.fitBounds(bounds, { padding: [40, 40] })
  }
}

const fetchPosition = async () => {
  if (!orderId.value) return
  try {
    const res = await request.get('/delivery/position/' + orderId.value)
    position.value = res.data
    await nextTick()
    if (mapContainer.value) {
      if (!mapInstance) initMap()
      renderPosition(res.data)
    }
  } catch { /* polling, ignore errors */ }
}

const startPositionPolling = () => {
  stopPositionPolling()
  if (hasActiveDelivery.value) {
    fetchPosition()
    positionTimer = setInterval(fetchPosition, 5000)
  }
}

const stopPositionPolling = () => {
  if (positionTimer) {
    clearInterval(positionTimer)
    positionTimer = null
  }
}

const destroyMap = () => {
  stopPositionPolling()
  clearMarkers()
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
}

onMounted(() => {
  nextTick(() => {
    if (hasActiveDelivery.value) {
      initMap()
      fetchPosition()
    }
  })
})

onBeforeUnmount(() => {
  destroyMap()
})

watch(
  () => route.query.orderId,
  async (value) => {
    await initializePage(value)
  },
  { immediate: true }
)
</script>

<style scoped>
.track-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.track-location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.delivery-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
}

.delivery-actions .compact-note {
  width: 100%;
}

.delivery-tracking-page__empty-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--spacing-3);
}

.delivery-map {
  width: 100%;
  height: 360px;
  border-radius: 16px;
  overflow: hidden;
  background: #e9e7e4;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.8); opacity: 0.5; }
}
</style>
