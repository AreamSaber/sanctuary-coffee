<template>
  <div class="app-page delivery-manage-page">
    <section class="app-page-header">
      <div>
        <h1 class="app-page-header__title">配送管理</h1>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="delivery-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="配送方式" name="method">
        <el-card class="app-panel-card">
          <template #header>
            <div class="card-header">
              <div class="card-header__copy">
                <strong>配送方式管理</strong>
                <span>维护前台可选配送方式、基础运费和免邮门槛。</span>
              </div>
              <el-button type="primary" @click="openMethodDialog()">新增配送方式</el-button>
            </div>
          </template>

          <div class="app-toolbar delivery-toolbar">
            <el-input
              v-model="methodSearch.keyword"
              clearable
              placeholder="按名称、说明搜索"
              @keyup.enter="loadMethodList"
            />
            <el-select v-model="methodSearch.status" clearable placeholder="状态筛选">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadMethodList">查询</el-button>
          </div>

          <div class="app-table-shell">
            <el-table :data="methodList" border v-loading="methodLoading">
              <el-table-column prop="methodName" label="配送方式" min-width="160" />
              <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
              <el-table-column prop="freight" label="基础运费" width="120">
                <template #default="{ row }">¥{{ formatMoney(row.freight) }}</template>
              </el-table-column>
              <el-table-column prop="freeThreshold" label="免邮门槛" width="120">
                <template #default="{ row }">¥{{ formatMoney(row.freeThreshold) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="(value) => handleMethodStatusChange(row, value)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" text type="primary" @click="openMethodDialog(row)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteMethod(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="app-pagination">
            <el-pagination
              v-model:current-page="methodPagination.pageNum"
              v-model:page-size="methodPagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="methodPagination.total"
              layout="total, sizes, prev, pager, next"
              @current-change="loadMethodList"
              @size-change="handleMethodPageSizeChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="配送区域" name="region">
        <el-card class="app-panel-card">
          <template #header>
            <div class="card-header">
              <strong>配送区域管理</strong>
              <el-button type="primary" @click="openRegionDialog()">新增区域</el-button>
            </div>
          </template>

          <div class="app-table-shell">
            <el-table
              :data="regionTreeData"
              row-key="id"
              border
              v-loading="regionLoading"
              :tree-props="{ children: 'children' }"
            >
              <el-table-column prop="regionName" label="区域名称" min-width="180" />
              <el-table-column prop="regionCode" label="区域编码" width="140" />
              <el-table-column prop="level" label="层级" width="100">
                <template #default="{ row }">
                  {{ getLevelName(row.level) }}
                </template>
              </el-table-column>
              <el-table-column prop="deliveryFee" label="配送费" width="120">
                <template #default="{ row }">¥{{ row.deliveryFee || 0 }}</template>
              </el-table-column>
              <el-table-column prop="minOrderAmount" label="起送价" width="120">
                <template #default="{ row }">¥{{ row.minOrderAmount || 0 }}</template>
              </el-table-column>
              <el-table-column prop="estimatedTime" label="预计时长" width="120">
                <template #default="{ row }">{{ row.estimatedTime || 0 }} 分钟</template>
              </el-table-column>
              <el-table-column prop="staffCount" label="配送员" width="100" />
              <el-table-column label="启用" width="100">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.status"
                    :active-value="1"
                    :inactive-value="0"
                    @change="(value) => handleRegionStatusChange(row, value)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" text type="primary" @click="openRegionDialog(row)">编辑</el-button>
                  <el-button size="small" text type="danger" @click="handleDeleteRegion(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="配送员" name="staff">
        <el-card class="app-panel-card">
          <template #header>
            <div class="card-header">
              <strong>配送员管理</strong>
              <el-button type="primary" @click="openStaffDialog()">新增配送员</el-button>
            </div>
          </template>

          <div class="app-toolbar delivery-toolbar">
            <el-input
              v-model="staffSearch.keyword"
              clearable
              placeholder="按姓名、手机号、编号搜索"
              @keyup.enter="loadStaffList"
            />
            <el-select v-model="staffSearch.status" clearable placeholder="状态筛选">
              <el-option label="空闲" value="IDLE" />
              <el-option label="配送中" value="BUSY" />
              <el-option label="离线" value="OFFLINE" />
              <el-option label="休息" value="REST" />
            </el-select>
            <el-button type="primary" @click="loadStaffList">查询</el-button>
          </div>

          <div class="app-table-shell">
            <el-table :data="staffList" border v-loading="staffLoading">
              <el-table-column prop="staffCode" label="编号" width="180" />
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="phone" label="手机号" width="140" />
              <el-table-column prop="regionName" label="负责区域" min-width="180">
                <template #default="{ row }">
                  {{ row.regionName || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="statusDesc" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStaffStatusType(row.status)">
                    {{ row.statusDesc || row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="currentOrders" label="当前单量" width="100" />
              <el-table-column prop="todayOrders" label="今日单量" width="100" />
              <el-table-column prop="totalOrders" label="累计单量" width="100" />
              <el-table-column prop="rating" label="评分" width="100" />
              <el-table-column prop="vehicleType" label="交通工具" width="120">
                <template #default="{ row }">
                  {{ formatVehicleType(row.vehicleType) }}
                </template>
              </el-table-column>
              <el-table-column label="启用" width="100">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.enabled"
                    :active-value="1"
                    :inactive-value="0"
                    @change="(value) => handleStaffEnableChange(row, value)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" text type="primary" @click="openStaffDialog(row)">编辑</el-button>
                  <el-button size="small" text type="info" @click="openAssignDialog(row)">分配区域</el-button>
                  <el-dropdown @command="(status) => handleStaffStatusChange(row, status)">
                    <el-button size="small" text type="warning">切换状态</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="IDLE">空闲</el-dropdown-item>
                        <el-dropdown-item command="BUSY">配送中</el-dropdown-item>
                        <el-dropdown-item command="OFFLINE">离线</el-dropdown-item>
                        <el-dropdown-item command="REST">休息</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <el-button size="small" text type="danger" @click="handleDeleteStaff(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="app-pagination">
            <el-pagination
              v-model:current-page="staffPagination.pageNum"
              v-model:page-size="staffPagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="staffPagination.total"
              layout="total, sizes, prev, pager, next"
              @current-change="loadStaffList"
              @size-change="handleStaffPageSizeChange"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="methodDialogVisible"
      :title="methodForm.id ? '编辑配送方式' : '新增配送方式'"
      width="min(560px, 92vw)"
      @closed="resetMethodForm"
    >
      <el-form ref="methodFormRef" :model="methodForm" :rules="methodRules" label-width="100px">
        <el-form-item label="方式名称" prop="methodName">
          <el-input v-model="methodForm.methodName" placeholder="例如：标准配送" />
        </el-form-item>
        <el-form-item label="基础运费">
          <el-input-number v-model="methodForm.freight" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="免邮门槛">
          <el-input-number v-model="methodForm.freeThreshold" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="methodForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="methodForm.description" type="textarea" :rows="3" placeholder="配送时效、适用范围或自提说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="methodDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMethod">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="regionDialogVisible"
      :title="regionForm.id ? '编辑区域' : '新增区域'"
      width="min(640px, 92vw)"
      @closed="resetRegionForm"
    >
      <el-form ref="regionFormRef" :model="regionForm" :rules="regionRules" label-width="100px">
        <el-form-item label="区域名称" prop="regionName">
          <el-input v-model="regionForm.regionName" />
        </el-form-item>
        <el-form-item label="区域编码" prop="regionCode">
          <el-input v-model="regionForm.regionCode" />
        </el-form-item>
        <el-form-item label="上级区域">
          <el-cascader
            v-model="regionForm.parentId"
            :options="regionTreeData"
            :props="regionCascaderProps"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="层级" prop="level">
          <el-select v-model="regionForm.level" style="width: 100%">
            <el-option label="省/直辖市" :value="1" />
            <el-option label="城市" :value="2" />
            <el-option label="区/县" :value="3" />
            <el-option label="街道/站点" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="配送费">
          <el-input-number v-model="regionForm.deliveryFee" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="起送价">
          <el-input-number v-model="regionForm.minOrderAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="预计时长">
          <el-input-number v-model="regionForm.estimatedTime" :min="10" :max="180" />
        </el-form-item>
        <el-form-item label="配送范围">
          <el-input-number v-model="regionForm.deliveryRange" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="regionForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRegion">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="staffDialogVisible"
      :title="staffForm.id ? '编辑配送员' : '新增配送员'"
      width="min(640px, 92vw)"
      @closed="resetStaffForm"
    >
      <el-form ref="staffFormRef" :model="staffForm" :rules="staffRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="staffForm.name" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input-number v-model="staffForm.userId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="staffForm.phone" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idNumber">
          <el-input v-model="staffForm.idNumber" />
        </el-form-item>
        <el-form-item label="负责区域">
          <el-cascader
            v-model="staffForm.regionId"
            :options="regionTreeData"
            :props="regionCascaderProps"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="交通工具">
          <el-select v-model="staffForm.vehicleType" style="width: 100%">
            <el-option label="自行车" value="BIKE" />
            <el-option label="电动车" value="EBIKE" />
            <el-option label="摩托车" value="MOTORCYCLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌号">
          <el-input v-model="staffForm.vehicleNumber" />
        </el-form-item>
        <el-form-item label="健康证号">
          <el-input v-model="staffForm.healthCertNo" />
        </el-form-item>
        <el-form-item label="健康证到期">
          <el-date-picker
            v-model="staffForm.healthCertExpiry"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="staffDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStaff">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配配送区域" width="min(520px, 92vw)">
      <el-form label-width="100px">
        <el-form-item label="配送员">
          <el-input :model-value="assignTarget?.name || ''" disabled />
        </el-form-item>
        <el-form-item label="目标区域">
          <el-cascader
            v-model="assignRegionId"
            :options="regionTreeData"
            :props="regionCascaderProps"
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignRegion">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignStaffToRegion,
  createMethod,
  createRegion,
  createStaff,
  deleteMethod,
  deleteRegion,
  deleteStaff,
  getMethodPage,
  getRegionTree,
  getStaffPage,
  updateMethod,
  updateRegion,
  updateStaff,
  updateStaffStatus
} from '@/api/delivery'

const route = useRoute()
const router = useRouter()
const deliveryTabs = new Set(['method', 'region', 'staff'])
const resolveRouteTab = () => (deliveryTabs.has(route.query.tab) ? route.query.tab : 'method')

const activeTab = ref(resolveRouteTab())

const methodList = ref([])
const methodLoading = ref(false)
const methodDialogVisible = ref(false)
const methodFormRef = ref()
const methodSearch = reactive({
  keyword: '',
  status: ''
})
const methodPagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const methodForm = reactive({
  id: null,
  methodName: '',
  description: '',
  freight: 0,
  freeThreshold: 0,
  status: 1
})
const methodRules = {
  methodName: [{ required: true, message: '请输入配送方式名称', trigger: 'blur' }]
}

const regionTreeData = ref([])
const regionLoading = ref(false)
const regionDialogVisible = ref(false)
const regionFormRef = ref()
const regionCascaderProps = {
  value: 'id',
  label: 'regionName',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

const regionForm = reactive({
  id: null,
  regionName: '',
  regionCode: '',
  parentId: 0,
  level: 1,
  deliveryFee: 0,
  minOrderAmount: 0,
  estimatedTime: 30,
  deliveryRange: 5,
  status: 1
})

const regionRules = {
  regionName: [{ required: true, message: '请输入区域名称', trigger: 'blur' }],
  regionCode: [{ required: true, message: '请输入区域编码', trigger: 'blur' }],
  level: [{ required: true, message: '请选择区域层级', trigger: 'change' }]
}

const staffList = ref([])
const staffLoading = ref(false)
const staffDialogVisible = ref(false)
const staffFormRef = ref()
const staffSearch = reactive({
  keyword: '',
  status: ''
})
const staffPagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const staffForm = reactive({
  id: null,
  userId: null,
  name: '',
  phone: '',
  idNumber: '',
  regionId: null,
  vehicleType: 'EBIKE',
  vehicleNumber: '',
  healthCertNo: '',
  healthCertExpiry: '',
  status: 'OFFLINE',
  enabled: 1
})

const staffRules = {
  userId: [{ required: true, message: '请输入关联用户ID', trigger: 'change' }],
  name: [{ required: true, message: '请输入配送员姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  idNumber: [{ required: true, message: '请输入身份证号', trigger: 'blur' }]
}

const assignDialogVisible = ref(false)
const assignTarget = ref(null)
const assignRegionId = ref(null)

onMounted(async () => {
  await loadMethodList()
  await loadRegionTree()
  await loadStaffList()
})

watch(() => route.query.tab, () => {
  activeTab.value = resolveRouteTab()
})

const handleTabChange = (tabName) => {
  if (!deliveryTabs.has(tabName)) {
    return
  }
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab: tabName
    }
  })
}

const loadMethodList = async () => {
  methodLoading.value = true
  try {
    const res = await getMethodPage({
      pageNum: methodPagination.pageNum,
      pageSize: methodPagination.pageSize,
      keyword: methodSearch.keyword || undefined,
      status: methodSearch.status === '' ? undefined : methodSearch.status
    })
    methodList.value = res.data.records || []
    methodPagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('配送方式加载失败')
  } finally {
    methodLoading.value = false
  }
}

const handleMethodPageSizeChange = async () => {
  methodPagination.pageNum = 1
  await loadMethodList()
}

const loadRegionTree = async () => {
  regionLoading.value = true
  try {
    const res = await getRegionTree()
    regionTreeData.value = res.data || []
  } catch (error) {
    ElMessage.error('配送区域加载失败')
  } finally {
    regionLoading.value = false
  }
}

const loadStaffList = async () => {
  staffLoading.value = true
  try {
    const res = await getStaffPage({
      pageNum: staffPagination.pageNum,
      pageSize: staffPagination.pageSize,
      keyword: staffSearch.keyword || undefined,
      status: staffSearch.status || undefined
    })
    staffList.value = res.data.records || []
    staffPagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('配送员列表加载失败')
  } finally {
    staffLoading.value = false
  }
}

const handleStaffPageSizeChange = async () => {
  staffPagination.pageNum = 1
  await loadStaffList()
}

const openMethodDialog = (row = null) => {
  resetMethodForm()
  if (row) {
    Object.assign(methodForm, {
      id: row.id,
      methodName: row.methodName,
      description: row.description || '',
      freight: Number(row.freight || 0),
      freeThreshold: Number(row.freeThreshold || 0),
      status: row.status ?? 1
    })
  }
  methodDialogVisible.value = true
}

const submitMethod = async () => {
  await methodFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (methodForm.id) {
        await updateMethod(buildMethodPayload(methodForm))
        ElMessage.success('配送方式更新成功')
      } else {
        await createMethod(buildMethodPayload(methodForm))
        ElMessage.success('配送方式创建成功')
      }
      methodDialogVisible.value = false
      await loadMethodList()
    } catch (error) {
      ElMessage.error(error.message || '配送方式保存失败')
    }
  })
}

