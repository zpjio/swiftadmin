<template>
  <div class="operlog-container">
    <NCard :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline label-width="80" class="mb-4">
        <NFormItem label="操作标题" path="title">
          <NInput
            v-model:value="queryForm.title"
            placeholder="请输入操作标题"
            clearable
            class="w-48"
          />
        </NFormItem>
        <NFormItem label="操作人员" path="operName">
          <NInput
            v-model:value="queryForm.operName"
            placeholder="请输入操作人员"
            clearable
            class="w-48"
          />
        </NFormItem>
        <NFormItem label="业务类型" path="businessType">
          <NSelect
            v-model:value="queryForm.businessType"
            :options="businessTypeOptions"
            placeholder="请选择业务类型"
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
        <NFormItem label="操作时间" path="dateRange">
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
              <template #icon>
                <icon-mdi-magnify />
              </template>
              搜索
            </NButton>
            <NButton @click="handleReset">
              <template #icon>
                <icon-mdi-refresh />
              </template>
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
          <template #icon>
            <icon-mdi-delete />
          </template>
          批量删除
        </NButton>
        <NPopconfirm @positive-click="handleClearAll">
          <template #trigger>
            <NButton type="error" secondary>
              <template #icon>
                <icon-mdi-delete-sweep />
              </template>
              清空全部
            </NButton>
          </template>
          确定要清空所有操作日志吗？此操作不可恢复！
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

    <!-- 详情弹窗 -->
    <NModal v-model:show="showDetailModal" preset="dialog" title="操作日志详情" :closable="true" class="w-[800px]">
      <NDescriptions :column="2" bordered>
        <NDescriptionsItem label="日志 ID">{{ currentDetail?.id }}</NDescriptionsItem>
        <NDescriptionsItem label="操作标题">{{ currentDetail?.title }}</NDescriptionsItem>
        <NDescriptionsItem label="业务类型">
          {{ getBusinessTypeLabel(currentDetail?.businessType) }}
        </NDescriptionsItem>
        <NDescriptionsItem label="请求方法">{{ currentDetail?.requestMethod }}</NDescriptionsItem>
        <NDescriptionsItem label="操作人员">{{ currentDetail?.operName }}</NDescriptionsItem>
        <NDescriptionsItem label="部门名称">{{ currentDetail?.deptName || '-' }}</NDescriptionsItem>
        <NDescriptionsItem label="操作 URL">{{ currentDetail?.operUrl }}</NDescriptionsItem>
        <NDescriptionsItem label="主机信息">
          {{ currentDetail?.operIp }} {{ currentDetail?.operLocation || '' }}
        </NDescriptionsItem>
        <NDescriptionsItem label="操作状态">
          <NTag :type="currentDetail?.status === 0 ? 'success' : 'error'">
            {{ currentDetail?.status === 0 ? '成功' : '失败' }}
          </NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="耗时">{{ currentDetail?.costTime }}ms</NDescriptionsItem>
        <NDescriptionsItem label="操作时间" :span="2">
          {{ currentDetail?.operTime }}
        </NDescriptionsItem>
        <NDescriptionsItem label="请求参数" :span="2">
          <NCode :code="formatJson(currentDetail?.operParam)" language="json" :wrap="true" />
        </NDescriptionsItem>
        <NDescriptionsItem label="返回结果" :span="2">
          <NCode :code="formatJson(currentDetail?.jsonResult)" language="json" :wrap="true" />
        </NDescriptionsItem>
        <NDescriptionsItem label="错误信息" :span="2" v-if="currentDetail?.errorMsg">
          <NText type="error">{{ currentDetail?.errorMsg }}</NText>
        </NDescriptionsItem>
      </NDescriptions>
      <template #action>
        <NButton @click="showDetailModal = false">关闭</NButton>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, type DataTableColumns } from 'naive-ui'
import { getOperLogPageApi, deleteOperLogApi, clearOperLogApi, getOperLogDetailApi } from '@/api/operlog'
import type { OperLogVO, OperLogQuery } from '@/api/operlog'

const message = useMessage()
const loading = ref(false)
const tableData = ref<OperLogVO[]>([])
const selectedRowKeys = ref<number[]>([])
const showDetailModal = ref(false)
const currentDetail = ref<OperLogVO | null>(null)

