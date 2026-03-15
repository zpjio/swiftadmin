import { get, post, put, del } from '@/utils/request'

/**
 * 菜单类型
 */
export type MenuType = 1 | 2 | 3 // 1-目录 2-菜单 3-按钮

/**
 * 菜单信息
 */
export interface MenuVO {
  id: string
  parentId: string
  name: string
  path: string
  component: string
  type: MenuType
  icon: string
  sortOrder: number
  permission: string
  status: number
  createTime: string
  updateTime: string
  children?: MenuVO[]
}

/**
 * 菜单树节点（简化版，用于选择）
 */
export interface MenuTreeVO {
  id: string
  parentId: string
  label: string
  type: MenuType
  children?: MenuTreeVO[]
}

/**
 * 菜单创建/更新参数
 */
export interface MenuDTO {
  id?: string
  parentId: string
  name: string
  path?: string
  component?: string
  type: MenuType
  icon?: string
  sortOrder?: number
  permission?: string
  status?: number
}

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
