<template>
  <div class="app-page admin-workbench-page">
    <section class="workbench-hero">
      <div class="workbench-hero__content">
        <p class="section-eyebrow">运营中枢</p>
        <h1 class="app-page-header__title">门店后台工作台</h1>
        <p class="section-caption">
          把订单、库存、会员、营销和财务收在同一个入口里，先看需要处理的事项，再进入对应页面完成操作。
        </p>
      </div>

      <div class="app-page-actions workbench-hero__actions">
        <el-button :icon="Setting" plain @click="router.push('/admin/rbac')">角色与权限</el-button>
        <el-button :icon="Histogram" plain @click="router.push('/statistics')">运营看板</el-button>
        <el-button :icon="DataAnalysis" type="primary" @click="router.push('/analytics')">用户分析</el-button>
      </div>
    </section>

    <section class="metric-grid" aria-label="经营指标">
      <button
        v-for="card in metricCards"
        :key="card.key"
        class="metric-card"
        :class="`metric-card--${card.tone}`"
        type="button"
        @click="card.route && router.push(card.route)"
      >
        <span class="metric-card__icon">
          <el-icon><component :is="card.icon" /></el-icon>
        </span>
        <span class="metric-card__label">{{ card.label }}</span>
        <strong class="metric-card__value">{{ card.value }}</strong>
        <span class="metric-card__meta">{{ card.meta }}</span>
      </button>
    </section>

    <section class="workbench-layout">
      <div class="workbench-layout__main">
        <section class="ops-panel">
          <div class="ops-panel__head">
            <div>
              <p class="section-kicker">今日优先处理</p>
              <h2>把注意力放在会影响经营的事项上</h2>
            </div>
            <el-button :icon="Refresh" text type="primary" :loading="loading" @click="loadWorkbench">刷新数据</el-button>
          </div>

          <div class="action-list">
            <button
              v-for="item in actionItems"
              :key="item.key"
              class="action-item"
              :class="`action-item--${item.tone}`"
              type="button"
              @click="router.push(item.route)"
            >
              <span class="action-item__icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="action-item__body">
                <strong>{{ item.title }}</strong>
                <small>{{ item.description }}</small>
              </span>
              <span class="action-item__value">{{ item.value }}</span>
              <el-icon class="action-item__arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </section>

        <section class="ops-panel">
          <div class="ops-panel__head">
            <div>
              <p class="section-kicker">后台导航</p>
              <h2>按业务场景进入管理页面</h2>
            </div>
            <span class="compact-note">已接通 {{ connectedRouteCount }} 个入口</span>
          </div>

          <div class="module-section-list">
            <article v-for="group in moduleSections" :key="group.key" class="module-section">
              <div class="module-section__head">
                <span class="module-section__icon">
                  <el-icon><component :is="group.icon" /></el-icon>
                </span>
                <div>
                  <h3>{{ group.title }}</h3>
                  <p>{{ group.description }}</p>
                </div>
                <span class="module-section__count">{{ group.connectedCount }}/{{ group.items.length }}</span>
              </div>

              <div class="module-grid">
                <button
                  v-for="item in group.items"
                  :key="item.code"
                  class="module-card"
                  type="button"
                  :disabled="!item.path"
                  @click="item.path && router.push(item.path)"
                >
                  <span>
                    <strong>{{ item.name }}</strong>
                    <small>{{ item.description }}</small>
                  </span>
                  <el-tag :type="item.path ? 'success' : 'warning'" size="small" effect="plain">
                    {{ item.path ? '可进入' : '待接通' }}
                  </el-tag>
                </button>
              </div>
            </article>
          </div>
        </section>

        <section class="workbench-tables">
          <div class="ops-panel ops-panel--table">
            <div class="ops-panel__head">
              <div>
                <p class="section-kicker">库存预警</p>
                <h2>低库存商品</h2>
              </div>
              <el-button text type="primary" @click="router.push('/product/stock-warning')">去补货</el-button>
            </div>

            <el-table :data="lowStockList" size="small" empty-text="暂无低库存数据">
              <el-table-column prop="productName" label="商品" min-width="150" />
              <el-table-column prop="categoryName" label="分类" width="110" />
              <el-table-column prop="stock" label="库存" width="80" align="center" />
              <el-table-column prop="salesCount" label="销量" width="80" align="center" />
            </el-table>
          </div>

          <div class="ops-panel ops-panel--table">
            <div class="ops-panel__head">
              <div>
                <p class="section-kicker">财务协同</p>
                <h2>待开票申请</h2>
              </div>
              <el-button text type="primary" @click="router.push('/financial/invoice')">发票中心</el-button>
            </div>

            <el-table :data="pendingInvoices" size="small" empty-text="暂无待开票数据">
              <el-table-column prop="invoiceNo" label="发票号" min-width="150" />
              <el-table-column prop="title" label="抬头" min-width="120" />
              <el-table-column prop="amount" label="金额" width="90">
                <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="createTime" label="申请时间" width="170" />
            </el-table>
          </div>
        </section>
      </div>

      <aside class="workbench-layout__side">
        <section class="ops-panel operator-panel">
          <div class="operator-panel__avatar">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div>
            <p class="section-kicker">当前登录</p>
            <h2>{{ operatorName }}</h2>
            <div class="operator-panel__roles">
              <el-tag v-for="role in roleTags" :key="role" type="info" effect="plain">{{ role }}</el-tag>
            </div>
          </div>
        </section>

        <section class="ops-panel">
          <div class="ops-panel__head ops-panel__head--compact">
            <div>
              <p class="section-kicker">系统可用性</p>
              <h2>后台入口状态</h2>
            </div>
          </div>

          <div class="readiness-list">
            <div v-for="item in readinessItems" :key="item.label" class="readiness-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section class="ops-panel">
          <div class="ops-panel__head ops-panel__head--compact">
            <div>
              <p class="section-kicker">快速入口</p>
              <h2>常用后台动作</h2>
            </div>
          </div>

          <div class="quick-links">
            <button v-for="link in quickLinks" :key="link.path" type="button" @click="router.push(link.path)">
              <el-icon><component :is="link.icon" /></el-icon>
              <span>{{ link.label }}</span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Bell,
  Collection,
  DataAnalysis,
  Finished,
  Goods,
  Histogram,
  Lock,
  Refresh,
  Setting,
  Tickets,
  User,
  UserFilled,
  Van,
  Wallet,
  Warning
} from '@element-plus/icons-vue'
import { getRealTimeStats } from '@/api/analytics'
import { getDeliveryMethods } from '@/api/delivery'
import { getAllInvoices } from '@/api/financial'
import { getMemberList, getCouponPage } from '@/api/member'
import { getPromotionPage } from '@/api/promotion'
import { getLowStockProducts, getSalesStatistics, getUserStatistics } from '@/api/statistics'
import { useUserStore } from '@/stores/user'
import {
  ADMIN_PERMISSION_GROUPS,
  ADMIN_ROUTE_REGISTRY,
  getRolePermissionCodes,
  resolveRoleLabel
} from '@/utils/permission'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const salesStats = ref({})
const userStats = ref({})
const realtimeStats = ref({})
const lowStockList = ref([])
const pendingInvoices = ref([])
const pendingInvoiceTotal = ref(0)
const memberTotal = ref(0)
const couponTotal = ref(0)
const promotionTotal = ref(0)
const deliveryMethodCount = ref(0)

