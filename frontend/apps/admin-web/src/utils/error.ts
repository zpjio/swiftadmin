/**
 * 错误处理工具函数
 */

/**
 * Axios 错误响应结构
 */
interface AxiosErrorResponse {
  status?: number
  statusText?: string
  data?: {
    message?: string
    msg?: string
    error?: string
  }
}

/**
 * Axios 错误结构
 */
interface AxiosError {
  response?: AxiosErrorResponse
  message?: string
  code?: string
  name?: string
}

/**
 * 从错误对象中提取错误消息
 * @param error 错误对象
 * @param defaultMessage 默认错误消息
 * @returns 错误消息字符串
 */
export function getErrorMessage(error: unknown, defaultMessage = '操作失败'): string {
  if (!error) return defaultMessage

  // 字符串错误
  if (typeof error === 'string') return error

  // Error 对象
  if (error instanceof Error) return error.message

  // API 响应错误 { message: string }
  if (typeof error === 'object') {
    const err = error as Record<string, unknown>

    // Axios 格式: error.response.data.message
    const axiosErr = err as AxiosError
    if (axiosErr.response?.data?.message) {
      return axiosErr.response.data.message
    }
    if (axiosErr.response?.data?.msg) {
      return axiosErr.response.data.msg
    }
    if (axiosErr.response?.data?.error) {
      return axiosErr.response.data.error
    }

    // 通用格式
    if (typeof err.message === 'string') return err.message
    if (typeof err.msg === 'string') return err.msg
    if (typeof err.error === 'string') return err.error
  }

  return defaultMessage
}

/**
 * 判断是否为取消错误 (Axios CancelToken 或 AbortController)
 */
export function isCancelError(error: unknown): boolean {
  if (!error) return false
  if (error instanceof Error) {
    return error.name === 'CanceledError' || error.message.includes('cancel')
  }
  return false
}

/**
 * 判断是否为网络错误
 */
export function isNetworkError(error: unknown): boolean {
  if (!error) return false
  if (error instanceof Error) {
    return (
      error.message.includes('Network Error') ||
      error.message.includes('network') ||
      error.message.includes('timeout')
    )
  }
  return false
}

/**
 * 判断是否为认证错误 (401)
 * 支持 Axios 错误格式和通用错误格式
 */
export function isAuthError(error: unknown): boolean {
  if (!error) return false
  if (typeof error === 'object') {
    const err = error as Record<string, unknown>

    // Axios 格式: error.response.status
    const axiosErr = err as AxiosError
    if (axiosErr.response?.status === 401) return true

    // 通用格式
    if (err.status === 401 || err.code === 401) return true
  }
  return false
}

/**
 * 获取 HTTP 状态码
 */
export function getErrorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== 'object') return undefined

  const err = error as Record<string, unknown>

  // Axios 格式
  const axiosErr = err as AxiosError
  if (axiosErr.response?.status) {
    return axiosErr.response.status
  }

  // 通用格式
  const status = err.status ?? err.code
  return typeof status === 'number' ? status : undefined
}