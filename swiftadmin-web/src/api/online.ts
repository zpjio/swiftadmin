import { get, post, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 在线用户信息
 */
export interface OnlineUserVO {
  sessionId: string
  userId: number
  username: string
  nickname: string
  deptName: string
  ip: string
  location: string
  browser: string
  os: string
  loginTime: string
  expireTime: string
  ttl: number
}

/**
 * 在线用户查询参数
 */
export interface OnlineUserQuery {
  page?: number
  size?: number
  username?: string
  ip?: string
}

/**
 * 分页查询在线用户
 */
export function getOnlineUserPageApi(params: OnlineUserQuery) {
  return get<PageResult<OnlineUserVO>>('/system/online/page', params)
}

/**
 * 获取所有在线用户列表
 */
export function getOnlineUserListApi() {
  return get<OnlineUserVO[]>('/system/online/list')
}

/**
 * 获取在线用户数量
 */
export function getOnlineUserCountApi() {
  return get<number>('/system/online/count')
}

/**
 * 强制用户下线
 */
export function forceLogoutApi(sessionId: string) {
  return del(`/system/online/${sessionId}`)
}

/**
 * 批量强制用户下线
 */
export function forceLogoutBatchApi(sessionIds: string[]) {
  return post<number>('/system/online/batch', sessionIds)
}
