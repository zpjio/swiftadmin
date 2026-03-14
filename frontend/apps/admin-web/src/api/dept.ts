import { get, post, put, del } from '@/utils/request'

/**
 * 部门信息
 */
export interface DeptVO {
  id: number
  parentId: number
  name: string
  sortOrder: number
  leader: string
  phone: string
  email: string
  status: number
  createTime: string
  updateTime: string
  children?: DeptVO[]
}

/**
 * 部门创建/更新参数
 */
export interface DeptDTO {
  id?: number
  parentId: number
  name: string
  sortOrder?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
}

/**
 * 获取部门树
 * @param status 状态过滤
 * @param silent 静默模式，权限不足时不显示错误提示
 */
export function getDeptTreeApi(status?: number, silent?: boolean) {
  return get<DeptVO[]>('/system/dept/tree', { status }, { silent })
}

/**
 * 获取部门详情
 */
export function getDeptDetailApi(id: number) {
  return get<DeptVO>(`/system/dept/${id}`)
}

/**
 * 创建部门
 */
export function createDeptApi(data: DeptDTO) {
  return post<number>('/system/dept', data)
}

/**
 * 更新部门
 */
export function updateDeptApi(data: DeptDTO) {
  return put('/system/dept', data)
}

/**
 * 删除部门
 */
export function deleteDeptApi(id: number) {
  return del(`/system/dept/${id}`)
}

/**
 * 获取子部门 ID 列表
 */
export function getDeptChildrenIdsApi(id: number) {
  return get<number[]>(`/system/dept/${id}/children`)
}
