import type { RouteRecordRaw } from 'vue-router'

// 重定向路由（用于刷新页面）
export const redirectRoute: RouteRecordRaw = {
  path: '/redirect',
  component: () => import('@/layout/index.vue'),
  children: [
    {
      path: '/redirect/:pathMatch(.*)*',
      component: () => import('@/views/error/redirect.vue')
    }
  ]
}
