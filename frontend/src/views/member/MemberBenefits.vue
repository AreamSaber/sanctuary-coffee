<template>
  <div class="member-benefits-container">
    <el-card shadow="never" class="overview-card">
      <div class="overview">
        <div>
          <p class="eyebrow">Member Benefits</p>
          <h2>会员等级与权益配置</h2>
          <p class="overview-desc">维护会员等级、权益模板，并把可用权益绑定到不同等级。</p>
        </div>
        <div class="overview-stats">
          <div class="stat-card">
            <strong>{{ total }}</strong>
            <span>会员等级</span>
          </div>
          <div class="stat-card">
            <strong>{{ benefitTotal }}</strong>
            <span>权益模板</span>
          </div>
          <div class="stat-card">
            <strong>{{ boundBenefitCount }}</strong>
            <span>已绑定权益</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="等级设置" name="levels">
          <div class="tab-toolbar">
            <div class="toolbar-title">
              <h3>会员等级</h3>
              <span>成长值门槛、等级折扣和启停状态</span>
            </div>
            <el-button type="primary" @click="handleLevelAdd">
              <el-icon><Plus /></el-icon>
              添加等级
            </el-button>
          </div>

          <el-table :data="levelList" style="width: 100%" v-loading="levelLoading">
            <el-table-column prop="levelCode" label="等级编号" width="100" align="center" />
            <el-table-column label="等级图标" width="100" align="center">
              <template #default="{ row }">
                <el-image v-if="isImageIcon(row.icon)" :src="row.icon" class="level-icon" fit="cover" />
                <el-tag v-else type="info" effect="plain">{{ row.icon || '默认' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="levelName" label="等级名称" min-width="140">
              <template #default="{ row }">
                <div class="level-name">
                  <span>{{ row.levelName }}</span>
                  <el-tag v-if="row.levelCode === 1" type="info" size="small">基础</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="requiredPoints" label="所需成长值" width="130" align="center">
              <template #default="{ row }">
                <strong class="growth-value">{{ row.requiredPoints ?? 0 }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="discountRate" label="等级折扣" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="getDiscountType(row.discountRate)">{{ formatDiscount(row.discountRate) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="等级描述" min-width="220" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleLevelStatusChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleLevelEdit(row)">编辑</el-button>
                <el-button type="danger" size="small" text @click="handleLevelDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="loadLevels"
          />
        </el-tab-pane>

        <el-tab-pane label="权益模板" name="benefits">
          <div class="tab-toolbar">
            <div class="toolbar-title">
              <h3>权益模板</h3>
              <span>配置生日礼券、免配送费、积分倍率等权益能力</span>
            </div>
            <el-button type="primary" @click="handleBenefitAdd">
              <el-icon><Plus /></el-icon>
              添加权益
            </el-button>
          </div>

          <el-form :inline="true" :model="benefitQuery" class="search-form">
            <el-form-item label="关键词">
              <el-input
                v-model="benefitQuery.keyword"
                placeholder="权益名称 / 编码"
                clearable
                style="width: 220px"
                @keyup.enter="handleBenefitSearch"
              />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="benefitQuery.benefitType" placeholder="全部类型" clearable style="width: 160px">
                <el-option v-for="item in benefitTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="benefitQuery.status" placeholder="全部状态" clearable style="width: 140px">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleBenefitSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="handleBenefitReset">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="benefitList" style="width: 100%" v-loading="benefitLoading">
            <el-table-column prop="benefitName" label="权益名称" min-width="160">
              <template #default="{ row }">
                <div class="benefit-name">
                  <span>{{ row.benefitName }}</span>
                  <small>{{ row.benefitCode }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="benefitTypeText" label="权益类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getBenefitTypeTag(row.benefitType)">{{ row.benefitTypeText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="valueText" label="权益值" width="140" align="center" />
            <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="row.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleBenefitStatusChange(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleBenefitEdit(row)">编辑</el-button>
                <el-button type="danger" size="small" text @click="handleBenefitDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="benefitPagination.pageNum"
            v-model:page-size="benefitPagination.pageSize"
            :total="benefitTotal"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="loadBenefits"
          />
        </el-tab-pane>

        <el-tab-pane label="等级权益绑定" name="bindings">
          <div class="tab-toolbar">
            <div class="toolbar-title">
              <h3>权益矩阵</h3>
              <span>为每个会员等级配置可见权益，用户会员中心会同步展示</span>
            </div>
            <el-button @click="reloadBindingData">
              <el-icon><RefreshLeft /></el-icon>
              刷新
            </el-button>
          </div>

          <div class="level-matrix" v-loading="matrixLoading">
            <el-empty v-if="levelMatrix.length === 0" description="暂无会员等级" />
            <div v-for="level in levelMatrix" v-else :key="level.levelId" class="matrix-row">
              <div class="matrix-level">
                <el-tag :type="getLevelTagType(level.levelCode)" effect="dark">{{ level.levelName }}</el-tag>
                <span>成长值 {{ level.requiredPoints ?? 0 }} 起</span>
                <small>{{ formatDiscount(level.discountRate) }}</small>
              </div>
              <div class="matrix-benefits">
                <el-empty v-if="!level.benefits?.length" description="未绑定权益" :image-size="48" />
                <template v-else>
                  <el-tag
                    v-for="benefit in level.benefits"
                    :key="benefit.id"
                    :type="getBenefitTypeTag(benefit.benefitType)"
                    effect="plain"
                  >
                    {{ benefit.benefitName }} · {{ benefit.valueText }}
                  </el-tag>
                </template>
              </div>
              <el-button type="primary" plain @click="openBindingDialog(level)">配置权益</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="使用记录" name="usage">
          <div class="tab-toolbar">
            <div class="toolbar-title">
              <h3>权益使用流水</h3>
              <span>追踪免配送费、积分倍率和退款扣回等权益实际生效记录</span>
            </div>
            <el-button @click="loadUsage">
              <el-icon><RefreshLeft /></el-icon>
              刷新
            </el-button>
          </div>

          <el-form :inline="true" :model="usageQuery" class="search-form">
            <el-form-item label="用户ID">
              <el-input v-model="usageQuery.userId" placeholder="用户ID" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="权益类型">
              <el-select v-model="usageQuery.benefitType" placeholder="全部类型" clearable style="width: 160px">
                <el-option v-for="item in benefitTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务场景">
              <el-select v-model="usageQuery.businessType" placeholder="全部场景" clearable style="width: 180px">
                <el-option v-for="item in usageBusinessTypes" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUsageSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
              <el-button @click="handleUsageReset">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="usageList" style="width: 100%" v-loading="usageLoading">
            <el-table-column prop="userId" label="用户ID" width="100" align="center" />
            <el-table-column prop="benefitName" label="权益" min-width="160">
              <template #default="{ row }">
                <div class="benefit-name">
                  <span>{{ row.benefitName }}</span>
                  <small>{{ row.benefitTypeText }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="businessTypeText" label="业务场景" width="130">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.businessTypeText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="businessId" label="业务ID" width="110" align="center" />
            <el-table-column label="权益效果" width="150" align="center">
              <template #default="{ row }">
                <strong :class="getUsageEffectClass(row)">{{ formatUsageEffect(row) }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="statusText" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getUsageStatusType(row.status)">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>

          <el-pagination
            v-model:current-page="usagePagination.pageNum"
            v-model:page-size="usagePagination.pageSize"
            :total="usageTotal"
            layout="total, prev, pager, next"
            class="pagination"
            @current-change="loadUsage"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="levelDialogVisible" :title="levelDialogTitle" width="520px" @closed="resetLevelForm">
      <el-form ref="levelFormRef" :model="levelForm" :rules="levelRules" label-width="110px">
        <el-form-item label="等级编号" prop="levelCode">
          <el-input-number v-model="levelForm.levelCode" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="等级名称" prop="levelName">
          <el-input v-model="levelForm.levelName" placeholder="请输入等级名称，如：黄金会员" />
        </el-form-item>
        <el-form-item label="成长值门槛" prop="requiredPoints">
          <el-input-number v-model="levelForm.requiredPoints" :min="0" :max="999999" />
          <span class="input-hint">达到该成长值升级到此等级</span>
        </el-form-item>
        <el-form-item label="折扣百分比" prop="discountRate">
          <el-input-number v-model="discountPercent" :min="50" :max="100" :precision="1" />
          <span class="input-suffix">%</span>
        </el-form-item>
        <el-form-item label="等级图标" prop="icon">
          <el-input v-model="levelForm.icon" placeholder="图标名称或图片 URL（可选）" />
        </el-form-item>
        <el-form-item label="等级描述" prop="description">
          <el-input v-model="levelForm.description" type="textarea" :rows="3" placeholder="请描述该等级定位" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="levelForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleLevelSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="benefitDialogVisible" :title="benefitDialogTitle" width="560px" @closed="resetBenefitForm">
      <el-form ref="benefitFormRef" :model="benefitForm" :rules="benefitRules" label-width="110px">
        <el-form-item label="权益名称" prop="benefitName">
          <el-input v-model="benefitForm.benefitName" placeholder="如：双倍积分" />
        </el-form-item>
        <el-form-item label="权益编码" prop="benefitCode">
          <el-input v-model="benefitForm.benefitCode" placeholder="如：POINTS_DOUBLE" />
        </el-form-item>
        <el-form-item label="权益类型" prop="benefitType">
          <el-select v-model="benefitForm.benefitType" placeholder="请选择权益类型" style="width: 100%">
            <el-option v-for="item in benefitTypes" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="权益值" prop="benefitValue">
          <el-input-number
            v-model="benefitForm.benefitValue"
            :min="0"
            :max="benefitForm.benefitType === 1 ? 1 : 9999"
            :step="benefitForm.benefitType === 1 ? 0.01 : 0.1"
            :precision="2"
          />
          <span class="input-hint">{{ benefitValueHint }}</span>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="benefitForm.icon" placeholder="图标名称或图片 URL（可选）" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="benefitForm.description" type="textarea" :rows="3" placeholder="请输入权益说明" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="benefitForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="benefitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBenefitSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindingDialogVisible" title="配置等级权益" width="620px">
      <div v-if="bindingLevel" class="binding-dialog">
        <div class="binding-title">
          <el-tag :type="getLevelTagType(bindingLevel.levelCode)" effect="dark">{{ bindingLevel.levelName }}</el-tag>
          <span>选择该等级可享受的权益</span>
        </div>
        <el-checkbox-group v-model="checkedBenefitIds" class="benefit-checkboxes">
          <el-checkbox
            v-for="benefit in activeBenefitOptions"
            :key="benefit.id"
            :label="benefit.id"
            border
          >
            <strong>{{ benefit.benefitName }}</strong>
            <small>{{ benefit.benefitTypeText }} · {{ benefit.valueText }}</small>
          </el-checkbox>
        </el-checkbox-group>
        <el-empty v-if="activeBenefitOptions.length === 0" description="暂无启用权益，请先创建权益模板" />
      </div>
      <template #footer>
        <el-button @click="bindingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBindingSubmit">保存绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  createLevel,
  createMemberBenefit,
  deleteLevel,
  deleteMemberBenefit,
  getActiveMemberBenefits,
  getBenefitUsagePage,
  getLevelBenefitMatrix,
  getMemberBenefitPage,
  getMemberLevelPage,
  saveLevelBenefitBindings,
  updateLevel,
  updateLevelStatus,
  updateMemberBenefit,
  updateMemberBenefitStatus
} from '@/api/member'

const activeTab = ref('levels')

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const levelLoading = ref(false)
const levelList = ref([])

const levelDialogVisible = ref(false)
const levelDialogTitle = ref('添加等级')
const levelFormRef = ref()
const discountPercent = ref(100)
const levelForm = reactive({
  id: null,
  levelCode: 1,
  levelName: '',
  requiredPoints: 0,
  discountRate: 1,
  icon: '',
  description: '',
  status: 1
})

const levelRules = {
  levelCode: [{ required: true, message: '请输入等级编号', trigger: 'blur' }],
  levelName: [{ required: true, message: '请输入等级名称', trigger: 'blur' }],
  requiredPoints: [{ required: true, message: '请输入成长值门槛', trigger: 'blur' }]
}

const benefitTypes = [
  { value: 1, label: '专属折扣' },
  { value: 2, label: '积分倍率' },
  { value: 3, label: '免配送费' },
  { value: 4, label: '生日礼券' },
  { value: 5, label: '专属服务' }
]

const benefitLoading = ref(false)
const benefitList = ref([])
const benefitTotal = ref(0)
const benefitPagination = reactive({
  pageNum: 1,
  pageSize: 10
})
const benefitQuery = reactive({
  keyword: '',
  benefitType: null,
  status: null
})

const benefitDialogVisible = ref(false)
const benefitDialogTitle = ref('添加权益')
const benefitFormRef = ref()
const benefitForm = reactive({
  id: null,
  benefitName: '',
  benefitCode: '',
  benefitType: 2,
  benefitValue: 1,
  icon: '',
  description: '',
  status: 1
})

const benefitRules = {
  benefitName: [{ required: true, message: '请输入权益名称', trigger: 'blur' }],
  benefitCode: [{ required: true, message: '请输入权益编码', trigger: 'blur' }],
  benefitType: [{ required: true, message: '请选择权益类型', trigger: 'change' }]
}

const matrixLoading = ref(false)
const levelMatrix = ref([])
const activeBenefitOptions = ref([])
const bindingDialogVisible = ref(false)
const bindingLevel = ref(null)
const checkedBenefitIds = ref([])

const usageLoading = ref(false)
const usageList = ref([])
const usageTotal = ref(0)
const usagePagination = reactive({
  pageNum: 1,
  pageSize: 10
})
const usageQuery = reactive({
  userId: '',
  benefitType: null,
  businessType: ''
})

const usageBusinessTypes = [
  { value: 'PAYMENT_CREATE', label: '支付创建' },
  { value: 'PAYMENT_REWARD', label: '支付奖励' },
  { value: 'REFUND_ROLLBACK', label: '退款扣回' }
]

const boundBenefitCount = computed(() =>
  levelMatrix.value.reduce((sum, level) => sum + (level.benefits?.length || 0), 0)
)

const benefitValueHint = computed(() => {
  const hintMap = {
    1: '0.95 表示 9.5 折',
    2: '2 表示双倍积分',
    3: '免配送费可留空或填 0',
    4: '可填写生日券面额',
    5: '专属服务可留空或填 0'
  }
  return hintMap[benefitForm.benefitType] || ''
})

const loadLevels = async () => {
  levelLoading.value = true
  try {
    const res = await getMemberLevelPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    levelList.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error('加载等级列表失败')
  } finally {
    levelLoading.value = false
  }
}

const loadBenefits = async () => {
  benefitLoading.value = true
  try {
    const res = await getMemberBenefitPage({
      ...benefitPagination,
      ...benefitQuery
    })
    benefitList.value = res.data?.records || []
    benefitTotal.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error('加载权益模板失败')
  } finally {
    benefitLoading.value = false
  }
}

const loadActiveBenefitOptions = async () => {
  try {
    const res = await getActiveMemberBenefits()
    activeBenefitOptions.value = res.data || []
  } catch (error) {
    activeBenefitOptions.value = []
  }
}

const loadLevelMatrix = async () => {
  matrixLoading.value = true
  try {
    const res = await getLevelBenefitMatrix()
    levelMatrix.value = res.data || []
  } catch (error) {
    ElMessage.error('加载权益矩阵失败')
  } finally {
    matrixLoading.value = false
  }
}

const loadUsage = async () => {
  usageLoading.value = true
  try {
    const res = await getBenefitUsagePage({
      pageNum: usagePagination.pageNum,
      pageSize: usagePagination.pageSize,
      userId: usageQuery.userId || undefined,
      benefitType: usageQuery.benefitType,
      businessType: usageQuery.businessType || undefined
    })
    usageList.value = res.data?.records || []
    usageTotal.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error('加载权益使用记录失败')
  } finally {
    usageLoading.value = false
  }
}

const reloadBindingData = async () => {
  await Promise.all([loadActiveBenefitOptions(), loadLevelMatrix()])
}

const handleTabChange = async (tabName) => {
  if (tabName === 'benefits') {
    await loadBenefits()
  }
  if (tabName === 'bindings') {
    await reloadBindingData()
  }
  if (tabName === 'usage') {
    await loadUsage()
  }
}

const handleLevelAdd = () => {
  levelDialogTitle.value = '添加等级'
  discountPercent.value = 100
  levelDialogVisible.value = true
}

const handleLevelEdit = (row) => {
  levelDialogTitle.value = '编辑等级'
  Object.assign(levelForm, {
    id: row.id,
    levelCode: row.levelCode,
    levelName: row.levelName,
    requiredPoints: row.requiredPoints ?? 0,
    discountRate: Number(row.discountRate ?? 1),
    icon: row.icon || '',
    description: row.description || '',
    status: row.status ?? 1
  })
  discountPercent.value = Number(levelForm.discountRate) * 100
  levelDialogVisible.value = true
}

const handleLevelDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除等级“${row.levelName}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteLevel(row.id)
    ElMessage.success('删除成功')
    await loadLevels()
    await loadLevelMatrix()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleLevelStatusChange = async (row) => {
  try {
    await updateLevelStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    await loadLevelMatrix()
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleLevelSubmit = async () => {
  if (!levelFormRef.value) return
  await levelFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const payload = {
        ...levelForm,
        discountRate: Number(discountPercent.value || 100) / 100
      }
      if (payload.id) {
        await updateLevel(payload)
        ElMessage.success('更新成功')
      } else {
        await createLevel(payload)
        ElMessage.success('创建成功')
      }
      levelDialogVisible.value = false
      await loadLevels()
      await loadLevelMatrix()
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    }
  })
}

const resetLevelForm = () => {
  Object.assign(levelForm, {
    id: null,
    levelCode: 1,
    levelName: '',
    requiredPoints: 0,
    discountRate: 1,
    icon: '',
    description: '',
    status: 1
  })
  discountPercent.value = 100
  levelFormRef.value?.clearValidate()
}

const handleBenefitSearch = () => {
  benefitPagination.pageNum = 1
  loadBenefits()
}

const handleBenefitReset = () => {
  benefitQuery.keyword = ''
  benefitQuery.benefitType = null
  benefitQuery.status = null
  handleBenefitSearch()
}

const handleBenefitAdd = () => {
  benefitDialogTitle.value = '添加权益'
  benefitDialogVisible.value = true
}

const handleBenefitEdit = (row) => {
  benefitDialogTitle.value = '编辑权益'
  Object.assign(benefitForm, {
    id: row.id,
    benefitName: row.benefitName,
    benefitCode: row.benefitCode,
    benefitType: row.benefitType,
    benefitValue: Number(row.benefitValue ?? 0),
    icon: row.icon || '',
    description: row.description || '',
    status: row.status ?? 1
  })
  benefitDialogVisible.value = true
}

const handleBenefitDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`删除权益“${row.benefitName}”后会同步移除等级绑定，是否继续？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMemberBenefit(row.id)
    ElMessage.success('删除成功')
    await loadBenefits()
    await reloadBindingData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBenefitStatusChange = async (row) => {
  try {
    await updateMemberBenefitStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    await reloadBindingData()
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const handleBenefitSubmit = async () => {
  if (!benefitFormRef.value) return
  await benefitFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const payload = { ...benefitForm }
      if (payload.benefitType >= 3 && Number(payload.benefitValue || 0) === 0) {
        payload.benefitValue = null
      }
      if (payload.id) {
        await updateMemberBenefit(payload.id, payload)
        ElMessage.success('更新成功')
      } else {
        await createMemberBenefit(payload)
        ElMessage.success('创建成功')
      }
      benefitDialogVisible.value = false
      await loadBenefits()
      await reloadBindingData()
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    }
  })
}

const resetBenefitForm = () => {
  Object.assign(benefitForm, {
    id: null,
    benefitName: '',
    benefitCode: '',
    benefitType: 2,
    benefitValue: 1,
    icon: '',
    description: '',
    status: 1
  })
  benefitFormRef.value?.clearValidate()
}

const openBindingDialog = async (level) => {
  await loadActiveBenefitOptions()
  bindingLevel.value = level
  checkedBenefitIds.value = (level.benefits || []).map((benefit) => benefit.id)
  bindingDialogVisible.value = true
}

const handleBindingSubmit = async () => {
  if (!bindingLevel.value) return
  try {
    await saveLevelBenefitBindings(bindingLevel.value.levelId, checkedBenefitIds.value)
    ElMessage.success('等级权益保存成功')
    bindingDialogVisible.value = false
    await loadLevelMatrix()
  } catch (error) {
    ElMessage.error(error.message || '等级权益保存失败')
  }
}

const handleUsageSearch = () => {
  usagePagination.pageNum = 1
  loadUsage()
}

const handleUsageReset = () => {
  usageQuery.userId = ''
  usageQuery.benefitType = null
  usageQuery.businessType = ''
  handleUsageSearch()
}

const formatDiscount = (rate) => `${(Number(rate ?? 1) * 10).toFixed(1)}折`

const getDiscountType = (rate) => {
  const value = Number(rate ?? 1)
  if (value >= 0.98) return 'info'
  if (value >= 0.95) return 'success'
  if (value >= 0.9) return 'warning'
  return 'danger'
}

const getBenefitTypeTag = (type) => {
  const typeMap = {
    1: 'warning',
    2: 'success',
    3: 'primary',
    4: 'danger',
    5: 'info'
  }
  return typeMap[type] || 'info'
}

const formatUsageEffect = (row) => {
  const amount = Number(row.effectAmount || 0)
  const points = Number(row.effectPoints || 0)
  if (amount !== 0) {
    return `${amount > 0 ? '-' : '+'}¥${Math.abs(amount).toFixed(2)}`
  }
  if (points !== 0) {
    return `${points > 0 ? '+' : '-'}${Math.abs(points)} 积分`
  }
  return '-'
}

const getUsageEffectClass = (row) => {
  const amount = Number(row.effectAmount || 0)
  const points = Number(row.effectPoints || 0)
  if (amount < 0 || points < 0) return 'usage-negative'
  return 'usage-positive'
}

const getUsageStatusType = (status) => {
  if (status === 2) return 'danger'
  return 'success'
}

const getLevelTagType = (code) => {
  if (code >= 5) return 'danger'
  if (code >= 4) return 'warning'
  if (code >= 3) return 'success'
  return 'info'
}

const isImageIcon = (icon) => /^https?:\/\//.test(icon || '') || (icon || '').startsWith('/')

onMounted(async () => {
  await Promise.all([loadLevels(), loadBenefits(), reloadBindingData(), loadUsage()])
})
</script>

<style lang="scss" scoped>
.member-benefits-container {
  padding: 20px;
}

.overview-card {
  margin-bottom: 20px;
}

.overview {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 8px;
  color: #a87338;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.overview h2 {
  margin: 0;
  font-size: 26px;
}

.overview-desc {
  margin: 8px 0 0;
  color: #606266;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(110px, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff7ec 0%, #f6e4c8 100%);
  text-align: center;
}

.stat-card strong {
  display: block;
  color: #5a361c;
  font-size: 24px;
}

.stat-card span {
  color: #8a6a50;
  font-size: 12px;
}

.tab-toolbar,
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar-title h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.toolbar-title span {
  color: #909399;
  font-size: 13px;
}

.search-form {
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 12px;
  background: #f5f7fa;
}

.level-name,
.benefit-name,
.matrix-level {
  display: flex;
  align-items: center;
  gap: 8px;
}

.benefit-name {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.benefit-name small,
.matrix-level small {
  color: #909399;
}

.level-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
}

.growth-value {
  color: #409eff;
}

.input-hint,
.input-suffix {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.level-matrix {
  display: grid;
  gap: 14px;
}

.matrix-row {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 16px;
  background: #fff;
}

.matrix-level {
  flex-wrap: wrap;
}

.matrix-benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.binding-dialog {
  display: grid;
  gap: 18px;
}

.binding-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.benefit-checkboxes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.benefit-checkboxes :deep(.el-checkbox) {
  height: auto;
  margin-right: 0;
  padding: 12px;
}

.benefit-checkboxes :deep(.el-checkbox__label) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  white-space: normal;
}

.benefit-checkboxes small {
  color: #909399;
}

.usage-positive {
  color: #67c23a;
}

.usage-negative {
  color: #f56c6c;
}

@media (max-width: 900px) {
  .overview,
  .tab-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .overview-stats,
  .benefit-checkboxes {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .matrix-row {
    grid-template-columns: 1fr;
  }
}
</style>
