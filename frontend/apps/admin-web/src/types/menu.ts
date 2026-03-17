/**
 * 菜单类型枚举
 */
export const MenuType = {
  /** 目录 */
  DIRECTORY: 1,
  /** 菜单 */
  MENU: 2,
  /** 按钮 */
  BUTTON: 3,
} as const

export type MenuTypeEnum = typeof MenuType[keyof typeof MenuType]

/**
 * 菜单信息
 */
export interface MenuVO {
  id: string
  parentId: string
  name: string
  path: string
  component: string | null
  type: MenuTypeEnum | null
  icon: string | null
  sortOrder: number
  permission: string | null
  status: number | null
  createTime?: string
  updateTime?: string
  children?: MenuVO[]
}

/**
 * 菜单树节点（简化版，用于选择）
 */
export interface MenuTreeVO {
  id: string
  parentId: string
  label: string
  type: MenuTypeEnum
  children?: MenuTreeVO[]
}

/**
 * 菜单创建/更新参数
 */
export interface MenuDTO {
  id?: string
  parentId: string
  name: string
  path?: string
  component?: string
  type: MenuTypeEnum
  icon?: string
  sortOrder?: number
  permission?: string
  status?: number
}

/**
 * 路由元信息
 * 注意：使用显式的索引签名以兼容 Record<PropertyKey, unknown>
 */
export interface RouteMeta {
  title: string
  icon?: string
  permission?: string
  hidden?: boolean
  keepAlive?: boolean
  [key: string]: unknown
  [key: symbol]: unknown
}