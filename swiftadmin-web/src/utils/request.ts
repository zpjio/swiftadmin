import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { showMessage } from './status'

/**
 * 后端统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  traceId?: string
  timestamp?: number
}

function isAnonymousAuth(url?: string): boolean {
  if (!url) return false
  const path = url.split('?')[0] ?? ''
  return (
    path.endsWith('/auth/login') ||
    path.endsWith('/auth/refresh') ||
    path.includes('/auth/sms/') ||
    path.includes('/auth/captcha') ||
    path.endsWith('/auth/password/force-change') ||
    path.includes('/auth/social/url/') ||
    path.includes('/auth/social/callback/') ||
    path.includes('/auth/login-config')
  )
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  },
  // 跨域请求时携带凭证
  withCredentials: false
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    if (!isAnonymousAuth(config.url)) {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果是 blob 类型（文件下载），直接返回完整响应
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data

    // 后端成功码为 0
    if (res.code !== 0) {
      // 401: 未授权，需要重新登录
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
      }

      const msg = showMessage(res.code, res.message)
      return Promise.reject(new Error(msg))
    }

    // 直接返回业务数据，避免 res.data.data
    return res.data
  },
  (error) => {
    console.error('[Response Error]', error.response?.data)
    const responseData = error.response?.data
    const message = responseData?.message || error.message || '网络错误'
    return Promise.reject(new Error(message))
  }
)

// 过滤空参数
function filterParams(params?: any): any {
  if (!params) return params
  const result: any = {}
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
  })
  return result
}

// 封装请求方法
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  // 响应拦截器把 AxiosResponse 解包成 data，axios 1.21 的声明跟不上这个变换。
  return service.get(url, { params: filterParams(params), ...config }) as Promise<T>
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as Promise<T>
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as Promise<T>
}

export function del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, { params, ...config }) as Promise<T>
}

export default service
