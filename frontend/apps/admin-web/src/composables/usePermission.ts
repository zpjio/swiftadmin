import { computed } from 'vue'
import { useUserStore } from '@/store'

/**
 * 权限判断组合式函数
 * 提供权限检查的便捷方法
 */
export function usePermission() {
  const userStore = useUserStore()

  /**
   * 判断是否为超级管理员
   */
  const isSuperAdmin = computed(() => {
    return userStore.permissions.includes('*:*:*')
  })

  /**
   * 判断是否有指定权限
   * @param permission 权限标识
   */
  function hasPermission(permission: string): boolean {
    if (isSuperAdmin.value) {
      return true
    }
    return userStore.permissions.includes(permission)
  }

  /**
   * 判断是否有任意一个权限
   * @param permissions 权限标识数组
   */
  function hasAnyPermission(permissions: string[]): boolean {
    if (isSuperAdmin.value) {
      return true
    }
    return permissions.some((p) => userStore.permissions.includes(p))
  }

  /**
   * 判断是否有所有权限
   * @param permissions 权限标识数组
   */
  function hasAllPermissions(permissions: string[]): boolean {
    if (isSuperAdmin.value) {
      return true
    }
    return permissions.every((p) => userStore.permissions.includes(p))
  }

  /**
   * 判断是否有指定角色
   * @param role 角色标识
   */
  function hasRole(role: string): boolean {
    return userStore.roles.includes(role)
  }

  /**
   * 判断是否有任意一个角色
   * @param roles 角色标识数组
   */
  function hasAnyRole(roles: string[]): boolean {
    return roles.some((r) => userStore.roles.includes(r))
  }

  /**
   * 过滤有权限的按钮列表
   * @param buttons 按钮配置列表
   */
  function filterAuthorized<T extends { permission?: string }>(buttons: T[]): T[] {
    if (isSuperAdmin.value) {
      return buttons
    }
    return buttons.filter(
      (btn) => !btn.permission || userStore.permissions.includes(btn.permission)
    )
  }

  return {
    permissions: computed(() => userStore.permissions),
    roles: computed(() => userStore.roles),
    isSuperAdmin,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    hasAnyRole,
    filterAuthorized
  }
}
