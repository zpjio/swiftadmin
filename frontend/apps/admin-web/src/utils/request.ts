import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  AxiosError,
} from 'axios'
import { createDiscreteApi } from 'naive-ui'
import {
  BusinessError,
  NetworkError,
  TimeoutError,
  CancelError,
  shouldRedirectLogin,
  shouldShowMessage,
  type FieldError,
  type ApiResponse,
} from '@/types'
import router from '@/router'

// 使用 naive-ui 的 createDiscreteApi 创建全局消息实例
const { message: naiveMessage } = createDiscreteApi(['message'])

/**
 * 显示错误消息
 */
function showErrorMessage(msg: string) {
  naiveMessage.error(msg)
}

/**
 * 处理未授权（登录过期）
 * 统一清理状态并跳转登录页
 */
let isRedirecting = false // 防止重复跳转
function handleUnauthorized(message = '登录已过期，请重新登录') {
  if (isRedirecting) return
  isRedirecting = true

  // 清理本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('tempToken')

  // 显示提示
  showErrorMessage(message)

  // 使用 Vue Router 进行 SPA 内跳转，避免页面刷新
  router.push('/login').finally(() => {
    // 延迟重置标志，确保跳转完成
    setTimeout(() => {
      isRedirecting = false
    }, 100)
  })
}

/**
 * 扩展的请求配置，支持静默模式
 */
export interface RequestOptions extends AxiosRequestConfig {
  /** 静默模式，不显示错误提示 */
  silent?: boolean
  /** 是否显示成功提示 */
  showSuccess?: boolean
  /** 成功提示消息 */
  successMessage?: string
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 优先使用 token，其次使用 tempToken（修改密码场景）
    const token = localStorage.getItem('token') || localStorage.getItem('tempToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果是 blob 类型（文件下载），直接返回完整响应
    if (response.config.responseType === 'blob') {
      return response as unknown as AxiosResponse<ApiResponse>
    }

    const res = response.data
    const options = response.config as RequestOptions
    const silent = options.silent

    // 后端成功码为 0
    if (res.code !== 0) {
      // 创建业务错误对象，保留完整信息
      const error = new BusinessError(res.code, res.message, {
        traceId: res.traceId,
        fieldErrors: res.error?.fieldErrors,
        detail: res.error,
      })

      // Token 过期或无效，跳转登录（handleUnauthorized 内部已显示提示）
      if (shouldRedirectLogin(res.code)) {
        handleUnauthorized()
        return Promise.reject(error)
      }

      // 非静默模式且需要提示
      if (!silent && shouldShowMessage(error.type)) {
        showErrorMessage(error.getFormattedMessage())
      }

      return Promise.reject(error)
    }

    // 成功提示（可选）
    if (options.showSuccess && options.successMessage) {
      naiveMessage.success(options.successMessage)
    }

    // 直接返回业务数据
    return res.data as unknown as AxiosResponse<ApiResponse>
  },
  (error: AxiosError<ApiResponse>) => {
    const options = error.config as RequestOptions
    const silent = options?.silent

    // 请求取消
    if (axios.isCancel(error)) {
      return Promise.reject(new CancelError())
    }

    // 请求超时
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      if (!silent) {
        showErrorMessage('请求超时，请稍后重试')
      }
      return Promise.reject(new TimeoutError())
    }

    // 网络错误
    if (!error.response) {
      if (!silent) {
        showErrorMessage('网络连接失败，请检查网络')
      }
      return Promise.reject(new NetworkError())
    }

    // HTTP 错误响应
    const responseData = error.response.data
    const status = error.response.status

    // 401 未授权（Security 层拦截，非业务层返回）
    if (status === 401) {
      handleUnauthorized('登录已过期，请重新登录')
      return Promise.reject(new BusinessError(102001, '未登录或登录已过期'))
    }

    // 403 禁止访问
    if (status === 403) {
      if (!silent) {
        showErrorMessage('无权限访问')
      }
      return Promise.reject(new BusinessError(102002, '无权限访问'))
    }

    // 其他 HTTP 错误
    const message = responseData?.message || getHttpErrorMessage(status)
    if (!silent) {
      showErrorMessage(message)
    }

    return Promise.reject(
      new BusinessError(responseData?.code || status, message, {
        traceId: responseData?.traceId,
      })
    )
  }
)

/**
 * 获取 HTTP 状态码对应的错误消息
 */
function getHttpErrorMessage(status: number): string {
  const messages: Record<number, string> = {
    400: '请求参数错误',
    404: '请求的资源不存在',
    405: '请求方法不允许',
    408: '请求超时',
    500: '服务器内部错误',
    502: '网关错误',
    503: '服务暂不可用',
    504: '网关超时',
  }
  return messages[status] || `请求失败 (${status})`
}

/**
 * 过滤空参数
 */
function filterParams(params?: Record<string, unknown>): Record<string, unknown> | undefined {
  if (!params) return params
  const result: Record<string, unknown> = {}
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
  })
  return Object.keys(result).length > 0 ? result : undefined
}

/**
 * GET 请求
 * @param url 请求地址
 * @param params 查询参数（会被过滤空值）
 * @param config 请求配置
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function get<T = unknown>(url: string, params?: any, config?: RequestOptions): Promise<T> {
  return service.get(url, { params: filterParams(params), ...config })
}

/**
 * POST 请求
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: RequestOptions
): Promise<T> {
  return service.post(url, data, config)
}

/**
 * PUT 请求
 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  config?: RequestOptions
): Promise<T> {
  return service.put(url, data, config)
}

/**
 * DELETE 请求
 */
export function del<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  config?: RequestOptions
): Promise<T> {
  return service.delete(url, { params: filterParams(params), ...config })
}

/**
 * 文件下载
 */
export function download(
  url: string,
  params?: Record<string, unknown>,
  filename?: string,
  options?: RequestOptions
): Promise<void> {
  const silent = options?.silent

  return service
    .get(url, {
      params: filterParams(params),
      responseType: 'blob',
      ...options,
    })
    .then((response: AxiosResponse<Blob>) => {
      const blob = response.data

      // 检查是否为错误响应（后端可能返回 JSON 错误）
      if (blob.type === 'application/json') {
        return blob.text().then((text) => {
          try {
            const errorData = JSON.parse(text) as ApiResponse
            const error = new BusinessError(errorData.code, errorData.message, {
              traceId: errorData.traceId,
            })
            if (!silent) {
              showErrorMessage(error.getFormattedMessage())
            }
            throw error
          } catch (e) {
            // 如果是 BusinessError，直接抛出
            if (e instanceof BusinessError) {
              throw e
            }
            // JSON 解析失败
            if (!silent) {
              showErrorMessage('文件下载失败')
            }
            throw new BusinessError(504001, '文件下载失败')
          }
        })
      }

      const contentDisposition = response.headers['content-disposition']
      let downloadFilename = filename

      if (!downloadFilename && contentDisposition) {
        const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (match && match[1]) {
          downloadFilename = decodeURIComponent(match[1].replace(/['"]/g, ''))
        }
      }

      if (!downloadFilename) {
        downloadFilename = 'download'
      }

      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = downloadFilename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(link.href)
    })
}

// 导出类型和错误类
export { BusinessError, NetworkError, TimeoutError, CancelError }
// 重导出 ApiResponse 便于使用
export type { ApiResponse, FieldError } from '@/types'

export default service