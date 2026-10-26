import type { FieldError } from './error'

/**
 * 分页响应结果
 */
export interface PageResult<T = unknown> {
  /** 数据列表 */
  records: T[]
  /** 总记录数 */
  totalRow: number
  /** 每页大小 */
  pageSize: number
  /** 当前页码 */
  pageNumber: number
  /** 总页数 */
  totalPage: number
}

/**
 * 分页请求参数
 */
export interface PageQuery {
  /** 当前页码 */
  pageNumber?: number
  /** 每页大小 */
  pageSize?: number
  /** 排序字段 */
  orderBy?: string
  /** 排序方式: asc | desc */
  order?: 'asc' | 'desc'
}

/**
 * 后端统一响应格式
 */
export interface ApiResponse<T = unknown> {
  /** 响应码：0 表示成功，其他表示失败 */
  code: number
  /** 响应消息 */
  message: string
  /** 响应数据 */
  data: T
  /** 链路追踪 ID */
  traceId?: string
  /** 响应时间戳 */
  timestamp?: number
  /** 错误详情 */
  error?: {
    type: string
    fieldErrors?: FieldError[]
    retryable?: boolean
    docUrl?: string
  }
}

// 重导出错误相关类型
export {
  ErrorModule,
  ErrorType,
  BusinessError,
  NetworkError,
  TimeoutError,
  CancelError,
  parseErrorCode,
  getErrorType,
  shouldRedirectLogin,
  shouldShowMessage,
  isSystemError,
  isParamError,
  type FieldError,
  type ErrorDetail
} from './error'

// 重导出菜单相关类型
export {
  MenuType,
  type MenuTypeEnum,
  type MenuVO,
  type MenuTreeVO,
  type MenuDTO,
  type RouteMeta
} from './menu'
