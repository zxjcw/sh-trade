import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { Role, User } from '@/types'

function readUser(): User | null {
  try {
    const raw = localStorage.getItem('sgj_user')
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('sgj_token') ?? '',
    user: readUser(),
    loading: false
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state): Role => state.user?.role ?? 'student',
    isVerified: (state) => state.user?.verifyStatus === 'verified',
    isStudent: (state) => (state.user?.role ?? 'student') === 'student',
    displayName: (state) => state.user?.nickname ?? '游客',
    balance: (state) => state.user?.balance ?? 0
  },

  actions: {
    persist() {
      if (this.token) localStorage.setItem('sgj_token', this.token)
      else localStorage.removeItem('sgj_token')
      if (this.user) localStorage.setItem('sgj_user', JSON.stringify(this.user))
      else localStorage.removeItem('sgj_user')
    },

    setUser(user: User) {
      this.user = user
      this.persist()
    },

    async login(username: string, password: string) {
      const result = await authApi.login({ username, password })
      this.token = result.token
      this.user = result.user
      this.persist()
      return result.user
    },

    async register(payload: { username: string; password: string; nickname: string; contact?: string }) {
      const result = await authApi.register(payload)
      this.token = result.token
      this.user = result.user
      this.persist()
      return result.user
    },

    async fetchMe() {
      if (!this.token) return null
      this.loading = true
      try {
        const me = await authApi.me()
        this.user = me
        this.persist()
        return me
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 忽略登出接口失败，本地状态照常清理
      }
      this.token = ''
      this.user = null
      this.persist()
    }
  }
})
