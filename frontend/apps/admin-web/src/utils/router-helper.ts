import type { RouteRecordRaw } from 'vue-router'

/**
 * 菜单类型
 */
export const MenuType = {
  DIRECTORY: 1, // 目录
  MENU: 2,      // 菜单
  BUTTON: 3,    // 按钮
} as const

export type MenuTypeEnum = typeof MenuType[keyof typeof MenuType]

/**
 * 菜单数据接口
 */
export interface MenuVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string | null
  type: number | null
  icon: string | null
  sortOrder: number
  permission: string | null
  status: number | null
  children?: MenuVO[]
}

/**
 * 路由元信息
 */
export interface CustomRouteMeta {
  title: string
  icon?: string
  permission?: string
  hidden?: boolean
  keepAlive?: boolean
}

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
    const route: any = {
      path: menu.path,
      name: getMenuName(menu.path),
      component: loadComponent(menu.component),
      meta: {
        title: menu.name,
        icon: menu.icon || undefined,
        permission: menu.permission || undefined,
        hidden: false,
        keepAlive: true, // 默认启用页面缓存
      },
    }

    routes.push(route)
  })

  return routes
}

/**
 * 从路径生成路由名称
 */
function getMenuName(path: string): string {
  // 处理空路径
  if (!path) {
    return 'Menu_' + Date.now()
  }
  
  // 移除开头的 /
  const cleanPath = path.replace(/^\//, '')
  // 将 /system/user 转换为 SystemUser
  return cleanPath
    .split('/')
    .filter(segment => segment.length > 0)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join('')
}

/**
 * 动态加载组件
 */
function loadComponent(component?: string | null) {
  if (!component) {
    // 默认使用 Layout
    return () => import('@/layout/index.vue')
  }

  // 组件路径映射
  const componentMap: Record<string, () => Promise<any>> = {
    'dashboard/index': () => import('@/views/dashboard/index.vue'),
    'system/user/index': () => import('@/views/system/user/index.vue'),
    'system/role/index': () => import('@/views/system/role/index.vue'),
    'system/menu/index': () => import('@/views/system/menu/index.vue'),
    'system/dept/index': () => import('@/views/system/dept/index.vue'),
    'system/online/index': () => import('@/views/system/online/index.vue'),
    'system/dict/index': () => import('@/views/system/dict/index.vue'),
    'system/file/index': () => import('@/views/system/file/index.vue'),
    'system/config/index': () => import('@/views/system/config/index.vue'),
    'monitor/operlog/index': () => import('@/views/monitor/operlog/index.vue'),
    'monitor/loginlog/index': () => import('@/views/monitor/loginlog/index.vue'),
    'monitor/online/index': () => import('@/views/system/online/index.vue'),
    'profile/index': () => import('@/views/profile/index.vue'),
  }

  return componentMap[component] || (() => import('@/views/error/404.vue'))
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
