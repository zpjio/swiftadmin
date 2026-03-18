import type { TreeOption, TreeSelectOption } from 'naive-ui'

/**
 * 树形数据节点接口
 */
export interface TreeNodeBase {
  id: string | number
  name: string
  children?: TreeNodeBase[]
}

/**
 * 将树形数据转换为 NTree 选项格式
 * @param items 树形数据数组
 * @param options 转换选项
 * @returns NTree 选项数组
 */
export function convertToTreeOptions<T extends TreeNodeBase>(
  items: T[],
  options: {
    labelField?: keyof T
    keyField?: keyof T
    childrenField?: keyof T
  } = {}
): TreeOption[] {
  const {
    labelField = 'name' as keyof T,
    keyField = 'id' as keyof T,
    childrenField = 'children' as keyof T,
  } = options

  return items.map((item) => {
    const children = item[childrenField] as T[] | undefined
    const hasChildren = children && children.length > 0
    return {
      key: String(item[keyField]),
      label: String(item[labelField]),
      children: hasChildren ? convertToTreeOptions(children!, options) : undefined,
    }
  })
}

/**
 * 将树形数据转换为 NTreeSelect 选项格式
 * @param items 树形数据数组
 * @param options 转换选项
 * @returns NTreeSelect 选项数组
 */
export function convertToTreeSelectOptions<T extends TreeNodeBase>(
  items: T[],
  options: {
    labelField?: keyof T
    keyField?: keyof T
    childrenField?: keyof T
    rootLabel?: string
    rootKey?: string | number
  } = {}
): TreeSelectOption[] {
  const {
    labelField = 'name' as keyof T,
    keyField = 'id' as keyof T,
    childrenField = 'children' as keyof T,
    rootLabel,
    rootKey,
  } = options

  const convert = (items: T[]): TreeSelectOption[] => {
    return items.map((item) => {
      const children = item[childrenField] as T[] | undefined
      const hasChildren = children && children.length > 0
      return {
        key: item[keyField] as string | number,
        label: String(item[labelField]),
        children: hasChildren ? convert(children!) : undefined,
      }
    })
  }

  if (rootLabel !== undefined) {
    return [
      {
        key: rootKey ?? 0,
        label: rootLabel,
        children: convert(items),
      },
    ]
  }

  return convert(items)
}

/**
 * 获取树中所有节点 ID（包括父节点和子节点）
 * @param items 树形数据数组
 * @param options 配置选项
 * @returns 所有节点 ID 数组
 */
export function getAllIds<T extends TreeNodeBase>(
  items: T[],
  options: {
    keyField?: keyof T
    childrenField?: keyof T
  } = {}
): (string | number)[] {
  const { keyField = 'id' as keyof T, childrenField = 'children' as keyof T } = options
  const ids: (string | number)[] = []

  const traverse = (nodes: T[]) => {
    for (const node of nodes) {
      ids.push(node[keyField] as string | number)
      const children = node[childrenField] as T[] | undefined
      if (children && children.length > 0) {
        traverse(children)
      }
    }
  }

  traverse(items)
  return ids
}

/**
 * 获取树中所有父节点 ID
 * @param items 树形数据数组
 * @param options 配置选项
 * @returns 父节点 ID 数组
 */
export function getParentIds<T extends TreeNodeBase>(
  items: T[],
  options: {
    keyField?: keyof T
    childrenField?: keyof T
  } = {}
): (string | number)[] {
  const { keyField = 'id' as keyof T, childrenField = 'children' as keyof T } = options
  const ids: (string | number)[] = []

  const traverse = (nodes: T[]) => {
    for (const node of nodes) {
      const children = node[childrenField] as T[] | undefined
      if (children && children.length > 0) {
        ids.push(node[keyField] as string | number)
        traverse(children)
      }
    }
  }

  traverse(items)
  return ids
}

/**
 * 在树中查找节点
 * @param items 树形数据数组
 * @param predicate 判断条件
 * @param childrenField 子节点字段名
 * @returns 找到的节点或 null
 */
export function findInTree<T extends TreeNodeBase>(
  items: T[],
  predicate: (node: T) => boolean,
  childrenField: keyof T = 'children' as keyof T
): T | null {
  for (const item of items) {
    if (predicate(item)) {
      return item
    }
    const children = item[childrenField] as T[] | undefined
    if (children) {
      const found = findInTree(children, predicate, childrenField)
      if (found) return found
    }
  }
  return null
}