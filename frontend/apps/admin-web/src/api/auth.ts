import { post } from '@/utils/request'

/**
 * 登录方式
 */
export type LoginType = 'password' | 'sms' | 'wecom' | 'dingtalk' | 'feishu'

/**
 * 登录请求参数
 */
export interface LoginParams {
  /** 登录方式 */
  type: LoginType
  /** 用户名（账号密码登录） */
  username?: string
  /** 密码（账号密码登录） */
  password?: string
  /** 手机号（短信登录） */
  phone?: string
  /** 验证码（短信登录） */
  code?: string
  /** 图形验证码 ID */
  captchaId?: string
  /** 图形验证码 */
  captchaCode?: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
  deptId?: number
  deptName?: string
  email?: string
  phone?: string
  sex?: number
  status?: number
}

/**
 * 登录响应
 */
export interface LoginResult {
  /** 访问 Token */
  accessToken: string
  /** 刷新 Token */
  refreshToken: string
  /** 过期时间（秒） */
  expiresIn: number
  /** Token 类型 */
  tokenType: string
  /** 用户信息 */
  userInfo?: {
    id: string
    username?: string
    nickname: string
    avatar: string
    source: string
    roles?: string[]
    permissions?: string[]
  }
  /** 临时 Token（密码过期时使用） */
  tempToken?: string
  /** 是否需要强制修改密码 */
  requirePasswordChange?: boolean
  /** 密码是否过期 */
  passwordExpired?: boolean
  /** 密码剩余天数 */
  passwordRemainingDays?: number
  /** 是否需要绑定 */
  needBind?: boolean
}

/**
 * 用户登录
 * @param data 登录参数
 */
export function loginApi(data: LoginParams) {
  return post<LoginResult>('/auth/login', data)
}

/**
 * 用户登出
 */
export function logoutApi() {
  return post('/auth/logout')
}

/**
 * 刷新 Token
 * @param refreshToken 刷新 Token
 */
export function refreshTokenApi(refreshToken: string) {
  return post<LoginResult>('/auth/refresh', null, {
    headers: {
      'Refresh-Token': refreshToken,
    },
  })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUserApi() {
  return post<UserInfo>('/auth/userinfo')
}

/**
 * 修改密码参数
 */
export interface ChangePasswordParams {
  userId: number
  oldPassword: string
  newPassword: string
}

/**
 * 修改密码
 */
export function changePasswordApi(data: ChangePasswordParams) {
  return post('/auth/password/change', null, {
    params: {
      userId: data.userId,
      oldPassword: data.oldPassword,
      newPassword: data.newPassword,
    },
  })
}

/**
 * 强制修改密码参数
 */
export interface ForceChangePasswordParams {
  tempToken: string
  newPassword: string
}

/**
 * 强制修改密码响应
 */
export interface ForceChangePasswordResult {
  accessToken: string
  refreshToken: string
}

/**
 * 强制修改密码（使用临时 Token）
 */
export function forceChangePasswordApi(data: ForceChangePasswordParams) {
  return post<ForceChangePasswordResult>('/auth/password/force-change', null, {
    params: {
      tempToken: data.tempToken,
      newPassword: data.newPassword,
    },
  })
}

/**
 * 权限信息
 */
export interface PermissionInfo {
  permissions: string[]
  roles: string[]
  dataScope: number
  isSuperAdmin?: boolean
}

/**
 * 检查权限刷新
 */
export function checkPermissionRefreshApi() {
  return post<{ needRefresh: boolean; timestamp?: string }>('/auth/permission/check-refresh')
}

/**
 * 刷新权限
 */
export function refreshPermissionApi() {
  return post<PermissionInfo>('/auth/permission/refresh')
}

/**
 * 获取权限信息
 */
export function getPermissionInfoApi() {
  return post<PermissionInfo>('/auth/permission/info')
}
