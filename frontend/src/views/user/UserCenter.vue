<template>
  <div class="app-page user-center-page">
    <section class="app-page-header app-page-header--compact">
      <div>
        <h1 class="app-page-header__title">个人中心</h1>
      </div>
      <div class="app-page-actions">
        <el-button @click="router.push('/user/address')">地址管理</el-button>
      </div>
    </section>

    <section class="page-grid page-grid--sidebar">
      <el-card class="stack-card user-center-side" shadow="never">
        <div class="stack-card__body user-center-side__body">
          <div class="user-center-profile">
            <el-avatar :size="72" :src="userInfo.avatar || undefined" class="user-center-profile__avatar">
              {{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) || 'U' }}
            </el-avatar>
            <strong class="user-center-profile__name">{{ userInfo.nickname || userInfo.username }}</strong>
            <span class="user-center-profile__meta">{{ userInfo.email || '未设置邮箱' }}</span>
            <span class="user-center-profile__meta">{{ userInfo.phone || '未设置手机号' }}</span>
          </div>

          <el-menu :default-active="activeMenu" class="user-center-menu" @select="handleMenuSelect">
            <el-menu-item index="info">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="password">
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </el-menu-item>
            <el-menu-item index="address" @click="router.push('/user/address')">
              <el-icon><Location /></el-icon>
              <span>地址管理</span>
            </el-menu-item>
          </el-menu>
        </div>
      </el-card>

      <el-card class="stack-card" shadow="never">
        <div class="stack-card__header">
          <div>
            <h2 class="stack-card__title">{{ activeMenu === 'info' ? '个人资料' : '修改密码' }}</h2>
            <p class="compact-note">
              {{ activeMenu === 'info' ? '维护昵称、联系方式和基础信息。' : '修改后下次登录生效。' }}
            </p>
          </div>
        </div>

        <div class="stack-card__body">
          <el-form
            v-if="activeMenu === 'info'"
            ref="userFormRef"
            :model="userForm"
            :rules="userRules"
            label-width="100px"
            class="user-form"
          >
            <div class="summary-grid user-center-summary">
              <div class="summary-card">
                <span class="summary-card__label">账号</span>
                <strong class="summary-card__value">{{ userInfo.username || '-' }}</strong>
              </div>
              <div class="summary-card">
                <span class="summary-card__label">会员昵称</span>
                <strong class="summary-card__value">{{ userForm.nickname || '-' }}</strong>
              </div>
            </div>

            <div class="form-section">
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>

              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="userForm.nickname" placeholder="请输入昵称" />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userForm.email" placeholder="请输入邮箱" />
              </el-form-item>

              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userForm.phone" placeholder="请输入手机号" />
              </el-form-item>

              <el-form-item label="性别">
                <el-radio-group v-model="userForm.gender">
                  <el-radio-button :label="0">保密</el-radio-button>
                  <el-radio-button :label="1">男</el-radio-button>
                  <el-radio-button :label="2">女</el-radio-button>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="生日">
                <el-date-picker
                  v-model="userForm.birthday"
                  type="date"
                  placeholder="选择日期"
                  style="width: 100%"
                />
              </el-form-item>
            </div>

            <el-form-item class="form-actions">
              <el-button :loading="loading" type="primary" size="large" @click="handleUpdateInfo">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>

          <el-form
            v-else
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="user-form password-form"
          >
            <div class="form-section">
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  placeholder="请输入原密码"
                  show-password
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（6-20位）"
                  show-password
                />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                  show-password
                />
              </el-form-item>
            </div>

            <el-form-item class="form-actions">
              <el-button :loading="loading" type="primary" size="large" @click="handleChangePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Location, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { changePassword, updateUserInfo } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const activeMenu = ref('info')
const loading = ref(false)

const userInfo = ref({})
const userFormRef = ref()
const passwordFormRef = ref()

const userForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: null
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const userRules = {
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const validateNewPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度为6-20位'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

onMounted(async () => {
  await loadUserInfo()
})

const loadUserInfo = async () => {
  try {
    await userStore.getUserInfo()
    userInfo.value = userStore.userInfo
    Object.assign(userForm, {
      nickname: userInfo.value.nickname,
      email: userInfo.value.email,
      phone: userInfo.value.phone,
      gender: userInfo.value.gender || 0,
      birthday: userInfo.value.birthday
    })
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const handleMenuSelect = (index) => {
  if (index === 'address') {
    return
  }
  activeMenu.value = index
}

const handleUpdateInfo = async () => {
  await userFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      await updateUserInfo(userForm)
      ElMessage.success('保存成功')
      await loadUserInfo()
    } catch (error) {
      console.error('保存失败:', error)
    } finally {
      loading.value = false
    }
  })
}

const handleChangePassword = async () => {
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
      ElMessage.success('密码修改成功，请重新登录')
      passwordFormRef.value.resetFields()
    } catch (error) {
      console.error('密码修改失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.user-center-side__body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.user-center-profile {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  align-items: center;
  padding: var(--spacing-4);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.5);
}

.user-center-profile__avatar {
  border: 1px solid rgba(107, 101, 91, 0.12);
}

.user-center-profile__name {
  font-size: var(--text-lg);
}

.user-center-profile__meta {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.user-center-menu {
  border-right: none;
  background: transparent;
}

.user-center-summary {
  margin-bottom: var(--spacing-5);
}

.user-form {
  max-width: 760px;
}

.form-section {
  padding: var(--spacing-5);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.46);
}

.password-form {
  max-width: 560px;
}

.form-actions {
  margin-top: var(--spacing-6);
  margin-bottom: 0;
}

.form-actions :deep(.el-form-item__content) {
  display: flex;
  gap: var(--spacing-4);
}

@media (max-width: 768px) {
  .user-center-page :deep(.el-form-item__label) {
    width: 92px !important;
  }
}
</style>
