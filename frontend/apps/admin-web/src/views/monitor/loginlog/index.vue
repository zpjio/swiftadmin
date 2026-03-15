<template>
  <div class="loginlog-container">
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
        <NButton
          type="error"
          :disabled="selectedRowKeys.length === 0"
          @click="handleBatchDelete"
        >
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
        :checked-row-keys="selectedRowKeys"
        :pagination="pagination"
        @update:checked-row-keys="onCheckedRowKeysChange"
        @update:page="onPageChange"
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { getLoginLogPageApi, deleteLoginLogApi, clearLoginLogApi } from '@/api/loginlog'
import type { LoginLogVO, LoginLogQuery } from '@/api/loginlog'

const message = useMessage()
const loading = ref(false)
const tableData = ref<LoginLogVO[]>([])
const selectedRowKeys = ref<number[]>([])

const queryForm = reactive<LoginLogQuery & { dateRange?: [number, number] }>({
  page: 1,
  pageSize: 10,
  username: '',
  ip: '',
  status: undefined,
  beginTime: '',
  endTime: '',
  dateRange: undefined,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  itemCount: 0,
  onChange: (page: number) => {
    pagination.page = page
    queryForm.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    queryForm.pageSize = pageSize
    pagination.page = 1
    queryForm.page = 1
    loadData()
  },
})

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 },
]

const columns: DataTableColumns = [
  { type: 'selection', fixed: 'left' },
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => (pagination.page - 1) * pagination.pageSize + index + 1,
  },
  { title: '用户名', key: 'username', width: 120, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h('span', { class: row.status === 1 ? 'text-green-600' : 'text-red-600' }, row.status === 1 ? '✓ 成功' : '✗ 失败'),
  },
  { title: '登录 IP', key: 'ipAddress', width: 140 },
  { title: '浏览器/操作系统', key: 'userAgent', width: 200, ellipsis: { tooltip: true } },
  { title: '提示信息', key: 'message', width: 160, ellipsis: { tooltip: true } },
  { title: '登录时间', key: 'loginTime', width: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) =>
      h('button', { class: 'text-red-600 hover:underline', onClick: () => handleDelete(row) }, '删除'),
  },
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLoginLogPageApi(queryForm)
    tableData.value = res.records || []
    pagination.itemCount = res.totalRow || 0
  } catch (error: any) {
    message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.page = 1
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  queryForm.username = ''
  queryForm.ip = ''
  queryForm.status = undefined
  queryForm.beginTime = ''
  queryForm.endTime = ''
  queryForm.dateRange = undefined
  handleSearch()
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
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的日志')
    return
  }
  try {
    await deleteLoginLogApi(selectedRowKeys.value)
    message.success('删除成功')
    selectedRowKeys.value = []
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

const onCheckedRowKeysChange = (keys: number[]) => {
  selectedRowKeys.value = keys
}

const onPageChange = (page: number) => {
  pagination.page = page
  queryForm.page = page
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.loginlog-container {
  height: 100%;
}

.mb-4 {
  margin-bottom: 16px;
}

.w-48 {
  width: 12rem;
}

.w-64 {
  width: 16rem;
}

.text-green-600 {
  color: #059669;
}

.text-red-600 {
  color: #dc2626;
}

.hover\:underline:hover {
  text-decoration: underline;
}
</style>
