import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 角色查询参数
 */
export interface RoleQuery {
  page?: number
  pageSize?: number
  name?: string
  code?: string
  status?: number
}

/**
 * 角色信息
 */
export interface RoleVO {
  id: string
  name: string
  code: string
  description: string
  status: number
  dataScope: number
  menuIds: string[]
  deptIds: string[]
  createTime: string
  updateTime: string
}

/**
 * 角色创建/更新参数
 */
export interface RoleDTO {
  id?: string
  name: string
  code: string
  description?: string
  status?: number
  dataScope?: number
  menuIds?: string[]
  deptIds?: string[]
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
export function getRoleDetailApi(id: string) {
  return get<RoleVO>(`/system/role/${id}`)
}

/**
 * 获取角色列表（下拉选择）
 * @param silent 静默模式，权限不足时不显示错误提示
 */
export function getRoleListApi(silent?: boolean) {
  return get<RoleVO[]>('/system/role/list', undefined, { silent })
}

/**
 * 创建角色
 */
export function createRoleApi(data: RoleDTO) {
  return post<string>('/system/role', data)
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
export function deleteRoleApi(ids: string[]) {
  return del(`/system/role/${ids.join(',')}`)
}

/**
 * 为角色分配菜单权限
 */
export function assignMenusApi(roleId: string, menuIds: string[]) {
  return put('/system/role/menu', null, {
    params: { roleId, menuIds }
  })
}
