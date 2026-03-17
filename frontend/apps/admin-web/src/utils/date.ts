/**
 * 日期格式化工具函数
 */

/**
 * 格式化日期时间
 * @param date 日期字符串、时间戳或 Date 对象
 * @param format 格式模板，默认 'YYYY-MM-DD HH:mm:ss'
 * @returns 格式化后的日期字符串
 */
export function formatDateTime(
  date: string | number | Date | null | undefined,
  format = 'YYYY-MM-DD HH:mm:ss'
): string {
  if (!date) return ''

  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date

  if (isNaN(d.getTime())) return ''

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化日期（不含时间）
 * @param date 日期字符串、时间戳或 Date 对象
 * @returns 格式化后的日期字符串 (YYYY-MM-DD)
 */
export function formatDate(date: string | number | Date | null | undefined): string {
  return formatDateTime(date, 'YYYY-MM-DD')
}

/**
 * 格式化时间（不含日期）
 * @param date 日期字符串、时间戳或 Date 对象
 * @returns 格式化后的时间字符串 (HH:mm:ss)
 */
export function formatTime(date: string | number | Date | null | undefined): string {
  return formatDateTime(date, 'HH:mm:ss')
}

/**
 * 相对时间格式化（几分钟前、几小时前等）
 * @param date 日期字符串、时间戳或 Date 对象
 * @returns 相对时间字符串
 */
export function formatRelativeTime(date: string | number | Date | null | undefined): string {
  if (!date) return ''

  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date

  if (isNaN(d.getTime())) return ''

  const now = new Date()
  const diff = now.getTime() - d.getTime()

  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (seconds < 60) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 7) return `${days} 天前`

  // 超过 7 天返回具体日期
  return formatDate(d)
}

/**
 * 表格列渲染器：格式化日期时间
 * 用于 NDataTable 列定义
 */
export function renderDateTime(
  row: Record<string, unknown>,
  key: string = 'createTime'
): string {
  const value = row[key]
  if (value === null || value === undefined) return ''
  return formatDateTime(String(value))
}