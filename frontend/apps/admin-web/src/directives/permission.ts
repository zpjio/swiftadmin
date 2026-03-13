import { useUserStore } from '@/store'
import type { Directive, DirectiveBinding } from 'vue'

/**
 * 权限指令模块
 * 提供 v-permission 和 v-auth 指令用于按钮级权限控制
 */

/**
 * 检查是否有权限
 */
function checkPermission(value: string | string[]): boolean {
  const userStore = useUserStore()
  const permissions = userStore.permissions || []

  // 超级管理员拥有所有权限
  if (permissions.includes('*:*:*')) {
    return true
  }

  if (Array.isArray(value)) {
    return value.some(perm => permissions.includes(perm))
  }

  return permissions.includes(value)
}

/**
 * 检查是否有角色
 */
function checkRole(value: string | string[]): boolean {
  const userStore = useUserStore()
  const roles = userStore.roles || []

  if (Array.isArray(value)) {
    return value.some(role => roles.includes(role))
  }

  return roles.includes(value)
}

/**
 * v-permission 指令
 * 用法：
 * - v-permission="'system:user:add'" - 单个权限
 * - v-permission="['system:user:add', 'system:user:edit']" - 多个权限（或关系）
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding

    if (!value) {
      return
    }

    if (!checkPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * v-auth 指令 - v-permission 的别名
 * 用法：v-auth="'system:user:add'"
 */
export const auth: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding

    if (!value) {
      return
    }

    if (!checkPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * v-role 指令 - 角色控制
 * 用法：
 * - v-role="'admin'" - 单个角色
 * - v-role="['admin', 'manager']" - 多个角色（或关系）
 */
export const role: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding

    if (!value) {
      return
    }

    if (!checkRole(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 设置权限指令
 */
export function setupPermissionDirective(app: any) {
  app.directive('permission', permission)
  app.directive('auth', auth)
  app.directive('role', role)
}

// 导出工具函数供组合式 API 使用
export { checkPermission, checkRole }