const groupIconMap = {
  governance: Lock,
  customer: User,
  catalog: Goods,
  trade: Wallet,
  delivery: Van,
  marketing: Tickets
}

const currentPermissionCodes = computed(() => (
  userStore.userInfo?.permissionCodes?.length
    ? userStore.userInfo.permissionCodes
    : getRolePermissionCodes(userStore.roles)
))

const roleTags = computed(() => (userStore.roles || []).map(resolveRoleLabel))

const operatorName = computed(() => (
  userStore.userInfo?.nickname || userStore.userInfo?.username || '后台管理员'
))

const metricCards = computed(() => [
  {
    key: 'sales',
    label: '累计销售额',
    value: `¥${formatAmount(salesStats.value.totalSales)}`,
    meta: `今日销售 ¥${formatAmount(salesStats.value.todaySales)}`,
    route: '/statistics',
    icon: Wallet,
    tone: 'finance'
  },
  {
    key: 'orders',
    label: '累计订单',
    value: Number(salesStats.value.totalOrders || 0),
    meta: `今日订单 ${Number(salesStats.value.todayOrders || 0)} 单`,
    route: '/order/manage',
    icon: Collection,
    tone: 'trade'
  },
  {
    key: 'users',
    label: '会员总量',
    value: memberTotal.value || Number(userStats.value.totalUsers || 0),
    meta: `今日新增 ${Number(realtimeStats.value.todayNewUsers || 0)} 人`,
    route: '/member',
    icon: User,
    tone: 'customer'
  },
  {
    key: 'inventory',
    label: '低库存',
    value: lowStockList.value.length,
    meta: '影响可售商品与履约',
    route: '/product/stock-warning',
    icon: Warning,
    tone: 'risk'
  },
  {
    key: 'invoice',
    label: '待开票',
    value: pendingInvoiceTotal.value,
    meta: `促销 ${promotionTotal.value} / 券 ${couponTotal.value} / 配送 ${deliveryMethodCount.value}`,
    route: '/financial/invoice',
    icon: Tickets,
    tone: 'invoice'
  }
])

