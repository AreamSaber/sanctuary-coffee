<template>
  <div class="register-page">
    <section class="auth-layout">
      <div class="auth-layout__intro">
        <div class="auth-layout__brand">
          <span class="auth-layout__brand-mark"></span>
          Sanctuary Coffee
        </div>

        <div>
          <p class="auth-layout__eyebrow">Create Account</p>
          <h1 class="auth-layout__title">创建账户后开始下单与管理</h1>
        </div>
      </div>

      <div class="auth-layout__panel">
        <div class="auth-layout__panel-head">
          <h2 class="auth-layout__panel-title">注册新账户</h2>
          <p class="auth-layout__panel-desc">填写基础信息，注册后即可进入统一页面流程。</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="用户名（4-20 位）"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="密码（6-20 位）"
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
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              show-password
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="邮箱"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon><Message /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="手机号"
              size="large"
              clearable
            >
              <template #prefix>
                <el-icon><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="register-form__terms">
            <el-checkbox v-model="agreeTerms">
              我已阅读并同意
              <a href="#" class="register-form__link" @click.prevent>《服务条款》</a>
              与
              <a href="#" class="register-form__link" @click.prevent>《隐私政策》</a>
            </el-checkbox>
          </div>

          <el-button
            :loading="loading"
            type="primary"
            size="large"
            class="register-form__submit"
            @click="handleRegister"
          >
            <span v-if="!loading">创建账户</span>
            <span v-else>正在注册...</span>
          </el-button>
        </el-form>

        <div class="auth-layout__footer">
          <span>已经有账户？</span>
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Message, Phone, User } from '@element-plus/icons-vue'
import { register } from '@/api/auth'

const router = useRouter()

const registerFormRef = ref()
const loading = ref(false)
const agreeTerms = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: ''
})

const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (!/^[a-zA-Z0-9_]+$/.test(value)) {
    callback(new Error('用户名只能包含字母、数字和下划线'))
  } else if (value.length < 4 || value.length > 20) {
    callback(new Error('用户名长度为 4-20 位'))
  } else {
    callback()
  }
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度为 6-20 位'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateOptionalEmail = (rule, value, callback) => {
  const normalized = value?.trim()
  if (!normalized) {
    callback()
    return
  }

  if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(normalized)) {
    callback(new Error('请输入正确的邮箱地址'))
    return
  }

  callback()
}

const validateOptionalPhone = (rule, value, callback) => {
  const normalized = value?.trim()
  if (!normalized) {
    callback()
    return
  }

  if (!/^1[3-9]\d{9}$/.test(normalized)) {
    callback(new Error('请输入正确的手机号'))
    return
  }

  callback()
}

const normalizeOptionalValue = (value) => {
  const normalized = value?.trim()
  return normalized ? normalized : null
}

const registerRules = {
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  email: [{ validator: validateOptionalEmail, trigger: 'blur' }],
  phone: [{ validator: validateOptionalPhone, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!agreeTerms.value) {
    ElMessage.warning('请先阅读并同意服务条款')
    return
  }

  await registerFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      await register({
        ...registerForm,
        email: normalizeOptionalValue(registerForm.email),
        phone: normalizeOptionalValue(registerForm.phone)
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      console.error('注册失败:', error)
      ElMessage.error(error.msg || '注册失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.register-form__terms {
  margin: 4px 0 20px;
  color: var(--color-text-secondary);
}

.register-form__link {
  color: var(--color-primary);
}

.register-form__submit {
  width: 100%;
}
</style>
