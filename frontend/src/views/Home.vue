<template>
  <div class="home-page">
    <section class="home-hero scene-section scene-section--dark">
      <div class="home-hero__copy">
        <h1 class="home-hero__title">欢迎回来，{{ displayName }}</h1>

        <div class="home-hero__actions">
          <el-button type="primary" size="large" @click="router.push('/shop')">开始选购</el-button>
          <el-button v-if="canAccessMemberRoutes" size="large" @click="router.push('/order')">查看订单</el-button>
          <el-button v-if="canAccessAdminRoutes" size="large" @click="router.push('/admin')">进入后台</el-button>
        </div>
      </div>

      <div class="home-hero__aside">
        <div class="scene-metrics">
          <div v-for="item in heroStats" :key="item.label" class="home-hero__metric">
            <span class="home-hero__metric-value">{{ item.value }}</span>
            <span class="home-hero__metric-label">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="scene-section">
      <div class="home-section__heading">
        <h2 class="home-section__title">今天最常用的入口</h2>
      </div>

      <div class="home-actions">
        <button
          v-for="action in quickActions"
          :key="action.label"
          class="home-action-card"
          type="button"
          @click="router.push(action.path)"
        >
          <span class="home-action-card__glow" :style="{ background: action.accent }"></span>
          <div class="home-action-card__icon" :style="{ background: action.accent }">
            <el-icon><component :is="action.icon" /></el-icon>
          </div>
          <div class="home-action-card__copy">
            <span class="home-action-card__value">{{ action.value }}</span>
            <strong>{{ action.label }}</strong>
            <span>{{ action.description }}</span>
          </div>
        </button>
      </div>
    </section>

    <section class="scene-section">
      <div class="home-section__heading">
        <h2 class="home-section__title">按真实工作流拆开的模块</h2>
      </div>

      <div class="home-feature-grid">
        <button
          v-for="feature in featureLinks"
          :key="feature.label"
          class="home-feature-card"
          type="button"
          @click="router.push(feature.path)"
        >
          <div class="home-feature-card__icon" :style="{ background: feature.background }">
            <el-icon><component :is="feature.icon" /></el-icon>
          </div>
          <div class="home-feature-card__copy">
            <span class="home-feature-card__eyebrow">{{ feature.eyebrow }}</span>
            <strong>{{ feature.label }}</strong>
            <span>{{ feature.description }}</span>
          </div>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Coin,
  Goods,
  List,
  PieChart,
  Present,
  ShoppingBag,
  ShoppingCart,
  Star,
  Ticket,
  TrendCharts,
  Van
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { MEMBER_ROLES, ROLE_ADMIN, ROLE_DELIVERY, hasAnyRole } from '@/utils/permission'
import { getCartList } from '@/api/cart'
import { getOrderPage } from '@/api/order'
import { getMyReviews } from '@/api/review'
import { getMyCoupons } from '@/api/member'
import { getSalesStatistics, getUserStatistics } from '@/api/statistics'
import { getRealTimeStats } from '@/api/analytics'

const router = useRouter()
const userStore = useUserStore()

const cartCount = ref(0)
const orderCount = ref(0)
const reviewCount = ref(0)
const couponCount = ref(0)

const adminStats = ref({
  todaySales: 0,
  todayOrders: 0,
  monthSales: 0,
  totalUsers: 0,
  activeUsers: 0
})

const displayName = computed(() => userStore.nickname || userStore.username || '朋友')
const userRoles = computed(() => userStore.roles || [])
const canAccessMemberRoutes = computed(() => hasAnyRole(userRoles.value, MEMBER_ROLES))
const canAccessAdminRoutes = computed(() => hasAnyRole(userRoles.value, [ROLE_ADMIN]))

const resetQuickStats = () => {
  cartCount.value = 0
  orderCount.value = 0
  reviewCount.value = 0
  couponCount.value = 0
}

const loadQuickStats = async () => {
  if (!canAccessMemberRoutes.value) {
    resetQuickStats()
    return
  }

  const [cartRes, orderRes, reviewRes, couponRes] = await Promise.allSettled([
    getCartList(),
    getOrderPage({ pageNum: 1, pageSize: 1 }),
    getMyReviews({ pageNum: 1, pageSize: 1 }),
    getMyCoupons(0)
  ])

  if (cartRes.status === 'fulfilled') {
    const list = cartRes.value?.data || []
    cartCount.value = list.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  } else {
    cartCount.value = 0
  }

  if (orderRes.status === 'fulfilled') {
    orderCount.value = Number(orderRes.value?.data?.total || 0)
  } else {
    orderCount.value = 0
  }

  if (reviewRes.status === 'fulfilled') {
    reviewCount.value = Number(reviewRes.value?.data?.total || 0)
  } else {
    reviewCount.value = 0
  }

  if (couponRes.status === 'fulfilled') {
    const list = couponRes.value?.data || []
    couponCount.value = Array.isArray(list) ? list.length : 0
  } else {
    couponCount.value = 0
  }
}

