<template>
  <div class="channel-container">
    <NCard title="渠道管理" :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline class="mb-4">
        <NFormItem label="渠道名称" path="channelName">
          <NInput v-model:value="queryForm.channelName" placeholder="请输入渠道名称" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="渠道类型" path="channelType">
          <NSelect
            v-model:value="queryForm.channelType"
            :options="channelTypeOptions"
            placeholder="请选择渠道类型"
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

      <!-- 操作按钮 -->
      <NSpace class="mb-4">
        <NButton v-auth="'message:channel:add'" type="primary" @click="handleAdd">
          <template #icon><icon-mdi-plus /></template>
          新增渠道
        </NButton>
      </NSpace>

      <!-- 表格 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="pagination"
      />
    </NCard>

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 700px;">
      <NForm ref="formRef" :model="form" :rules="rules" label-width="100">
        <NFormItem label="渠道名称" path="channelName">
          <NInput v-model:value="form.channelName" placeholder="请输入渠道名称" />
        </NFormItem>
        <NFormItem label="渠道类型" path="channelType">
          <NSelect
            v-model:value="form.channelType"
            :options="channelTypeOptions"
            placeholder="请选择渠道类型"
            :disabled="isEdit"
          />
        </NFormItem>
        <NFormItem label="渠道编码" path="channelCode">
          <NInput v-model:value="form.channelCode" placeholder="请输入渠道编码" :disabled="isEdit" />
        </NFormItem>
        <NFormItem label="配置信息" path="config">
          <NInput v-model:value="form.config" type="textarea" placeholder="请输入JSON格式配置" :rows="6" />
        </NFormItem>
        <NFormItem label="优先级" path="priority">
          <NInputNumber v-model:value="form.priority" :min="0" :max="100" placeholder="数字越大优先级越高" style="width: 200px" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="form.status">
            <NRadio :value="1">启用</NRadio>
            <NRadio :value="0">禁用</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="备注" path="remark">
          <NInput v-model:value="form.remark" type="textarea" placeholder="请输入备注" :rows="2" />
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
import { ref, reactive, h, onMounted } from 'vue'
import { useMessage, useDialog, NTag, NButton, NSpace, NSwitch, type DataTableColumns } from 'naive-ui'
import {
  getChannelPageApi,
  getChannelDetailApi,
  createChannelApi,
  updateChannelApi,
  deleteChannelApi,
  updateChannelStatusApi,
  testChannelApi,
  type MessageChannel,
} from '@/api/message'
import { useDict } from '@/composables/useDict'

const message = useMessage()
const dialog = useDialog()
const formRef = ref<any>(null)
const loading = ref(false)
const tableData = ref<MessageChannel[]>([])
const showModal = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const modalTitle = ref('')

// 渠道类型选项
const { options: channelTypeOptions, getLabel: getChannelTypeLabel } = useDict('msg_channel_type')

const queryForm = reactive({
  page: 1,
  pageSize: 10,
  channelName: undefined as string | undefined,
  channelType: undefined as string | undefined,
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

const form = reactive({
  id: undefined as number | undefined,
  channelName: '',
  channelType: 'email',
  channelCode: '',
  config: '{}',
  status: 1,
  priority: 0,
  remark: '',
})

const rules = {
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  config: [{ required: true, message: '请输入配置信息', trigger: 'blur' }],
}

const columns: DataTableColumns<MessageChannel> = [
  { title: '渠道名称', key: 'channelName', width: 150 },
  { title: '渠道编码', key: 'channelCode', width: 150 },
  {
    title: '渠道类型',
    key: 'channelType',
    width: 100,
    render: (row) => h(NTag, { type: 'info' }, () => getChannelTypeLabel(row.channelType)),
  },
  { title: '优先级', key: 'priority', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) =>
      h(NSwitch, {
        value: row.status === 1,
        onUpdateValue: (val: boolean) => handleStatusChange(row, val),
      }),
  },
  { title: '备注', key: 'remark', width: 200, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: (row) =>
      h(NSpace, null, () => [
        h(NButton, { text: true, type: 'primary', onClick: () => handleEdit(row) }, () => '编辑'),
        h(NButton, { text: true, type: 'warning', onClick: () => handleTest(row) }, () => '测试'),
        h(NButton, { text: true, type: 'error', onClick: () => handleDelete(row.id) }, () => '删除'),
      ]),
  },
]

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getChannelPageApi(queryForm)
    tableData.value = res?.records || []
    pagination.itemCount = res?.totalRow || 0
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  queryForm.page = 1
  loadData()
}

function handleReset() {
  queryForm.channelName = undefined
  queryForm.channelType = undefined
  handleSearch()
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增渠道'
  Object.assign(form, {
    id: undefined,
    channelName: '',
    channelType: 'email',
    channelCode: '',
    config: '{}',
    status: 1,
    priority: 0,
    remark: '',
  })
  showModal.value = true
}

async function handleEdit(row: MessageChannel) {
  isEdit.value = true
  modalTitle.value = '编辑渠道'
  try {
    const res = await getChannelDetailApi(row.id)
    Object.assign(form, res)
    showModal.value = true
  } catch (error) {
    message.error('获取渠道详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true
    if (isEdit.value) {
      await updateChannelApi(form)
      message.success('更新成功')
    } else {
      await createChannelApi(form)
      message.success('创建成功')
    }
    showModal.value = false
    loadData()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(row: MessageChannel, val: boolean) {
  try {
    await updateChannelStatusApi(row.id, val ? 1 : 0)
    message.success('状态更新成功')
    loadData()
  } catch (error) {
    message.error('状态更新失败')
  }
}

async function handleTest(row: MessageChannel) {
  try {
    const res = await testChannelApi(row.id)
    if (res) {
      message.success('渠道连接测试成功')
    } else {
      message.error('渠道连接测试失败')
    }
  } catch (error) {
    message.error('渠道连接测试失败')
  }
}

function handleDelete(id: number) {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除该渠道吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteChannelApi([id])
        message.success('删除成功')
        loadData()
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}
</script>

<style scoped>
.channel-container {
  padding: 16px;
}
</style>