const handleDeleteMethod = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除配送方式“${row.methodName}”吗？`, '提示', { type: 'warning' })
    await deleteMethod(row.id)
    ElMessage.success('配送方式已删除')
    await loadMethodList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '配送方式删除失败')
    }
  }
}

const handleMethodStatusChange = async (row, status) => {
  try {
    await updateMethod(buildMethodPayload({ ...row, status }))
    ElMessage.success('配送方式状态已更新')
    await loadMethodList()
  } catch (error) {
    ElMessage.error('配送方式状态更新失败')
    await loadMethodList()
  }
}

const openRegionDialog = (row = null) => {
  resetRegionForm()
  if (row) {
    Object.assign(regionForm, {
      id: row.id,
      regionName: row.regionName,
      regionCode: row.regionCode,
      parentId: row.parentId ?? 0,
      level: row.level,
      deliveryFee: Number(row.deliveryFee || 0),
      minOrderAmount: Number(row.minOrderAmount || 0),
      estimatedTime: row.estimatedTime || 30,
      deliveryRange: Number(row.deliveryRange || 5),
      status: row.status ?? 1
    })
  }
  regionDialogVisible.value = true
}

const submitRegion = async () => {
  await regionFormRef.value.validate(async (valid) => {
    if (!valid) return

    const payload = {
      ...regionForm,
      parentId: regionForm.parentId ?? 0
    }

    try {
      if (regionForm.id) {
        await updateRegion(payload)
        ElMessage.success('区域更新成功')
      } else {
        await createRegion(payload)
        ElMessage.success('区域创建成功')
      }
      regionDialogVisible.value = false
      await loadRegionTree()
    } catch (error) {
      ElMessage.error(error.message || '区域保存失败')
    }
  })
}

const handleDeleteRegion = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除区域“${row.regionName}”吗？`, '提示', { type: 'warning' })
    await deleteRegion(row.id)
    ElMessage.success('区域已删除')
    await loadRegionTree()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '区域删除失败')
    }
  }
}

