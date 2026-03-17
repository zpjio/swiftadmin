/**
 * 错误模块编码
 * 错误码格式: XYYZZZ
 *   X   - 模块编码
 *   YY  - 错误类型
 *   ZZZ - 序号
 */
export const ErrorModule = {
  /** 成功 */
  SUCCESS: 0,
  /** 公共模块 */
  COMMON: 1,
  /** 认证模块 */
  AUTH: 2,
  /** 用户模块 */
  USER: 3,
  /** 系统模块 */
  SYSTEM: 4,
  /** 文件模块 */
  FILE: 5,
  /** 消息模块 */
  MESSAGE: 6,
  /** 定时任务模块 */
  SCHEDULE: 7,
} as const

export type ErrorModuleType = typeof ErrorModule[keyof typeof ErrorModule]

/**
 * 错误类型编码
 */
export const ErrorType = {
  /** 成功 */
  SUCCESS: 0,
  /** 参数错误 */
  PARAM: 1,
  /** 权限错误 */
  AUTH: 2,
  /** 业务错误 */
  BIZ: 3,
  /** 系统错误 */
  SYSTEM: 4,
} as const

export type ErrorTypeValue = typeof ErrorType[keyof typeof ErrorType]

/**
 * 字段级错误信息
 */
export interface FieldError {
  /** 字段名 */
  field: string
  /** 错误消息 */
  message: string
  /** 被拒绝的值 */
  rejectedValue?: unknown
}

/**
 * 错误详情
 */
export interface ErrorDetail {
  /** 错误类型: PARAM | AUTH | BIZ | SYSTEM */
  type: string
  /** 字段级错误（表单校验） */
  fieldErrors?: FieldError[]
  /** 是否需要重试 */
  retryable?: boolean
  /** 文档链接 */
  docUrl?: string
}

/**
 * 解析错误码
 * @param code 错误码
 * @returns 解析后的错误信息
 */
export function parseErrorCode(code: number): {
  module: ErrorModuleType
  type: ErrorTypeValue
  seq: number
} {
  // 成功码
  if (code === 0) {
    return { module: ErrorModule.SUCCESS, type: ErrorType.SUCCESS, seq: 0 }
  }

  // 边界校验：错误码应为 1-999999 之间的正整数
  if (!Number.isInteger(code) || code < 1 || code > 999999) {
    return { module: ErrorModule.COMMON, type: ErrorType.SYSTEM, seq: 0 }
  }

  return {
    module: Math.floor(code / 100000) as ErrorModuleType,
    type: Math.floor((code / 1000) % 100) as ErrorTypeValue,
    seq: code % 1000,
  }
}

/**
 * 获取错误类型
 * @param code 错误码
 * @returns 错误类型
 */
export function getErrorType(code: number): ErrorTypeValue {
  return parseErrorCode(code).type
}

/**
 * 是否需要提示用户
 * @param type 错误类型
 * @returns 是否需要提示
 */
export function shouldShowMessage(type: ErrorTypeValue): boolean {
  return type !== ErrorType.SUCCESS
}

/**
 * 是否需要跳转登录
 * @param code 错误码
 * @returns 是否需要跳转
 */
export function shouldRedirectLogin(code: number): boolean {
  // 认证模块的权限错误需要跳转登录
  const { module, type } = parseErrorCode(code)
  return module === ErrorModule.AUTH && type === ErrorType.AUTH
}

/**
 * 是否为系统错误
 * @param code 错误码
 * @returns 是否为系统错误
 */
export function isSystemError(code: number): boolean {
  return getErrorType(code) === ErrorType.SYSTEM
}

/**
 * 是否为参数错误
 * @param code 错误码
 * @returns 是否为参数错误
 */
export function isParamError(code: number): boolean {
  return getErrorType(code) === ErrorType.PARAM
}

/**
 * 业务错误类
 * 保留完整的错误信息，便于调用方精细化处理
 */
export class BusinessError extends Error {
  /** 错误码 */
  readonly code: number

  /** 错误类型 */
  readonly type: ErrorTypeValue

  /** 错误模块 */
  readonly module: ErrorModuleType

  /** 链路追踪 ID */
  readonly traceId?: string

  /** 字段级错误 */
  readonly fieldErrors?: FieldError[]

  /** 错误详情 */
  readonly detail?: ErrorDetail

  constructor(
    code: number,
    message: string,
    options?: {
      traceId?: string
      fieldErrors?: FieldError[]
      detail?: ErrorDetail
    }
  ) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
    this.type = getErrorType(code)
    this.module = parseErrorCode(code).module
    this.traceId = options?.traceId
    this.fieldErrors = options?.fieldErrors
    this.detail = options?.detail
  }

  /**
   * 是否为参数错误
   */
  isParamError(): boolean {
    return this.type === ErrorType.PARAM
  }

  /**
   * 是否为权限错误
   */
  isAuthError(): boolean {
    return this.type === ErrorType.AUTH
  }

  /**
   * 是否为业务错误
   */
  isBusinessError(): boolean {
    return this.type === ErrorType.BIZ
  }

  /**
   * 是否为系统错误
   */
  isSystemError(): boolean {
    return this.type === ErrorType.SYSTEM
  }

  /**
   * 是否需要跳转登录
   */
  needRedirectLogin(): boolean {
    return shouldRedirectLogin(this.code)
  }

  /**
   * 获取格式化的错误信息（包含 traceId）
   */
  getFormattedMessage(): string {
    if (this.traceId && this.traceId !== 'N/A') {
      return `${this.message} (追踪ID: ${this.traceId})`
    }
    return this.message
  }
}

/**
 * 网络错误类
 */
export class NetworkError extends Error {
  constructor(message: string = '网络连接失败，请检查网络') {
    super(message)
    this.name = 'NetworkError'
  }
}

/**
 * 请求超时错误类
 */
export class TimeoutError extends Error {
  constructor(message: string = '请求超时，请稍后重试') {
    super(message)
    this.name = 'TimeoutError'
  }
}

/**
 * 请求取消错误类
 */
export class CancelError extends Error {
  constructor(message: string = '请求已取消') {
    super(message)
    this.name = 'CancelError'
  }
}