<template>
  <div class="app-shell">
    <header class="app-shell__header">
      <div class="app-shell__topline">
        <button class="app-shell__menu-trigger" type="button" @click="drawerVisible = true">
          <el-icon><MenuIcon /></el-icon>
        </button>

        <button class="app-shell__brand" type="button" @click="router.push('/home')">
          <span class="app-shell__brand-mark"></span>
          <span class="app-shell__brand-copy">
            <span class="app-shell__brand-name">Sanctuary Coffee</span>
          </span>
        </button>

        <div class="app-shell__menu-wrap">
          <el-menu
            :default-active="activeMenu"
            :class="['app-shell__menu', { 'app-shell__menu--spread': spreadMenu }]"
            mode="horizontal"
            :ellipsis="false"
            background-color="transparent"
            text-color="rgba(255, 245, 236, 0.7)"
            active-text-color="#fffaf4"
            @select="handleMenuSelect"
          >
            <template v-for="item in filteredNavigation" :key="item.key">
              <el-menu-item v-if="!item.children" :index="item.path">
                <span class="app-shell__menu-label">{{ item.label }}</span>
              </el-menu-item>

              <el-sub-menu v-else :index="item.key">
                <template #title>
                  <span class="app-shell__menu-label">{{ item.label }}</span>
                </template>
                <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                  {{ child.label }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </div>

        <div class="app-shell__signal">
          <span class="app-shell__signal-eyebrow">{{ currentStory.eyebrow }}</span>
          <strong class="app-shell__signal-title">{{ currentPageLabel }}</strong>
        </div>

        <div class="app-shell__toolbar">
          <div class="app-shell__clock">
            <span class="app-shell__clock-date">{{ currentDateLabel }}</span>
            <strong class="app-shell__clock-role">{{ roleLabel }}</strong>
          </div>

          <el-dropdown placement="bottom-end" trigger="click" @command="handleCommand">
            <button class="app-shell__profile" type="button">
              <el-avatar :size="38" :src="userStore.avatar || undefined" class="app-shell__avatar">
                {{ userInitial }}
              </el-avatar>
              <div class="app-shell__profile-copy">
                <span class="app-shell__profile-name">{{ userStore.nickname || userStore.username || 'Guest' }}</span>
                <span class="app-shell__profile-role">{{ roleLabel }}</span>
              </div>
              <el-icon class="app-shell__profile-chevron"><ArrowDown /></el-icon>
            </button>

            <template #dropdown>
              <el-dropdown-menu class="app-shell__dropdown">
                <el-dropdown-item command="user">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="address">
                  <el-icon><Location /></el-icon>
                  地址管理
                </el-dropdown-item>
                <el-dropdown-item v-if="canAccessMemberRoutes" command="order">
                  <el-icon><List /></el-icon>
                  我的订单
                </el-dropdown-item>
                <el-dropdown-item v-if="canAccessMemberRoutes" command="invoice">
                  <el-icon><Document /></el-icon>
                  发票中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="app-shell__main">
      <div v-if="showContextStrip" class="app-shell__context-strip">
        <div>
          <h1 class="app-shell__context-title">{{ currentPageLabel }}</h1>
        </div>
        <div class="app-shell__context-meta">
          <span class="scene-pill">{{ currentStory.eyebrow }}</span>
          <span class="scene-pill">{{ roleLabel }}</span>
        </div>
      </div>

      <div class="app-shell__content">
        <slot />
      </div>
    </main>

    <el-drawer
      v-model="drawerVisible"
      :with-header="false"
      class="app-shell__drawer"
      direction="ltr"
      size="min(360px, 88vw)"
    >
      <div class="app-shell__drawer-head">
        <div class="app-shell__drawer-brand">
          <span class="app-shell__brand-mark"></span>
          <div>
            <div class="app-shell__drawer-title">Sanctuary Coffee</div>
            <div class="app-shell__drawer-subtitle">{{ currentPageLabel }}</div>
          </div>
        </div>
      </div>

      <el-menu :default-active="activeMenu" class="app-shell__drawer-menu" @select="handleMenuSelect">
        <template v-for="item in filteredNavigation" :key="item.key">
          <el-menu-item v-if="!item.children" :index="item.path">
            <span>{{ item.label }}</span>
          </el-menu-item>

          <el-sub-menu v-else :index="item.key">
            <template #title>
              <span>{{ item.label }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <span>{{ child.label }}</span>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>

      <div class="app-shell__drawer-actions">
        <el-button plain @click="handleCommand('user')">个人中心</el-button>
        <el-button v-if="canAccessMemberRoutes" plain @click="handleCommand('invoice')">发票中心</el-button>
        <el-button v-if="canAccessMemberRoutes" plain @click="handleCommand('order')">我的订单</el-button>
        <el-button type="danger" plain @click="handleCommand('logout')">退出登录</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  AUTH_ROLES,
  MEMBER_ROLES,
  PERMISSION_CODES,
  ROLE_ADMIN,
  ROLE_DELIVERY,
  getRolePermissionCodes,
  hasAnyPermission,
  hasAnyRole
} from '@/utils/permission'
import {
  ArrowDown,
  DataLine,
  Document,
  Goods,
  House,
  List,
  Location,
  Menu as MenuIcon,
  Present,
  ShoppingBag,
  ShoppingCart,
  SwitchButton,
  Ticket,
  TrendCharts,
  User,
  Van
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const drawerVisible = ref(false)
const currentDateLabel = ref('')

let timeTicker = 0

const navigation = [
  { key: 'home', path: '/home', label: '首页', icon: House, roles: AUTH_ROLES },
  {
    key: 'admin',
    label: '后台',
    icon: House,
    roles: [ROLE_ADMIN],
    children: [
      {
        path: '/admin',
        label: '后台工作台',
        icon: House,
        roles: [ROLE_ADMIN],
        permissions: [PERMISSION_CODES.ADMIN_WORKBENCH]
      },
      {
        path: '/admin/rbac',
        label: '权限管理',
        icon: User,
        roles: [ROLE_ADMIN],
        permissions: [PERMISSION_CODES.RBAC_VIEW]
      }
    ]
  },
  { key: 'shop', path: '/shop', label: '商品商店', icon: ShoppingBag, roles: AUTH_ROLES },
  {
    key: 'product',
    label: '商品',
    icon: Goods,
    children: [
      { path: '/product', label: '商品列表', icon: Goods, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.PRODUCT_LIST] },
      { path: '/product/category', label: '分类管理', icon: MenuIcon, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.PRODUCT_CATEGORY] },
      { path: '/product/stock-warning', label: '库存预警', icon: Goods, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.PRODUCT_WARNING] }
    ]
  },
  {
    key: 'orders',
    label: '交易',
    icon: ShoppingCart,
    children: [
      { path: '/order/manage', label: '订单管理', icon: List, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.ORDER_MANAGE] },
      { path: '/payment/refund', label: '退款管理', icon: Document, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.ORDER_REFUND] },
      { path: '/order/after-sale', label: '售后管理', icon: Document, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.ORDER_AFTER_SALE] },
      { path: '/cart', label: '购物车', icon: ShoppingCart, roles: MEMBER_ROLES },
      { path: '/order', label: '订单中心', icon: List, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.ORDER_MEMBER] },
      { path: '/order/after-sales', label: '我的售后', icon: Document, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.ORDER_MEMBER] },
      { path: '/payment', label: '支付台', icon: Document, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.PAYMENT_MANAGE] },
      {
        path: '/financial/invoice',
        label: '发票中心',
        icon: Document,
        roles: MEMBER_ROLES,
        permissions: [PERMISSION_CODES.INVOICE_SELF, PERMISSION_CODES.INVOICE_MANAGE]
      }
    ]
  },
  {
    key: 'members',
    label: '会员',
    icon: User,
    children: [
      { path: '/member', label: '会员管理', icon: User, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.MEMBER_MANAGE] },
      { path: '/member/center', label: '会员中心', icon: User, roles: MEMBER_ROLES },
      { path: '/member/benefits', label: '会员权益', icon: User, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.MEMBER_BENEFIT] }
    ]
  },
  {
    key: 'campaigns',
    label: '活动',
    icon: Present,
    children: [
      { path: '/coupon', label: '领券中心', icon: Ticket, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.COUPON_CENTER] },
      { path: '/coupon/manage', label: '优惠券管理', icon: Ticket, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.COUPON_MANAGE] },
      { path: '/promotion', label: '促销活动', icon: Present, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.PROMOTION_MANAGE] }
    ]
  },
  {
    key: 'reviews',
    label: '评价',
    icon: ShoppingBag,
    children: [
      { path: '/review/my', label: '我的评价', icon: ShoppingBag, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.REVIEW_SELF] },
      { path: '/review/manage', label: '评价总览', icon: ShoppingBag, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.REVIEW_MANAGE] },
    ]
  },
  {
    key: 'signals',
    label: '数据',
    icon: DataLine,
    children: [
      { path: '/statistics', label: '运营看板', icon: DataLine, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.DASHBOARD_VIEW] },
      { path: '/analytics', label: '用户分析', icon: TrendCharts, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.USER_ANALYTICS] },
      { path: '/financial/report', label: '财务报表', icon: Document, roles: [ROLE_ADMIN], permissions: [PERMISSION_CODES.FINANCIAL_REPORT] }
    ]
  },
  {
    key: 'delivery',
    label: '配送',
    icon: Van,
    children: [
      {
        path: '/delivery/manage',
        label: '配送管理',
        icon: Van,
        roles: [ROLE_ADMIN],
        permissions: [PERMISSION_CODES.DELIVERY_MANAGE]
      },
      { path: '/delivery/tasks', label: '配送任务', icon: Van, roles: [ROLE_ADMIN, ROLE_DELIVERY], permissions: [PERMISSION_CODES.DELIVERY_TRACKING] },
      { path: '/delivery/tracking', label: '物流追踪', icon: Location, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.DELIVERY_TRACKING] }
    ]
  }
]

