import { ref, computed, shallowRef } from 'vue'

/**
 * 表格选择组合式函数
 * @param keyField 主键字段名，默认 'id'
 * @returns 选择状态和操作方法
 */
export function useTableSelection<T extends Record<string, unknown>>(
  keyField: keyof T = 'id' as keyof T
) {
  const selectedKeys = ref<Array<string | number>>([])
  // 使用 shallowRef 避免深层响应式转换，简化类型推断
  const selectedRows = shallowRef<T[]>([])

  /**
   * 处理选择变化
   */
  const onSelectionChange = (keys: Array<string | number>) => {
    selectedKeys.value = keys
    // 清空 rows，因为无法从 keys 反推 rows
    selectedRows.value = []
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
    selectedRows.value = [...rows]
  }

  /**
   * 切换单个选中状态
   */
  const toggleSelection = (row: T): void => {
    const key = row[keyField] as string | number
    const index = selectedKeys.value.indexOf(key)
    if (index > -1) {
      // 取消选中：同时移除 key 和 row
      selectedKeys.value = selectedKeys.value.filter((k) => k !== key)
      selectedRows.value = selectedRows.value.filter((r) => r[keyField] !== key)
    } else {
      // 添加选中：同时添加 key 和 row
      selectedKeys.value = [...selectedKeys.value, key]
      selectedRows.value = [...selectedRows.value, row]
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
    return selectedKeys.value
      .map((n) => {
        const num = Number(n)
        return Number.isNaN(num) ? null : num
      })
      .filter((n): n is number => n !== null)
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