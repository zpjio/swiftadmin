<template>
  <div class="p-4 h-full">
    <NCard title="发送记录" :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline class="mb-4">
        <NFormItem label="消息ID" path="messageId">
          <NInput
            v-model:value="queryForm.messageId"
            placeholder="请输入消息ID"
            clearable
            style="width: 200px"
          />
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
        <NFormItem label="接收地址" path="toAddress">
          <NInput
            v-model:value="queryForm.toAddress"
            placeholder="请输入接收地址"
            clearable
            style="width: 180px"
          />
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

      <!-- 表格 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="paginationProps"
      />
    </NCard>

    <!-- 详情弹窗 -->
    <NModal v-model:show="showDetailModal" title="发送详情" preset="card" style="width: 700px">
      <NDescriptions :column="2" label-placement="left" bordered>
        <NDescriptionsItem label="消息ID">{{ currentRecord?.messageId }}</NDescriptionsItem>
        <NDescriptionsItem label="渠道类型">{{
          getChannelTypeLabel(currentRecord?.channelType ?? '')
        }}</NDescriptionsItem>
        <NDescriptionsItem label="接收地址">{{ currentRecord?.toAddress }}</NDescriptionsItem>
        <NDescriptionsItem label="状态">
          <NTag :type="getStatusType(currentRecord?.status)">{{
            getStatusLabel(currentRecord?.status)
          }}</NTag>
        </NDescriptionsItem>
        <NDescriptionsItem label="主题" :span="2">{{
          currentRecord?.subject || '-'
        }}</NDescriptionsItem>
        <NDescriptionsItem label="模板编码">{{
          currentRecord?.templateCode || '-'
        }}</NDescriptionsItem>
        <NDescriptionsItem label="业务类型">{{
          getBizTypeLabel(currentRecord?.bizType ?? '') || '-'
        }}</NDescriptionsItem>
        <NDescriptionsItem label="发送时间">{{
          formatDateTime(currentRecord?.sendTime)
        }}</NDescriptionsItem>
        <NDescriptionsItem label="到达时间">{{
          formatDateTime(currentRecord?.arriveTime)
        }}</NDescriptionsItem>
        <NDescriptionsItem label="耗时">{{
          currentRecord?.costTime ? currentRecord.costTime + 'ms' : '-'
        }}</NDescriptionsItem>
        <NDescriptionsItem label="重试次数">{{ currentRecord?.retryCount }}</NDescriptionsItem>
        <NDescriptionsItem label="消息内容" :span="2">
          <div style="max-height: 200px; overflow-y: auto">{{ currentRecord?.content }}</div>
        </NDescriptionsItem>
        <NDescriptionsItem v-if="currentRecord?.errorMsg" label="错误信息" :span="2">
          <NText type="error">{{ currentRecord.errorMsg }}</NText>
        </NDescriptionsItem>
      </NDescriptions>

      <template #footer>
        <NSpace justify="end">
          <NButton
            v-if="currentRecord?.status === 'failed' && currentRecord?.retryCount < 3"
            type="warning"
            @click="handleRetry"
          >
            重试发送
          </NButton>
          <NButton @click="showDetailModal = false">关闭</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useMessage, NTag, NButton, type DataTableColumns } from 'naive-ui'
import {
  getMessageRecordPageApi,
  getMessageRecordDetailApi,
  retryMessageApi,
  type MessageRecord,
  type ChannelType,
  type MessageStatus
} from '@/api/message'
import { useDict } from '@/composables/useDict'
import { usePagination } from '@/composables'
import { formatDateTime } from '@/utils/date'

const message = useMessage()
const loading = ref(false)
const tableData = ref<MessageRecord[]>([])
const showDetailModal = ref(false)
const currentRecord = ref<MessageRecord | null>(null)

// 渠道类型选项
const { options: channelTypeOptions, getLabel: getChannelTypeLabel } = useDict('msg_channel_type')

// 消息类型选项
const { getLabel: getBizTypeLabel } = useDict('msg_type')

const statusOptions = [
  { label: '待发送', value: 'pending' },
  { label: '发送中', value: 'sending' },
  { label: '发送成功', value: 'success' },
  { label: '发送失败', value: 'failed' }
]

function getStatusLabel(status?: string) {
  const item = statusOptions.find((s) => s.value === status)
  return item?.label || status
}

function getStatusType(status?: string): 'default' | 'info' | 'success' | 'error' | 'warning' {
  const map: Record<string, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
    pending: 'default',
    sending: 'info',
    success: 'success',
    failed: 'error'
  }
  return map[status || ''] || 'default'
}

const queryForm = reactive({
  messageId: undefined as string | undefined,
  channelType: undefined as ChannelType | undefined,
  toAddress: undefined as string | undefined,
  status: undefined as MessageStatus | undefined
})

// 分页
const { paginationProps, setTotal, getParams } = usePagination({
  onChange: () => loadData(),
  onPageSizeChange: () => loadData()
})

const columns: DataTableColumns<MessageRecord> = [
  { title: '消息ID', key: 'messageId', width: 200, ellipsis: { tooltip: true } },
  {
    title: '渠道类型',
    key: 'channelType',
    width: 100,
    render: (row) => h(NTag, { type: 'info' }, () => getChannelTypeLabel(row.channelType ?? ''))
  },
  { title: '接收地址', key: 'toAddress', width: 180, ellipsis: { tooltip: true } },
  { title: '主题', key: 'subject', width: 150, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => h(NTag, { type: getStatusType(row.status) }, () => getStatusLabel(row.status))
  },
  { title: '重试次数', key: 'retryCount', width: 80 },
  { title: '发送时间', key: 'sendTime', width: 170, render: (row) => formatDateTime(row.sendTime) },
  {
    title: '创建时间',
    key: 'createTime',
    width: 170,
    render: (row) => formatDateTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: (row) =>
      h(NButton, { text: true, type: 'primary', onClick: () => handleDetail(row) }, () => '详情')
  }
]

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMessageRecordPageApi({ ...queryForm, ...getParams() })
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
  queryForm.messageId = undefined
  queryForm.channelType = undefined
  queryForm.toAddress = undefined
  queryForm.status = undefined
  loadData()
}

async function handleDetail(row: MessageRecord) {
  try {
    const res = await getMessageRecordDetailApi(row.id)
    currentRecord.value = res
    showDetailModal.value = true
  } catch {
    message.error('获取详情失败')
  }
}

async function handleRetry() {
  if (!currentRecord.value) return
  try {
    const res = await retryMessageApi(currentRecord.value.id)
    if (res?.success) {
      message.success('重试发送成功')
    } else {
      message.error(res?.errorMsg || '重试发送失败')
    }
    showDetailModal.value = false
    loadData()
  } catch {
    message.error('重试发送失败')
  }
}
</script>