const userRoles = computed(() => userStore.roles || [])
const userPermissions = computed(() => (
  userStore.userInfo?.permissionCodes?.length
    ? userStore.userInfo.permissionCodes
    : getRolePermissionCodes(userRoles.value)
))
const canAccessMemberRoutes = computed(() => hasAnyRole(userRoles.value, MEMBER_ROLES))

const canAccessNavigationItem = (item) => {
  const roleMatched = hasAnyRole(userRoles.value, item.roles)
  const permissionMatched = hasAnyPermission(userPermissions.value, item.permissions)
  return roleMatched && permissionMatched
}

const filteredNavigation = computed(() =>
  navigation
    .map((item) => {
      if (item.children) {
        const children = item.children.filter((child) => canAccessNavigationItem(child))
        return children.length > 0 && canAccessNavigationItem(item) ? { ...item, children } : null
      }
      return canAccessNavigationItem(item) ? item : null
    })
    .filter(Boolean)
)

const spreadMenu = computed(() => filteredNavigation.value.length >= 6)

const flattenedLinks = computed(() =>
  filteredNavigation.value.flatMap((item) =>
    item.children ? item.children : [{ path: item.path, label: item.label, icon: item.icon }]
  )
)

const sortedLinks = computed(() =>
  [...flattenedLinks.value].sort((left, right) => right.path.length - left.path.length)
)

