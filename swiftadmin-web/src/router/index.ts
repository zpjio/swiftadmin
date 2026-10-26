import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { convertMenusToRoutes } from '@/utils/router-helper'
import { getUserMenusApi } from '@/api/menu'
import { useUserStore } from '@/store'
import type { MenuVO } from '@/api/menu'
import { redirectRoute } from './redirect'

// 静态路由（不需要权限）
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/auth/ChangePassword.vue'),
    meta: { title: '修改密码' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

// 基础布局路由（占位，动态路由会添加到这里）
export const layoutRoute: RouteRecordRaw = {
  path: '/',
  name: 'Layout',
  component: () => import('@/layout/index.vue'),
  redirect: '/dashboard',
  children: [
    {
      path: 'dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/index.vue'),
      meta: { title: '工作台', icon: 'mdi-dashboard' }
    },
    {
      path: 'profile',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { title: '个人设置', icon: 'mdi-account-cog' }
    }
  ]
}

// 先不添加 404 路由，等动态路由加载完再添加
const router = createRouter({
  history: createWebHistory(),
  routes: [...staticRoutes.filter((r) => r.name !== 'NotFound'), layoutRoute, redirectRoute]
})

// 是否已添加动态路由
let isDynamicRoutesAdded = false

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - SwiftAdmin`
  }

  // 检查是否已登录
  const token = localStorage.getItem('token')
  const tempToken = localStorage.getItem('tempToken')

  // 放行登录页和修改密码页
  if (to.path === '/login' || to.path === '/change-password') {
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  // 其他页面需要 token 或 tempToken
  if (!token && !tempToken) {
    next('/login')
    return
  }

  // 添加动态路由
  if (!isDynamicRoutesAdded) {
    try {
      // 获取用户菜单
      const res = await getUserMenusApi()
      const menus = res as any as MenuVO[]

      // 转换为路由配置
      const dynamicRoutes = convertMenusToRoutes(menus)

      // 添加到布局路由
      if (dynamicRoutes.length > 0) {
        dynamicRoutes.forEach((route) => {
          router.addRoute('Layout', route)
        })
      }

      // 保存权限和菜单到 store
      const userStore = useUserStore()
      const allPermissions = extractPermissions(menus)
      userStore.permissions = allPermissions
      userStore.userMenus = menus // 保存原始菜单树用于生成侧边栏

      isDynamicRoutesAdded = true

      // 添加 404 路由（必须在所有路由之后）
      router.addRoute(staticRoutes.find((r) => r.name === 'NotFound')!)

      // 重要：添加完路由后，使用 replace: true 重新导航到目标路径
      // 这样 Vue Router 会重新匹配路由，找到刚添加的动态路由
      next({ ...to, replace: true })
    } catch (error) {
      console.error('[Router] 加载动态路由失败:', error)
      // 加载失败则跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('tempToken')
      next('/login')
    }
    return
  }

  next()
})

/**
 * 提取所有权限标识
 */
function extractPermissions(menus: any[]): string[] {
  const permissions: string[] = []

  menus.forEach((menu) => {
    if (menu.permission) {
      permissions.push(menu.permission)
    }
    if (menu.children && menu.children.length > 0) {
      permissions.push(...extractPermissions(menu.children))
    }
  })

  // 添加超级管理员权限标识
  if (permissions.length === 0) {
    permissions.push('*:*:*')
  }

  return permissions
}

export default router
