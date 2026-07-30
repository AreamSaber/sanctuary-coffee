<template>
  <div class="login-page">
    <section class="auth-layout">
      <div class="auth-layout__intro">
        <div class="auth-layout__brand">
          <span class="auth-layout__brand-mark"></span>
          Sanctuary Coffee
        </div>

        <div>
          <p class="auth-layout__eyebrow">Member Access</p>
          <h1 class="auth-layout__title">登录后直接进入工作台</h1>
        </div>
      </div>

      <div class="auth-layout__panel">
        <div class="auth-layout__panel-head">
          <h2 class="auth-layout__panel-title">欢迎回来</h2>
          <p class="auth-layout__panel-desc">输入账户信息继续处理订单、商品和会员事务。</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
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
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="login-form__meta">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <a href="#" class="login-form__link" @click.prevent>忘记密码？</a>
          </div>

          <el-button
            :loading="loading"
            type="primary"
            size="large"
            class="login-form__submit"
            @click="handleLogin"
          >
            <span v-if="!loading">进入控制台</span>
            <span v-else>正在登录...</span>
          </el-button>
        </el-form>

        <div class="auth-layout__footer">
          <span>还没有账户？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { fetchPasswordInitStatus } from '@/utils/passwordInit'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ]
}

const redirectToSetupIfNeeded = async () => {
  try {
    const status = await fetchPasswordInitStatus()
    if (status.required) {
      router.replace('/setup/passwords')
    }
  } catch (error) {
    // 后端未启动或网络异常时，保留登录页自身提示，不阻塞普通登录。
  }
}

const handleLogin = async () => {
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }

    loading.value = true
    try {
      await userStore.login(loginForm)
      ElMessage.success('登录成功')
      router.push('/home')
    } catch (error) {
      console.error('登录失败:', error)
      ElMessage.error(error.message || error.msg || '登录失败，请检查用户名和密码')
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  redirectToSetupIfNeeded()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3);
  margin: 4px 0 20px;
}

.login-form__link {
  color: var(--color-primary);
  font-size: var(--text-sm);
}

.login-form__submit {
  width: 100%;
}

@media (max-width: 768px) {
  .login-form__meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