const currentLink = computed(
  () => sortedLinks.value.find((item) => route.path === item.path || route.path.startsWith(`${item.path}/`)) || null
)

const currentPageLabel = computed(() => currentLink.value?.label || route.meta?.title || '控制台')

const activeMenu = computed(() => currentLink.value?.path || '')

const currentStory = computed(() => {
  const stories = [
    {
      test: (path) => path.startsWith('/home'),
      eyebrow: '首页'
    },
    {
      test: (path) => path.startsWith('/admin'),
      eyebrow: '后台'
    },
    {
      test: (path) => path.startsWith('/shop') || path.startsWith('/product'),
      eyebrow: '商品'
    },
    {
      test: (path) => path.startsWith('/cart') || path.startsWith('/order') || path.startsWith('/payment'),
      eyebrow: '交易'
    },
    {
      test: (path) => path.startsWith('/member') || path.startsWith('/user'),
      eyebrow: '会员'
    },
    {
      test: (path) => path.startsWith('/coupon') || path.startsWith('/promotion'),
      eyebrow: '活动'
    },
    {
      test: (path) => path.startsWith('/review'),
      eyebrow: '评价'
    },
    {
      test: (path) => path.startsWith('/delivery'),
      eyebrow: '配送'
    },
    {
      test: (path) => path.startsWith('/statistics') || path.startsWith('/analytics') || path.startsWith('/financial'),
      eyebrow: '数据'
    }
  ]

  return stories.find((item) => item.test(route.path)) || {
    eyebrow: '工作区'
  }
})

const routesWithOwnHero = new Set([
  '/home',
  '/admin',
  '/admin/rbac',
  '/shop',
  '/statistics',
  '/delivery/manage',
  '/delivery/tasks',
  '/financial/invoice',
  '/order',
  '/order/manage',
  '/order/after-sale',
  '/payment/refund',
  '/review/manage',
  '/product/stock-warning'
])

