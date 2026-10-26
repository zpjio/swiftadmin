/**
 * 认证相关工具函数
 */

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refreshToken'
const TEMP_TOKEN_KEY = 'tempToken'

/**
 * 获取访问令牌
 * 优先使用 token，其次使用 tempToken（修改密码等场景）
 */
export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY) || localStorage.getItem(TEMP_TOKEN_KEY)
}

/**
 * 获取刷新令牌
 */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

/**
 * 设置访问令牌
 */
export function setAuthToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 设置刷新令牌
 */
export function setRefreshToken(token: string): void {
  localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

/**
 * 清除所有令牌
 */
export function clearAuthTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TEMP_TOKEN_KEY)
}

/**
 * 构建认证请求头
 */
export function buildAuthHeader(): Record<string, string> {
  const token = getAuthToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/**
 * 检查是否已登录
 */
export function isAuthenticated(): boolean {
  return !!getAuthToken()
}
