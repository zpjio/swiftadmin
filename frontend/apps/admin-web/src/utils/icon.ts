/**
 * 图标工具函数
 * 统一管理项目中的图标格式
 *
 * 存储格式：mdi:xxx（如 mdi:account）- 与 @iconify 格式一致
 * 静态组件：<icon-mdi-xxx /> - unplugin-icons 自动导入
 * 动态 class：mdi:xxx - Tailwind + @iconify/tailwind
 */

/**
 * 获取图标的 class 名称（用于 Tailwind + @iconify/tailwind）
 * @param icon 图标名称，如 'mdi:account'
 * @returns class 名称，如 'mdi:account'
 */
export function getIconClass(icon: string | undefined | null): string {
  if (!icon) return ''
  // 如果已经是 mdi:xxx 格式，直接返回
  if (icon.startsWith('mdi:')) return icon
  // 如果是 mdi-xxx 格式，转换为 mdi:xxx
  if (icon.startsWith('mdi-')) return icon.replace('mdi-', 'mdi:')
  // 其他情况，添加 mdi: 前缀
  return `mdi:${icon}`
}

/**
 * 获取存储格式的图标名称
 * @param icon 图标名称
 * @returns 标准化的图标名称，如 'mdi:account'
 */
export function normalizeIconName(icon: string | undefined | null): string {
  if (!icon) return ''
  // 如果已经是 mdi:xxx 格式，直接返回
  if (icon.startsWith('mdi:')) return icon
  // 如果是 mdi-xxx 格式，转换为 mdi:xxx
  if (icon.startsWith('mdi-')) return icon.replace('mdi-', 'mdi:')
  // 其他情况，添加 mdi: 前缀
  return `mdi:${icon}`
}

/**
 * 转换为 unplugin-icons 组件格式
 * @param icon 图标名称，如 'mdi:account'
 * @returns 组件标签名格式，如 'mdi-account'
 */
export function toComponentFormat(icon: string | undefined | null): string {
  if (!icon) return ''
  // mdi:account -> mdi-account
  return icon.replace('mdi:', 'mdi-')
}

/**
 * 创建图标渲染函数（用于 h() 函数）
 * @param icon 图标名称
 * @param extraClass 额外的 class
 * @returns VNode
 */
export function renderIcon(icon: string | undefined | null, extraClass: string = '') {
  return (h: any) => h('i', { class: `${getIconClass(icon)} ${extraClass}`.trim() })
}