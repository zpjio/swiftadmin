<template>
  <div class="job-container">
    <NCard title="定时任务管理" :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline class="mb-4">
        <NFormItem label="任务名称" path="jobName">
          <NInput v-model:value="queryForm.jobName" placeholder="请输入任务名称" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="任务分组" path="jobGroup">
          <NInput v-model:value="queryForm.jobGroup" placeholder="请输入任务分组" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSelect
            v-model:value="queryForm.status"
            :options="statusOptions"
            placeholder="请选择状态"
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

      <!-- 操作按钮 -->
      <NSpace class="mb-4">
        <NButton v-auth="'schedule:job:add'" type="primary" @click="handleAdd">
          <template #icon><icon-mdi-plus /></template>
          新增任务
        </NButton>
        <NButton v-auth="'schedule:job:delete'" type="error" :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
          <template #icon><icon-mdi-delete /></template>
          批量删除
        </NButton>
      </NSpace>

      <!-- 表格 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :checked-row-keys="selectedRowKeys"
        :pagination="pagination"
        :scroll-x="1400"
        @update:checked-row-keys="onCheckedRowKeysChange"
      />
    </NCard>

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 700px;">
      <NForm ref="formRef" :model="form" :rules="rules" label-width="120px">
        <NFormItem label="任务名称" path="jobName">
          <NInput v-model:value="form.jobName" placeholder="请输入任务名称" />
        </NFormItem>
        <NFormItem label="任务分组" path="jobGroup">
          <NInput v-model:value="form.jobGroup" placeholder="请输入任务分组" />
        </NFormItem>
        <NFormItem label="Bean名称" path="beanName">
          <NInput v-model:value="form.beanName" placeholder="请输入Bean名称（如：socialTokenRefreshTask）" />
        </NFormItem>
        <NFormItem label="方法名称" path="methodName">
          <NInput v-model:value="form.methodName" placeholder="请输入方法名称（如：refreshToken）" />
        </NFormItem>
        <NFormItem label="方法参数" path="methodParams">
          <NInput v-model:value="form.methodParams" type="textarea" placeholder="请输入方法参数（JSON数组格式）" :rows="3" />
        </NFormItem>
        <NFormItem label="Cron表达式" path="cronExpression">
          <NSpace vertical style="width: 100%">
            <NInput v-model:value="form.cronExpression" placeholder="请输入Cron表达式（如：0/5 * * * * ?）" />
            <CronEditor v-model="form.cronExpression" />
          </NSpace>
        </NFormItem>
        <NFormItem label="超时时间（秒）" path="timeout">
          <NInputNumber v-model:value="form.timeout" :min="1" :max="3600" placeholder="请输入超时时间" style="width: 100%" />
        </NFormItem>
        <NFormItem label="重试次数" path="retryTimes">
          <NInputNumber v-model:value="form.retryTimes" :min="0" :max="10" placeholder="请输入重试次数" style="width: 100%" />
        </NFormItem>
        <NFormItem label="重试间隔（秒）" path="retryInterval">
          <NInputNumber v-model:value="form.retryInterval" :min="0" :max="3600" placeholder="请输入重试间隔" style="width: 100%" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="form.status">
            <NSpace>
              <NRadio :value="0">正常</NRadio>
              <NRadio :value="1">暂停</NRadio>
            </NSpace>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="备注" path="remark">
          <NInput v-model:value="form.remark" type="textarea" placeholder="请输入备注" :rows="3" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" :loading="saving" @click="handleSave">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, useDialog, type DataTableColumns, type DataTableRowKey, type FormRules, NButton as NButtonComponent, NTag } from 'naive-ui'
import {
  getJobPageApi,
  getJobDetailApi,
  createJobApi,
  updateJobApi,
  deleteJobApi,
  enableJobApi,
  pauseJobApi,
  triggerJobApi,
  type JobVO,
  type JobDTO,
  JobStatus,
  type JobQuery,
} from '@/api/schedule'
import CronEditor from '@/components/schedule/CronEditor.vue'

const message = useMessage()
const dialog = useDialog()
const formRef = ref<any>(null)
const loading = ref(false)
const tableData = ref<JobVO[]>([])
const selectedRowKeys = ref<DataTableRowKey[]>([])
const showModal = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const modalTitle = ref('')

const statusOptions = [
  { label: '正常', value: 0 },
  { label: '暂停', value: 1 },
]

const queryForm = reactive<JobQuery>({
  page: 1,
  pageSize: 10,
  jobName: undefined,
  jobGroup: undefined,
  status: undefined,
})

const form = reactive<JobDTO>({
  jobName: '',
  jobGroup: 'DEFAULT',
  beanName: '',
  methodName: '',
  methodParams: '',
  cronExpression: '',
  timeout: 300,
  retryTimes: 0,
  retryInterval: 0,
  status: 0,
  remark: '',
})

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  jobGroup: [{ required: true, message: '请输入任务分组', trigger: 'blur' }],
  beanName: [{ required: true, message: '请输入Bean名称', trigger: 'blur' }],
  methodName: [{ required: true, message: '请输入方法名称', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }],
  timeout: [{ required: true, type: 'number', message: '请输入超时时间', trigger: 'blur' }],
  retryTimes: [{ required: true, type: 'number', message: '请输入重试次数', trigger: 'blur' }],
  retryInterval: [{ required: true, type: 'number', message: '请输入重试间隔', trigger: 'blur' }],
}

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

