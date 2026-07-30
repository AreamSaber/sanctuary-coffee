export const ROLE_ADMIN = 'ROLE_ADMIN'
export const ROLE_USER = 'ROLE_USER'
export const ROLE_DELIVERY = 'ROLE_DELIVERY'

export const AUTH_ROLES = [ROLE_ADMIN, ROLE_USER, ROLE_DELIVERY]
export const MEMBER_ROLES = [ROLE_ADMIN, ROLE_USER]

export const PERMISSION_CODES = Object.freeze({
  ADMIN_WORKBENCH: 'admin:workbench:view',
  RBAC_VIEW: 'rbac:view',
  USER_CENTER: 'user:center:view',
  USER_ADDRESS: 'user:address:view',
  USER_ANALYTICS: 'user:analytics:view',
  PRODUCT_LIST: 'product:list',
  PRODUCT_CATEGORY: 'product:category',
  PRODUCT_SKU: 'product:sku',
  PRODUCT_STOCK: 'product:stock',
  PRODUCT_WARNING: 'product:warning',
  ORDER_MEMBER: 'order:member',
  ORDER_MANAGE: 'order:manage',
  ORDER_REFUND: 'order:refund',
  ORDER_AFTER_SALE: 'order:after-sale',
  PAYMENT_MANAGE: 'payment:manage',
  INVOICE_SELF: 'invoice:self',
  INVOICE_MANAGE: 'invoice:manage',
  DELIVERY_MANAGE: 'delivery:manage',
  DELIVERY_METHOD: 'delivery:method',
  DELIVERY_REGION: 'delivery:region',
  DELIVERY_STAFF: 'delivery:staff',
  DELIVERY_TRACKING: 'delivery:tracking',
  MEMBER_MANAGE: 'member:manage',
  MEMBER_BENEFIT: 'member:benefit',
  COUPON_CENTER: 'coupon:center',
  COUPON_MANAGE: 'coupon:manage',
  PROMOTION_MANAGE: 'promotion:manage',
  REVIEW_SELF: 'review:self',
  REVIEW_MANAGE: 'review:manage',
  DASHBOARD_VIEW: 'dashboard:view',
  FINANCIAL_REPORT: 'financial:report',
  OPERATION_ANALYTICS: 'operation:analytics'
})

const ALL_PERMISSION_CODES = Object.freeze(Object.values(PERMISSION_CODES))

const ROLE_PERMISSION_MAP = Object.freeze({
  [ROLE_ADMIN]: ALL_PERMISSION_CODES,
  [ROLE_USER]: [
    PERMISSION_CODES.USER_CENTER,
    PERMISSION_CODES.USER_ADDRESS,
    PERMISSION_CODES.ORDER_MEMBER,
    PERMISSION_CODES.INVOICE_SELF,
    PERMISSION_CODES.COUPON_CENTER,
    PERMISSION_CODES.REVIEW_SELF,
    PERMISSION_CODES.DELIVERY_TRACKING
  ],
  [ROLE_DELIVERY]: [
    PERMISSION_CODES.DELIVERY_TRACKING,
    PERMISSION_CODES.ORDER_MEMBER
  ]
})