const queryForm = reactive<OperLogQuery & { dateRange?: [number, number] }>({
  page: 1,
  pageSize: 10,
  title: '',
  operName: '',
  businessType: undefined,
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

// 业务类型选项
const businessTypeOptions = [
  { label: '其他', value: 0 },
  { label: '新增', value: 1 },
  { label: '修改', value: 2 },
  { label: '删除', value: 3 },
  { label: '授权', value: 4 },
  { label: '导出', value: 5 },
  { label: '导入', value: 6 },
  { label: '强退', value: 7 },
  { label: '生成代码', value: 8 },
  { label: '清空数据', value: 9 },
  { label: '创建', value: 10 },
  { label: '刷新', value: 11 },
]

// 状态选项
const statusOptions = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 },
]

// 表格列定义
const columns: DataTableColumns = [
  { type: 'selection', fixed: 'left' },
  {
    title: '日志 ID',
    key: 'id',
    width: 80,
    ellipsis: { tooltip: true },
  },
  {
    title: '操作标题',
    key: 'title',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: '业务类型',
    key: 'businessType',
    width: 100,
    render: (row) => getBusinessTypeLabel(row.businessType),
  },
  {
    title: '请求方法',
    key: 'requestMethod',
    width: 100,
    render: (row) =>
      h(
        'span',
        { class: 'font-mono' },
        row.requestMethod
      ),
  },
  {
    title: '操作人员',
    key: 'operName',
    width: 100,
    ellipsis: { tooltip: true },
  },
  {
    title: '操作 IP',
    key: 'operIp',
    width: 120,
    ellipsis: { tooltip: true },
  },
  {
    title: '操作地点',
    key: 'operLocation',
    width: 120,
    ellipsis: { tooltip: true },
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(
        'span',
        { class: row.status === 0 ? 'text-green-600' : 'text-red-600' },
        row.status === 0 ? '✓ 成功' : '✗ 失败'
      ),
  },
  {
    title: '耗时',
    key: 'costTime',
    width: 80,
    render: (row) => `${row.costTime}ms`,
  },
  {
    title: '操作时间',
    key: 'operTime',
    width: 160,
    ellipsis: { tooltip: true },
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) =>
      h(
        'button',
        {
          class: 'text-blue-600 hover:underline',
          onClick: () => handleViewDetail(row),
        },
        '详情'
      ),
  },
]

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getOperLogPageApi(queryForm)
    tableData.value = res.records || []
    pagination.itemCount = res.totalRow || 0
  } catch (error: any) {
    message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  queryForm.page = 1
  pagination.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.title = ''
  queryForm.operName = ''
  queryForm.businessType = undefined
  queryForm.status = undefined
  queryForm.beginTime = ''
  queryForm.endTime = ''
  queryForm.dateRange = undefined
  handleSearch()
}

// 日期范围变化
const onDateRangeChange = (value: [number, number] | null) => {
  if (value) {
    queryForm.beginTime = new Date(value[0]).toISOString().split('T')[0]
    queryForm.endTime = new Date(value[1]).toISOString().split('T')[0]
  } else {
    queryForm.beginTime = ''
    queryForm.endTime = ''
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的日志')
    return
  }

  try {
    await deleteOperLogApi(selectedRowKeys.value)
    message.success('删除成功')
    selectedRowKeys.value = []
    loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 清空全部
const handleClearAll = async () => {
  try {
    await clearOperLogApi()
    message.success('清空成功')
    loadData()
  } catch (error: any) {
    message.error(error.message || '清空失败')
  }
}

// 查看详情
const handleViewDetail = async (row: OperLogVO) => {
  try {
    const detail = await getOperLogDetailApi(row.id)
    currentDetail.value = detail as any as OperLogVO
    showDetailModal.value = true
  } catch (error: any) {
    message.error(error.message || '加载详情失败')
  }
}

// 获取业务类型标签
const getBusinessTypeLabel = (type?: number) => {
  if (type === undefined) return '-'
  const option = businessTypeOptions.find((opt) => opt.value === type)
  return option?.label || '-'
}

// 格式化 JSON
const formatJson = (json?: string) => {
  if (!json) return ''
  try {
    return JSON.stringify(JSON.parse(json), null, 2)
  } catch {
    return json
  }
}

// 选中行变化
const onCheckedRowKeysChange = (keys: number[]) => {
  selectedRowKeys.value = keys
}

// 分页变化
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
.operlog-container {
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

.font-mono {
  font-family: monospace;
}

.text-green-600 {
  color: #059669;
}

.text-red-600 {
  color: #dc2626;
}

.text-blue-600 {
  color: #2563eb;
}

.hover\:underline:hover {
  text-decoration: underline;
}
</style>