const loadAdminStats = async () => {
  try {
    const [salesRes, userRes, realtimeRes] = await Promise.allSettled([
      getSalesStatistics(),
      getUserStatistics(),
      getRealTimeStats()
    ])
    if (salesRes.status === 'fulfilled' && salesRes.value?.data) {
      const d = salesRes.value.data
      adminStats.value.todaySales = d.todaySales || 0
      adminStats.value.todayOrders = d.todayOrders || 0
      adminStats.value.monthSales = d.monthSales || 0
    }
    if (userRes.status === 'fulfilled' && userRes.value?.data) {
      adminStats.value.totalUsers = userRes.value.data.totalUsers || 0
      adminStats.value.activeUsers = userRes.value.data.activeUsers || 0
    }
  } catch { /* ignore */ }
}

const fmtCurrency = (v) => {
  const n = Number(v)
  if (!n) return '¥0'
  if (n >= 10000) return '¥' + (n / 10000).toFixed(1) + '万'
  return '¥' + n.toFixed(0)
}

const heroStats = computed(() =>
  canAccessAdminRoutes.value
    ? [
        { label: '今日销售额', value: fmtCurrency(adminStats.value.todaySales) },
        { label: '今日订单', value: adminStats.value.todayOrders },
        { label: '本月销售额', value: fmtCurrency(adminStats.value.monthSales) },
        { label: '总用户数', value: adminStats.value.totalUsers },
        { label: '活跃用户', value: adminStats.value.activeUsers }
      ]
    : [
        { label: '活跃模块', value: '12' },
        { label: '统一界面', value: '1' },
        { label: '高频入口', value: '8' }
      ]
)

const quickActions = computed(() =>
  [
    {
      label: '购物车',
      path: '/cart',
      icon: ShoppingCart,
      value: cartCount.value,
      description: '回到待下单商品，继续完成当前链路。',
      accent: 'linear-gradient(135deg, #6b655b 0%, #d2aa7d 100%)',
      roles: MEMBER_ROLES
    },
    {
      label: '我的订单',
      path: '/order',
      icon: List,
      value: orderCount.value,
      description: '查看支付、发货、签收和售后状态。',
      accent: 'linear-gradient(135deg, #0f766e 0%, #55b89a 100%)',
      roles: MEMBER_ROLES
    },
    {
      label: '评价中心',
      path: '/review/my',
      icon: Star,
      value: reviewCount.value,
      description: '统一处理待评价和已评价内容。',
      accent: 'linear-gradient(135deg, #425a83 0%, #6b86ba 100%)',
      roles: MEMBER_ROLES
    },
    {
      label: '优惠券',
      path: '/coupon',
      icon: Ticket,
      value: couponCount.value,
      description: '查看可领取、可用和已发放的优惠券。',
      accent: 'linear-gradient(135deg, #c4934f 0%, #d2aa7d 100%)',
      roles: MEMBER_ROLES
    }
  ].filter((item) => hasAnyRole(userRoles.value, item.roles))
)

const featureLinks = computed(() => [
  {
    label: '后台工作台',
    path: '/admin',
    icon: TrendCharts,
    eyebrow: 'Admin',
    description: '以统一后台入口总览权限、联调状态与管理模块。',
    background: 'linear-gradient(135deg, #111827 0%, #4b5563 100%)',
    roles: [ROLE_ADMIN]
  },
  {
    label: '商品商店',
    path: '/shop',
    icon: ShoppingBag,
    eyebrow: 'Browse',
    description: '从编辑化陈列视角浏览商品、促销和评价。',
    background: 'linear-gradient(135deg, #6b655b 0%, #8e8679 100%)',
    roles: null
  },
  {
    label: '商品管理',
    path: '/product',
    icon: Goods,
    eyebrow: 'Catalog',
    description: '集中维护商品、分类、库存和上下架状态。',
    background: 'linear-gradient(135deg, #3d2f2a 0%, #6b655b 100%)',
    roles: [ROLE_ADMIN]
  },
  {
    label: '促销活动',
    path: '/promotion',
    icon: Present,
    eyebrow: 'Campaigns',
    description: '统一管理优惠券、活动规则和转化刺激。',
    background: 'linear-gradient(135deg, #9b4d48 0%, #d2aa7d 100%)',
    roles: [ROLE_ADMIN]
  },
  {
    label: '会员权益',
    path: '/member/benefits',
    icon: Coin,
    eyebrow: 'Membership',
    description: '查看积分、等级、权益与留存动作。',
    background: 'linear-gradient(135deg, #37556b 0%, #6e95b4 100%)',
    roles: [ROLE_ADMIN]
  },
  {
    label: '发票中心',
    path: '/financial/invoice',
    icon: PieChart,
    eyebrow: 'Finance',
    description: '进入发票申请、开票与财务联动入口。',
    background: 'linear-gradient(135deg, #4d8f73 0%, #84c3a6 100%)',
    roles: MEMBER_ROLES
  },
  {
    label: '配送管理',
    path: '/delivery/manage',
    icon: Van,
    eyebrow: 'Dispatch',
    description: '查看区域、配送员和履约状态。',
    background: 'linear-gradient(135deg, #584d67 0%, #9385aa 100%)',
    roles: [ROLE_ADMIN]
  },
  {
    label: '我的配送任务',
    path: '/delivery/tasks',
    icon: Van,
    eyebrow: 'Delivery',
    description: '处理待接单、待取货和送达确认任务。',
    background: 'linear-gradient(135deg, #8a4b2f 0%, #d98c52 100%)',
    roles: [ROLE_DELIVERY]
  },
  {
    label: '用户分析',
    path: '/analytics',
    icon: TrendCharts,
    eyebrow: 'Signals',
    description: '从行为和趋势视角观察用户决策。',
    background: 'linear-gradient(135deg, #111827 0%, #3f4a5a 100%)',
    roles: [ROLE_ADMIN]
  }
].filter((item) => hasAnyRole(userRoles.value, item.roles)))

