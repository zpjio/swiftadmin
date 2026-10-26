import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
  sex?: number
  phone?: string
  email?: string
  deptName?: string
  roleNames?: string[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const refreshToken = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const userMenus = ref<any[]>([]) // 用户菜单树（用于生成侧边栏菜单）

  // 设置 Token
  const setToken = (newToken: string, newRefreshToken?: string) => {
    token.value = newToken
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
    }
    localStorage.setItem('token', newToken)
  }

  // 设置用户信息
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
    roles.value = info.roles || []
    permissions.value = info.permissions || []
  }

  // 登出
  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  // 初始化 Token
  const initToken = () => {
    const savedToken = localStorage.getItem('token')
    const savedRefreshToken = localStorage.getItem('refreshToken')
    if (savedToken) {
      token.value = savedToken
    }
    if (savedRefreshToken) {
      refreshToken.value = savedRefreshToken
    }
  }

  return {
    token,
    refreshToken,
    userInfo,
    roles,
    permissions,
    userMenus,
    setToken,
    setUserInfo,
    logout,
    initToken
  }
})
