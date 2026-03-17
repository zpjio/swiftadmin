<template>
  <NCard title="在线用户">
    <div class="toolbar mb-4">
      <NForm :model="queryForm" inline class="search-form">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="queryForm.username" placeholder="请输入用户名" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="IP 地址" path="ip">
          <NInput v-model:value="queryForm.ip" placeholder="请输入 IP 地址" clearable style="width: 180px" />
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
      
      <div class="actions">
        <NButton type="error" :disabled="checkedRowKeys.length === 0" @click="handleBatchKickout">
          <template #icon><icon-mdi-logout /></template>
          批量强退 ({{ checkedRowKeys.length }})
        </NButton>
      </div>
    </div>
    
    <NDataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row) => row.sessionId"
      :checked-row-keys="checkedRowKeys"
      @update:checked-row-keys="onChecked"
    />
  </NCard>
</template>

<script setup lang="ts">
import { reactive, ref, h, onMounted } from 'vue'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { getOnlineUserPageApi, forceLogoutApi, forceLogoutBatchApi } from '@/api/online'
import type { OnlineUserVO, OnlineUserQuery } from '@/api/online'
import { formatDateTime } from '@/utils/date'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tableData = ref<OnlineUserVO[]>([])
const queryForm = reactive<OnlineUserQuery>({
  page: 1,
  pageSize: 10,
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
    pagination.page = 1
    queryForm.page = 1
    queryForm.pageSize = pageSize
    loadData()
  },
})

const checkedRowKeys = ref<string[]>([])

// 表格列定义
const columns: DataTableColumns<OnlineUserVO> = [
  { type: 'selection' },
  {
    title: '用户 ID',
    key: 'userId',
    width: 80,
  },
  {
    title: '用户名',
    key: 'username',
    width: 120,
  },
  {
    title: '昵称',
    key: 'nickname',
    width: 120,
  },
  {
    title: '部门',
    key: 'deptName',
    width: 150,
  },
  {
    title: 'IP 地址',
    key: 'ip',
    width: 140,
  },
  {
    title: '登录地点',
    key: 'location',
    width: 180,
    ellipsis: { tooltip: true },
  },
  {
    title: '浏览器',
    key: 'browser',
    width: 150,
  },
  {
    title: '操作系统',
    key: 'os',
    width: 120,
  },
  {
    title: '登录时间',
    key: 'loginTime',
    width: 180,
    render: (row) => formatDateTime(row.loginTime) || '-',
  },
  {
    title: '剩余时间',
    key: 'ttl',
    width: 100,
    render: (row) => {
      const minutes = Math.floor(row.ttl / 60)
      return `${minutes}分钟`
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right',
    render: (row) => {
      return h(
        'button',
        {
          class: 'n-button n-button--default-type n-button--medium-type',
          style: { padding: '0 8px', background: 'none', border: 'none', color: '#ff4d4f', cursor: 'pointer' },
          onClick: () => handleKickout(row),
        },
        '强退'
      )
    },
  },
]

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getOnlineUserPageApi(queryForm)
    tableData.value = res.records || []
    pagination.page = res.pageNumber || 1
    pagination.pageSize = res.pageSize || 10
    pagination.itemCount = res.totalRow || 0
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.username = undefined
  queryForm.ip = undefined
  handleSearch()
}

// 强退单个用户
const handleKickout = (row: OnlineUserVO) => {
  dialog.warning({
    title: '确认强退',
    content: `确定要强制用户 "${row.username}" 下线吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await forceLogoutApi(row.sessionId)
        message.success('强退成功')
        loadData()
      } catch (error) {
        console.error('强退失败:', error)
      }
    },
  })
}

// 批量强退
const handleBatchKickout = () => {
  dialog.warning({
    title: '确认批量强退',
    content: `确定要强制 ${checkedRowKeys.value.length} 个用户下线吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await forceLogoutBatchApi(checkedRowKeys.value)
        message.success('批量强退成功')
        checkedRowKeys.value = []
        loadData()
      } catch (error) {
        console.error('批量强退失败:', error)
      }
    },
  })
}

// 选择
const onChecked = (rowKeys: (string | number)[]) => {
  checkedRowKeys.value = rowKeys as string[]
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>
