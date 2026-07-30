<template>
  <div class="login-page password-init-page">
    <section class="auth-layout">
      <div class="auth-layout__intro">
        <div class="auth-layout__brand">
          <span class="auth-layout__brand-mark"></span>
          Sanctuary Coffee
        </div>

        <div>
          <p class="auth-layout__eyebrow">First Run Setup</p>
          <h1 class="auth-layout__title">先为测试账号设置统一初始密码</h1>
        </div>
      </div>

      <div class="auth-layout__panel" v-loading="loadingStatus">
        <div class="auth-layout__panel-head">
          <h2 class="auth-layout__panel-title">初始化测试账号密码</h2>
          <p class="auth-layout__panel-desc">
            检测到 {{ status.pendingCount || 0 }} 个账号密码为空。完成这一步后，系统会返回后台页面。
          </p>
        </div>

        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="本次设置会统一覆盖所有空密码测试账号"
          description="适合首次导入演示数据库后的快速初始化；后续你仍然可以在系统内再修改个人密码。"
          class="password-init__alert"
        />

        <div v-if="status.users.length" class="password-init__users">
          <span v-for="user in status.users" :key="user.id" class="password-init__user-chip">
            {{ user.username }}
            <small v-if="user.nickname"> / {{ user.nickname }}</small>
          </span>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleSubmit"
        >
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="统一初始密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认统一初始密码"
              size="large"
              show-password
              @keyup.enter="handleSubmit"
            >
              <template #prefix>
                <el-icon><Key /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button
            :loading="submitting"
            type="primary"
            size="large"
            class="login-form__submit"
            @click="handleSubmit"
          >
            <span v-if="!submitting">完成初始化</span>
            <span v-else>正在写入密码...</span>
          </el-button>
        </el-form>

        <div class="auth-layout__footer">
          <span>初始化完成后将自动跳转到登录页。</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Key, Lock } from '@element-plus/icons-vue'
import { initializeBlankPasswords } from '@/api/auth'
import { clearPasswordInitStatusCache, fetchPasswordInitStatus } from '@/utils/passwordInit'

const router = useRouter()
const formRef = ref()
const loadingStatus = ref(false)
const submitting = ref(false)
const status = ref({
  required: false,
  pendingCount: 0,
  users: []
})

const form = reactive({
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入初始化密码'))
    return
  }

  if (value !== form.password) {
    callback(new Error('两次输入的初始化密码不一致'))
    return
  }

  callback()
}

const rules = {
  password: [
    { required: true, message: '请输入统一初始密码', trigger: 'blur' },
    { min: 6, max: 50, message: '初始化密码长度需在 6 到 50 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const resolveFallbackRoute = () => {
  return localStorage.getItem('token') ? '/admin' : '/login'
}

const loadStatus = async () => {
  loadingStatus.value = true
  try {
    const data = await fetchPasswordInitStatus(true)
    if (!data.required) {
      router.replace(resolveFallbackRoute())
      return
    }
    status.value = data
  } catch (error) {
    ElMessage.error(error.message || '初始化状态加载失败')
  } finally {
    loadingStatus.value = false
  }
}

const handleSubmit = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    submitting.value = true
    try {
      const res = await initializeBlankPasswords(form)
      clearPasswordInitStatusCache()
      ElMessage.success(`初始化完成，已更新 ${res.data.updatedCount} 个测试账号`)
      router.replace(resolveFallbackRoute())
    } catch (error) {
      ElMessage.error(error.message || '初始化密码失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadStatus()
})
</script>

<style scoped>
.password-init-page :deep(.auth-layout__intro) {
  color: #1f1a17;
}

.password-init-page :deep(.auth-layout__brand) {
  color: rgba(31, 26, 23, 0.78);
}

.password-init-page :deep(.auth-layout__eyebrow) {
  color: rgba(31, 26, 23, 0.64);
}

.password-init-page :deep(.auth-layout__title),
.password-init-page :deep(.auth-layout__panel-title),
.password-init-page :deep(.auth-layout__panel-desc),
.password-init-page :deep(.auth-layout__footer),
.password-init-page :deep(.el-alert__title),
.password-init-page :deep(.el-alert__description) {
  color: #1f1a17;
}

.password-init-page :deep(.el-form-item__error),
.password-init-page :deep(.el-input__inner),
.password-init-page :deep(.el-input__prefix-inner),
.password-init-page :deep(.el-input__suffix-inner) {
  color: #1f1a17;
}

.password-init-page :deep(.el-input__inner::placeholder) {
  color: rgba(31, 26, 23, 0.52);
}

.password-init-page :deep(.el-alert) {
  --el-alert-title-color: #1f1a17;
  --el-alert-description-color: rgba(31, 26, 23, 0.78);
}

.password-init__alert {
  margin-bottom: 20px;
}

.password-init__users {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 22px;
}

.password-init__user-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(31, 26, 23, 0.08);
  color: #1f1a17;
  font-size: var(--text-sm);
  font-weight: 600;
}

.password-init__user-chip small {
  opacity: 0.7;
}
</style>
