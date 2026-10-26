/**
 * URL 处理工具函数
 */

/**
 * 确保 URL 有协议头
 * @param url 原始 URL
 * @param protocol 默认协议，默认 'https'
 * @returns 带协议头的 URL
 */
export function normalizeUrl(url: string | undefined | null, protocol = 'https'): string {
  if (!url) return ''

  // 已经有协议头
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  // 以 // 开头（协议相对 URL）
  if (url.startsWith('//')) {
    return `${protocol}:${url}`
  }

  // 添加协议头和双斜杠
  return `${protocol}://${url}`
}

/**
 * 判断是否为绝对 URL (带协议头)
 */
export function isAbsoluteUrl(url: string): boolean {
  return /^https?:\/\//i.test(url)
}

/**
 * 拼接 URL 路径
 * @param base 基础路径
 * @param path 相对路径
 * @returns 完整路径
 */
export function joinUrl(base: string, path: string): string {
  if (!base && !path) return ''
  if (!base) return path
  if (!path) return base

  const cleanBase = base.replace(/\/+$/, '')
  const cleanPath = path.replace(/^\/+/, '')
  return `${cleanBase}/${cleanPath}`
}

/**
 * 从 URL 中提取文件名
 * @param url URL 或文件路径
 * @returns 文件名
 */
export function extractFilename(url: string): string {
  if (!url) return ''
  const parts = url.split('/')
  return parts[parts.length - 1] || ''
}

/**
 * 获取文件扩展名
 * @param filename 文件名或 URL
 * @returns 扩展名（小写，不含点）
 */
export function getFileExtension(filename: string): string {
  if (!filename) return ''
  const name = extractFilename(filename)
  const dotIndex = name.lastIndexOf('.')
  if (dotIndex === -1 || dotIndex === name.length - 1) return ''
  return name.slice(dotIndex + 1).toLowerCase()
}
