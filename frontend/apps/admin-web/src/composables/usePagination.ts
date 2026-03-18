import { reactive, computed, type ComputedRef } from 'vue'

/**
 * 分页参数接口
 */
export interface PaginationParams {
  page: number
  pageSize: number
}

/**
 * 分页状态接口
 */
export interface PaginationState extends PaginationParams {
  showSizePicker: boolean
  pageSizes: number[]
  itemCount: number
  onChange?: (page: number) => void
  onUpdatePageSize?: (pageSize: number) => void
}

/**
 * 分页组合式函数返回值
 */
export interface UsePaginationReturn {
  state: PaginationState
  getParams: () => PaginationParams
  paginationProps: ComputedRef<PaginationState>
  goToPage: (page: number) => void
  setPageSize: (pageSize: number) => void
  reset: () => void
  resetPage: () => void
  setTotal: (total: number) => void
  getIndex: (index: number) => number
}

/**
 * 分页组合式函数
 * @param options 分页配置选项
 * @returns 分页状态和操作方法
 *
 * @example
 * ```ts
 * const { state, paginationProps, setTotal, getParams } = usePagination({
 *   onChange: () => loadData(),
 *   onPageSizeChange: () => loadData(),
 * })
 *
 * // 在模板中
 * <NDataTable :pagination="paginationProps" />
 *
 * // 加载数据后设置总数
 * setTotal(res.totalRow)
 *
 * // 获取请求参数
 * const params = { ...queryForm, ...getParams() }
 * ```
 */
export function usePagination(options: {
  defaultPageSize?: number
  pageSizes?: number[]
  onChange?: (page: number) => void
  onPageSizeChange?: (pageSize: number) => void
} = {}): UsePaginationReturn {
  const {
    defaultPageSize = 10,
    pageSizes = [10, 20, 50, 100],
    onChange,
    onPageSizeChange,
  } = options

  const state = reactive<PaginationState>({
    page: 1,
    pageSize: defaultPageSize,
    showSizePicker: true,
    pageSizes,
    itemCount: 0,
    onChange: (page: number) => {
      state.page = page
      onChange?.(page)
    },
    onUpdatePageSize: (pageSize: number) => {
      state.pageSize = pageSize
      state.page = 1 // 重置到第一页
      onPageSizeChange?.(pageSize)
    },
  })

  /**
   * 跳转到指定页
   */
  const goToPage = (page: number) => {
    state.page = page
    onChange?.(page)
  }

  /**
   * 设置每页条数
   */
  const setPageSize = (pageSize: number) => {
    state.pageSize = pageSize
    state.page = 1
    onPageSizeChange?.(pageSize)
  }

  /**
   * 重置分页到第一页
   */
  const reset = () => {
    state.page = 1
    state.itemCount = 0
  }

  /**
   * 只重置页码到第一页（不重置总数）
   */
  const resetPage = () => {
    state.page = 1
  }

  /**
   * 设置总条数
   */
  const setTotal = (total: number) => {
    state.itemCount = total
  }

  /**
   * 获取分页请求参数
   */
  const getParams = (): PaginationParams => ({
    page: state.page,
    pageSize: state.pageSize,
  })

  /**
   * 计算序号
   */
  const getIndex = (index: number): number => {
    return (state.page - 1) * state.pageSize + index + 1
  }

  /**
   * Naive UI 分页配置（computed 以支持响应式更新）
   */
  const paginationProps = computed(() => state)

  return {
    state,
    getParams,
    paginationProps,
    goToPage,
    setPageSize,
    reset,
    resetPage,
    setTotal,
    getIndex,
  }
}