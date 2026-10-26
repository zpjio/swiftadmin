import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/store'

/**
 * v-auth 指令 - 按钮级权限控制
 * 用法：<NButton v-auth="'system:user:add'">新增</NButton>
 *      <NButton v-auth="['system:user:add', 'system:user:edit']">操作</NButton>
 */
export const auth: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()

    // 获取用户权限
    const permissions = userStore.permissions || []

    // 检查是否有超级管理员权限
    const hasSuperAdmin = permissions.includes('*:*:*')

    if (hasSuperAdmin) {
      // 超级管理员拥有所有权限，直接返回
      return
    }

    // 判断是否有权限
    let hasPermission = false

    if (Array.isArray(value)) {
      // 如果传入的是数组，只要有一个权限就显示
      hasPermission = value.some((perm: string) => permissions.includes(perm))
    } else if (typeof value === 'string') {
      // 如果传入的是字符串，检查是否包含该权限
      hasPermission = permissions.includes(value)
    }

    // 如果没有权限，移除元素
    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * v-hasRole 指令 - 角色控制
 * 用法：<div v-hasRole="'admin'">管理员可见</div>
 *      <div v-hasRole="['admin', 'manager']">管理员或经理可见</div>
 */
export const hasRole: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()

    // 获取用户角色
    const roles = userStore.roles || []

    // 判断是否有角色
    let hasRole = false

    if (Array.isArray(value)) {
      // 如果传入的是数组，只要有一个角色就显示
      hasRole = value.some((role: string) => roles.includes(role))
    } else if (typeof value === 'string') {
      // 如果传入的是字符串，检查是否包含该角色
      hasRole = roles.includes(value)
    }

    // 如果没有角色，移除元素
    if (!hasRole) {
      el.parentNode?.removeChild(el)
    }
  }
}
