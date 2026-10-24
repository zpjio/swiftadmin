import { get, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 操作日志信息
 */
export interface OperLogVO {
  id: number
  title: string
  businessType: number
  method: string
  requestMethod: string
  operatorType: number
  operName: string
  deptName: string
  operUrl: string
  operIp: string
  operLocation: string
  operParam: string
  jsonResult: string
  status: number
  errorMsg: string
  operTime: string
  costTime: number
}

/**
 * 操作日志查询参数
 */
export interface OperLogQuery {
  page?: number
  size?: number
  title?: string
  operName?: string
  businessType?: number
  status?: number
  beginTime?: string
  endTime?: string
}

/**
 * 分页查询操作日志列表
 */
export function getOperLogPageApi(params: OperLogQuery) {
  return get<PageResult<OperLogVO>>('/system/operlog/page', params)
}

/**
 * 获取操作日志详情
 */
export function getOperLogDetailApi(id: number) {
  return get<OperLogVO>(`/system/operlog/${id}`)
}

/**
 * 批量删除操作日志
 */
export function deleteOperLogApi(ids: number[]) {
  return del(`/system/operlog/${ids.join(',')}`)
}

/**
 * 清空所有操作日志
 */
export function clearOperLogApi() {
  return del('/system/operlog/clear')
}
