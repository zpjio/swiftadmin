import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 用户查询参数
 */
export interface UserQuery {
  page?: number
  size?: number
  username?: string
  phone?: string
  email?: string
  status?: number
  deptId?: number
  roleId?: number
  createTime?: [string, string]
}

/**
 * 用户信息
 */
export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  sex: number
  status: number
  deptId: number
  deptName: string
  roleIds: number[]
  roleNames: string[]
  remark: string
  createTime: string
  updateTime: string
}

/**
 * 用户创建/更新参数
 */
export interface UserDTO {
  id?: number
  username: string
  password?: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  sex?: number
  status?: number
  deptId?: number
  roleIds?: number[]
  remark?: string
}

/**
 * 分页查询用户列表
 */
export function getUserPageApi(params: UserQuery) {
  return get<PageResult<UserVO>>('/system/user/page', params)
}

/**
 * 获取用户详情
 */
export function getUserDetailApi(id: number) {
  return get<UserVO>(`/system/user/${id}`)
}

/**
 * 创建用户
 */
export function createUserApi(data: UserDTO) {
  return post<number>('/system/user', data)
}

/**
 * 更新用户
 */
export function updateUserApi(data: UserDTO) {
  return put('/system/user', data)
}

/**
 * 批量删除用户
 */
export function deleteUserApi(ids: number[]) {
  return del(`/system/user/${ids.join(',')}`)
}

/**
 * 更新用户状态
 */
export function updateUserStatusApi(userId: number, status: number) {
  return put('/system/user/status', null, {
    params: { userId, status }
  })
}

/**
 * 重置用户密码
 */
export function resetPasswordApi(userId: number, password: string) {
  return put('/system/user/reset-password', null, {
    params: { userId, password }
  })
}

/**
 * 为用户分配角色
 */
export function assignRolesApi(userId: number, roleIds: number[]) {
  return put('/system/user/role', null, {
    params: { userId, roleIds }
  })
}

/**
 * 获取当前用户信息（个人中心用）
 */
export function getCurrentUserInfoApi() {
  return get<UserVO>('/system/user/current')
}

/**
 * 更新当前用户信息
 */
export function updateCurrentUserInfoApi(data: Partial<UserDTO>) {
  return put('/system/user', data)
}
