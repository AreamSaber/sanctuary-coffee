<template>
  <div class="app-page rbac-manage-page">
    <section class="rbac-hero">
      <div class="rbac-hero__content">
        <p class="section-eyebrow">权限配置</p>
        <h1 class="app-page-header__title">角色与权限配置</h1>
        <p class="section-caption">
          按管理员、财务、配送、运营等岗位分配可进入页面和可操作能力，让后台权限跟真实分工保持一致。
        </p>
      </div>

      <div class="app-page-actions rbac-hero__actions">
        <el-button :icon="Back" plain @click="router.push('/admin')">返回工作台</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button :icon="CopyDocument" type="primary" @click="copySnapshot">复制配置摘要</el-button>
      </div>
    </section>

    <section class="summary-grid" aria-label="权限配置概览">
      <article v-for="item in summaryCards" :key="item.key" class="summary-card" :class="`summary-card--${item.tone}`">
        <span class="summary-card__icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span class="summary-card__label">{{ item.label }}</span>
        <strong class="summary-card__value">{{ item.value }}</strong>
        <span class="summary-card__meta">{{ item.meta }}</span>
      </article>
    </section>

    <section class="page-grid page-grid--content">
      <div class="role-grid">
        <article v-for="role in roleCards" :key="role.id" class="role-card">
          <div class="role-card__head">
            <span class="role-card__icon">
              <el-icon><UserFilled /></el-icon>
            </span>
            <div>
              <strong>{{ role.roleName }}</strong>
              <small>{{ role.description || '按业务职责配置后台范围' }}</small>
            </div>
            <el-tag size="small" :type="role.status === 1 ? 'success' : 'info'" effect="plain">
              {{ role.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </div>

          <div class="role-card__count">
            <strong>{{ getRolePermissionCount(role) }}</strong>
            <span>项可用能力</span>
          </div>

          <div class="role-card__scope">
            <span v-for="ability in getRoleAbilityPreview(role)" :key="`${role.id}-${ability}`">{{ ability }}</span>
          </div>

          <div class="role-card__actions">
            <el-button :icon="Setting" size="small" @click="openPermissionAssign(role)">配置能力</el-button>
            <el-button :icon="Edit" size="small" text @click="openRoleDialog(role)">编辑</el-button>
          </div>
        </article>
      </div>

      <div class="rbac-manage-page__two-column">
        <section class="ops-panel">
          <div class="ops-panel__head ops-panel__head--compact">
            <div>
              <p class="section-kicker">我的后台范围</p>
              <h2>当前账号可以处理哪些事</h2>
            </div>
          </div>

          <div class="session-panel">
            <div class="session-panel__row">
              <span class="session-panel__label">当前岗位</span>
              <div class="session-panel__value">
                <el-tag v-for="role in currentRoles" :key="role" type="info" effect="plain">{{ role }}</el-tag>
                <el-tag v-if="currentRoles.length === 0" type="info" effect="plain">未分配岗位</el-tag>
              </div>
            </div>
            <div class="session-panel__row">
              <span class="session-panel__label">可进入页面</span>
              <strong class="session-panel__number">{{ currentAccessibleRoutes.length }}</strong>
            </div>
            <div class="session-panel__row">
              <span class="session-panel__label">覆盖业务模块</span>
              <strong class="session-panel__number">{{ currentAccessibleGroupCount }}</strong>
            </div>
          </div>

          <div class="accessible-list">
            <button
              v-for="routeItem in currentAccessibleRoutes.slice(0, 6)"
              :key="routeItem.permission"
              class="accessible-link"
              type="button"
              @click="router.push(routeItem.path)"
            >
              <span>
                <strong>{{ routeItem.title }}</strong>
                <small>{{ getRouteGroupTitle(routeItem.permission) }}</small>
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>

        <section class="ops-panel">
          <div class="ops-panel__head ops-panel__head--compact">
            <div>
              <p class="section-kicker">业务模块覆盖</p>
              <h2>按场景检查授权范围</h2>
            </div>
          </div>

          <div class="module-access-list">
            <div v-for="module in moduleAccessRows" :key="module.key" class="module-access">
              <div class="module-access__row">
                <strong>{{ module.title }}</strong>
                <span>{{ module.accessible }}/{{ module.total }}</span>
              </div>
              <el-progress :percentage="module.percentage" :stroke-width="6" :show-text="false" />
            </div>
          </div>
        </section>
      </div>

      <section class="ops-panel ops-panel--tabs">
        <el-tabs v-model="activeTab" class="rbac-tabs">
          <el-tab-pane label="角色配置" name="roles">
            <div class="table-toolbar">
              <el-input
                v-model="roleQuery.keyword"
                clearable
                :prefix-icon="Search"
                placeholder="搜索角色或职责"
                @keyup.enter="loadRoles"
              />
              <el-select v-model="roleQuery.status" clearable placeholder="状态" class="status-select">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
              <el-button :icon="Search" :loading="loading" @click="loadRoles">查询</el-button>
              <el-button :icon="Plus" type="primary" @click="openRoleDialog()">新增角色</el-button>
            </div>

            <el-table v-loading="loading" :data="roles" size="small" empty-text="暂无角色数据">
              <el-table-column label="角色名称" min-width="190">
                <template #default="{ row }">
                  <div class="identity-cell">
                    <strong>{{ row.roleName }}</strong>
                    <small>系统标识 {{ row.roleCode }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="职责说明" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">{{ row.description || '暂无职责说明' }}</template>
              </el-table-column>
              <el-table-column label="授权范围" min-width="170">
                <template #default="{ row }">
                  <div class="coverage-cell">
                    <strong>{{ getRoleCoverageText(row) }}</strong>
                    <small>{{ getRoleModuleCount(row) }} 个业务模块</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300" fixed="right">
                <template #default="{ row }">
                  <el-button :icon="Setting" size="small" @click="openPermissionAssign(row)">配置能力</el-button>
                  <el-button :icon="Edit" size="small" @click="openRoleDialog(row)">编辑</el-button>
                  <el-button :icon="Delete" size="small" type="danger" plain @click="handleDeleteRole(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="能力目录" name="permissions">
            <div class="table-toolbar table-toolbar--end">
              <el-button :icon="Plus" type="primary" @click="openPermissionDialog()">新增顶层能力</el-button>
            </div>

            <el-table
              v-loading="loading"
              :data="permissionTree"
              row-key="id"
              size="small"
              default-expand-all
              empty-text="暂无能力数据"
              :tree-props="{ children: 'children' }"
            >
              <el-table-column label="能力名称" min-width="230">
                <template #default="{ row }">
                  <div class="identity-cell">
                    <strong>{{ row.permissionName }}</strong>
                    <small>{{ row.permissionCode }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="getPermissionTypeTag(row.permissionType)" size="small" effect="plain">
                    {{ formatPermissionType(row.permissionType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="页面路径" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="muted-text">{{ row.path || getPermissionRoutePath(row.permissionCode) || '未绑定页面' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="320" fixed="right">
                <template #default="{ row }">
                  <el-button :icon="Plus" size="small" @click="openPermissionDialog(null, row)">新增下级</el-button>
                  <el-button :icon="Edit" size="small" @click="openPermissionDialog(row)">编辑</el-button>
                  <el-button :icon="Delete" size="small" type="danger" plain @click="handleDeletePermission(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="人员分配" name="users">
            <div class="table-toolbar">
              <el-input
                v-model="userQuery.keyword"
                clearable
                :prefix-icon="Search"
                placeholder="搜索姓名、账号、手机或邮箱"
                @keyup.enter="loadUsers"
              />
              <el-select v-model="userQuery.status" clearable placeholder="状态" class="status-select">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
              <el-button :icon="Search" :loading="loading" @click="loadUsers">查询</el-button>
            </div>

            <el-table v-loading="loading" :data="userPage.records" size="small" empty-text="暂无人员数据">
              <el-table-column label="人员" min-width="180">
                <template #default="{ row }">
                  <div class="identity-cell">
                    <strong>{{ row.nickname || row.username }}</strong>
                    <small>{{ row.username }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="联系方式" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="identity-cell identity-cell--muted">
                    <span>{{ row.phone || '未填写手机' }}</span>
                    <small>{{ row.email || '未填写邮箱' }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="岗位角色" min-width="240">
                <template #default="{ row }">
                  <div class="tag-list">
                    <el-tag v-for="roleCode in row.roleCodes" :key="roleCode" size="small" effect="plain">
                      {{ resolveRoleLabel(roleCode) }}
                    </el-tag>
                    <el-tag v-if="!row.roleCodes?.length" size="small" type="info" effect="plain">未分配</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button :icon="Operation" size="small" @click="openUserRoleDialog(row)">调整岗位</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="table-pagination">
              <el-pagination
                v-model:current-page="userQuery.pageNum"
                v-model:page-size="userQuery.pageSize"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                :total="userPage.total"
                @size-change="loadUsers"
                @current-change="loadUsers"
              />
            </div>
          </el-tab-pane>

          <el-tab-pane label="访问矩阵" name="matrix">
            <el-table :data="permissionRows" size="small" empty-text="暂无访问矩阵">
              <el-table-column prop="group" label="业务模块" width="150" />
              <el-table-column label="能力项" min-width="240">
                <template #default="{ row }">
                  <div class="identity-cell">
                    <strong>{{ row.name }}</strong>
                    <small>{{ row.description }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="当前账号" width="120" align="center">
                <template #default="{ row }">
                  <el-tag :type="canAccessPermission(row.code) ? 'success' : 'info'" size="small">
                    {{ canAccessPermission(row.code) ? '可用' : '未授权' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="可进入页面" min-width="180">
                <template #default="{ row }">
                  <button v-if="row.path" class="route-pill" type="button" @click="router.push(row.path)">
                    <span>{{ row.routeTitle || row.name }}</span>
                    <el-icon><ArrowRight /></el-icon>
                  </button>
                  <span v-else class="muted-text">暂未绑定页面</span>
                </template>
              </el-table-column>
              <el-table-column label="系统标识" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="muted-code">{{ row.code }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </section>

    <el-dialog v-model="roleDialogVisible" :title="roleForm.id ? '编辑岗位角色' : '新增岗位角色'" width="520px">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="96px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="例如：财务专员" />
        </el-form-item>
        <el-form-item label="系统标识" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="例如：ROLE_FINANCE" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="roleForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="职责说明">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="描述岗位负责的业务范围和边界" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" :title="permissionForm.id ? '编辑能力' : '新增能力'" width="560px">
      <el-form ref="permissionFormRef" :model="permissionForm" :rules="permissionRules" label-width="96px">
        <el-form-item label="上级能力">
          <el-select v-model="permissionForm.parentId" clearable filterable placeholder="不选则为顶层能力">
            <el-option label="顶层能力" :value="0" />
            <el-option
              v-for="permission in flatPermissionOptions"
              :key="permission.id"
              :label="permission.displayName"
              :value="permission.id"
              :disabled="permission.id === permissionForm.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="能力名称" prop="permissionName">
          <el-input v-model="permissionForm.permissionName" placeholder="例如：退款审核" />
        </el-form-item>
        <el-form-item label="系统标识" prop="permissionCode">
          <el-input v-model="permissionForm.permissionCode" placeholder="例如：order:refund" />
        </el-form-item>
        <el-form-item label="能力类型" prop="permissionType">
          <el-radio-group v-model="permissionForm.permissionType">
            <el-radio-button :label="1">业务模块</el-radio-button>
            <el-radio-button :label="2">页面入口</el-radio-button>
            <el-radio-button :label="3">页面操作</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="页面路径">
          <el-input v-model="permissionForm.path" placeholder="可选，例如：/payment/refund" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="permissionForm.icon" placeholder="可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="permissionForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="permissionForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePermission">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="assignDrawerVisible" :title="`为「${currentRole?.roleName || ''}」选择可用能力`" size="min(560px, 92vw)">
      <div class="drawer-body">
        <div class="drawer-summary">
          <span class="drawer-summary__icon">
            <el-icon><Lock /></el-icon>
          </span>
          <div>
            <strong>{{ currentRole?.roleName || '当前角色' }}</strong>
            <p>保存后，该角色可进入的页面和可操作能力会按勾选结果更新。</p>
          </div>
        </div>
        <el-tree
          ref="permissionTreeRef"
          :data="permissionTree"
          node-key="id"
          show-checkbox
          check-strictly
          default-expand-all
          :props="{ label: 'permissionName', children: 'children' }"
        >
          <template #default="{ data }">
            <span class="permission-tree-node">
              <span>
                <strong>{{ data.permissionName }}</strong>
                <small>{{ data.path || getPermissionRoutePath(data.permissionCode) || formatPermissionType(data.permissionType) }}</small>
              </span>
              <em>{{ data.permissionCode }}</em>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="assignDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRolePermissions">保存配置</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="userRoleDialogVisible" title="调整人员岗位" width="520px">
      <div class="assign-user-panel">
        <div class="assign-user-panel__subject">
          <span class="assign-user-panel__avatar">
            <el-icon><UserFilled /></el-icon>
          </span>
          <div>
            <strong>{{ currentUser?.nickname || currentUser?.username }}</strong>
            <span>{{ currentUser?.username }}</span>
          </div>
        </div>
        <el-checkbox-group v-model="selectedUserRoleIds" class="role-checkbox-grid">
          <el-checkbox v-for="role in roles" :key="role.id" :label="role.id">
            <span class="role-checkbox-label">
              <strong>{{ role.roleName }}</strong>
              <small>{{ role.description || role.roleCode }}</small>
            </span>
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="userRoleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUserRoles">保存岗位</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Back,
  CopyDocument,
  Delete,
  DocumentChecked,
  Edit,
  Finished,
  Lock,
  Menu,
  Operation,
  Plus,
  Refresh,
  Search,
  Setting,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  ADMIN_PERMISSION_GROUPS,
  ADMIN_ROUTE_REGISTRY,
  getRolePermissionCodes,
  hasAnyPermission,
  resolveRoleLabel
} from '@/utils/permission'
import {
  assignRolePermissions,
  assignUserRoles,
  createPermission,
  createRole,
  deletePermission,
  deleteRole,
  getPermissionTree,
  getRbacUsers,
  getRoles,
  updatePermission,
  updateRole
} from '@/api/rbac'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const activeTab = ref('roles')
const roles = ref([])
const permissionTree = ref([])
const userPage = ref({ records: [], total: 0 })
const roleFormRef = ref()
const permissionFormRef = ref()
const permissionTreeRef = ref()
const roleDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const assignDrawerVisible = ref(false)
const userRoleDialogVisible = ref(false)
const currentRole = ref(null)
const currentUser = ref(null)
const selectedUserRoleIds = ref([])

const roleQuery = reactive({
  keyword: '',
  status: null
})

const userQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: null
})

const roleForm = reactive({
  id: null,
  roleName: '',
  roleCode: '',
  description: '',
  status: 1
})

const permissionForm = reactive({
  id: null,
  parentId: 0,
  permissionName: '',
  permissionCode: '',
  permissionType: 2,
  path: '',
  icon: '',
  sortOrder: 0,
  status: 1
})

const roleRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [
    { required: true, message: '请输入系统标识', trigger: 'blur' },
    { pattern: /^[A-Z0-9_:]+$/, message: '仅支持大写字母、数字、冒号和下划线', trigger: 'blur' }
  ]
}

const permissionRules = {
  permissionName: [{ required: true, message: '请输入能力名称', trigger: 'blur' }],
  permissionCode: [
    { required: true, message: '请输入系统标识', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9:_-]+$/, message: '仅支持字母、数字、冒号、下划线和横线', trigger: 'blur' }
  ],
  permissionType: [{ required: true, message: '请选择能力类型', trigger: 'change' }]
}

const currentPermissionCodes = computed(() => (
  userStore.userInfo?.permissionCodes?.length
    ? userStore.userInfo.permissionCodes
    : getRolePermissionCodes(userStore.roles)
))

const currentRoles = computed(() => (userStore.roles || []).map(resolveRoleLabel))

const connectedRoutes = computed(() => ADMIN_ROUTE_REGISTRY.filter((item) => item.status === 'connected'))

const routeRegistryMap = computed(() => new Map(
  ADMIN_ROUTE_REGISTRY.map((route) => [route.permission, route])
))

const currentAccessibleRoutes = computed(() => connectedRoutes.value.filter((item) => (
  hasAnyPermission(currentPermissionCodes.value, [item.permission])
)))

const permissionCatalogMap = computed(() => new Map(
  ADMIN_PERMISSION_GROUPS.flatMap((group) => group.permissions.map((permission) => ([
    permission.code,
    {
      ...permission,
      groupKey: group.key,
      groupTitle: group.title,
      route: routeRegistryMap.value.get(permission.code)
    }
  ])))
))

const moduleAccessRows = computed(() => ADMIN_PERMISSION_GROUPS.map((group) => {
  const accessible = group.permissions.filter((permission) => canAccessPermission(permission.code)).length
  const connected = group.permissions.filter((permission) => routeRegistryMap.value.get(permission.code)?.path).length
  const total = group.permissions.length

  return {
    ...group,
    accessible,
    connected,
    total,
    percentage: total ? Math.round((accessible / total) * 100) : 0
  }
}))

const currentAccessibleGroupCount = computed(() => (
  moduleAccessRows.value.filter((module) => module.accessible > 0).length
))

const summaryCards = computed(() => [
  {
    key: 'roles',
    label: '岗位角色',
    value: roles.value.length,
    meta: '按职责分配后台范围',
    icon: UserFilled,
    tone: 'people'
  },
  {
    key: 'abilities',
    label: '能力目录',
    value: flatPermissionOptions.value.length,
    meta: `${connectedRoutes.value.length} 个页面入口已接通`,
    icon: Menu,
    tone: 'ability'
  },
  {
    key: 'members',
    label: '后台人员',
    value: userPage.value.total || userPage.value.records.length,
    meta: '支持按岗位分工',
    icon: DocumentChecked,
    tone: 'members'
  },
  {
    key: 'scope',
    label: '我的可进入页',
    value: currentAccessibleRoutes.value.length,
    meta: `覆盖 ${currentAccessibleGroupCount.value} 个业务模块`,
    icon: Finished,
    tone: 'scope'
  }
])

const roleCards = computed(() => roles.value.slice(0, 4))

const permissionRows = computed(() => (
  ADMIN_PERMISSION_GROUPS.flatMap((group) => group.permissions.map((permission) => {
    const route = routeRegistryMap.value.get(permission.code)
    return {
      group: group.title,
      groupKey: group.key,
      ...permission,
      path: route?.path || '',
      routeTitle: route?.title || ''
    }
  }))
))

const flatPermissionOptions = computed(() => flattenPermissions(permissionTree.value))

const permissionById = computed(() => new Map(
  flatPermissionOptions.value.map((permission) => [permission.id, permission])
))

const permissionIdCatalogMap = computed(() => new Map(
  flatPermissionOptions.value.map((permission) => [
    permission.id,
    permissionCatalogMap.value.get(permission.permissionCode)
  ])
))

onMounted(() => {
  loadAll()
})

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadRoles(), loadPermissionTree(), loadUsers()])
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await getRoles({
    keyword: roleQuery.keyword || undefined,
    status: roleQuery.status ?? undefined
  })
  roles.value = res.data || []
}

async function loadPermissionTree() {
  const res = await getPermissionTree()
  permissionTree.value = res.data || []
}

async function loadUsers() {
  const res = await getRbacUsers({
    pageNum: userQuery.pageNum,
    pageSize: userQuery.pageSize,
    keyword: userQuery.keyword || undefined,
    status: userQuery.status ?? undefined
  })
  userPage.value = {
    records: res.data?.records || [],
    total: res.data?.total || 0
  }
}

function openRoleDialog(row) {
  Object.assign(roleForm, {
    id: row?.id || null,
    roleName: row?.roleName || '',
    roleCode: row?.roleCode || '',
    description: row?.description || '',
    status: row?.status ?? 1
  })
  roleDialogVisible.value = true
  nextTick(() => roleFormRef.value?.clearValidate())
}

async function saveRole() {
  await roleFormRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      roleName: roleForm.roleName,
      roleCode: roleForm.roleCode,
      description: roleForm.description,
      status: roleForm.status
    }
    if (roleForm.id) {
      await updateRole(roleForm.id, payload)
    } else {
      await createRole(payload)
    }
    ElMessage.success('角色已保存')
    roleDialogVisible.value = false
    await loadRoles()
  } finally {
    saving.value = false
  }
}

async function handleDeleteRole(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '删除角色', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('角色已删除')
  await Promise.all([loadRoles(), loadUsers()])
}

function openPermissionDialog(row, parent) {
  Object.assign(permissionForm, {
    id: row?.id || null,
    parentId: parent?.id ?? row?.parentId ?? 0,
    permissionName: row?.permissionName || '',
    permissionCode: row?.permissionCode || '',
    permissionType: row?.permissionType || 2,
    path: row?.path || '',
    icon: row?.icon || '',
    sortOrder: row?.sortOrder || 0,
    status: row?.status ?? 1
  })
  permissionDialogVisible.value = true
  nextTick(() => permissionFormRef.value?.clearValidate())
}

async function savePermission() {
  await permissionFormRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      parentId: permissionForm.parentId || 0,
      permissionName: permissionForm.permissionName,
      permissionCode: permissionForm.permissionCode,
      permissionType: permissionForm.permissionType,
      path: permissionForm.path,
      icon: permissionForm.icon,
      sortOrder: permissionForm.sortOrder,
      status: permissionForm.status
    }
    if (permissionForm.id) {
      await updatePermission(permissionForm.id, payload)
    } else {
      await createPermission(payload)
    }
    ElMessage.success('能力已保存')
    permissionDialogVisible.value = false
    await Promise.all([loadPermissionTree(), loadRoles()])
  } finally {
    saving.value = false
  }
}

async function handleDeletePermission(row) {
  await ElMessageBox.confirm(`确认删除能力「${row.permissionName}」及其下级吗？`, '删除能力', { type: 'warning' })
  await deletePermission(row.id)
  ElMessage.success('能力已删除')
  await Promise.all([loadPermissionTree(), loadRoles()])
}

async function openPermissionAssign(row) {
  currentRole.value = row
  assignDrawerVisible.value = true
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(row.permissionIds || [])
}

async function saveRolePermissions() {
  if (!currentRole.value) {
    return
  }
  saving.value = true
  try {
    const permissionIds = permissionTreeRef.value?.getCheckedKeys() || []
    await assignRolePermissions(currentRole.value.id, permissionIds)
    ElMessage.success('角色能力已更新')
    assignDrawerVisible.value = false
    await Promise.all([loadRoles(), userStore.getUserInfo()])
  } finally {
    saving.value = false
  }
}

function openUserRoleDialog(row) {
  currentUser.value = row
  selectedUserRoleIds.value = [...(row.roleIds || [])]
  userRoleDialogVisible.value = true
}

async function saveUserRoles() {
  if (!currentUser.value) {
    return
  }
  saving.value = true
  try {
    await assignUserRoles(currentUser.value.id, selectedUserRoleIds.value)
    ElMessage.success('人员岗位已更新')
    userRoleDialogVisible.value = false
    await loadUsers()
    if (currentUser.value.id === userStore.userInfo?.id) {
      await userStore.getUserInfo()
    }
  } finally {
    saving.value = false
  }
}

function getRolePermissionIds(role) {
  return Array.isArray(role?.permissionIds) ? role.permissionIds : []
}

function getRolePermissionCount(role) {
  return getRolePermissionIds(role).length
}

function getRoleModuleCount(role) {
  const moduleSet = new Set(
    getRolePermissionIds(role)
      .map((permissionId) => permissionIdCatalogMap.value.get(permissionId)?.groupTitle)
      .filter(Boolean)
  )
  return moduleSet.size
}

function getRoleCoverageText(role) {
  const permissionCount = getRolePermissionCount(role)
  if (!permissionCount) {
    return '尚未配置能力'
  }
  return `${permissionCount} 项能力`
}

function getRoleAbilityPreview(role) {
  const names = getRolePermissionIds(role)
    .map((permissionId) => permissionById.value.get(permissionId)?.permissionName)
    .filter(Boolean)

  return names.length ? names.slice(0, 4) : ['未配置具体能力']
}

function canAccessPermission(code) {
  return hasAnyPermission(currentPermissionCodes.value, [code])
}

function getRouteGroupTitle(permissionCode) {
  return permissionCatalogMap.value.get(permissionCode)?.groupTitle || '后台页面'
}

function getPermissionRoutePath(permissionCode) {
  return routeRegistryMap.value.get(permissionCode)?.path || ''
}

function formatPermissionType(type) {
  const typeMap = {
    1: '业务模块',
    2: '页面入口',
    3: '页面操作'
  }
  return typeMap[type] || '能力项'
}

function getPermissionTypeTag(type) {
  const typeMap = {
    1: 'info',
    2: 'success',
    3: 'warning'
  }
  return typeMap[type] || 'info'
}

function flattenPermissions(nodes, depth = 0) {
  return nodes.flatMap((node) => [
    {
      ...node,
      depth,
      displayName: `${'　'.repeat(depth)}${node.permissionName}`
    },
    ...flattenPermissions(node.children || [], depth + 1)
  ])
}

async function copySnapshot() {
  const snapshot = {
    roleLabels: currentRoles.value,
    accessiblePages: currentAccessibleRoutes.value.map((route) => ({
      title: route.title,
      path: route.path,
      module: getRouteGroupTitle(route.permission)
    })),
    moduleCoverage: moduleAccessRows.value.map((module) => ({
      title: module.title,
      accessible: module.accessible,
      total: module.total
    })),
    permissionCodes: currentPermissionCodes.value
  }

  try {
    await navigator.clipboard.writeText(JSON.stringify(snapshot, null, 2))
    ElMessage.success('配置摘要已复制')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制浏览器内容')
  }
}
</script>

<style scoped>
.rbac-manage-page {
  --panel-bg: rgba(255, 250, 246, 0.94);
  --panel-border: rgba(107, 101, 91, 0.16);
  --panel-soft: rgba(255, 255, 255, 0.54);
  --panel-strong: rgba(255, 250, 246, 0.98);
  --panel-hover-border: rgba(107, 101, 91, 0.34);
  color: var(--color-text);
}

.rbac-manage-page .page-grid--content {
  grid-template-columns: minmax(0, 1fr);
}

.rbac-manage-page h1,
.rbac-manage-page h2,
.rbac-manage-page h3,
.rbac-manage-page strong {
  color: var(--color-text);
}

.rbac-manage-page .app-page-header__title {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 3.5rem);
  font-weight: 500;
  line-height: 0.95;
  letter-spacing: -0.03em;
}

.rbac-manage-page h2 {
  font-family: var(--font-serif);
  font-size: clamp(1.35rem, 2.2vw, 2rem);
  font-weight: 500;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.rbac-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
}

.rbac-hero__content {
  min-width: 0;
}

.rbac-hero__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.section-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  font-size: 0.78rem;
  font-weight: var(--font-semibold);
  letter-spacing: 0;
  color: var(--color-text-muted);
}

.section-caption {
  max-width: 760px;
  margin: 12px 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.summary-grid,
.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.summary-card,
.role-card,
.ops-panel {
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-bg);
}

.summary-card {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 8px 12px;
  align-items: center;
  padding: 18px;
}

.summary-card__icon,
.role-card__icon,
.drawer-summary__icon,
.assign-user-panel__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: var(--color-primary-50);
  color: var(--color-primary-dark);
}