export const ADMIN_PERMISSION_GROUPS = Object.freeze([
  {
    key: 'governance',
    title: '组织与权限',
    description: '围绕后台工作台、岗位职责和管理边界进行编排。',
    permissions: [
      {
        code: PERMISSION_CODES.ADMIN_WORKBENCH,
        name: '后台工作台',
        description: '统一后台主入口、经营提醒和业务导航。'
      },
      {
        code: PERMISSION_CODES.RBAC_VIEW,
        name: '角色与权限配置',
        description: '为岗位选择可进入页面和可操作能力。'
      }
    ]
  },
  {
    key: 'customer',
    title: '用户与会员',
    description: '用户信息、地址、会员列表、会员权益和行为分析。',
    permissions: [
      {
        code: PERMISSION_CODES.USER_CENTER,
        name: '用户中心',
        description: '维护用户资料与个人中心主链路。'
      },
      {
        code: PERMISSION_CODES.USER_ADDRESS,
        name: '地址管理',
        description: '查看与编辑收货地址。'
      },
      {
        code: PERMISSION_CODES.MEMBER_MANAGE,
        name: '会员管理',
        description: '会员资料、等级、积分与运营动作。'
      },
      {
        code: PERMISSION_CODES.MEMBER_BENEFIT,
        name: '会员权益配置',
        description: '等级折扣、成长值与权益矩阵编排。'
      },
      {
        code: PERMISSION_CODES.USER_ANALYTICS,
        name: '用户行为分析',
        description: '实时活跃、转化漏斗和行为明细分析。'
      }
    ]
  },
  {
    key: 'catalog',
    title: '商品与库存',
    description: '商品、分类、规格/SKU 与库存日志闭环。',
    permissions: [
      {
        code: PERMISSION_CODES.PRODUCT_LIST,
        name: '商品管理',
        description: '商品基本信息、上下架与图片维护。'
      },
      {
        code: PERMISSION_CODES.PRODUCT_CATEGORY,
        name: '分类管理',
        description: '分类树、层级与排序维护。'
      },
      {
        code: PERMISSION_CODES.PRODUCT_SKU,
        name: '规格 / SKU 视图',
        description: '在商品新增和编辑表单内维护规格定义、SKU 明细、价格、库存和启停状态。'
      },
      {
        code: PERMISSION_CODES.PRODUCT_STOCK,
        name: '库存日志',
        description: '补货、库存调整、变更记录与预警闭环。'
      },
      {
        code: PERMISSION_CODES.PRODUCT_WARNING,
        name: '库存预警',
        description: '低库存、紧急补货与出入库记录。'
      }
    ]
  },
  {
    key: 'trade',
    title: '交易与财务',
    description: '订单、退款、售后、支付与发票能力。',
    permissions: [
      {
        code: PERMISSION_CODES.ORDER_MANAGE,
        name: '订单管理后台',
        description: '管理员可按订单号、用户、订单状态和支付状态统一检索订单并查看详情。'
      },
      {
        code: PERMISSION_CODES.ORDER_REFUND,
        name: '退款管理',
        description: '管理员可按退款单号、订单号、用户和状态统一检索退款并查看售后详情。'
      },
      {
        code: PERMISSION_CODES.ORDER_AFTER_SALE,
        name: '售后管理',
        description: '管理员可按售后单号、订单号、用户、类型和状态统一检索售后并查看详情。'
      },
      {
        code: PERMISSION_CODES.PAYMENT_MANAGE,
        name: '支付与结算',
        description: '支付确认、订单结算与退款申请。'
      },
      {
        code: PERMISSION_CODES.INVOICE_MANAGE,
        name: '发票管理',
        description: '发票申请、开票、重发与后台审核。'
      },
      {
        code: PERMISSION_CODES.FINANCIAL_REPORT,
        name: '财务报表',
        description: '日报、月报、年报与导出。'
      }
    ]
  },
  {
    key: 'delivery',
    title: '配送与履约',
    description: '配送方式、区域、配送员和轨迹。',
    permissions: [
      {
        code: PERMISSION_CODES.DELIVERY_MANAGE,
        name: '配送管理',
        description: '配送方式、区域、配送员、分配和异常处理的统一后台权限。'
      },
      {
        code: PERMISSION_CODES.DELIVERY_METHOD,
        name: '配送方式',
        description: '配送方式数据存在，前端已在工作台展示并预留管理入口。'
      },
      {
        code: PERMISSION_CODES.DELIVERY_REGION,
        name: '配送区域',
        description: '区域树、费用、时效和可配送范围。'
      },
      {
        code: PERMISSION_CODES.DELIVERY_STAFF,
        name: '配送员管理',
        description: '配送员资料、状态切换和区域分配。'
      },
      {
        code: PERMISSION_CODES.DELIVERY_TRACKING,
        name: '物流轨迹',
        description: '订单配送详情、时间线和履约动作。'
      }
    ]
  },
  {
    key: 'marketing',
    title: '营销与运营',
    description: '优惠券、促销、评价和运营分析。',
    permissions: [
      {
        code: PERMISSION_CODES.COUPON_MANAGE,
        name: '优惠券管理',
        description: '优惠券发行、状态切换与有效期管理。'
      },
      {
        code: PERMISSION_CODES.PROMOTION_MANAGE,
        name: '促销活动',
        description: '折扣、满减、秒杀等活动维护。'
      },
      {
        code: PERMISSION_CODES.REVIEW_MANAGE,
        name: '评价管理',
        description: '评价总览、商品评论与质量反馈。'
      },
      {
        code: PERMISSION_CODES.DASHBOARD_VIEW,
        name: '运营看板',
        description: '销售、库存、分类和关键指标看板。'
      },
      {
        code: PERMISSION_CODES.OPERATION_ANALYTICS,
        name: '运营分析',
        description: '用户行为、热销商品与趋势分析。'
      }
    ]
  }
])

