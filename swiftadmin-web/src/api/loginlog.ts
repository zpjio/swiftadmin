import { get, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 登录日志信息
 */
export interface LoginLogVO {
  id: number
  username: string
  ipAddress: string
  userAgent: string
  status: number
  message: string
  loginTime: string
}

/**
 * 登录日志查询参数
 */
export interface LoginLogQuery {
  page?: number
  size?: number
  username?: string
  ip?: string
  status?: number
  beginTime?: string
  endTime?: string
}

/**
 * 分页查询登录日志列表
 */
export function getLoginLogPageApi(params: LoginLogQuery) {
  return get<PageResult<LoginLogVO>>('/auth/loginlog/page', params)
}

/**
 * 获取登录日志详情
 */
export function getLoginLogDetailApi(id: number) {
  return get<LoginLogVO>(`/auth/loginlog/${id}`)
}

/**
 * 批量删除登录日志
 */
export function deleteLoginLogApi(ids: number[]) {
  return del(`/auth/loginlog/${ids.join(',')}`)
}

/**
 * 清空所有登录日志
 */
export function clearLoginLogApi() {
  return del('/auth/loginlog/clear')
}
