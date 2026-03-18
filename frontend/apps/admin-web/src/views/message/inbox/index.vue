<template>
  <div class="inbox-container">
    <NCard title="站内信" :bordered="false">
      <!-- 搜索栏 -->
      <NForm :model="queryForm" inline class="mb-4">
        <NFormItem label="阅读状态" path="isRead">
          <NSelect
            v-model:value="queryForm.isRead"
            :options="readStatusOptions"
            placeholder="请选择阅读状态"
            clearable
            style="width: 120px"
          />
        </NFormItem>
        <NFormItem label="消息类型" path="msgType">
          <NSelect
            v-model:value="queryForm.msgType"
            :options="msgTypeOptions"
            placeholder="请选择消息类型"
            clearable
            style="width: 150px"
          />
        </NFormItem>
        <NFormItem label="关键词" path="keyword">
          <NInput v-model:value="queryForm.keyword" placeholder="搜索标题/内容" clearable style="width: 200px" />
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
        <NButton v-auth="'message:inbox:send'" type="primary" @click="handleSend">
          <template #icon><icon-mdi-send /></template>
          发送站内信
        </NButton>
        <NButton @click="handleMarkAllRead">
          <template #icon><icon-mdi-check-all /></template>
          全部已读
        </NButton>
        <NButton v-auth="'message:inbox:delete'" type="error" :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
          <template #icon><icon-mdi-delete /></template>
          批量删除
        </NButton>
      </NSpace>

      <!-- 未读消息提示 -->
      <NAlert v-if="unreadCount > 0" type="info" class="mb-4">
        您有 {{ unreadCount }} 条未读消息
      </NAlert>

      <!-- 表格 -->
      <NDataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row) => row.id"
        :checked-row-keys="selectedRowKeys"
        :pagination="paginationProps"
        @update:checked-row-keys="onCheckedRowKeysChange"
      />
    </NCard>

    <!-- 发送站内信弹窗 -->
    <NModal v-model:show="showSendModal" title="发送站内信" preset="card" style="width: 600px;">
      <NForm ref="sendFormRef" :model="sendForm" :rules="sendRules" label-width="100">
        <NFormItem label="接收用户" path="userIds">
          <NInput v-model:value="sendForm.userIds" placeholder="请输入用户ID，多个用逗号分隔" />
        </NFormItem>
        <NFormItem label="标题" path="title">
          <NInput v-model:value="sendForm.title" placeholder="请输入消息标题" />
        </NFormItem>
        <NFormItem label="消息类型" path="msgType">
          <NSelect
            v-model:value="sendForm.msgType"
            :options="msgTypeOptions"
            placeholder="请选择消息类型"
          />
        </NFormItem>
        <NFormItem label="内容" path="content">
          <NInput v-model:value="sendForm.content" type="textarea" placeholder="请输入消息内容" :rows="5" />
        </NFormItem>
        <NFormItem label="优先级" path="priority">
          <NInputNumber v-model:value="sendForm.priority" :min="0" :max="10" style="width: 150px" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showSendModal = false">取消</NButton>
          <NButton type="primary" :loading="sending" @click="handleSendSubmit">发送</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 消息详情弹窗 -->
    <NModal v-model:show="showDetailModal" title="消息详情" preset="card" style="width: 600px;">
      <NDescriptions :column="1" label-placement="left" bordered>
        <NDescriptionsItem label="标题">{{ currentMessage?.title }}</NDescriptionsItem>
        <NDescriptionsItem label="发送者">{{ currentMessage?.senderName || '系统' }}</NDescriptionsItem>
        <NDescriptionsItem label="发送时间">{{ formatDateTime(currentMessage?.createTime) }}</NDescriptionsItem>
        <NDescriptionsItem label="消息类型">{{ getMsgTypeLabel(currentMessage?.msgType ?? '') }}</NDescriptionsItem>
        <NDescriptionsItem label="内容">
          <div v-html="currentMessage?.content?.replace(/\n/g, '<br>')"></div>
        </NDescriptionsItem>
      </NDescriptions>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDetailModal = false">关闭</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useMessage, useDialog, NTag, NButton, NSpace, type DataTableColumns, type DataTableRowKey } from 'naive-ui'
import {
  getInboxPageApi,
  getInboxDetailApi,
  getUnreadCountApi,
  sendInboxApi,
  markInboxReadApi,
  markAllInboxReadApi,
  setInboxStarApi,
  deleteInboxApi,
  type InboxMessage,
} from '@/api/message'
import { useDict } from '@/composables/useDict'
import { usePagination } from '@/composables'
import { formatDateTime } from '@/utils/date'

