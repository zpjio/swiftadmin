import { h, type VNode } from 'vue'
import { ACTION_COLORS } from '@/utils/constants'

/**
 * 操作按钮配置
 */
export interface ActionButtonConfig {
  /** 按钮文本 */
  label: string
  /** 点击回调 */
  onClick: () => void
  /** 按钮颜色 */
  color?: string
  /** 是否显示（默认 true） */
  show?: boolean
}

/**
 * 操作按钮样式
 */
const BUTTON_STYLE = {
  padding: '0 8px',
  background: 'none',
  border: 'none',
  cursor: 'pointer'
}

/**
 * 渲染单个操作按钮
 */
export function renderActionButton(label: string, color: string, onClick: () => void): VNode {
  return h(
    'button',
    {
      style: { ...BUTTON_STYLE, color },
      onClick
    },
    label
  )
}

/**
 * 渲染操作按钮组
 * @param buttons 按钮配置数组
 * @returns VNode
 */
export function renderActionButtons(buttons: ActionButtonConfig[]): VNode {
  const visibleButtons = buttons.filter((btn) => btn.show !== false)
  return h(
    'div',
    { style: { display: 'flex', gap: '8px' } },
    visibleButtons.map((btn) =>
      renderActionButton(btn.label, btn.color || ACTION_COLORS.primary, btn.onClick)
    )
  )
}

/**
 * 创建编辑按钮配置
 */
export function editAction(onClick: () => void, show = true): ActionButtonConfig {
  return {
    label: '编辑',
    color: ACTION_COLORS.primary,
    onClick,
    show
  }
}

/**
 * 创建删除按钮配置
 */
export function deleteAction(onClick: () => void, show = true): ActionButtonConfig {
  return {
    label: '删除',
    color: ACTION_COLORS.danger,
    onClick,
    show
  }
}

/**
 * 表格操作列渲染组合式函数
 *
 * @example
 * ```ts
 * const { renderActions, edit, del } = useTableActions()
 *
 * // 在列定义中
 * {
 *   title: '操作',
 *   key: 'actions',
 *   render: (row) => renderActions([
 *     edit(() => handleEdit(row), hasPermission('xxx:edit')),
 *     del(() => handleDelete(row), hasPermission('xxx:delete')),
 *   ])
 * }
 * ```
 */
export function useTableActions() {
  return {
    renderActionButton,
    renderActionButtons,
    renderActions: renderActionButtons,
    edit: editAction,
    del: deleteAction
  }
}
