import { reactive } from 'vue'

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
}

/**
 * 分页组合式函数
 * @param options 分页配置选项
 * @returns 分页状态和操作方法
 */
export function usePagination(options: {
  defaultPageSize?: number
  pageSizes?: number[]
  onChange?: (page: number) => void
  onPageSizeChange?: (pageSize: number) => void
} = {}) {
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
    state.page = 1 // 重置到第一页
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
   * 设置总条数
   */
  const setTotal = (total: number) => {
    state.itemCount = total
  }

  /**
   * 获取分页请求参数（直接返回响应式对象的属性）
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
   * Naive UI 分页配置
   */
  const getPaginationProps = () => ({
    page: state.page,
    pageSize: state.pageSize,
    showSizePicker: state.showSizePicker,
    pageSizes: state.pageSizes,
    itemCount: state.itemCount,
    onChange: goToPage,
    onUpdatePageSize: setPageSize,
  })

  return {
    state,
    getParams,
    getPaginationProps,
    goToPage,
    setPageSize,
    reset,
    setTotal,
    getIndex,
  }
}