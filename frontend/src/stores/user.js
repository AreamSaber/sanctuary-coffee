import { defineStore } from 'pinia'
import { login, logout } from '@/api/auth'
import { getUserInfo } from '@/api/user'

const getStoredUserInfo = () => {
  const raw = localStorage.getItem('userInfo')
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    localStorage.removeItem('userInfo')
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: getStoredUserInfo()
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.nickname || '',
    avatar: (state) => state.userInfo?.avatar || '',
    roles: (state) => state.userInfo?.roles || []
  },
  
  actions: {
    // 登录
    async login(loginForm) {
      try {
        const res = await login(loginForm)
        this.token = res.data.token
        this.userInfo = res.data.userInfo
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
        return Promise.resolve(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },
    
    // 获取用户信息
    async getUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
        return Promise.resolve(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },
    
    // 退出登录
    async logout() {
      try {
        await logout()
        this.token = ''
        this.userInfo = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        return Promise.resolve()
      } catch (error) {
        // 即使接口失败，也清除本地数据
        this.token = ''
        this.userInfo = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        return Promise.reject(error)
      }
    }
  }
})
