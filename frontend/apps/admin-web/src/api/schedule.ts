import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 任务状态
 */
export const JobStatus = {
  NORMAL: 0,
  PAUSED: 1
} as const

export type JobStatusType = (typeof JobStatus)[keyof typeof JobStatus]

/**
 * 任务执行状态
 */
export const JobExecuteStatus = {
  RUNNING: 0,
  SUCCESS: 1,
  FAILED: 2
} as const

export type JobExecuteStatusType = (typeof JobExecuteStatus)[keyof typeof JobExecuteStatus]

/**
 * 执行类型
 */
export const ExecuteType = {
  AUTO: 1,
  MANUAL: 2
} as const

export type ExecuteTypeValue = (typeof ExecuteType)[keyof typeof ExecuteType]

/**
 * 定时任务信息
 */
export interface JobVO {
  id: string
  jobName: string
  jobGroup: string
  beanName: string
  methodName: string
  methodParams?: string
  cronExpression: string
  timeout: number
  retryTimes: number
  retryInterval: number
  status: number
  remark?: string
  createTime: string
  updateTime: string
  nextExecuteTime?: string
}

/**
 * 定时任务 DTO
 */
export interface JobDTO {
  id?: string
  jobName: string
  jobGroup: string
  beanName: string
  methodName: string
  methodParams?: string
  cronExpression: string
  timeout: number
  retryTimes: number
  retryInterval: number
  status?: number
  remark?: string
}

/**
 * 定时任务查询参数
 */
export interface JobQuery {
  page?: number
  pageSize?: number
  jobName?: string
  jobGroup?: string
  beanName?: string
  status?: number
  createTimeBegin?: string
  createTimeEnd?: string
}

/**
 * 任务日志信息
 */
export interface JobLogVO {
  id: string
  jobId: string
  jobName: string
  jobGroup: string
  executeStatus: number
  executeTime: string
  costTime?: number
  executeResult?: string
  errorMsg?: string
  stackTrace?: string
  retryCount: number
  executeType: number
  createTime: string
}

/**
 * 任务日志查询参数
 */
export interface JobLogQuery {
  page?: number
  pageSize?: number
  jobId?: string
  jobName?: string
  jobGroup?: string
  executeStatus?: number
  executeType?: number
  executeTimeBegin?: string
  executeTimeEnd?: string
}

/**
 * 分页查询任务列表
 */
export function getJobPageApi(params: JobQuery) {
  return get<PageResult<JobVO>>('/schedule/job/page', params)
}

/**
 * 获取任务详情
 */
export function getJobDetailApi(id: string) {
  return get<JobVO>(`/schedule/job/${id}`)
}

/**
 * 创建任务
 */
export function createJobApi(data: JobDTO) {
  return post<string>('/schedule/job', data)
}

/**
 * 更新任务
 */
export function updateJobApi(data: JobDTO) {
  return put<void>('/schedule/job', data)
}

/**
 * 删除任务
 */
export function deleteJobApi(ids: string[]) {
  return del<void>(`/schedule/job/${ids.join(',')}`)
}

/**
 * 启用任务
 */
export function enableJobApi(id: string) {
  return put<void>(`/schedule/job/${id}/enable`)
}

/**
 * 暂停任务
 */
export function pauseJobApi(id: string) {
  return put<void>(`/schedule/job/${id}/pause`)
}

/**
 * 立即执行任务
 */
export function triggerJobApi(id: string) {
  return post<string>(`/schedule/job/${id}/trigger`)
}

/**
 * 分页查询任务日志列表
 */
export function getJobLogPageApi(params: JobLogQuery) {
  return get<PageResult<JobLogVO>>('/schedule/log/page', params)
}

/**
 * 获取任务日志详情
 */
export function getJobLogDetailApi(id: string) {
  return get<JobLogVO>(`/schedule/log/${id}`)
}
