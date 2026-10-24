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
 * 状态选项（用于下拉选择）
 */
export const StatusOptions = [
  { label: '全部', value: undefined },
  { label: '正常', value: Status.ENABLED },
  { label: '禁用', value: Status.DISABLED },
]

/**
 * 状态选项（不带"全部"选项，用于表单）
 */
export const StatusFormOptions = [
  { label: '正常', value: Status.ENABLED },
  { label: '禁用', value: Status.DISABLED },
]

/**
 * 性别常量
 */
export const Sex = {
  UNKNOWN: 0,
  MALE: 1,
  FEMALE: 2,
} as const

export type SexType = typeof Sex[keyof typeof Sex]

/**
 * 性别标签
 */
export const SexLabels: Record<number, string> = {
  [Sex.UNKNOWN]: '未知',
  [Sex.MALE]: '男',
  [Sex.FEMALE]: '女',
}

/**
 * 获取性别标签
 */
export function getSexLabel(sex: number): string {
  return SexLabels[sex] || '未知'
}

/**
 * 性别选项（用于下拉选择）
 */
export const SexOptions = [
  { label: '未知', value: Sex.UNKNOWN },
  { label: '男', value: Sex.MALE },
  { label: '女', value: Sex.FEMALE },
] as const

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

/**
 * 状态颜色映射
 */
export const StatusColors: Record<number, string> = {
  [Status.DISABLED]: ACTION_COLORS.danger,
  [Status.ENABLED]: ACTION_COLORS.success,
}

/**
 * 渲染状态标签
 * @param status 状态值
 * @returns 带颜色的状态文本
 */
export function renderStatus(status: number): { text: string; color: string } {
  return {
    text: getStatusLabel(status),
    color: StatusColors[status] || ACTION_COLORS.info,
  }
}