const handleRegionStatusChange = async (row, status) => {
  try {
    await updateRegion(buildRegionPayload(row, { status }))
    ElMessage.success('区域状态已更新')
    await loadRegionTree()
  } catch (error) {
    ElMessage.error('区域状态更新失败')
    await loadRegionTree()
  }
}

const openStaffDialog = (row = null) => {
  resetStaffForm()
  if (row) {
    Object.assign(staffForm, {
      id: row.id,
      userId: row.userId,
      name: row.name,
      phone: row.phone,
      idNumber: row.idNumber,
      regionId: row.regionId,
      vehicleType: row.vehicleType || 'EBIKE',
      vehicleNumber: row.vehicleNumber || '',
      healthCertNo: row.healthCertNo || '',
      healthCertExpiry: row.healthCertExpiry || '',
      status: row.status || 'OFFLINE',
      enabled: row.enabled ?? 1
    })
  }
  staffDialogVisible.value = true
}

const submitStaff = async () => {
  await staffFormRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (staffForm.id) {
        await updateStaff(buildStaffPayload(staffForm))
        ElMessage.success('配送员更新成功')
      } else {
        await createStaff(buildStaffPayload(staffForm))
        ElMessage.success('配送员创建成功')
      }
      staffDialogVisible.value = false
      await loadStaffList()
    } catch (error) {
      ElMessage.error(error.message || '配送员保存失败')
    }
  })
}

