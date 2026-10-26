<template>
  <div class="h-full">
    <NCard :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline label-width="80" class="mb-4">
        <NFormItem label="用户名" path="username">
          <NInput
            v-model:value="queryForm.username"
            placeholder="请输入用户名"
            clearable
            class="w-48"
          />
        </NFormItem>
        <NFormItem label="IP 地址" path="ip">
          <NInput
            v-model:value="queryForm.ip"
            placeholder="请输入 IP 地址"
            clearable
            class="w-48"
          />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSelect
            v-model:value="queryForm.status"
            :options="statusOptions"
            placeholder="请选择状态"
            clearable
            class="w-48"
          />
        </NFormItem>
        <NFormItem label="登录时间" path="dateRange">
          <NDatePicker
            v-model:value="queryForm.dateRange"
            type="daterange"
            placeholder="选择日期范围"
            class="w-64"
            @update:value="onDateRangeChange"
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

      <!-- 操作按钮 -->
      <NSpace class="mb-4">
        <NButton type="error" :disabled="!hasSelection" @click="handleBatchDelete">
          <template #icon><icon-mdi-delete /></template>
          批量删除
        </NButton>
        <NPopconfirm @positive-click="handleClearAll">
          <template #trigger>
            <NButton type="error" secondary>
              <template #icon><icon-mdi-delete-sweep /></template>
              清空全部
            </NButton>
          </template>
          确定要清空所有登录日志吗？此操作不可恢复！
        </NPopconfirm>
      </NSpace>

      <!-- 表格 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :checked-row-keys="selectedKeys"
        :pagination="paginationProps"
        @update:checked-row-keys="onSelectionChange"
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { getLoginLogPageApi, deleteLoginLogApi, clearLoginLogApi } from '@/api/loginlog'
import type { LoginLogVO, LoginLogQuery } from '@/api/loginlog'
import { formatDateTime } from '@/utils/date'
import { usePagination, useTableSelection } from '@/composables'

const message = useMessage()
const loading = ref(false)
const tableData = ref<LoginLogVO[]>([])
const { selectedKeys, hasSelection, clearSelection, onSelectionChange } =
  useTableSelection<LoginLogVO>()

const queryForm = reactive<LoginLogQuery & { dateRange?: [number, number] }>({
  username: '',
  ip: '',
  status: undefined,
  beginTime: '',
  endTime: '',
  dateRange: undefined
})

// 分页
const { paginationProps, setTotal, getIndex } = usePagination({
  onChange: () => loadData(),
  onPageSizeChange: () => loadData()
})

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const columns: DataTableColumns<LoginLogVO> = [
  { type: 'selection', fixed: 'left' },
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => getIndex(index)
  },
  { title: '用户名', key: 'username', width: 120, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(
        'span',
        { class: row.status === 1 ? 'text-green-600' : 'text-red-600' },
        row.status === 1 ? '✓ 成功' : '✗ 失败'
      )
  },
  { title: '登录 IP', key: 'ipAddress', width: 140 },
  { title: '浏览器/操作系统', key: 'userAgent', width: 200, ellipsis: { tooltip: true } },
  { title: '提示信息', key: 'message', width: 160, ellipsis: { tooltip: true } },
  {
    title: '登录时间',
    key: 'loginTime',
    width: 160,
    render: (row) => formatDateTime(row.loginTime) || '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) =>
      h(
        'button',
        { class: 'text-red-600 hover:underline', onClick: () => handleDelete(row) },
        '删除'
      )
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLoginLogPageApi(queryForm)
    tableData.value = res.records || []
    setTotal(res.totalRow || 0)
  } catch (error: any) {
    message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadData()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.ip = ''
  queryForm.status = undefined
  queryForm.beginTime = ''
  queryForm.endTime = ''
  queryForm.dateRange = undefined
  loadData()
}

const onDateRangeChange = (value: [number, number] | null) => {
  if (value) {
    queryForm.beginTime = new Date(value[0]).toISOString().split('T')[0]
    queryForm.endTime = new Date(value[1]).toISOString().split('T')[0]
  } else {
    queryForm.beginTime = ''
    queryForm.endTime = ''
  }
}

const handleDelete = async (row: LoginLogVO) => {
  try {
    await deleteLoginLogApi([row.id])
    message.success('删除成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

const handleBatchDelete = async () => {
  if (!hasSelection.value) {
    message.warning('请选择要删除的日志')
    return
  }
  try {
    await deleteLoginLogApi(selectedKeys.value as string[])
    message.success('删除成功')
    clearSelection()
    loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

const handleClearAll = async () => {
  try {
    await clearLoginLogApi()
    message.success('清空成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '清空失败')
  }
}

onMounted(() => {
  loadData()
})
</script>
