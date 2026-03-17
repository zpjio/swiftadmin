import { ref, computed } from 'vue'

/**
 * 表格选择组合式函数
 * @param keyField 主键字段名，默认 'id'
 * @returns 选择状态和操作方法
 */
export function useTableSelection<T extends Record<string, unknown>>(
  keyField: keyof T = 'id' as keyof T
) {
  const selectedKeys = ref<Array<string | number>>([])
  const selectedRows = ref<T[]>([])

  /**
   * 处理选择变化
   */
  const onSelectionChange = (keys: Array<string | number>) => {
    selectedKeys.value = keys
  }

  /**
   * 处理选择变化（带行数据）
   */
  const onSelectionChangeWithRows = (keys: Array<string | number>, rows: T[]) => {
    selectedKeys.value = keys
    selectedRows.value = rows
  }

  /**
   * 清空选择
   */
  const clearSelection = () => {
    selectedKeys.value = []
    selectedRows.value = []
  }

  /**
   * 全选
   */
  const selectAll = (rows: T[]) => {
    selectedKeys.value = rows.map((row) => row[keyField] as string | number)
    selectedRows.value = rows
  }

  /**
   * 切换单个选中状态
   */
  const toggleSelection = (row: T) => {
    const key = row[keyField] as string | number
    const index = selectedKeys.value.indexOf(key)
    if (index > -1) {
      selectedKeys.value.splice(index, 1)
      selectedRows.value = selectedRows.value.filter((r) => r[keyField] !== key)
    } else {
      selectedKeys.value.push(key)
      selectedRows.value.push(row)
    }
  }

  /**
   * 是否有选中项
   */
  const hasSelection = computed(() => selectedKeys.value.length > 0)

  /**
   * 选中数量
   */
  const selectionCount = computed(() => selectedKeys.value.length)

  /**
   * 获取选中的主键数组（字符串类型）
   */
  const getSelectedIds = (): string[] => {
    return selectedKeys.value.map(String)
  }

  /**
   * 获取选中的主键数组（数字类型）
   */
  const getSelectedNumbers = (): number[] => {
    return selectedKeys.value.map(Number).filter((n) => !isNaN(n))
  }

  /**
   * 获取选中的行数据
   */
  const getSelectedRows = (): T[] => {
    return [...selectedRows.value]
  }

  /**
   * Naive UI NDataTable 的 row-key 函数
   */
  const getRowKey = (row: T): string | number => {
    return row[keyField] as string | number
  }

  return {
    selectedKeys,
    selectedRows,
    hasSelection,
    selectionCount,
    onSelectionChange,
    onSelectionChangeWithRows,
    clearSelection,
    selectAll,
    toggleSelection,
    getSelectedIds,
    getSelectedNumbers,
    getSelectedRows,
    getRowKey,
  }
}