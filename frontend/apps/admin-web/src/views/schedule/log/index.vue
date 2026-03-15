<template>
  <div class="log-container">
    <NCard title="定时任务执行日志" :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline class="mb-4">
        <NFormItem label="任务名称" path="jobName">
          <NInput v-model:value="queryForm.jobName" placeholder="请输入任务名称" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="任务分组" path="jobGroup">
          <NInput v-model:value="queryForm.jobGroup" placeholder="请输入任务分组" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="执行状态" path="executeStatus">
          <NSelect
            v-model:value="queryForm.executeStatus"
            :options="executeStatusOptions"
            placeholder="请选择执行状态"
            clearable
            style="width: 120px"
          />
        </NFormItem>
        <NFormItem label="执行类型" path="executeType">
          <NSelect
            v-model:value="queryForm.executeType"
            :options="executeTypeOptions"
            placeholder="请选择执行类型"
            clearable
            style="width: 120px"
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

      <!-- 表格 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="pagination"
      />
    </NCard>

    <!-- 日志详情弹窗 -->
    <NModal v-model:show="showDetailModal" title="日志详情" preset="card" style="width: 800px;">
      <NDescriptions :column="2" bordered>
        <NDescriptionsItem label="日志ID">{{ currentLog.id }}</NDescriptionsItem>
        <NDescriptionsItem label="任务名称">{{ currentLog.jobName }}</NDescriptionsItem>
        <NDescriptionsItem label="任务分组">{{ currentLog.jobGroup }}</NDescriptionsItem>
        <NDescriptionsItem label="执行状态">
          <NTag :type="getExecuteStatusTag(currentLog.executeStatus)">
            {{ getExecuteStatusLabel(currentLog.executeStatus) }}
          </NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="执行时间">{{ currentLog.executeTime }}</NDescriptionsItem>
        <NDescriptionsItem label="耗时">{{ currentLog.costTime }} ms</NDescriptionsItem>
        <NDescriptionsItem label="重试次数">{{ currentLog.retryCount }}</NDescriptionsItem>
        <NDescriptionsItem label="执行类型">
          {{ currentLog.executeType === 1 ? '自动执行' : '手动触发' }}
        </NDescriptionsItem>
      </NDescriptions>

      <div v-if="currentLog.executeResult" class="mt-4">
        <h4 class="text-sm font-medium mb-2">执行结果</h4>
        <NCode :code="currentLog.executeResult" language="json" />
      </div>

      <div v-if="currentLog.errorMsg" class="mt-4">
        <h4 class="text-sm font-medium mb-2 text-red-500">错误信息</h4>
        <NAlert type="error" :title="currentLog.errorMsg" />
      </div>

      <div v-if="currentLog.stackTrace" class="mt-4">
        <h4 class="text-sm font-medium mb-2">异常堆栈</h4>
        <NCode :code="currentLog.stackTrace" language="text" />
      </div>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDetailModal = false">关闭</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, type DataTableColumns, NButton as NButtonComponent, NTag } from 'naive-ui'
import {
  getJobLogPageApi,
  getJobLogDetailApi,
  type JobLogVO,
  type JobLogQuery,
  JobExecuteStatus,
  ExecuteType,
} from '@/api/schedule'

const message = useMessage()
const loading = ref(false)
const tableData = ref<JobLogVO[]>([])
const showDetailModal = ref(false)
const currentLog = ref<JobLogVO>({} as JobLogVO)

const executeStatusOptions = [
  { label: '执行中', value: 0 },
  { label: '成功', value: 1 },
  { label: '失败', value: 2 },
]

const executeTypeOptions = [
  { label: '自动执行', value: 1 },
  { label: '手动触发', value: 2 },
]

const queryForm = reactive<JobLogQuery>({
  page: 1,
  pageSize: 10,
  jobName: undefined,
  jobGroup: undefined,
  executeStatus: undefined,
  executeType: undefined,
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

const columns: DataTableColumns<JobLogVO> = [
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => (pagination.page - 1) * pagination.pageSize + index + 1,
  },
  { title: '任务名称', key: 'jobName', width: 150 },
  { title: '任务分组', key: 'jobGroup', width: 120 },
  {
    title: '执行状态',
    key: 'executeStatus',
    width: 100,
    render: (row) => {
      const tag = getExecuteStatusTag(row.executeStatus)
      const text = getExecuteStatusLabel(row.executeStatus)
      return h(
        NTag,
        { type: tag, size: 'small' },
        { default: () => text },
      )
    },
  },
  { title: '执行时间', key: 'executeTime', width: 160 },
  {
    title: '耗时',
    key: 'costTime',
    width: 100,
    render: (row) => row.costTime ? `${row.costTime}ms` : '-',
  },
  { title: '重试次数', key: 'retryCount', width: 80 },
  {
    title: '执行类型',
    key: 'executeType',
    width: 100,
    render: (row) => (row.executeType === ExecuteType.AUTO ? '自动执行' : '手动触发'),
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) => {
      return h(
        NButtonComponent,
        {
          size: 'small',
          type: 'info',
          onClick: () => handleViewDetail(row),
        },
        { default: () => '查看' },
      )
    },
  },
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getJobLogPageApi(queryForm)
    tableData.value = res.records
    pagination.itemCount = res.totalRow
  } catch (error) {
    message.error('加载日志列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  queryForm.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(queryForm, {
    page: 1,
    pageSize: 10,
    jobName: undefined,
    jobGroup: undefined,
    executeStatus: undefined,
    executeType: undefined,
  })
  loadData()
}

const handleViewDetail = async (row: JobLogVO) => {
  try {
    const res = await getJobLogDetailApi(row.id)
    currentLog.value = res
    showDetailModal.value = true
  } catch (error) {
    // 错误消息已在 request.ts 拦截器中统一显示
  }
}

const getExecuteStatusLabel = (status: JobExecuteStatus) => {
  switch (status) {
    case JobExecuteStatus.RUNNING:
      return '执行中'
    case JobExecuteStatus.SUCCESS:
      return '成功'
    case JobExecuteStatus.FAILED:
      return '失败'
    default:
      return '-'
  }
}

const getExecuteStatusTag = (status: JobExecuteStatus) => {
  switch (status) {
    case JobExecuteStatus.RUNNING:
      return 'info'
    case JobExecuteStatus.SUCCESS:
      return 'success'
    case JobExecuteStatus.FAILED:
      return 'error'
    default:
      return 'default'
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.log-container {
  padding: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-4 {
  margin-top: 16px;
}
</style>
