<template>
  <div class="h-full">
    <NCard :bordered="false">
      <!-- 搜索栏 -->
      <div class="flex justify-between items-start flex-wrap gap-4 mb-4">
        <NForm :model="queryForm" inline label-width="80" class="flex flex-wrap gap-4">
          <NFormItem label="文件名" path="fileName">
            <NInput
              v-model:value="queryForm.fileName"
              placeholder="请输入文件名"
              clearable
              style="width: 180px"
            />
          </NFormItem>
          <NFormItem label="文件类型" path="fileType">
            <NSelect
              v-model:value="queryForm.fileType"
              :options="fileTypeOptions"
              placeholder="请选择类型"
              clearable
              style="width: 150px"
            />
          </NFormItem>
          <NFormItem>
            <NSpace>
              <NButton type="primary" @click="handleSearch">
                <template #icon><icon-mdi-magnify /></template>
                搜索
              </NButton>
              <NButton @click="handleReset">
                <template #icon><icon-mdi-refresh /></template>
                重置
              </NButton>
            </NSpace>
          </NFormItem>
        </NForm>

        <NSpace>
          <NUpload
            :action="uploadUrl"
            :headers="uploadHeaders"
            :name="'file'"
            :show-file-list="false"
            @finish="handleUploadFinish"
            @error="handleUploadError"
          >
            <NButton type="primary">
              <template #icon><icon-mdi-upload /></template>
              上传文件
            </NButton>
          </NUpload>
          <NButton @click="handleCreateFolder">
            <template #icon><icon-mdi-folder-plus /></template>
            新建文件夹
          </NButton>
          <NButton type="error" :disabled="!hasSelection" @click="handleBatchDelete">
            <template #icon><icon-mdi-delete /></template>
            批量删除
          </NButton>
        </NSpace>
      </div>

      <!-- 文件列表 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :checked-row-keys="selectedKeys"
        :pagination="paginationProps"
        @update:checked-row-keys="onSelectionChange"
      />

      <!-- 文件预览弹窗 -->
      <NModal
        v-model:show="showPreview"
        preset="dialog"
        :title="previewTitle"
        :style="{ width: '800px' }"
      >
        <div class="min-h-[400px] flex items-center justify-center">
          <img
            v-if="previewType === 'image'"
            :src="previewUrl"
            class="max-w-full max-h-[600px] object-contain"
          />
          <video
            v-else-if="previewType === 'video'"
            :src="previewUrl"
            controls
            class="max-w-full max-h-[600px]"
          />
          <iframe
            v-else-if="previewType === 'pdf'"
            :src="previewUrl"
            class="w-full h-[600px] border-none"
          />
          <div v-else class="text-center text-gray-400">
            <icon-mdi-file class="text-6xl" />
            <p class="mt-4">该文件类型不支持预览</p>
          </div>
        </div>
      </NModal>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, h, computed } from 'vue'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { getFilePageApi, deleteFileApi, downloadFileApi } from '@/api/file'
import type { FileVO, FileQuery } from '@/api/file'
import { getIconClass } from '@/utils/icon'
import { buildAuthHeader } from '@/utils/auth'
import { formatDateTime } from '@/utils/date'
import { useTablePage, useConfirm, useTableActions } from '@/composables'

const message = useMessage()
const { confirmDelete, confirmBatchDelete } = useConfirm()
const { renderActions } = useTableActions()

const fileTypeOptions = [
  { label: '图片', value: 'image' },
  { label: '文档', value: 'doc' },
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
  { label: '其他', value: 'other' }
]

// 上传配置
const uploadUrl = computed(() => `${import.meta.env.VITE_API_BASE_URL}/system/file/upload`)
const uploadHeaders = computed(() => buildAuthHeader())

// 表格分页
const {
  loading,
  tableData,
  queryForm,
  paginationProps,
  getIndex,
  loadData,
  handleSearch,
  handleReset,
  selection
} = useTablePage<FileVO, FileQuery>({
  fetchFn: getFilePageApi,
  defaultQuery: {
    fileName: undefined,
    fileType: undefined
  },
  enableSelection: true,
  immediate: true
})

// 选择相关的便捷访问
const selectedKeys = selection!.selectedKeys
const hasSelection = selection!.hasSelection
const clearSelection = selection!.clearSelection
const onSelectionChange = selection!.onSelectionChange

