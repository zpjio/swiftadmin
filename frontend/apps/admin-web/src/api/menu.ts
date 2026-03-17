import { get, post, put, del } from '@/utils/request'
import type { MenuVO, MenuTreeVO, MenuDTO } from '@/types'

// 重导出类型，保持向后兼容
export type { MenuVO, MenuTreeVO, MenuDTO } from '@/types'

/**
 * 菜单类型别名（向后兼容）
 */
export type MenuType = 1 | 2 | 3 // 1-目录 2-菜单 3-按钮

/**
 * 获取当前用户菜单（动态路由用）
 */
export function getUserMenusApi() {
  return get<MenuVO[]>('/system/menu/user')
}

/**
 * 获取菜单树
 */
export function getMenuTreeApi(status?: number) {
  return get<MenuVO[]>('/system/menu/tree', { status })
}

/**
 * 获取菜单详情
 */
export function getMenuDetailApi(id: string) {
  return get<MenuVO>(`/system/menu/${id}`)
}

/**
 * 获取菜单下拉树（用于选择父级）
 */
export function getMenuSelectTreeApi() {
  return get<MenuTreeVO[]>('/system/menu/select-tree')
}

/**
 * 获取当前用户菜单
 */
export function getUserMenuApi() {
  return get<MenuVO[]>('/system/menu/user')
}

/**
 * 创建菜单
 */
export function createMenuApi(data: MenuDTO) {
  return post<string>('/system/menu', data)
}

/**
 * 更新菜单
 */
export function updateMenuApi(data: MenuDTO) {
  return put('/system/menu', data)
}

/**
 * 删除菜单
 */
export function deleteMenuApi(id: string) {
  return del(`/system/menu/${id}`)
}
