import { ref, reactive, type Ref, type ComputedRef } from 'vue'
import { usePagination, type PaginationParams, type PaginationState } from './usePagination'
import { useTableSelection, type UseTableSelectionReturn } from './useTableSelection'
import type { PageResult } from '@/types'

/**
 * 分页查询函数类型
 */
export type PageFetchFn<T, Q extends Record<string, any>> = (params: Q & PaginationParams) => Promise<PageResult<T>>

/**
 * useTablePage 配置选项
 */
export interface UseTablePageOptions<T, Q extends Record<string, any>> {
  /** 分页查询 API 函数 */
  fetchFn: PageFetchFn<T, Q>
  /** 默认查询参数 */
  defaultQuery?: Q
  /** 默认每页条数 */
  defaultPageSize?: number
  /** 是否启用选择功能 */
  enableSelection?: boolean
  /** 是否立即加载 */
  immediate?: boolean
}

/**
 * useTablePage 返回值接口
 */
export interface UseTablePageReturn<T, Q extends Record<string, any>> {
  /** 加载状态 */
  loading: Ref<boolean>
  /** 表格数据 */
  tableData: Ref<T[]>
  /** 查询参数 */
  queryForm: Q
  /** 分页属性（直接传给 NDataTable） */
  paginationProps: ComputedRef<PaginationState>
  /** 设置总数 */
  setTotal: (total: number) => void
  /** 获取序号 */
  getIndex: (index: number) => number
  /** 加载数据 */
  loadData: () => Promise<void>
  /** 搜索（重置到第一页并加载） */
  handleSearch: () => void
  /** 重置查询条件 */
  handleReset: () => void
  /** 选择相关（当 enableSelection 为 true 时可用） */
  selection?: UseTableSelectionReturn<T>
}

/**
 * 表格分页组合式函数
 *
 * 整合了加载状态、表格数据、分页、选择等常见表格功能
 *
 * @param options 配置选项
 * @returns 表格相关状态和方法
 *
 * @example
 * ```ts
 * // 在组件中使用
 * const { loading, tableData, queryForm, paginationProps, loadData, handleSearch, handleReset, selection } = useTablePage({
 *   fetchFn: getUserPageApi,
 *   defaultQuery: { status: undefined },
 *   enableSelection: true,
 * })
 *
 * // 模板中
 * <NDataTable
 *   :data="tableData"
 *   :loading="loading"
 *   :pagination="paginationProps"
 *   :checked-row-keys="selection?.selectedKeys"
 *   @update:checked-row-keys="selection?.onSelectionChange"
 * />
 * ```
 */
export function useTablePage<T, Q extends Record<string, any> = Record<string, any>>(
  options: UseTablePageOptions<T, Q>
): UseTablePageReturn<T, Q> {
  const {
    fetchFn,
    defaultQuery,
    defaultPageSize = 10,
    enableSelection = false,
    immediate = false,
  } = options

  // 加载状态
  const loading = ref(false)

  // 表格数据
  const tableData = ref<T[]>([]) as Ref<T[]>

  // 查询参数
  const queryForm = reactive<Q>({
    ...defaultQuery,
  } as Q) as Q

  // 分页
  const { paginationProps, setTotal, getIndex, resetPage } = usePagination({
    defaultPageSize,
    onChange: () => loadData(),
    onPageSizeChange: () => loadData(),
  })

  // 选择功能（可选）
  const selection = enableSelection ? useTableSelection<T>() : undefined

  // 加载数据
  const loadData = async () => {
    loading.value = true
    try {
      const res = await fetchFn({
        ...queryForm,
        page: paginationProps.value.page,
        pageSize: paginationProps.value.pageSize,
      })
      tableData.value = res.records || []
      setTotal(res.totalRow || 0)
    } catch (error) {
      console.error('加载数据失败:', error)
      tableData.value = []
      setTotal(0)
    } finally {
      loading.value = false
    }
  }

  // 搜索
  const handleSearch = () => {
    resetPage()
    loadData()
  }

  // 重置查询条件
  const handleReset = () => {
    // 重置查询参数到默认值
    if (defaultQuery) {
      Object.keys(defaultQuery).forEach((key) => {
        ;(queryForm as any)[key] = (defaultQuery as any)[key]
      })
    } else {
      // 如果没有默认值，将所有属性重置为 undefined
      Object.keys(queryForm).forEach((key) => {
        ;(queryForm as any)[key] = undefined
      })
    }
    handleSearch()
  }

  // 立即加载
  if (immediate) {
    loadData()
  }

  return {
    loading,
    tableData,
    queryForm,
    paginationProps,
    setTotal,
    getIndex,
    loadData,
    handleSearch,
    handleReset,
    selection,
  }
}