const routeRegistryMap = computed(() => new Map(
  ADMIN_ROUTE_REGISTRY.map((route) => [route.permission, route])
))

const moduleSections = computed(() => (
  ADMIN_PERMISSION_GROUPS.map((group) => {
    const items = group.permissions.map((permission) => ({
      ...permission,
      path: routeRegistryMap.value.get(permission.code)?.path || ''
    }))

    return {
      ...group,
      icon: groupIconMap[group.key] || Setting,
      items,
      connectedCount: items.filter((item) => item.path).length
    }
  })
))

const connectedRouteCount = computed(() => (
  ADMIN_ROUTE_REGISTRY.filter((item) => item.status === 'connected').length
))

const actionItems = computed(() => {
  const items = [
    {
      key: 'inventory',
      title: '检查低库存商品',
      description: '优先补齐会影响下单的商品库存。',
      value: `${lowStockList.value.length} 项`,
      route: '/product/stock-warning',
      icon: Warning,
      tone: lowStockList.value.length > 0 ? 'risk' : 'safe'
    },
    {
      key: 'invoice',
      title: '处理待开票申请',
      description: '减少财务等待，保持订单闭环清晰。',
      value: `${pendingInvoiceTotal.value} 单`,
      route: '/financial/invoice',
      icon: Tickets,
      tone: pendingInvoiceTotal.value > 0 ? 'warning' : 'safe'
    },
    {
      key: 'member',
      title: '查看会员权益',
      description: '确认等级权益、生日礼券和发放记录。',
      value: `${memberTotal.value || 0} 人`,
      route: '/member/benefits',
      icon: Finished,
      tone: 'info'
    }
  ]

  return items
})

const readinessItems = computed(() => [
  { label: '可进入后台页', value: connectedRouteCount.value },
  { label: '业务模块', value: moduleSections.value.length },
  { label: '当前权限', value: currentPermissionCodes.value.length },
  { label: '当前角色', value: roleTags.value.length || 0 }
])

const quickLinks = [
  { label: '订单管理', path: '/order/manage', icon: Collection },
  { label: '商品管理', path: '/product', icon: Goods },
  { label: '退款审核', path: '/payment/refund', icon: Wallet },
  { label: '评价管理', path: '/review/manage', icon: Bell },
  { label: '权限配置', path: '/admin/rbac', icon: Setting }
]

const loadWorkbench = async () => {
  loading.value = true
  const results = await Promise.allSettled([
    getSalesStatistics(),
    getUserStatistics(),
    getRealTimeStats(),
    getLowStockProducts(15),
    getAllInvoices({ pageNum: 1, pageSize: 5, status: 'PENDING' }),
    getMemberList({ pageNum: 1, pageSize: 1 }),
    getCouponPage({ pageNum: 1, pageSize: 1 }),
    getPromotionPage({ pageNum: 1, pageSize: 1 }),
    getDeliveryMethods()
  ])

  const [
    salesRes,
    usersRes,
    realtimeRes,
    lowStockRes,
    invoiceRes,
    memberRes,
    couponRes,
    promotionRes,
    deliveryMethodRes
  ] = results

  if (salesRes.status === 'fulfilled') {
    salesStats.value = salesRes.value?.data || {}
  }
  if (usersRes.status === 'fulfilled') {
    userStats.value = usersRes.value?.data || {}
  }
  if (realtimeRes.status === 'fulfilled') {
    realtimeStats.value = realtimeRes.value?.data || {}
  }
  if (lowStockRes.status === 'fulfilled') {
    lowStockList.value = (lowStockRes.value?.data || []).slice(0, 6)
  }
  if (invoiceRes.status === 'fulfilled') {
    pendingInvoices.value = invoiceRes.value?.data?.records || []
    pendingInvoiceTotal.value = Number(invoiceRes.value?.data?.total || 0)
  }
  if (memberRes.status === 'fulfilled') {
    memberTotal.value = Number(memberRes.value?.data?.total || 0)
  }
  if (couponRes.status === 'fulfilled') {
    couponTotal.value = Number(couponRes.value?.data?.total || 0)
  }
  if (promotionRes.status === 'fulfilled') {
    promotionTotal.value = Number(promotionRes.value?.data?.total || 0)
  }
  if (deliveryMethodRes.status === 'fulfilled') {
    deliveryMethodCount.value = Array.isArray(deliveryMethodRes.value?.data)
      ? deliveryMethodRes.value.data.length
      : 0
  }

  const failedCount = results.filter((item) => item.status === 'rejected').length
  if (failedCount > 0) {
    ElMessage.warning(`工作台 ${results.length - failedCount} 项数据已更新，${failedCount} 项暂不可用`)
  }
  loading.value = false
}

