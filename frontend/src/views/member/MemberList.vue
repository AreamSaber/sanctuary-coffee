<template>
  <div class="member-list-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>会员管理</span>
          <el-tag type="info">共 {{ total }} 名会员</el-tag>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="会员等级">
          <el-select
            v-model="searchForm.levelId"
            placeholder="请选择等级"
            clearable
            style="width: 160px"
            @change="handleSearch"
          >
            <el-option
              v-for="level in memberLevels"
              :key="level.id"
              :label="level.levelName"
              :value="level.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名或昵称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshLeft /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="memberList" style="width: 100%" v-loading="loading">
        <el-table-column prop="userId" label="用户ID" width="90" />

        <el-table-column label="用户信息" min-width="220">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-detail">
                <div class="username">{{ row.username }}</div>
                <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="会员等级" width="140">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.levelName)">{{ row.levelName || '未分配' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="growthValue" label="成长值" width="100" sortable />
        <el-table-column prop="points" label="积分" width="100" sortable />

        <el-table-column label="累计消费" width="140" sortable>
          <template #default="{ row }">
            ¥{{ formatAmount(row.totalConsumption) }}
          </template>
        </el-table-column>

        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column prop="phone" label="手机号" width="140" />

        <el-table-column label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button link type="success" size="small" @click="adjustPoints(row)">
              <el-icon><Coin /></el-icon>
              积分
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadMemberList"
        @current-change="loadMemberList"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="会员详情" width="640px">
      <div v-if="currentMember" class="member-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户ID">{{ currentMember.userId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ currentMember.username }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ currentMember.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="会员等级">
            <el-tag :type="getLevelType(currentMember.levelName)">{{ currentMember.levelName || '未分配' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="成长值">{{ currentMember.growthValue }}</el-descriptions-item>
          <el-descriptions-item label="积分余额">{{ currentMember.points }}</el-descriptions-item>
          <el-descriptions-item label="累计消费" :span="2">
            ¥{{ formatAmount(currentMember.totalConsumption) }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ currentMember.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ currentMember.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间" :span="2">
            {{ formatTime(currentMember.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑会员" width="520px" @closed="resetEditForm">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="会员等级" prop="levelId">
          <el-select v-model="editForm.levelId" placeholder="请选择会员等级" style="width: 100%">
            <el-option
              v-for="level in memberLevels"
              :key="level.id"
              :label="level.levelName"
              :value="level.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pointsVisible" title="积分调整" width="420px">
      <el-form :model="pointsForm" label-width="90px">
        <el-form-item label="当前积分">
          <el-input :model-value="currentMember?.points ?? 0" disabled />
        </el-form-item>
        <el-form-item label="调整类型">
          <el-radio-group v-model="pointsForm.type">
            <el-radio :label="1">增加</el-radio>
            <el-radio :label="2">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="积分数量">
          <el-input-number v-model="pointsForm.points" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="pointsForm.remark" type="textarea" :rows="3" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPointsAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Coin, Edit, RefreshLeft, Search, User, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { adjustMemberPoints, getActiveLevels, getMemberList, updateMember } from '@/api/member'

const loading = ref(false)
const memberList = ref([])
const memberLevels = ref([])
const total = ref(0)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const searchForm = reactive({
  levelId: null,
  username: ''
})

const detailVisible = ref(false)
const editVisible = ref(false)
const pointsVisible = ref(false)
const editSubmitting = ref(false)
const currentMember = ref(null)
const editFormRef = ref()

const editForm = reactive({
  userId: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  levelId: null
})

const pointsForm = reactive({
  type: 1,
  points: 0,
  remark: ''
})

const editRules = {
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  levelId: [{ required: true, message: '请选择会员等级', trigger: 'change' }]
}

const loadMemberList = async () => {
  loading.value = true
  try {
    const res = await getMemberList({
      ...pagination,
      ...searchForm
    })
    memberList.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (error) {
    ElMessage.error('加载会员列表失败')
  } finally {
    loading.value = false
  }
}

const loadMemberLevels = async () => {
  try {
    const res = await getActiveLevels()
    memberLevels.value = res.data || []
  } catch (error) {
    memberLevels.value = []
    ElMessage.error('加载会员等级失败')
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadMemberList()
}

const handleReset = () => {
  searchForm.levelId = null
  searchForm.username = ''
  handleSearch()
}

const viewDetail = (row) => {
  currentMember.value = { ...row }
  detailVisible.value = true
}

const handleEdit = (row) => {
  currentMember.value = { ...row }
  editForm.userId = row.userId
  editForm.username = row.username || ''
  editForm.nickname = row.nickname || ''
  editForm.email = row.email || ''
  editForm.phone = row.phone || ''
  editForm.levelId = row.levelId || null
  editVisible.value = true
}

const resetEditForm = () => {
  editForm.userId = null
  editForm.username = ''
  editForm.nickname = ''
  editForm.email = ''
  editForm.phone = ''
  editForm.levelId = null
  editFormRef.value?.clearValidate()
}

const submitEdit = async () => {
  if (!editFormRef.value) {
    return
  }

  await editFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    editSubmitting.value = true
    try {
      await updateMember(editForm.userId, {
        nickname: normalizeOptionalValue(editForm.nickname),
        email: normalizeOptionalValue(editForm.email),
        phone: normalizeOptionalValue(editForm.phone),
        levelId: editForm.levelId
      })
      ElMessage.success('会员资料更新成功')
      editVisible.value = false
      await loadMemberList()
      if (currentMember.value?.userId === editForm.userId) {
        currentMember.value = {
          ...currentMember.value,
          nickname: normalizeOptionalValue(editForm.nickname),
          email: normalizeOptionalValue(editForm.email),
          phone: normalizeOptionalValue(editForm.phone),
          levelId: editForm.levelId,
          levelName: resolveLevelName(editForm.levelId)
        }
      }
    } catch (error) {
      ElMessage.error(error.message || '会员资料更新失败')
    } finally {
      editSubmitting.value = false
    }
  })
}

const adjustPoints = (row) => {
  currentMember.value = { ...row }
  pointsForm.type = 1
  pointsForm.points = 0
  pointsForm.remark = ''
  pointsVisible.value = true
}

const submitPointsAdjust = async () => {
  if (!currentMember.value?.userId) {
    return
  }
  if (!pointsForm.points || pointsForm.points < 1) {
    ElMessage.warning('请输入有效的积分数量')
    return
  }

  try {
    await adjustMemberPoints({
      userId: currentMember.value.userId,
      ...pointsForm
    })
    ElMessage.success('积分调整成功')
    pointsVisible.value = false
    await loadMemberList()
  } catch (error) {
    ElMessage.error('积分调整失败')
  }
}

const resolveLevelName = (levelId) => {
  return memberLevels.value.find((level) => level.id === levelId)?.levelName || '未分配'
}

const normalizeOptionalValue = (value) => {
  const normalized = value?.trim()
  return normalized ? normalized : null
}

const getLevelType = (levelName) => {
  if (!levelName) return 'info'
  if (levelName.includes('VIP') || levelName.includes('钻石')) return 'danger'
  if (levelName.includes('高级') || levelName.includes('黄金')) return 'warning'
  if (levelName.includes('中级') || levelName.includes('白银')) return 'success'
  return 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatAmount = (value) => {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number.toFixed(2) : '0.00'
}

onMounted(async () => {
  await loadMemberLevels()
  await loadMemberList()
})
</script>

<style scoped lang="scss">
.member-list-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  border-radius: 12px;
  background: #f5f7fa;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-detail {
  min-width: 0;
}

.username {
  font-weight: 600;
}

.nickname {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.member-detail {
  padding-top: 8px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