export const ADMIN_ROUTE_REGISTRY = Object.freeze([
  {
    path: '/admin',
    title: '后台工作台',
    permission: PERMISSION_CODES.ADMIN_WORKBENCH,
    status: 'connected'
  },
  {
    path: '/admin/rbac',
    title: '角色与权限配置',
    permission: PERMISSION_CODES.RBAC_VIEW,
    status: 'connected'
  },
  {
    path: '/user',
    title: '用户中心',
    permission: PERMISSION_CODES.USER_CENTER,
    status: 'connected'
  },
  {
    path: '/user/address',
    title: '地址管理',
    permission: PERMISSION_CODES.USER_ADDRESS,
    status: 'connected'
  },
  {
    path: '/product',
    title: '商品管理',
    permission: PERMISSION_CODES.PRODUCT_LIST,
    status: 'connected'
  },
  {
    path: '/product',
    title: '规格 / SKU 管理',
    permission: PERMISSION_CODES.PRODUCT_SKU,
    status: 'connected'
  },
  {
    path: '/product/stock-warning',
    title: '库存日志',
    permission: PERMISSION_CODES.PRODUCT_STOCK,
    status: 'connected'
  },
  {
    path: '/product/category',
    title: '分类管理',
    permission: PERMISSION_CODES.PRODUCT_CATEGORY,
    status: 'connected'
  },
  {
    path: '/product/stock-warning',
    title: '库存预警',
    permission: PERMISSION_CODES.PRODUCT_WARNING,
    status: 'connected'
  },
  {
    path: '/member',
    title: '会员管理',
    permission: PERMISSION_CODES.MEMBER_MANAGE,
    status: 'connected'
  },
  {
    path: '/order/manage',
    title: '订单管理',
    permission: PERMISSION_CODES.ORDER_MANAGE,
    status: 'connected'
  },
  {
    path: '/payment/refund',
    title: '退款管理',
    permission: PERMISSION_CODES.ORDER_REFUND,
    status: 'connected'
  },
  {
    path: '/payment',
    title: '支付与结算',
    permission: PERMISSION_CODES.PAYMENT_MANAGE,
    status: 'connected'
  },
  {
    path: '/order/after-sale',
    title: '售后管理',
    permission: PERMISSION_CODES.ORDER_AFTER_SALE,
    status: 'connected'
  },
  {
    path: '/member/benefits',
    title: '会员权益',
    permission: PERMISSION_CODES.MEMBER_BENEFIT,
    status: 'connected'
  },
  {
    path: '/coupon/manage',
    title: '优惠券管理',
    permission: PERMISSION_CODES.COUPON_MANAGE,
    status: 'connected'
  },
  {
    path: '/promotion',
    title: '促销活动',
    permission: PERMISSION_CODES.PROMOTION_MANAGE,
    status: 'connected'
  },
  {
    path: '/review/manage',
    title: '评价总览',
    permission: PERMISSION_CODES.REVIEW_MANAGE,
    status: 'connected'
  },
  {
    path: '/delivery/manage',
    title: '配送管理',
    permission: PERMISSION_CODES.DELIVERY_MANAGE,
    status: 'connected'
  },
  {
    path: '/delivery/manage?tab=method',
    title: '配送方式',
    permission: PERMISSION_CODES.DELIVERY_METHOD,
    status: 'connected'
  },
  {
    path: '/delivery/manage?tab=region',
    title: '配送区域',
    permission: PERMISSION_CODES.DELIVERY_REGION,
    status: 'connected'
  },
  {
    path: '/delivery/manage?tab=staff',
    title: '配送员管理',
    permission: PERMISSION_CODES.DELIVERY_STAFF,
    status: 'connected'
  },
  {
    path: '/delivery/tracking',
    title: '物流轨迹',
    permission: PERMISSION_CODES.DELIVERY_TRACKING,
    status: 'connected'
  },
  {
    path: '/financial/report',
    title: '财务报表',
    permission: PERMISSION_CODES.FINANCIAL_REPORT,
    status: 'connected'
  },
  {
    path: '/financial/invoice',
    title: '发票管理',
    permission: PERMISSION_CODES.INVOICE_MANAGE,
    status: 'connected'
  },
  {
    path: '/statistics',
    title: '运营看板',
    permission: PERMISSION_CODES.DASHBOARD_VIEW,
    status: 'connected'
  },
  {
    path: '/analytics',
    title: '用户分析',
    permission: PERMISSION_CODES.USER_ANALYTICS,
    status: 'connected'
  },
  {
    path: '/analytics',
    title: '运营分析',
    permission: PERMISSION_CODES.OPERATION_ANALYTICS,
    status: 'connected'
  }
])