onMounted(() => {
  loadQuickStats()
  if (canAccessAdminRoutes.value) {
    loadAdminStats()
  }
})

watch(
  () => userStore.roles,
  () => {
    loadQuickStats()
  }
)
</script>

<style scoped>
.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(280px, 0.9fr);
  gap: clamp(24px, 4vw, 48px);
  overflow: hidden;
}

.home-hero__copy,
.home-hero__aside {
  position: relative;
  z-index: 1;
}

.home-hero__title {
  max-width: 10ch;
  margin: 0;
  font-size: clamp(3rem, 7vw, 5.5rem);
  line-height: 0.96;
  color: var(--color-text-inverse);
}

.home-hero__title i {
  font-style: italic;
  font-weight: 400;
}

.home-hero__desc {
  max-width: 640px;
  margin-top: var(--spacing-5);
  font-size: var(--text-lg);
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
}

.home-hero__aside {
  display: grid;
  gap: var(--spacing-4);
  align-content: end;
}

.home-hero__note {
  padding: var(--spacing-5);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(18px);
}

.home-hero__note-label {
  display: inline-flex;
  margin-bottom: 10px;
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 245, 236, 0.58);
}

.home-hero__note strong {
  display: block;
  font-size: var(--text-xl);
  color: var(--color-text-inverse);
}

.home-hero__note p {
  margin-top: 10px;
}

.home-hero__metric {
  padding: var(--spacing-4);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.home-hero__metric-value {
  display: block;
  font-family: var(--font-serif);
  font-size: clamp(2rem, 3vw, 3rem);
  line-height: 1;
  color: var(--color-text-inverse);
}

.home-hero__metric-label {
  display: block;
  margin-top: 10px;
  color: rgba(255, 245, 236, 0.6);
}

.home-section__heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-5);
}

.home-section__title {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 2.8rem);
}

.home-section__desc {
  max-width: 520px;
  margin: 0;
}

.home-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--spacing-4);
}

.home-action-card,
.home-feature-card {
  position: relative;
  display: flex;
  gap: var(--spacing-4);
  width: 100%;
  padding: var(--spacing-5);
  border: 1px solid rgba(107, 101, 91, 0.08);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.52);
  box-shadow: var(--shadow-sm);
  text-align: left;
  transition: transform var(--transition-base), box-shadow var(--transition-base), border-color var(--transition-base);
}

.home-action-card:hover,
.home-feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: rgba(107, 101, 91, 0.16);
}

.home-action-card__glow {
  position: absolute;
  inset: auto 16px 16px auto;
  width: 80px;
  height: 80px;
  border-radius: 999px;
  filter: blur(36px);
  opacity: 0.18;
}

.home-action-card__icon,
.home-feature-card__icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 20px;
  color: #fff;
  flex-shrink: 0;
  font-size: 24px;
}

.home-action-card__copy,
.home-feature-card__copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.home-action-card__value {
  font-family: var(--font-serif);
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  line-height: 1;
  color: var(--color-text);
}

.home-action-card__copy strong,
.home-feature-card__copy strong {
  font-size: var(--text-base);
  color: var(--color-text);
}

.home-action-card__copy span:last-child,
.home-feature-card__copy span:last-child {
  color: var(--color-text-muted);
}

.home-feature-card__eyebrow {
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.home-feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--spacing-4);
}

@media (max-width: 1200px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .home-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-section__heading {
    flex-direction: column;
    align-items: flex-start;
  }

  .home-actions {
    grid-template-columns: 1fr;
  }
}
</style>