const message = useMessage()
const dialog = useDialog()
const formRef = ref<any>(null)
const sendFormRef = ref<any>(null)
const loading = ref(false)
const tableData = ref<InboxMessage[]>([])
const selectedRowKeys = ref<DataTableRowKey[]>([])
const showSendModal = ref(false)
const showDetailModal = ref(false)
const sending = ref(false)
const unreadCount = ref(0)
const currentMessage = ref<InboxMessage | null>(null)

// 消息类型选项
const { options: msgTypeOptions, getLabel: getMsgTypeLabel } = useDict('msg_type')

const readStatusOptions = [
  { label: '未读', value: 0 },
  { label: '已读', value: 1 },
]

const queryForm = reactive({
  isRead: undefined as number | undefined,
  msgType: undefined as string | undefined,
  keyword: undefined as string | undefined,
})

// 分页
const { paginationProps, setTotal, getParams } = usePagination({
  onChange: () => loadData(),
  onPageSizeChange: () => loadData(),
})

const sendForm = reactive({
  userIds: '',
  title: '',
  content: '',
  msgType: 'system',
  priority: 0,
})

const sendRules = {
  userIds: [{ required: true, message: '请输入接收用户ID', trigger: 'blur' }],
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
}

const columns: DataTableColumns<InboxMessage> = [
  { type: 'selection' },
  {
    title: '状态',
    key: 'isRead',
    width: 80,
    render: (row) => h(NTag, { type: row.isRead ? 'default' : 'error' }, () => row.isRead ? '已读' : '未读'),
  },
  {
    title: '星标',
    key: 'isStar',
    width: 60,
    render: (row) =>
      h(NButton, {
        text: true,
        type: row.isStar ? 'warning' : 'default',
        onClick: () => handleToggleStar(row),
      }, () => row.isStar ? '★' : '☆'),
  },
  { title: '标题', key: 'title', width: 200, ellipsis: { tooltip: true } },
  {
    title: '消息类型',
    key: 'msgType',
    width: 100,
    render: (row) => h(NTag, { type: 'info', size: 'small' }, () => getMsgTypeLabel(row.msgType ?? '')),
  },
  { title: '发送者', key: 'senderName', width: 100 },
  { title: '发送时间', key: 'createTime', width: 170, render: (row) => formatDateTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row) =>
      h(NSpace, null, () => [
        h(NButton, { text: true, type: 'primary', onClick: () => handleDetail(row) }, () => '查看'),
        h(NButton, { text: true, type: 'warning', onClick: () => handleMarkRead([row.id]) }, () => '标为已读'),
        h(NButton, { text: true, type: 'error', onClick: () => handleDelete([row.id]) }, () => '删除'),
      ]),
  },
]

onMounted(() => {
  loadData()
  loadUnreadCount()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getInboxPageApi({ ...queryForm, ...getParams() })
    tableData.value = res?.records || []
    setTotal(res?.totalRow || 0)
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCountApi()
    unreadCount.value = res?.total || 0
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  queryForm.isRead = undefined
  queryForm.msgType = undefined
  queryForm.keyword = undefined
  loadData()
}

function onCheckedRowKeysChange(keys: DataTableRowKey[]) {
  selectedRowKeys.value = keys
}

function handleSend() {
  Object.assign(sendForm, {
    userIds: '',
    title: '',
    content: '',
    msgType: 'system',
    priority: 0,
  })
  showSendModal.value = true
}

async function handleSendSubmit() {
  try {
    await sendFormRef.value?.validate()
    sending.value = true
    const res = await sendInboxApi(sendForm)
    message.success(`发送成功，共发送 ${res} 条消息`)
    showSendModal.value = false
  } catch (error) {
    console.error('发送失败:', error)
  } finally {
    sending.value = false
  }
}

async function handleDetail(row: InboxMessage) {
  try {
    const res = await getInboxDetailApi(row.id)
    currentMessage.value = res
    showDetailModal.value = true
    loadUnreadCount()
  } catch (error) {
    message.error('获取消息详情失败')
  }
}

async function handleMarkRead(ids: string[]) {
  try {
    await markInboxReadApi(ids)
    message.success('标记成功')
    loadData()
    loadUnreadCount()
  } catch (error) {
    message.error('标记失败')
  }
}

async function handleMarkAllRead() {
  try {
    await markAllInboxReadApi()
    message.success('已全部标记为已读')
    loadData()
    loadUnreadCount()
  } catch (error) {
    message.error('操作失败')
  }
}

async function handleToggleStar(row: InboxMessage) {
  try {
    await setInboxStarApi(row.id, !row.isStar)
    loadData()
  } catch (error) {
    message.error('操作失败')
  }
}

function handleDelete(ids: string[]) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${ids.length} 条消息吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteInboxApi(ids)
        message.success('删除成功')
        loadData()
        loadUnreadCount()
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的消息')
    return
  }
  handleDelete(selectedRowKeys.value as string[])
}
</script>

<style scoped>
.inbox-container {
  padding: 16px;
}
</style>