const showContextStrip = computed(() => !routesWithOwnHero.has(route.path))

const isAdmin = computed(() => userRoles.value.includes(ROLE_ADMIN))
const isDelivery = computed(() => userRoles.value.includes(ROLE_DELIVERY))
const roleLabel = computed(() => {
  if (isAdmin.value) {
    return '管理员'
  }
  if (isDelivery.value) {
    return '配送员'
  }
  return '会员用户'
})
const userInitial = computed(() => (userStore.nickname || userStore.username || 'U').charAt(0).toUpperCase())

watch(
  () => route.fullPath,
  () => {
    drawerVisible.value = false
  }
)

const syncCurrentDate = () => {
  currentDateLabel.value = new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric',
    weekday: 'short'
  }).format(new Date())
}

onMounted(async () => {
  syncCurrentDate()
  timeTicker = window.setInterval(syncCurrentDate, 60000)

  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch (error) {
      console.error('Failed to load user info in shell:', error)
    }
  }
})

onBeforeUnmount(() => {
  window.clearInterval(timeTicker)
})

const handleMenuSelect = (index) => {
  if (index?.startsWith('/')) {
    router.push(index)
  }
}

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (error) {
      if (error !== 'cancel') {
        console.error('Logout failed:', error)
      }
    }
    return
  }

  const routeMap = {
    user: '/user',
    address: '/user/address',
    order: '/order',
    invoice: '/financial/invoice'
  }
  const commandRoleMap = {
    order: MEMBER_ROLES,
    invoice: MEMBER_ROLES
  }

  if (routeMap[command]) {
    if (!hasAnyRole(userRoles.value, commandRoleMap[command])) {
      return
    }
    router.push(routeMap[command])
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-shell__header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(18, 13, 11, 0.94) 0%, rgba(25, 18, 15, 0.84) 100%);
  backdrop-filter: blur(24px);
}

.app-shell__topline,
.app-shell__content,
.app-shell__context-strip {
  width: min(var(--page-max-width), calc(100% - (var(--page-gutter, 20px) * 2)));
  margin: 0 auto;
}

.app-shell__topline {
  display: grid;
  grid-template-columns: auto minmax(420px, 1fr) minmax(116px, auto) auto;
  align-items: center;
  gap: 18px;
  padding: 12px 0;
}

.app-shell__menu-wrap {
  min-width: 0;
  overflow: hidden;
}

.app-shell__menu-trigger {
  display: none;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--color-text-inverse);
}

.app-shell__brand {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  gap: 14px;
  border: none;
  background: transparent;
  color: var(--color-text-inverse);
  text-align: left;
  white-space: nowrap;
}

.app-shell__brand-mark {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 0 10px rgba(255, 255, 255, 0.12);
}

.app-shell__brand-copy {
  display: flex;
  align-items: center;
}

.app-shell__brand-name {
  font-family: var(--font-serif);
  font-size: 1.55rem;
  font-style: italic;
  line-height: 1;
  letter-spacing: -0.03em;
}

.app-shell__signal {
  display: grid;
  min-width: 0;
  gap: 2px;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
}

.app-shell__signal-eyebrow {
  font-size: 0.68rem;
  font-weight: var(--font-bold);
  letter-spacing: 0.12em;
  color: rgba(255, 245, 236, 0.5);
  white-space: nowrap;
}

.app-shell__signal-title {
  max-width: 128px;
  overflow: hidden;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-inverse);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-shell__toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.app-shell__clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 92px;
}

.app-shell__clock-date {
  font-size: 0.7rem;
  font-weight: var(--font-bold);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 245, 236, 0.46);
}

.app-shell__clock-role {
  color: var(--color-text-inverse);
  font-size: var(--text-sm);
}

.app-shell__profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 11px 6px 6px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text-inverse);
}

.app-shell__avatar {
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22) 0%, rgba(255, 255, 255, 0.08) 100%);
  color: var(--color-text-inverse);
}

.app-shell__profile-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.app-shell__profile-name {
  max-width: 112px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
}

.app-shell__profile-role,
.app-shell__profile-chevron {
  color: rgba(255, 245, 236, 0.56);
  font-size: var(--text-xs);
}

.app-shell__menu {
  min-width: 0;
  width: 100%;
  overflow: hidden;
}