const handleDeleteStaff = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除配送员“${row.name}”吗？`, '提示', { type: 'warning' })
    await deleteStaff(row.id)
    ElMessage.success('配送员已删除')
    await loadStaffList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '配送员删除失败')
    }
  }
}

const handleStaffStatusChange = async (row, status) => {
  try {
    await updateStaffStatus(row.id, status)
    ElMessage.success('配送员状态已更新')
    await loadStaffList()
  } catch (error) {
    ElMessage.error('配送员状态更新失败')
  }
}

const handleStaffEnableChange = async (row, enabled) => {
  try {
    await updateStaff(buildStaffPayload({ ...row, enabled }))
    ElMessage.success('配送员启用状态已更新')
    await loadStaffList()
  } catch (error) {
    ElMessage.error('配送员启用状态更新失败')
    await loadStaffList()
  }
}

const openAssignDialog = (row) => {
  assignTarget.value = row
  assignRegionId.value = row.regionId ?? null
  assignDialogVisible.value = true
}

const submitAssignRegion = async () => {
  if (!assignTarget.value || !assignRegionId.value) {
    ElMessage.warning('请选择目标区域')
    return
  }

  try {
    await assignStaffToRegion(assignTarget.value.id, assignRegionId.value)
    ElMessage.success('区域分配成功')
    assignDialogVisible.value = false
    await loadStaffList()
    await loadRegionTree()
  } catch (error) {
    ElMessage.error(error.message || '区域分配失败')
  }
}

const resetMethodForm = () => {
  Object.assign(methodForm, {
    id: null,
    methodName: '',
    description: '',
    freight: 0,
    freeThreshold: 0,
    status: 1
  })
  methodFormRef.value?.clearValidate()
}

const resetRegionForm = () => {
  Object.assign(regionForm, {
    id: null,
    regionName: '',
    regionCode: '',
    parentId: 0,
    level: 1,
    deliveryFee: 0,
    minOrderAmount: 0,
    estimatedTime: 30,
    deliveryRange: 5,
    status: 1
  })
  regionFormRef.value?.clearValidate()
}

const resetStaffForm = () => {
  Object.assign(staffForm, {
    id: null,
    userId: null,
    name: '',
    phone: '',
    idNumber: '',
    regionId: null,
    vehicleType: 'EBIKE',
    vehicleNumber: '',
    healthCertNo: '',
    healthCertExpiry: '',
    status: 'OFFLINE',
    enabled: 1
  })
  staffFormRef.value?.clearValidate()
}

const buildMethodPayload = (row) => ({
  id: row.id,
  methodName: row.methodName,
  description: row.description || '',
  freight: row.freight ?? 0,
  freeThreshold: row.freeThreshold ?? 0,
  status: row.status ?? 1
})

const buildRegionPayload = (row, overrides = {}) => ({
  id: row.id,
  regionName: row.regionName,
  regionCode: row.regionCode,
  parentId: row.parentId ?? 0,
  level: row.level,
  deliveryFee: row.deliveryFee ?? 0,
  minOrderAmount: row.minOrderAmount ?? 0,
  estimatedTime: row.estimatedTime ?? 30,
  deliveryRange: row.deliveryRange ?? 5,
  status: row.status ?? 1,
  ...overrides
})

const buildStaffPayload = (row) => ({
  id: row.id,
  userId: row.userId || null,
  name: row.name,
  phone: row.phone,
  idNumber: row.idNumber,
  regionId: row.regionId || null,
  vehicleType: row.vehicleType || 'EBIKE',
  vehicleNumber: row.vehicleNumber || '',
  healthCertNo: row.healthCertNo || '',
  healthCertExpiry: row.healthCertExpiry || null,
  status: row.status || 'OFFLINE',
  enabled: row.enabled ?? 1
})

const getLevelName = (level) => {
  const map = {
    1: '省/直辖市',
    2: '城市',
    3: '区/县',
    4: '街道/站点'
  }
  return map[level] || level || '-'
}

const getStaffStatusType = (status) => {
  const map = {
    IDLE: 'success',
    BUSY: 'warning',
    OFFLINE: 'info',
    REST: 'danger'
  }
  return map[status] || 'info'
}

const formatVehicleType = (vehicleType) => {
  const map = {
    BIKE: '自行车',
    EBIKE: '电动车',
    MOTORCYCLE: '摩托车'
  }
  return map[vehicleType] || vehicleType || '-'
}

const formatMoney = (value) => Number(value || 0).toFixed(2)
</script>

<style scoped>
.delivery-tabs :deep(.el-tabs__nav-wrap) {
  overflow-x: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--spacing-4);
}

.card-header__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-header__copy strong {
  font-size: var(--text-lg);
}

.card-header__copy span {
  color: var(--color-text-muted);
}

.delivery-toolbar {
  margin-bottom: var(--spacing-4);
}

.delivery-toolbar :deep(.el-input),
.delivery-toolbar :deep(.el-select) {
  width: min(280px, 100%);
}

@media (max-width: 768px) {
  .delivery-toolbar :deep(.el-input),
  .delivery-toolbar :deep(.el-select),
  .delivery-toolbar :deep(.el-button) {
    width: 100%;
  }
}
</style>