.summary-card--people .summary-card__icon {
  background: rgba(101, 125, 147, 0.14);
  color: var(--color-info);
}

.summary-card--ability .summary-card__icon {
  background: var(--color-success-light);
  color: var(--color-success);
}

.summary-card--members .summary-card__icon {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.summary-card--scope .summary-card__icon {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

.summary-card__label {
  color: var(--color-text-muted);
  font-size: 0.86rem;
}

.summary-card__value {
  grid-column: 2;
  font-size: 1.85rem;
  line-height: 1.1;
}

.summary-card__meta {
  grid-column: 2;
  color: var(--color-text-muted);
}

.role-card {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.role-card:hover,
.summary-card:hover,
.ops-panel:hover {
  border-color: var(--panel-hover-border);
}

.role-card__head {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
}

.role-card__head div,
.identity-cell,
.coverage-cell,
.role-checkbox-label {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.role-card__head small,
.role-card__count span,
.role-card__scope span,
.summary-card__meta,
.identity-cell small,
.coverage-cell small,
.muted-text,
.muted-code,
.role-checkbox-label small,
.permission-tree-node small,
.permission-tree-node em,
.drawer-summary p,
.assign-user-panel__subject span {
  color: var(--color-text-muted);
}

.role-card__count {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.role-card__count strong {
  font-size: 1.8rem;
}

.role-card__scope,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-card__scope span {
  padding: 5px 9px;
  border-radius: 999px;
  background: var(--panel-soft);
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.role-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rbac-manage-page__two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.ops-panel {
  padding: 20px;
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

.ops-panel h2 {
  margin: 0;
}

.session-panel {
  display: grid;
  gap: 13px;
}

.session-panel__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-divider);
}

.session-panel__row:last-child {
  border-bottom: 0;
}

.session-panel__label {
  color: var(--color-text-muted);
}

.session-panel__value {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.session-panel__number {
  font-size: 1.45rem;
}

.accessible-list,
.module-access-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.accessible-link,
.route-pill {
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
  color: var(--color-text);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.accessible-link {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16px;
  align-items: center;
  gap: 12px;
  padding: 12px;
  text-align: left;
}

.accessible-link span {
  display: grid;
  gap: 4px;
}

.accessible-link small {
  color: var(--color-text-muted);
}

.accessible-link:hover,
.route-pill:hover {
  border-color: var(--panel-hover-border);
  background: rgba(255, 255, 255, 0.68);
  color: var(--color-text);
  transform: translateY(-1px);
}

.module-access {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
}

.module-access__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.module-access__row span {
  color: var(--color-text-muted);
  font-weight: var(--font-semibold);
  white-space: nowrap;
}

.ops-panel--tabs {
  min-width: 0;
  padding: 22px;
}

.rbac-tabs:deep(.el-tabs__header) {
  margin-bottom: 18px;
  border-bottom: 1px solid var(--panel-border);
}

.rbac-tabs:deep(.el-tabs__item) {
  height: 42px;
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rbac-tabs:deep(.el-tabs__item.is-active) {
  color: var(--color-primary);
}

.rbac-tabs:deep(.el-tabs__active-bar) {
  height: 2px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
}

.table-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.table-toolbar .el-input {
  max-width: 320px;
}

.table-toolbar--end {
  justify-content: flex-end;
}

.status-select {
  width: 128px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.identity-cell strong,
.identity-cell span,
.coverage-cell strong,
.role-checkbox-label strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-cell--muted span {
  color: var(--color-text-secondary);
}

.rbac-manage-page :deep(.el-button) {
  min-height: 42px;
  border-radius: var(--radius-full);
  letter-spacing: 0.04em;
}

.rbac-manage-page :deep(.el-button--primary) {
  background: linear-gradient(135deg, #6b655b 0%, #948877 100%);
  box-shadow: 0 18px 36px rgba(66, 57, 49, 0.22);
}

.rbac-manage-page :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5d574f 0%, #857b6d 100%);
}

.rbac-manage-page :deep(.el-button:not(.el-button--primary)) {
  border-color: rgba(107, 101, 91, 0.14);
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text);
}

.rbac-manage-page :deep(.el-input__wrapper),
.rbac-manage-page :deep(.el-select__wrapper),
.rbac-manage-page :deep(.el-textarea__inner) {
  border-radius: 8px !important;
  border-color: var(--panel-border);
  background: rgba(255, 255, 255, 0.72);
}

.rbac-manage-page :deep(.el-table) {
  --el-table-bg-color: var(--panel-strong);
  --el-table-tr-bg-color: var(--panel-strong);
  --el-table-header-bg-color: rgba(107, 101, 91, 0.06);
  --el-table-header-text-color: var(--color-text-muted);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.58);
  border: 1px solid var(--panel-border);
  border-radius: 8px;
}

.rbac-manage-page :deep(.el-table th.el-table__cell) {
  background: rgba(107, 101, 91, 0.06);
  color: var(--color-text-muted);
  font-size: 0.72rem;
  font-weight: var(--font-bold);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.rbac-manage-page :deep(.el-table td.el-table__cell) {
  background: var(--panel-strong);
  color: var(--color-text);
}

.rbac-manage-page :deep(.el-table .el-table__row:hover > td.el-table__cell) {
  background: rgba(255, 255, 255, 0.58) !important;
}

.rbac-manage-page :deep(.el-tag) {
  border-radius: 999px;
  font-weight: 700;
}

.rbac-manage-page :deep(.el-tag--success) {
  --el-tag-bg-color: var(--color-success-light);
  --el-tag-border-color: rgba(77, 143, 115, 0.18);
  --el-tag-text-color: var(--color-success);
}

.rbac-manage-page :deep(.el-tag--info) {
  --el-tag-bg-color: var(--color-info-light);
  --el-tag-border-color: rgba(101, 125, 147, 0.18);
  --el-tag-text-color: var(--color-info);
}

.rbac-manage-page :deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.72);
}

.rbac-manage-page :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
}

.route-pill {
  display: inline-grid;
  grid-template-columns: minmax(0, 1fr) 14px;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 7px 10px;
  text-align: left;
}

.route-pill span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-code {
  font-size: 0.82rem;
}

.drawer-body {
  display: grid;
  gap: 16px;
}

.drawer-summary {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
}

.drawer-summary p {
  margin: 4px 0 0;
  line-height: 1.6;
}

.permission-tree-node {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.permission-tree-node span {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.permission-tree-node strong,
.permission-tree-node small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-tree-node em {
  max-width: 160px;
  overflow: hidden;
  font-size: 0.75rem;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assign-user-panel {
  display: grid;
  gap: 18px;
}

.assign-user-panel__subject {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--panel-border);
  border-radius: 8px;
  background: var(--panel-soft);
}

.assign-user-panel__subject div {
  display: grid;
  gap: 3px;
}

.role-checkbox-grid {
  display: grid;
  gap: 10px;
}

.role-checkbox-grid:deep(.el-checkbox) {
  align-items: flex-start;
  height: auto;
  min-height: 44px;
  margin-right: 0;
  padding: 10px 0;
}

@media (max-width: 960px) {
  .rbac-hero,
  .ops-panel__head {
    flex-direction: column;
  }

  .rbac-hero__actions {
    justify-content: flex-start;
  }

  .rbac-manage-page__two-column {
    grid-template-columns: 1fr;
  }

  .table-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .table-toolbar .el-input,
  .status-select {
    width: 100%;
    max-width: none;
  }
}
</style>