.app-shell__menu-label {
  display: inline-block;
  flex: 0 1 auto;
  min-width: max-content;
  max-width: 100%;
  overflow: visible;
  font-size: 0.76rem;
  font-weight: var(--font-bold);
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.app-shell__menu.el-menu--horizontal {
  border-bottom: none;
  display: flex;
  align-items: center;
  width: 100%;
  height: 38px;
  gap: 4px;
  --el-menu-hover-bg-color: transparent;
}

.app-shell__menu.el-menu--horizontal:not(.app-shell__menu--spread) {
  justify-content: center;
}

.app-shell__menu--spread.el-menu--horizontal :deep(> .el-menu-item),
.app-shell__menu--spread.el-menu--horizontal :deep(> .el-sub-menu) {
  flex: 1 1 0;
  min-width: 0;
}

.app-shell__menu.el-menu--horizontal :deep(> .el-menu-item),
.app-shell__menu.el-menu--horizontal :deep(> .el-sub-menu > .el-sub-menu__title) {
  height: 38px;
  line-height: 38px;
  border-bottom: none !important;
  border-radius: 10px;
  padding: 0 9px;
  justify-content: center;
  color: rgba(255, 245, 236, 0.72);
}

.app-shell__menu--spread.el-menu--horizontal :deep(> .el-sub-menu > .el-sub-menu__title) {
  width: 100%;
}

.app-shell__menu.el-menu--horizontal :deep(> .el-sub-menu > .el-sub-menu__title .el-sub-menu__icon-arrow) {
  position: static;
  margin-left: 5px;
  margin-top: 0;
  color: rgba(255, 245, 236, 0.52);
}

.app-shell__menu.el-menu--horizontal :deep(> .el-menu-item:hover),
.app-shell__menu.el-menu--horizontal :deep(> .el-sub-menu > .el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.09);
  color: var(--color-text-inverse);
}

.app-shell__menu.el-menu--horizontal :deep(> .el-menu-item.is-active),
.app-shell__menu.el-menu--horizontal :deep(> .el-sub-menu.is-active > .el-sub-menu__title) {
  border-bottom: none !important;
  background: rgba(255, 250, 246, 0.14);
  box-shadow: inset 0 0 0 1px rgba(255, 250, 246, 0.1);
  color: var(--color-text-inverse);
}

.app-shell__main {
  padding: 18px 0 44px;
}

@media (max-width: 1440px) {
  .app-shell__topline {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .app-shell__signal {
    display: none;
  }

  .app-shell__clock {
    display: none;
  }
}

.app-shell__context-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-6);
  margin-bottom: var(--spacing-6);
  padding: 18px 28px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 30px;
  background: linear-gradient(135deg, rgba(255, 250, 246, 0.16) 0%, rgba(255, 248, 241, 0.1) 100%);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(20px);
}

.app-shell__context-title {
  margin: 0;
  font-size: clamp(1.6rem, 3vw, 2.6rem);
}

.app-shell__context-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--spacing-3);
}

.app-shell__content {
  position: relative;
}

.app-shell__drawer-head {
  margin-bottom: var(--spacing-6);
}

.app-shell__drawer-brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
}

.app-shell__drawer-title {
  font-family: var(--font-serif);
  font-size: var(--text-2xl);
  color: var(--color-text);
}

.app-shell__drawer-subtitle {
  margin-top: 6px;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.app-shell__drawer-menu {
  border-right: none;
  background: transparent;
}

.app-shell__drawer-actions {
  display: grid;
  gap: var(--spacing-3);
  margin-top: var(--spacing-6);
}

@media (max-width: 1200px) {
  .app-shell__menu-trigger {
    display: inline-grid;
  }

  .app-shell__topline {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .app-shell__signal {
    display: none;
  }

  .app-shell__menu-wrap {
    display: none;
  }
}

@media (max-width: 768px) {
  .app-shell__topline,
  .app-shell__content,
  .app-shell__context-strip {
    width: min(100%, calc(100% - 24px));
  }

  .app-shell__topline {
    gap: var(--spacing-3);
    padding-top: 18px;
    padding-bottom: 18px;
  }

  .app-shell__brand-name {
    font-size: 1.55rem;
  }

  .app-shell__clock,
  .app-shell__profile-copy,
  .app-shell__profile-chevron {
    display: none;
  }

  .app-shell__context-strip {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px;
  }
}
</style>