const formatAmount = (value) => Number(value || 0).toFixed(2)

onMounted(() => {
  loadWorkbench()
})
</script>

<style scoped>
.admin-workbench-page {
  --panel-bg: rgba(255, 250, 246, 0.94);
  --panel-border: rgba(107, 101, 91, 0.16);
  --panel-soft: rgba(255, 255, 255, 0.54);
}

.workbench-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
}

.workbench-hero__content {
  max-width: 760px;
}

.workbench-hero__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.section-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  color: var(--color-text-muted);
  font-size: 0.78rem;
  font-weight: var(--font-semibold);
  letter-spacing: 0;
}

.section-caption {
  max-width: 720px;
  margin: 12px 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
}

.metric-card,
.action-item,
.module-card,
.quick-links button {
  cursor: pointer;
}

.metric-card {
  display: grid;
  gap: 8px;
  min-height: 148px;
  padding: 18px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
  color: var(--color-text);
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.metric-card:hover,
.module-card:not(:disabled):hover,
.quick-links button:hover,
.action-item:hover {
  border-color: rgba(107, 101, 91, 0.34);
  transform: translateY(-1px);
}

.metric-card__icon,
.action-item__icon,
.module-section__icon,
.operator-panel__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--color-info-light);
  color: var(--color-info);
}

.metric-card--finance .metric-card__icon,
.action-item--info .action-item__icon {
  background: rgba(101, 125, 147, 0.14);
  color: var(--color-info);
}

.metric-card--trade .metric-card__icon,
.metric-card--customer .metric-card__icon,
.action-item--safe .action-item__icon {
  background: var(--color-success-light);
  color: var(--color-success);
}

.metric-card--risk .metric-card__icon,
.action-item--risk .action-item__icon {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

.metric-card--invoice .metric-card__icon,
.action-item--warning .action-item__icon {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.metric-card__label {
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.metric-card__value {
  font-size: 1.85rem;
  line-height: 1.1;
}

.metric-card__meta {
  color: var(--color-text-muted);
  line-height: 1.5;
}

.workbench-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.workbench-layout__main,
.workbench-layout__side,
.module-section-list {
  display: grid;
  gap: 18px;
}

.ops-panel {
  padding: 22px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
}

.ops-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.ops-panel__head--compact {
  margin-bottom: 14px;
}

.ops-panel h2,
.module-section h3 {
  margin: 0;
}

.compact-note,
.module-section p,
.module-card small,
.action-item small {
  color: var(--color-text-muted);
  line-height: 1.6;
}

.action-list {
  display: grid;
  gap: 10px;
}

.action-item {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
  color: var(--color-text);
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.action-item__body {
  display: grid;
  gap: 3px;
}

.action-item__value {
  color: var(--color-text-secondary);
  font-weight: var(--font-semibold);
  white-space: nowrap;
}

.action-item__arrow {
  color: var(--color-text-muted);
}

.module-section {
  display: grid;
  gap: 14px;
}

.module-section__head {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: start;
  gap: 14px;
}

.module-section__count {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--color-success-light);
  color: var(--color-success);
  font-weight: var(--font-semibold);
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 10px;
}

.module-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 96px;
  padding: 14px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
  color: var(--color-text);
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.module-card:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.module-card span:first-child {
  display: grid;
  gap: 6px;
}

.workbench-tables {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.ops-panel--table {
  min-width: 0;
}

.operator-panel {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
}

.operator-panel__avatar {
  width: 52px;
  height: 52px;
  background: var(--color-primary-50);
  color: var(--color-primary-dark);
}

.operator-panel__roles {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.readiness-list {
  display: grid;
  gap: 10px;
}

.readiness-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-divider);
}

.readiness-item:last-child {
  border-bottom: 0;
}

.readiness-item span {
  color: var(--color-text-muted);
}

.readiness-item strong {
  font-size: 1.25rem;
}

.quick-links {
  display: grid;
  gap: 10px;
}

.quick-links button {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) 16px;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
  color: var(--color-text);
  text-align: left;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

@media (max-width: 1180px) {
  .workbench-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .workbench-hero,
  .ops-panel__head {
    flex-direction: column;
  }

  .workbench-hero__actions {
    justify-content: flex-start;
  }

  .workbench-tables {
    grid-template-columns: 1fr;
  }
}
</style>
