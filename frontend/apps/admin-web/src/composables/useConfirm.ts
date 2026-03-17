import { useDialog, useMessage } from 'naive-ui'
import { getErrorMessage } from '@/utils/error'

/**
 * 确认对话框选项
 */
export interface ConfirmOptions {
  title?: string
  content: string
  confirmText?: string
  cancelText?: string
  type?: 'warning' | 'error' | 'info' | 'success'
}

/**
 * 确认对话框组合式函数
 *
 * 注意：必须在 NDialogProvider 和 NMessageProvider 组件内部使用
 *
 * @example
 * ```vue
 * <template>
 *   <NDialogProvider>
 *     <NMessageProvider>
 *       <MyComponent />
 *     </NMessageProvider>
 *   </NDialogProvider>
 * </template>
 *
 * <script setup>
 * import { useConfirm } from '@/composables'
 *
 * const { confirmDelete, confirmBatchDelete } = useConfirm()
 *
 * // 删除确认
 * confirmDelete('确定删除该数据吗？', async () => {
 *   await deleteApi(id)
 * })
 *
 * // 批量删除
 * confirmBatchDelete(3, async () => {
 *   await batchDeleteApi(ids)
 * })
 * </script>
 * ```
 */
export function useConfirm() {
  const dialog = useDialog()
  const message = useMessage()

  /**
   * 显示确认对话框（核心方法）
   */
  const showConfirm = (
    options: ConfirmOptions | string,
    onConfirm: () => Promise<void> | void,
    onCancel?: () => void
  ) => {
    const opts: ConfirmOptions = typeof options === 'string' ? { content: options } : options
    const {
      title = '确认操作',
      content,
      confirmText = '确定',
      cancelText = '取消',
      type = 'info',
    } = opts

    dialog[type]({
      title,
      content,
      positiveText: confirmText,
      negativeText: cancelText,
      onPositiveClick: async () => {
        try {
          await onConfirm()
        } catch (error: unknown) {
          message.error(getErrorMessage(error))
        }
      },
      onNegativeClick: onCancel,
    })
  }

  /**
   * 显示删除确认对话框
   * @param options 配置选项或内容字符串
   * @param onConfirm 确认回调
   * @param onCancel 取消回调
   */
  const confirmDelete = (
    options: ConfirmOptions | string,
    onConfirm: () => Promise<void> | void,
    onCancel?: () => void
  ) => {
    const opts: ConfirmOptions = typeof options === 'string' ? { content: options } : options
    showConfirm(
      {
        title: '确认删除',
        type: 'warning',
        ...opts,
      },
      onConfirm,
      onCancel
    )
  }

  /**
   * 批量删除确认
   * @param count 删除数量
   * @param onConfirm 确认回调
   */
  const confirmBatchDelete = (
    count: number,
    onConfirm: () => Promise<void> | void
  ) => {
    confirmDelete(
      {
        title: '确认批量删除',
        content: `确定要删除选中的 ${count} 条数据吗？`,
      },
      onConfirm
    )
  }

  return {
    confirm: showConfirm,
    confirmDelete,
    confirmBatchDelete,
  }
}