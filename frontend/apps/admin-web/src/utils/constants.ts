import { MenuType } from '@/types'

/**
 * 菜单类型标签
 */
export const MenuTypeLabels: Record<number, string> = {
  [MenuType.DIRECTORY]: '目录',
  [MenuType.MENU]: '菜单',
  [MenuType.BUTTON]: '按钮',
}

/**
 * 获取菜单类型标签
 */
export function getMenuTypeLabel(type: number): string {
  return MenuTypeLabels[type] || '未知'
}

/**
 * 状态常量
 */
export const Status = {
  /** 禁用 */
  DISABLED: 0,
  /** 正常 */
  ENABLED: 1,
} as const

export type StatusType = typeof Status[keyof typeof Status]

/**
 * 状态标签
 */
export const StatusLabels: Record<number, string> = {
  [Status.DISABLED]: '禁用',
  [Status.ENABLED]: '正常',
}

/**
 * 获取状态标签
 */
export function getStatusLabel(status: number): string {
  return StatusLabels[status] || '未知'
}

/**
 * 文件类型
 */
export const FileType = {
  IMAGE: 'image',
  VIDEO: 'video',
  AUDIO: 'audio',
  DOCUMENT: 'doc',
  OTHER: 'other',
} as const

export type FileTypeValue = typeof FileType[keyof typeof FileType]

/**
 * 文件扩展名图标映射
 * 返回不带前缀的图标名，配合 getIconClass 使用
 * @example getIconClass(FileTypeIcons['pdf']) // 返回 'mdi:file-pdf'
 */
export const FileTypeIcons: Record<string, string> = {
  // 图片
  jpg: 'image',
  jpeg: 'image',
  png: 'image',
  gif: 'image',
  webp: 'image',
  bmp: 'image',
  svg: 'image',
  // 文档
  pdf: 'file-pdf-box',
  doc: 'file-word-box',
  docx: 'file-word-box',
  xls: 'file-excel-box',
  xlsx: 'file-excel-box',
  ppt: 'file-powerpoint-box',
  pptx: 'file-powerpoint-box',
  txt: 'file-document-outline',
  // 压缩
  zip: 'zip-box',
  rar: 'zip-box',
  '7z': 'zip-box',
  // 视频
  mp4: 'video',
  webm: 'video',
  avi: 'video',
  mov: 'video',
  // 音频
  mp3: 'music',
  wav: 'music',
  flac: 'music',
}

/**
 * 获取文件类型图标
 * @param extension 文件扩展名
 * @returns 图标名（不带前缀）
 */
export function getFileTypeIcon(extension: string): string {
  const ext = extension?.toLowerCase() || ''
  return FileTypeIcons[ext] || 'file'
}

/**
 * 表格操作按钮样式
 */
export const TABLE_ACTION_STYLE = {
  button: {
    padding: '0 8px',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
  },
  container: {
    display: 'flex',
    gap: '8px',
  },
} as const

/**
 * 操作按钮颜色
 */
export const ACTION_COLORS = {
  primary: '#2563eb',
  success: '#059669',
  warning: '#d97706',
  danger: '#dc2626',
  info: '#0891b2',
} as const