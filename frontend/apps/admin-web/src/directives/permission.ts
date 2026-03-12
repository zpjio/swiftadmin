import { useUserStore } from '@/store'
import type { Directive, DirectiveBinding } from 'vue'

/**
 * 权限指令
 * 用法：
 * - v-permission="'system:user:add'" - 单个权限
 * - v-permission="['system:user:add', 'system:user:edit']" - 多个权限（或关系）
 * - v-auth="'system:user:add'" - 别名，同上
 */
export function setupPermissionDirective(app: any) {
  // v-permission 指令
  app.directive('permission', {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
      const { value } = binding
      
      if (!value) {
        return
      }
      
      const userStore = useUserStore()
      const permissions = userStore.permissions || []
      const allPermissions = permissions.includes('*:*:*') // 超级管理员
      
      let hasPermission = false
      
      if (Array.isArray(value)) {
        // 数组：或关系，有一个权限就显示
        hasPermission = value.some((perm: string) => {
          return allPermissions || permissions.includes(perm)
        })
      } else if (typeof value === 'string') {
        // 字符串：单个权限
        hasPermission = allPermissions || permissions.includes(value)
      }
      
      if (!hasPermission) {
        // 移除元素
        el.parentNode && el.parentNode.removeChild(el)
      }
    },
  } as Directive)
  
  // v-auth 指令（v-permission 的别名）
  app.directive('auth', {
    mounted(el: HTMLElement, binding: DirectiveBinding) {
      const { value } = binding
      
      if (!value) {
        return
      }
      
      const userStore = useUserStore()
      const permissions = userStore.permissions || []
      const allPermissions = permissions.includes('*:*:*') // 超级管理员
      
      let hasPermission = false
      
      if (Array.isArray(value)) {
        // 数组：或关系，有一个权限就显示
        hasPermission = value.some((perm: string) => {
          return allPermissions || permissions.includes(perm)
        })
      } else if (typeof value === 'string') {
        // 字符串：单个权限
        hasPermission = allPermissions || permissions.includes(value)
      }
      
      if (!hasPermission) {
        // 移除元素
        el.parentNode && el.parentNode.removeChild(el)
      }
    },
  } as Directive)
}

/**
 * 权限判断函数
 * @param permission 权限标识
 * @returns 是否有权限
 */
export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  const permissions = userStore.permissions || []
  
  // 超级管理员拥有所有权限
  if (permissions.includes('*:*:*')) {
    return true
  }
  
  return permissions.includes(permission)
}

/**
 * 权限判断函数（多个权限）
 * @param permissions 权限标识数组
 * @param type 判断类型：'and' | 'or'
 * @returns 是否有权限
 */
export function hasPermissions(permissions: string[], type: 'and' | 'or' = 'or'): boolean {
  const userStore = useUserStore()
  const userPermissions = userStore.permissions || []
  
  // 超级管理员拥有所有权限
  if (userPermissions.includes('*:*:*')) {
    return true
  }
  
  if (type === 'and') {
    return permissions.every((perm) => userPermissions.includes(perm))
  } else {
    return permissions.some((perm) => userPermissions.includes(perm))
  }
}
