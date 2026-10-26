import { get, del } from '@/utils/request'
import service from '@/utils/request'
import type { PageResult } from '@/types'

/**
 * 文件信息
 */
export interface FileVO {
  id: string
  fileName: string
  fileType: string
  fileSize: number
  fileUrl: string
  accessUrl: string
  filePath: string
  storageType: string
  bizType?: string
  createTime: string
}

/**
 * 文件查询参数
 */
export interface FileQuery {
  page?: number
  pageSize?: number
  fileName?: string
  fileType?: string
  bizType?: string
}

/**
 * 分页查询文件列表
 */
export function getFilePageApi(params: FileQuery) {
  return get<PageResult<FileVO>>('/system/file/page', params)
}

/**
 * 获取文件详情
 */
export function getFileDetailApi(id: string) {
  return get<FileVO>(`/system/file/${id}`)
}

/**
 * 批量删除文件
 */
export function deleteFileApi(ids: string[]) {
  return del(`/system/file/${ids.join(',')}`)
}

/**
 * 下载文件
 */
export async function downloadFileApi(id: string, fileName?: string) {
  const response = await service.get(`/system/file/download/${id}`, {
    responseType: 'blob'
  })

  // 优先使用传入的文件名，否则尝试从响应头获取
  let downloadName = fileName || `file_${id}`
  if (!fileName) {
    const contentDisposition = response.headers?.['content-disposition']
    if (contentDisposition) {
      // 尝试多种格式解析文件名
      const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
      if (utf8Match) {
        downloadName = decodeURIComponent(utf8Match[1])
      } else {
        const asciiMatch = contentDisposition.match(/filename="?([^";\n]+)"?/)
        if (asciiMatch) {
          downloadName = asciiMatch[1]
        }
      }
    }
  }

  // 创建下载链接
  const blob = new Blob([response.data as any])
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', downloadName)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
