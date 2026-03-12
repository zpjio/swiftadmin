import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 字典类型信息
 */
export interface DictTypeVO {
  id: number
  dictName: string
  dictType: string
  status: number
  remark: string
  createTime: string
  updateTime: string
}

/**
 * 字典数据信息
 */
export interface DictDataVO {
  id: number
  dictType: string
  label: string
  value: string
  sortOrder: number
  listClass: string
  isDefault: string
  status: number
  remark: string
  createTime: string
  updateTime: string
  deleted: number
}

/**
 * 字典类型查询参数
 */
export interface DictTypeQuery {
  page?: number
  size?: number
  dictName?: string
  dictType?: string
  status?: number
}

/**
 * 字典数据查询参数
 */
export interface DictDataQuery {
  page?: number
  size?: number
  dictType?: string
  dictLabel?: string
  status?: number
}

/**
 * 字典类型 DTO
 */
export interface DictTypeDTO {
  id?: number
  dictName: string
  dictType: string
  status: number
  remark?: string
}

/**
 * 字典数据 DTO
 */
export interface DictDataDTO {
  id?: number
  dictType: string
  label: string
  value: string
  sortOrder?: number
  listClass?: string
  isDefault?: string
  status: number
  remark?: string
}

/**
 * 分页查询字典类型列表
 */
export function getDictTypePageApi(params: DictTypeQuery) {
  return get<PageResult<DictTypeVO>>('/system/dict-type/page', params)
}

/**
 * 获取字典类型详情
 */
export function getDictTypeDetailApi(id: number) {
  return get<DictTypeVO>(`/system/dict-type/${id}`)
}

/**
 * 创建字典类型
 */
export function createDictTypeApi(data: DictTypeDTO) {
  return post('/system/dict-type', data)
}

/**
 * 更新字典类型
 */
export function updateDictTypeApi(data: DictTypeDTO) {
  return put('/system/dict-type', data)
}

/**
 * 删除字典类型
 */
export function deleteDictTypeApi(ids: number[]) {
  return del(`/system/dict-type/${ids.join(',')}`)
}

/**
 * 分页查询字典数据列表
 */
export function getDictDataPageApi(params: DictDataQuery) {
  return get<PageResult<DictDataVO>>('/system/dict-data/page', params)
}

/**
 * 获取字典数据详情
 */
export function getDictDataDetailApi(id: number) {
  return get<DictDataVO>(`/system/dict-data/${id}`)
}

/**
 * 创建字典数据
 */
export function createDictDataApi(data: DictDataDTO) {
  return post('/system/dict-data', data)
}

/**
 * 更新字典数据
 */
export function updateDictDataApi(data: DictDataDTO) {
  return put('/system/dict-data', data)
}

/**
 * 删除字典数据
 */
export function deleteDictDataApi(ids: number[]) {
  return del(`/system/dict-data/${ids.join(',')}`)
}

/**
 * 根据类型获取字典数据（用于下拉选择）
 */
export function getDictDataByTypeApi(dictType: string) {
  return get<DictDataVO[]>(`/system/dict/type/${dictType}`)
}

/**
 * 刷新字典缓存
 */
export function refreshDictCacheApi() {
  return post('/system/dict/refresh')
}
