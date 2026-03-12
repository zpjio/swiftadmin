/**
 * 分页响应结果
 */
export interface PageResult<T = any> {
  records: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
  totalPage: number
}

/**
 * 通用响应
 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: number
}
