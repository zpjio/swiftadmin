import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 系统配置信息
 */
export interface ConfigVO {
  id: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  description: string
  createTime: string
  updateTime: string
}

/**
 * 系统配置 DTO
 */
export interface ConfigDTO {
  id?: number
  configName: string
  configKey: string
  configValue: string
  configType: string
  description?: string
}

/**
 * 配置查询参数
 */
export interface ConfigQuery {
  page?: number
  pageSize?: number
  configName?: string
  configKey?: string
  configType?: string
}

/**
 * 分页查询配置列表
 */
export function getConfigPageApi(params: ConfigQuery) {
  return get<PageResult<ConfigVO>>('/system/config/page', params)
}

/**
 * 获取配置详情
 */
export function getConfigDetailApi(id: number) {
  return get<ConfigVO>(`/system/config/${id}`)
}

/**
 * 根据配置键获取配置
 */
export function getConfigByKeyApi(configKey: string) {
  return get<ConfigVO>(`/system/config/key/${configKey}`)
}

/**
 * 创建配置
 */
export function createConfigApi(data: ConfigDTO) {
  return post('/system/config', data)
}

/**
 * 更新配置
 */
export function updateConfigApi(data: ConfigDTO) {
  return put('/system/config', data)
}

/**
 * 批量删除配置
 */
export function deleteConfigApi(ids: number[]) {
  return del(`/system/config/${ids.join(',')}`)
}

/**
 * 刷新配置缓存
 */
export function refreshConfigCacheApi() {
  return post('/system/config/cache/refresh')
}
