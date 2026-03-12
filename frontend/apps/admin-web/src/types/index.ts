/**
 * 分页响应结果
 */
export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
  searchCount: boolean
  optimizeCountSql: boolean
  hitCount: boolean
  countId: string | null
  maxLimit: number | null
  orders: OrderItem[]
}

/**
 * 排序项
 */
export interface OrderItem {
  column: string
  asc: boolean
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
