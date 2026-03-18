<template>
  <div class="p-4 h-full">
    <NCard title="消息模板" :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline class="mb-4">
        <NFormItem label="模板名称" path="templateName">
          <NInput v-model:value="queryForm.templateName" placeholder="请输入模板名称" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="模板编码" path="templateCode">
          <NInput v-model:value="queryForm.templateCode" placeholder="请输入模板编码" clearable style="width: 180px" />
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
        <NButton v-auth="'message:template:add'" type="primary" @click="handleAdd">
          <template #icon><icon-mdi-plus /></template>
          新增模板
        </NButton>
        <NButton v-auth="'message:template:delete'" type="error" :disabled="!hasSelection" @click="handleBatchDelete">
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
        :checked-row-keys="selectedKeys"
        :pagination="paginationProps"
        @update:checked-row-keys="onSelectionChange"
      />
    </NCard>

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 600px;">
      <NForm ref="formRef" :model="form" :rules="rules" label-width="100">
        <NFormItem label="模板名称" path="templateName">
          <NInput v-model:value="form.templateName" placeholder="请输入模板名称" />
        </NFormItem>
        <NFormItem label="模板编码" path="templateCode">
          <NInput v-model:value="form.templateCode" placeholder="请输入模板编码" :disabled="isEdit" />
        </NFormItem>
        <NFormItem v-if="form.channelType === 'sms'" label="服务商模板码" path="providerTemplateCode">
          <NInput v-model:value="form.providerTemplateCode" placeholder="如阿里云 SMS_123456789" />
        </NFormItem>
        <NFormItem label="渠道类型" path="channelType">
          <NSelect
            v-model:value="form.channelType"
            :options="channelTypeOptions"
            placeholder="请选择渠道类型"
          />
        </NFormItem>
        <NFormItem v-if="form.channelType === 'email'" label="主题" path="subject">
          <NInput v-model:value="form.subject" placeholder="请输入邮件主题" />
        </NFormItem>
        <NFormItem label="模板内容" path="content">
          <NInput v-model:value="form.content" type="textarea" placeholder="请输入模板内容，使用 ${变量名} 表示变量" :rows="5" />
        </NFormItem>
        <NFormItem label="变量列表" path="variables">
          <NInput v-model:value="form.variables" placeholder='变量列表，JSON数组格式，如：["username","code"]' />
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
import { useMessage, useDialog, NTag, NButton, NSpace, NSwitch, type DataTableColumns, type DataTableRowKey } from 'naive-ui'
import {
  getTemplatePageApi,
  getTemplateDetailApi,
  createTemplateApi,
  updateTemplateApi,
  deleteTemplateApi,
  updateTemplateStatusApi,
  type MessageTemplate,
  type ChannelType,
} from '@/api/message'
import { useDict } from '@/composables/useDict'
import { usePagination, useTableSelection } from '@/composables'
import { formatDateTime } from '@/utils/date'

const message = useMessage()
const dialog = useDialog()
const formRef = ref<any>(null)
const loading = ref(false)
const tableData = ref<MessageTemplate[]>([])
const { selectedKeys, hasSelection, onSelectionChange } = useTableSelection<MessageTemplate>()
const showModal = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const modalTitle = ref('')

// 渠道类型选项
const { options: channelTypeOptions, getLabel: getChannelTypeLabel } = useDict('msg_channel_type')

const queryForm = reactive({
  templateName: undefined as string | undefined,
  templateCode: undefined as string | undefined,
  channelType: undefined as ChannelType | undefined,
})

// 分页
const { paginationProps, setTotal, getParams } = usePagination({
  onChange: () => loadData(),
  onPageSizeChange: () => loadData(),
})

const form = reactive({
  id: undefined as string | undefined,
  templateName: '',
  templateCode: '',
  providerTemplateCode: '',
  channelType: 'inbox' as ChannelType,
  subject: '',
  content: '',
  variables: '',
  status: 1,
  remark: '',
})

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择渠道类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
}

const columns: DataTableColumns<MessageTemplate> = [
  { type: 'selection' },
  { title: '模板名称', key: 'templateName', width: 120 },
  { title: '模板编码', key: 'templateCode', width: 130 },
  { title: '服务商模板码', key: 'providerTemplateCode', width: 140, ellipsis: { tooltip: true } },
  {
    title: '渠道类型',
    key: 'channelType',
    width: 100,
    render: (row) => h(NTag, { type: 'info' }, () => getChannelTypeLabel(row.channelType)),
  },
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
  { title: '创建时间', key: 'createTime', width: 170, render: (row) => formatDateTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: (row) =>
      h(NSpace, null, () => [
        h(NButton, { text: true, type: 'primary', onClick: () => handleEdit(row) }, () => '编辑'),
        h(NButton, { text: true, type: 'error', onClick: () => handleDelete([row.id]) }, () => '删除'),
      ]),
  },
]

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getTemplatePageApi({ ...queryForm, ...getParams() })
    tableData.value = res?.records || []
    setTotal(res?.totalRow || 0)
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  queryForm.templateName = undefined
  queryForm.templateCode = undefined
  queryForm.channelType = undefined
  loadData()
}

function handleBatchDelete() {
  if (!hasSelection.value) {
    message.warning('请选择要删除的记录')
    return
  }
  handleDelete(selectedKeys.value as string[])
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增模板'
  Object.assign(form, {
    id: undefined,
    templateName: '',
    templateCode: '',
    providerTemplateCode: '',
    channelType: 'inbox',
    subject: '',
    content: '',
    variables: '',
    status: 1,
    remark: '',
  })
  showModal.value = true
}

async function handleEdit(row: MessageTemplate) {
  isEdit.value = true
  modalTitle.value = '编辑模板'
  try {
    const res = await getTemplateDetailApi(row.id)
    Object.assign(form, res)
    showModal.value = true
  } catch (error) {
    message.error('获取模板详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    saving.value = true
    if (isEdit.value) {
      await updateTemplateApi(form)
      message.success('更新成功')
    } else {
      await createTemplateApi(form)
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

async function handleStatusChange(row: MessageTemplate, val: boolean) {
  try {
    await updateTemplateStatusApi(row.id, val ? 1 : 0)
    message.success('状态更新成功')
    loadData()
  } catch (error) {
    message.error('状态更新失败')
  }
}

function handleDelete(ids: string[]) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 条记录吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteTemplateApi(ids)
        message.success('删除成功')
        loadData()
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}
</script>

