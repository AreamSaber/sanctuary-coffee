import { createRouter, createWebHistory } from 'vue-router'
import {
  MEMBER_ROLES,
  PERMISSION_CODES,
  ROLE_ADMIN,
  ROLE_DELIVERY,
  getStoredPermissionCodes,
  getStoredRoles,
  hasAnyPermission,
  hasAnyRole
} from '@/utils/permission'

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/setup/passwords',
    name: 'PasswordInit',
    component: () => import('@/views/auth/PasswordInit.vue'),
    meta: { title: '初始化密码' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminWorkbench',
    component: () => import('@/views/admin/AdminWorkbench.vue'),
    meta: {
      title: '后台工作台',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.ADMIN_WORKBENCH]
    }
  },
  {
    path: '/admin/rbac',
    name: 'RbacManage',
    component: () => import('@/views/admin/RbacManage.vue'),
    meta: {
      title: '权限管理后台',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.RBAC_VIEW]
    }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('@/views/user/UserCenter.vue'),
    meta: { title: '个人中心', requiresAuth: true, permissions: [PERMISSION_CODES.USER_CENTER] }
  },
  {
    path: '/user/address',
    name: 'UserAddress',
    component: () => import('@/views/user/AddressManage.vue'),
    meta: { title: '地址管理', requiresAuth: true, permissions: [PERMISSION_CODES.USER_ADDRESS] }
  },
  {
    path: '/product',
    name: 'ProductList',
    component: () => import('@/views/product/ProductList.vue'),
    meta: {
      title: '商品管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.PRODUCT_LIST]
    }
  },
  {
    path: '/shop',
    name: 'ProductShop',
    component: () => import('@/views/product/ProductShop.vue'),
    meta: { title: '商品商城', requiresAuth: true }
  },
  {
    path: '/product/category',
    name: 'CategoryManage',
    component: () => import('@/views/product/CategoryManage.vue'),
    meta: {
      title: '分类管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.PRODUCT_CATEGORY]
    }
  },
  {
    path: '/product/stock-warning',
    name: 'StockWarning',
    component: () => import('@/views/product/StockWarning.vue'),
    meta: {
      title: '库存预警',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.PRODUCT_WARNING]
    }
  },
  {
    path: '/review/add',
    name: 'AddReview',
    component: () => import('@/views/review/AddReview.vue'),
    meta: { title: '发表评论', requiresAuth: true, roles: MEMBER_ROLES }
  },
  {
    path: '/review/my',
    name: 'MyReviews',
    component: () => import('@/views/review/MyReviews.vue'),
    meta: { title: '我的评论', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.REVIEW_SELF] }
  },
  {
    path: '/review/manage',
    name: 'ReviewManage',
    component: () => import('@/views/review/ReviewManage.vue'),
    meta: {
      title: '评价总览',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.REVIEW_MANAGE]
    }
  },
  {
    path: '/promotion',
    name: 'PromotionManage',
    component: () => import('@/views/promotion/PromotionManage.vue'),
    meta: {
      title: '促销管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.PROMOTION_MANAGE]
    }
  },
  {
    path: '/member',
    name: 'MemberList',
    component: () => import('@/views/member/MemberList.vue'),
    meta: {
      title: '会员管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.MEMBER_MANAGE]
    }
  },
  {
    path: '/member/center',
    name: 'MemberCenter',
    component: () => import('@/views/member/MemberCenter.vue'),
    meta: { title: '会员中心', requiresAuth: true, roles: MEMBER_ROLES }
  },
  {
    path: '/member/benefits',
    name: 'MemberBenefits',
    component: () => import('@/views/member/MemberBenefits.vue'),
    meta: {
      title: '会员权益',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.MEMBER_BENEFIT]
    }
  },
  {
    path: '/coupon',
    name: 'CouponCenter',
    component: () => import('@/views/coupon/CouponCenter.vue'),
    meta: { title: '优惠券中心', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.COUPON_CENTER] }
  },
  {
    path: '/coupon/manage',
    name: 'CouponManage',
    component: () => import('@/views/coupon/CouponManage.vue'),
    meta: {
      title: '优惠券管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.COUPON_MANAGE]
    }
  },
  {
    path: '/cart',
    name: 'ShoppingCart',
    component: () => import('@/views/cart/ShoppingCart.vue'),
    meta: { title: '购物车', requiresAuth: true, roles: MEMBER_ROLES }
  },
  {
    path: '/order/confirm',
    name: 'OrderConfirm',
    component: () => import('@/views/order/OrderConfirm.vue'),
    meta: { title: '确认订单', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.ORDER_MEMBER] }
  },
  {
    path: '/order',
    name: 'OrderList',
    component: () => import('@/views/order/OrderList.vue'),
    meta: { title: '我的订单', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.ORDER_MEMBER] }
  },
  {
    path: '/order/after-sales',
    name: 'MyAfterSales',
    component: () => import('@/views/order/MyAfterSales.vue'),
    meta: { title: '我的售后', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.ORDER_MEMBER] }
  },
  {
    path: '/order/after-sale/apply',
    name: 'AfterSaleApply',
    component: () => import('@/views/order/AfterSaleApply.vue'),
    meta: { title: '申请售后', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.ORDER_MEMBER] }
  },
  {
    path: '/order/manage',
    name: 'AdminOrderManage',
    component: () => import('@/views/order/AdminOrderManage.vue'),
    meta: {
      title: '订单管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.ORDER_MANAGE]
    }
  },
  {
    path: '/order/after-sale',
    name: 'AfterSaleManage',
    component: () => import('@/views/order/AfterSaleManage.vue'),
    meta: {
      title: '售后管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.ORDER_AFTER_SALE]
    }
  },
  {
    path: '/payment/refund',
    name: 'RefundManage',
    component: () => import('@/views/payment/RefundManage.vue'),
    meta: {
      title: '退款管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.ORDER_REFUND]
    }
  },
  {
    path: '/payment',
    name: 'PaymentPage',
    component: () => import('@/views/payment/PaymentPage.vue'),
    meta: { title: '订单支付', requiresAuth: true, roles: MEMBER_ROLES, permissions: [PERMISSION_CODES.PAYMENT_MANAGE] }
  },
  {
    path: '/delivery/manage',
    name: 'DeliveryManage',
    component: () => import('@/views/delivery/DeliveryManage.vue'),
    meta: {
      title: '配送管理',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.DELIVERY_MANAGE]
    }
  },
  {
    path: '/delivery/tasks',
    name: 'DeliveryTaskList',
    component: () => import('@/views/delivery/DeliveryTaskList.vue'),
    meta: {
      title: '配送任务',
      requiresAuth: true,
      roles: [ROLE_ADMIN, ROLE_DELIVERY],
      permissions: [PERMISSION_CODES.DELIVERY_TRACKING]
    }
  },
  {
    path: '/delivery/tracking',
    name: 'DeliveryTracking',
    component: () => import('@/views/delivery/DeliveryTracking.vue'),
    meta: { title: '配送跟踪', requiresAuth: true, permissions: [PERMISSION_CODES.DELIVERY_TRACKING] }
  },
  {
    path: '/financial/report',
    name: 'FinancialReport',
    component: () => import('@/views/financial/FinancialReport.vue'),
    meta: {
      title: '财务报表',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.FINANCIAL_REPORT]
    }
  },
  {
    path: '/financial/invoice',
    name: 'InvoiceManage',
    component: () => import('@/views/financial/InvoiceManage.vue'),
    meta: {
      title: '发票中心',
      requiresAuth: true,
      roles: MEMBER_ROLES,
      permissions: [PERMISSION_CODES.INVOICE_SELF, PERMISSION_CODES.INVOICE_MANAGE]
    }
  },
  {
    path: '/statistics',
    name: 'Dashboard',
    component: () => import('@/views/statistics/Dashboard.vue'),
    meta: {
      title: '数据统计',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.DASHBOARD_VIEW]
    }
  },
  {
    path: '/analytics',
    name: 'UserAnalytics',
    component: () => import('@/views/analytics/UserAnalytics.vue'),
    meta: {
      title: '数据分析',
      requiresAuth: true,
      roles: [ROLE_ADMIN],
      permissions: [PERMISSION_CODES.USER_ANALYTICS, PERMISSION_CODES.OPERATION_ANALYTICS]
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const PASSWORD_SETUP_PATH = '/setup/passwords'

const shouldRedirectToPasswordSetup = async (to, force = false) => {
  if (to.path === PASSWORD_SETUP_PATH) {
    return false
  }

  try {
    const { fetchPasswordInitStatus } = await import('@/utils/passwordInit')
    const status = await fetchPasswordInitStatus(force)
    if (status.required) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      return true
    }
  } catch (error) {
    // 后端未启动或网络异常时不阻断路由，保留页面自身的错误提示。
  }

  return false
}

router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta?.title) {
    document.title = `${to.meta.title} - Coffee Shop`
  }

  if (await shouldRedirectToPasswordSetup(to, Boolean(token))) {
    next(PASSWORD_SETUP_PATH)
    return
  }

  if (to.meta?.requiresAuth && !token) {
    next('/login')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && token) {
    next('/home')
    return
  }

  if (to.meta?.requiresAuth && Array.isArray(to.meta?.roles) && to.meta.roles.length > 0) {
    const userRoles = getStoredRoles()
    if (!hasAnyRole(userRoles, to.meta.roles)) {
      next('/home')
      return
    }
  }

  if (to.meta?.requiresAuth && Array.isArray(to.meta?.permissions) && to.meta.permissions.length > 0) {
    const permissionCodes = getStoredPermissionCodes()
    if (!hasAnyPermission(permissionCodes, to.meta.permissions)) {
      const userRoles = getStoredRoles()
      next(hasAnyRole(userRoles, [ROLE_ADMIN]) ? '/admin' : '/home')
      return
    }
  }

  next()
})

export default router
