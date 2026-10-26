import type { RouteRecordRaw } from 'vue-router'
import type { MenuVO, RouteMeta } from '@/types'

/**
 * 将后端菜单树转换为 Vue Router 路由配置
 * 后端返回的 path 已经是完整路径（如 /system/user），直接使用即可
 */
export function convertMenusToRoutes(menus: MenuVO[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  menus.forEach((menu) => {
    // 跳过没有 path 的菜单（按钮权限，path 为 null）
    if (!menu.path) {
      return
    }

    // 跳过禁用的菜单
    if (menu.status === 0) {
      return
    }

    // 目录类型（type=1）作为父级分组，不创建独立路由，只处理子菜单
    if (menu.type === 1) {
      // 递归处理子菜单
      if (menu.children && menu.children.length > 0) {
        const childRoutes = convertMenusToRoutes(menu.children)
        routes.push(...childRoutes)
      }
      return
    }

    // 菜单类型（type=2）创建实际路由，使用后端返回的完整路径
    const route: RouteRecordRaw = {
      path: menu.path,
      name: getMenuName(menu.path),
      component: loadComponent(menu.component),
      meta: {
        title: menu.name,
        icon: menu.icon || undefined,
        permission: menu.permission || undefined,
        hidden: false,
        keepAlive: true // 默认启用页面缓存
      } as RouteMeta
    }

    routes.push(route)
  })

  return routes
}

/**
 * 从路径生成路由名称
 */
export function getMenuName(path: string): string {
  // 处理空路径
  if (!path) {
    return 'Menu_' + Date.now()
  }

  // 移除开头的 /
  const cleanPath = path.replace(/^\//, '')
  // 将 /system/user 转换为 SystemUser
  return cleanPath
    .split('/')
    .filter((segment) => segment.length > 0)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join('')
}

// 组件缓存
const componentModules = import.meta.glob('../views/**/*.vue')

/**
 * 动态加载组件
 * 支持动态导入，无需手动维护映射表
 */
function loadComponent(component?: string | null) {
  if (!component) {
    // 默认使用 404 页面
    return () => import('@/views/error/404.vue')
  }

  // 标准化组件路径
  // 支持格式：'system/user/index' 或 'views/system/user/index'
  let normalizedPath = component
  if (normalizedPath.startsWith('views/')) {
    normalizedPath = normalizedPath.replace('views/', '')
  }

  const fullPath = `../views/${normalizedPath}.vue`

  // 检查组件是否存在
  if (componentModules[fullPath]) {
    return componentModules[fullPath]
  }

  // 组件不存在，返回 404 页面
  console.warn(`[Router] 组件不存在: ${fullPath}`)
  return () => import('@/views/error/404.vue')
}

/**
 * 获取所有权限标识
 */
export function getAllPermissions(menus: MenuVO[]): string[] {
  const permissions: string[] = []

  menus.forEach((menu) => {
    if (menu.permission) {
      permissions.push(menu.permission)
    }
    if (menu.children) {
      permissions.push(...getAllPermissions(menu.children))
    }
  })

  return permissions
}