export function normalizeRoles(roles) {
  if (!Array.isArray(roles)) {
    return []
  }
  return roles.filter((role) => typeof role === 'string' && role.length > 0)
}

export function normalizePermissions(permissionCodes) {
  if (!Array.isArray(permissionCodes)) {
    return []
  }
  return [...new Set(permissionCodes.filter((code) => typeof code === 'string' && code.length > 0))]
}

export function hasAnyRole(userRoles, requiredRoles) {
  if (!Array.isArray(requiredRoles) || requiredRoles.length === 0) {
    return true
  }

  const roleSet = new Set(normalizeRoles(userRoles))
  return requiredRoles.some((role) => roleSet.has(role))
}

export function getRolePermissionCodes(roles) {
  const roleList = normalizeRoles(roles)
  const permissionSet = new Set()

  roleList.forEach((role) => {
    const permissionCodes = ROLE_PERMISSION_MAP[role] || []
    permissionCodes.forEach((code) => permissionSet.add(code))
  })

  return [...permissionSet]
}

export function hasAnyPermission(userPermissionSource, requiredPermissions) {
  if (!Array.isArray(requiredPermissions) || requiredPermissions.length === 0) {
    return true
  }

  const providedPermissions = Array.isArray(userPermissionSource)
    ? userPermissionSource
    : userPermissionSource?.permissionCodes || getRolePermissionCodes(userPermissionSource?.roles)

  const permissionSet = new Set(normalizePermissions(providedPermissions))
  return requiredPermissions.some((code) => permissionSet.has(code))
}

export function hasAllPermissions(userPermissionSource, requiredPermissions) {
  if (!Array.isArray(requiredPermissions) || requiredPermissions.length === 0) {
    return true
  }

  const providedPermissions = Array.isArray(userPermissionSource)
    ? userPermissionSource
    : userPermissionSource?.permissionCodes || getRolePermissionCodes(userPermissionSource?.roles)

  const permissionSet = new Set(normalizePermissions(providedPermissions))
  return requiredPermissions.every((code) => permissionSet.has(code))
}

function getStoredUserInfo() {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    window.localStorage.removeItem('userInfo')
    return null
  }
}

export function getStoredRoles() {
  return normalizeRoles(getStoredUserInfo()?.roles)
}

export function getStoredPermissionCodes() {
  const userInfo = getStoredUserInfo()
  return normalizePermissions(userInfo?.permissionCodes || getRolePermissionCodes(userInfo?.roles))
}

export function resolveRoleLabel(roleCode) {
  const roleNameMap = {
    [ROLE_ADMIN]: '管理员',
    [ROLE_USER]: '会员用户',
    [ROLE_DELIVERY]: '配送员'
  }

  return roleNameMap[roleCode] || roleCode
}

export function getPermissionDisplayName(permissionCode) {
  for (const group of ADMIN_PERMISSION_GROUPS) {
    const permission = group.permissions.find((item) => item.code === permissionCode)
    if (permission) {
      return permission.name
    }
  }
  return permissionCode
}