const columns: DataTableColumns<JobVO> = [
  { type: 'selection', width: 60 },
  {
    title: '序号',
    key: 'index',
    width: 70,
    render: (_row, index) => (pagination.page - 1) * pagination.pageSize + index + 1,
  },
  { title: '任务名称', key: 'jobName', width: 160 },
  { title: '任务分组', key: 'jobGroup', width: 110 },
  { title: '状态', key: 'status', width: 90,
    render: (row) => {
      const tag = row.status === JobStatus.NORMAL ? 'success' : 'warning'
      const text = row.status === JobStatus.NORMAL ? '正常' : '暂停'
      return h(
        NTag,
        { type: tag, size: 'small' },
        { default: () => text },
      )
    },
  },
  { title: 'Bean名称', key: 'beanName', width: 180 },
  { title: '方法名称', key: 'methodName', width: 140 },
  {
    title: 'Cron表达式',
    key: 'cronExpression',
    width: 160,
    ellipsis: { tooltip: true },
  },
  {
    title: '下次执行时间',
    key: 'nextExecuteTime',
    width: 170,
    render: (row) => row.nextExecuteTime || '-',
  },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    fixed: 'right',
    render: (row) => {
      return h('div', { class: 'flex gap-2' }, [
        h(
          NButtonComponent,
          {
            size: 'small',
            onClick: () => handleEdit(row),
            'v-auth': 'schedule:job:edit',
          },
          { default: () => '编辑' },
        ),
        h(
          NButtonComponent,
          {
            size: 'small',
            type: row.status === JobStatus.NORMAL ? 'warning' : 'success',
            onClick: () => handleToggleStatus(row),
            'v-auth': row.status === JobStatus.NORMAL ? 'schedule:job:disable' : 'schedule:job:enable',
          },
          { default: () => (row.status === JobStatus.NORMAL ? '暂停' : '启用') },
        ),
        h(
          NButtonComponent,
          {
            size: 'small',
            type: 'info',
            onClick: () => handleTrigger(row),
            'v-auth': 'schedule:job:trigger',
          },
          { default: () => '触发' },
        ),
        h(
          NButtonComponent,
          {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row),
            'v-auth': 'schedule:job:delete',
          },
          { default: () => '删除' },
        ),
      ])
    },
  },
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getJobPageApi(queryForm)
    tableData.value = res.records
    pagination.itemCount = res.totalRow
  } catch (error) {
    message.error('加载任务列表失败')
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
    status: undefined,
  })
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  modalTitle.value = '新增任务'
  Object.assign(form, {
    jobName: '',
    jobGroup: 'DEFAULT',
    beanName: '',
    methodName: '',
    methodParams: '',
    cronExpression: '',
    timeout: 300,
    retryTimes: 0,
    retryInterval: 0,
    status: 0,
    remark: '',
  })
  showModal.value = true
}

const handleEdit = async (row: JobVO) => {
  isEdit.value = true
  modalTitle.value = '编辑任务'
  try {
    const res = await getJobDetailApi(row.id)
    Object.assign(form, res)
    showModal.value = true
  } catch (error) {
    message.error('获取任务详情失败')
  }
}

const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateJobApi(form)
      message.success('更新任务成功')
    } else {
      await createJobApi(form)
      message.success('创建任务成功')
    }
    showModal.value = false
    loadData()
  } catch (error) {
    message.error(isEdit.value ? '更新任务失败' : '创建任务失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (row: JobVO) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除任务"${row.jobName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteJobApi([row.id])
        message.success('删除任务成功')
        loadData()
      } catch (error) {
        message.error('删除任务失败')
      }
    },
  })
}

const handleBatchDelete = () => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个任务吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteJobApi(selectedRowKeys.value as string[])
        message.success('批量删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch (error) {
        message.error('批量删除失败')
      }
    },
  })
}

const handleToggleStatus = async (row: JobVO) => {
  try {
    if (row.status === JobStatus.NORMAL) {
      await pauseJobApi(row.id)
      message.success('暂停任务成功')
    } else {
      await enableJobApi(row.id)
      message.success('启用任务成功')
    }
    loadData()
  } catch (error) {
    message.error(row.status === JobStatus.NORMAL ? '暂停任务失败' : '启用任务失败')
  }
}

const handleTrigger = async (row: JobVO) => {
  dialog.info({
    title: '确认触发',
    content: `确定要立即执行任务"${row.jobName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await triggerJobApi(row.id)
        message.success('任务已触发，请查看执行日志')
      } catch (error) {
        message.error('触发任务失败')
      }
    },
  })
}

const onCheckedRowKeysChange = (keys: DataTableRowKey[]) => {
  selectedRowKeys.value = keys
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.job-container {
  padding: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
