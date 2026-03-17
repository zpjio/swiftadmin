import { defineStore } from 'pinia'
import { ref } from 'vue'

import type { MenuVO } from '@/types'
import { refreshPermissionApi, checkPermissionRefreshApi } from '@/api/auth'
import { resetRouter } from '@/router'

export interface UserInfo {
  id: string
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

  // 设置权限
  const setPermissions = (perms: string[]) => {
    permissions.value = perms || []
  }

  // 登出
  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    userMenus.value = [] // 清除菜单
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    // 重置路由状态，确保下一个用户能重新加载菜单
    resetRouter()
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

  // 检查并刷新权限
  const checkAndRefreshPermissions = async () => {
    try {
      const result = await checkPermissionRefreshApi()
      if (result.needRefresh) {
        return refreshUserPermissions()
      }
      return false
    } catch (error) {
      console.error('检查权限刷新失败:', error)
      return false
    }
  }

  // 刷新用户权限
  const refreshUserPermissions = async () => {
    try {
      const result = await refreshPermissionApi()
      permissions.value = result.permissions || []
      roles.value = result.roles || []
      console.log('权限已刷新:', result)
      return true
    } catch (error) {
      console.error('刷新权限失败:', error)
      return false
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
    setPermissions,
    setRoles: (newRoles: string[]) => { roles.value = newRoles },
    logout,
    initToken,
    checkAndRefreshPermissions,
    refreshUserPermissions,
  }
})