const columns: DataTableColumns<FileVO> = [
  { type: 'selection', fixed: 'left' },
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => getIndex(index)
  },
  {
    title: '文件名',
    key: 'fileName',
    width: 200,
    ellipsis: { tooltip: true },
    render: (row) =>
      h('div', { class: 'flex items-center gap-2' }, [
        h('i', { class: `${getIconClass(getFileIcon(row.fileType))} text-lg` }),
        h('span', row.fileName)
      ])
  },
  { title: '文件类型', key: 'fileType', width: 100 },
  { title: '文件大小', key: 'fileSize', width: 100, render: (row) => formatFileSize(row.fileSize) },
  { title: '存储路径', key: 'filePath', width: 200, ellipsis: { tooltip: true } },
  {
    title: '上传时间',
    key: 'createTime',
    width: 160,
    render: (row) => formatDateTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) =>
      renderActions([
        { label: '预览', color: '#2563eb', onClick: () => handlePreview(row) },
        { label: '下载', color: '#059669', onClick: () => handleDownload(row) },
        { label: '删除', color: '#dc2626', onClick: () => handleDelete(row) }
      ])
  }
]

// 预览相关
const showPreview = ref(false)
const previewTitle = ref('')
const previewUrl = ref('')
const previewType = ref<'image' | 'video' | 'pdf' | 'other'>('other')

const handleUploadFinish = ({ event }: any) => {
  const res = JSON.parse(event.target.responseText)
  if (res.code === 0) {
    message.success('上传成功')
    loadData()
  } else {
    message.error(res.message || '上传失败')
  }
}

const handleUploadError = () => {
  message.error('上传失败')
}

const handleCreateFolder = () => {
  message.info('新建文件夹功能开发中...')
}

const handleDelete = (row: FileVO) => {
  confirmDelete(`确定要删除文件 "${row.fileName}" 吗？`, async () => {
    await deleteFileApi([row.id])
    message.success('删除成功')
    loadData()
  })
}

const handleBatchDelete = () => {
  if (!hasSelection.value) {
    message.warning('请选择要删除的文件')
    return
  }
  confirmBatchDelete(selectedKeys.value.length, async () => {
    await deleteFileApi(selectedKeys.value as string[])
    message.success('删除成功')
    clearSelection()
    loadData()
  })
}

const handlePreview = (row: FileVO) => {
  previewTitle.value = row.fileName

  let url = row.accessUrl || row.fileUrl || ''
  // 确保 URL 有协议头
  if (url && !url.startsWith('http://') && !url.startsWith('https://')) {
    url = 'https://' + url
  }
  previewUrl.value = url

  const type = row.fileType?.toLowerCase() || ''
  if (type === 'image') {
    previewType.value = 'image'
  } else if (type === 'video') {
    previewType.value = 'video'
  } else if (type === 'pdf') {
    previewType.value = 'pdf'
  } else {
    // 根据文件名扩展名判断
    const ext = row.fileName?.split('.').pop()?.toLowerCase() || ''
    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'].includes(ext)) {
      previewType.value = 'image'
    } else if (['mp4', 'webm', 'ogg'].includes(ext)) {
      previewType.value = 'video'
    } else if (ext === 'pdf') {
      previewType.value = 'pdf'
    } else {
      previewType.value = 'other'
    }
  }

  showPreview.value = true
}

const handleDownload = async (row: FileVO) => {
  try {
    await downloadFileApi(row.id, row.fileName)
  } catch (error: any) {
    message.error(error.message || '下载失败')
  }
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return '-'
  const kb = bytes / 1024
  if (kb < 1024) return `${kb.toFixed(1)} KB`
  const mb = kb / 1024
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  const gb = mb / 1024
  return `${gb.toFixed(1)} GB`
}

const getFileIcon = (type?: string) => {
  const t = type?.toLowerCase() || ''
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(t)) return 'image'
  if (['pdf'].includes(t)) return 'file-pdf'
  if (['doc', 'docx'].includes(t)) return 'file-word'
  if (['xls', 'xlsx'].includes(t)) return 'file-excel'
  if (['ppt', 'pptx'].includes(t)) return 'file-powerpoint'
  if (['txt'].includes(t)) return 'file-document'
  if (['zip', 'rar', '7z'].includes(t)) return 'zip-box'
  if (['mp4', 'webm', 'ogg'].includes(t)) return 'video'
  if (['mp3', 'wav', 'flac'].includes(t)) return 'music'
  return 'file'
}
</script>
