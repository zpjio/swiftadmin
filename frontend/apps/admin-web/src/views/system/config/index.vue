<template>
  <div class="h-full">
    <NCard title="系统配置" :bordered="false">
      <!-- 搜索栏 -->
      <div class="flex justify-between items-start flex-wrap gap-4 mb-4">
        <NForm :model="queryForm" inline class="flex flex-wrap gap-4">
          <NFormItem label="配置名称" path="configName">
            <NInput v-model:value="queryForm.configName" placeholder="请输入配置名称" clearable style="width: 180px" />
          </NFormItem>
          <NFormItem label="配置键" path="configKey">
            <NInput v-model:value="queryForm.configKey" placeholder="请输入配置键" clearable style="width: 180px" />
          </NFormItem>
          <NFormItem label="配置类型" path="configType">
            <NSelect
              v-model:value="queryForm.configType"
              :options="configTypeOptions"
              placeholder="请选择配置类型"
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

        <NSpace>
          <NButton v-auth="'system:config:add'" type="primary" @click="handleAdd">
            <template #icon><icon-mdi-plus /></template>
            新增配置
          </NButton>
          <NButton @click="handleRefreshCache">
            <template #icon><icon-mdi-refresh /></template>
            刷新缓存
          </NButton>
          <NButton v-auth="'system:config:delete'" type="error" :disabled="!hasSelection" @click="handleBatchDelete">
            <template #icon><icon-mdi-delete /></template>
            批量删除
          </NButton>
        </NSpace>
      </div>

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
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 500px;">
      <NForm ref="formRef" :model="formData" :rules="rules" label-width="100">
        <NFormItem label="配置名称" path="configName">
          <NInput v-model:value="formData.configName" placeholder="请输入配置名称" />
        </NFormItem>
        <NFormItem label="配置键" path="configKey">
          <NInput v-model:value="formData.configKey" placeholder="请输入配置键（如：system.name）" :disabled="isEdit" />
        </NFormItem>
        <NFormItem label="配置值" path="configValue">
          <NInput v-model:value="formData.configValue" placeholder="请输入配置值" />
        </NFormItem>
        <NFormItem label="配置类型" path="configType">
          <NSelect
            v-model:value="formData.configType"
            :options="configTypeOptions"
            placeholder="请选择配置类型"
          />
        </NFormItem>
        <NFormItem label="描述" path="description">
          <NInput v-model:value="formData.description" type="textarea" placeholder="请输入配置描述" :rows="3" />
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
import { ref } from 'vue'
import { useMessage, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui'
import {
  getConfigPageApi,
  getConfigDetailApi,
  createConfigApi,
  updateConfigApi,
  deleteConfigApi,
  refreshConfigCacheApi,
  type ConfigVO,
  type ConfigDTO,
  type ConfigQuery,
} from '@/api/config'
import { useDict, usePermission, useTablePage, useConfirm, useTableActions, useFormReset } from '@/composables'

const message = useMessage()
const { hasPermission } = usePermission()
const { confirmDelete, confirmBatchDelete } = useConfirm()
const { renderActions, edit, del } = useTableActions()

const formRef = ref<FormInst | null>(null)
const showModal = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const modalTitle = ref('')

// 配置类型选项（从字典获取）
const { options: configTypeOptions, getLabel: getConfigTypeLabel } = useDict('sys_config_type')

// 表格分页
const {
  loading,
  tableData,
  queryForm,
  paginationProps,
  getIndex,
  loadData,
  handleSearch,
  handleReset,
  selection,
} = useTablePage<ConfigVO, ConfigQuery>({
  fetchFn: getConfigPageApi,
  defaultQuery: {
    configName: undefined,
    configKey: undefined,
    configType: undefined,
  },
  enableSelection: true,
  immediate: true,
})

// 选择相关的便捷访问
const selectedKeys = selection!.selectedKeys
const hasSelection = selection!.hasSelection
const clearSelection = selection!.clearSelection
const onSelectionChange = selection!.onSelectionChange

// 使用 useFormReset 管理表单
const { formData, resetForm, setForm } = useFormReset<ConfigDTO>({
  id: undefined,
  configName: '',
  configKey: '',
  configValue: '',
  configType: '',
  description: '',
})

const rules: FormRules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  configType: [{ required: true, message: '请选择配置类型', trigger: 'change' }],
}

const columns: DataTableColumns<ConfigVO> = [
  { type: 'selection', fixed: 'left' },
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => getIndex(index),
  },
  { title: '配置名称', key: 'configName', width: 150, ellipsis: { tooltip: true } },
  { title: '配置键', key: 'configKey', width: 180, ellipsis: { tooltip: true } },
  { title: '配置值', key: 'configValue', width: 200, ellipsis: { tooltip: true } },
  {
    title: '配置类型',
    key: 'configType',
    width: 120,
    render: (row) => getConfigTypeLabel(row.configType),
  },
  { title: '描述', key: 'description', width: 200, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render: (row) => renderActions([
      edit(() => handleEdit(row), hasPermission('system:config:edit')),
      del(() => handleDelete(row), hasPermission('system:config:delete')),
    ]),
  },
]

const handleAdd = () => {
  isEdit.value = false
  modalTitle.value = '新增配置'
  resetForm()
  showModal.value = true
}

const handleEdit = async (row: ConfigVO) => {
  isEdit.value = true
  modalTitle.value = '编辑配置'
  try {
    const detail = await getConfigDetailApi(row.id)
    setForm(detail)
    showModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取详情失败')
  }
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    saving.value = true

    if (isEdit.value) {
      await updateConfigApi(formData)
    } else {
      await createConfigApi(formData)
    }

    message.success('保存成功')
    showModal.value = false
    loadData()
  } catch (error: any) {
    if (error?.message) message.error(error.message)
  } finally {
    saving.value = false
  }
}

const handleDelete = (row: ConfigVO) => {
  confirmDelete(`确定要删除配置 "${row.configName}" 吗？`, async () => {
    await deleteConfigApi([row.id])
    message.success('删除成功')
    loadData()
  })
}

const handleBatchDelete = () => {
  if (!hasSelection.value) {
    message.warning('请选择要删除的配置')
    return
  }
  confirmBatchDelete(selectedKeys.value.length, async () => {
    await deleteConfigApi(selectedKeys.value as string[])
    message.success('删除成功')
    clearSelection()
    loadData()
  })
}

const handleRefreshCache = async () => {
  try {
    await refreshConfigCacheApi()
    message.success('刷新成功')
  } catch (error: any) {
    message.error(error.message || '刷新失败')
  }
}