import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 角色查询参数
 */
export interface RoleQuery {
  page?: number
  size?: number
  name?: string
  code?: string
  status?: number
}

/**
 * 角色信息
 */
export interface RoleVO {
  id: number
  name: string
  code: string
  description: string
  status: number
  dataScope: number
  packageId: number
  packageName: string
  menuIds: number[]
  deptIds: number[]
  createTime: string
  updateTime: string
}

/**
 * 角色创建/更新参数
 */
export interface RoleDTO {
  id?: number
  name: string
  code: string
  description?: string
  status?: number
  dataScope?: number
  packageId?: number
  menuIds?: number[]
  deptIds?: number[]
}

/**
 * 分页查询角色列表
 */
export function getRolePageApi(params: RoleQuery) {
  return get<PageResult<RoleVO>>('/system/role/page', params)
}

/**
 * 获取角色详情
 */
export function getRoleDetailApi(id: number) {
  return get<RoleVO>(`/system/role/${id}`)
}

/**
 * 获取角色列表（下拉选择）
 */
export function getRoleListApi() {
  return get<RoleVO[]>('/system/role/list')
}

/**
 * 创建角色
 */
export function createRoleApi(data: RoleDTO) {
  return post<number>('/system/role', data)
}

/**
 * 更新角色
 */
export function updateRoleApi(data: RoleDTO) {
  return put('/system/role', data)
}

/**
 * 批量删除角色
 */
export function deleteRoleApi(ids: number[]) {
  return del(`/system/role/${ids.join(',')}`)
}

/**
 * 为角色分配菜单权限
 */
export function assignMenusApi(roleId: number, menuIds: number[]) {
  return put('/system/role/menu', null, {
    params: { roleId, menuIds }
  })
}
