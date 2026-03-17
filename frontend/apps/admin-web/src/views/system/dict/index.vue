<template>
  <NCard title="字典管理" class="h-full">
    <div class="flex gap-4 h-full">
      <!-- 左侧：字典类型列表 -->
      <div class="w-80 flex-shrink-0 border-r pr-4">
        <div class="mb-4">
          <NInput v-model:value="typeQueryForm.dictName" placeholder="搜索字典名称" clearable @update:value="handleSearchType">
            <template #prefix>
              <icon-mdi-magnify />
            </template>
          </NInput>
        </div>
        <div class="mb-4 flex items-center justify-between">
          <span class="text-gray-500 text-sm">字典类型</span>
          <NSpace>
            <NButton v-auth="'system:dict:add'" type="primary" size="small" @click="handleAddType">
              <template #icon><icon-mdi-plus /></template>
              新增
            </NButton>
            <NButton size="small" quaternary @click="handleRefreshCache">
              <template #icon><icon-mdi-refresh /></template>
            </NButton>
          </NSpace>
        </div>
        <NSpin :show="typeLoading">
          <NScrollbar style="max-height: calc(100vh - 280px)">
            <div class="type-list">
              <div
                v-for="item in typeList"
                :key="item.id"
                :class="['type-item', selectedType?.id === item.id ? 'active' : '']"
                @click="handleSelectType(item)"
              >
                <div class="flex items-center justify-between">
                  <div class="flex-1 min-w-0">
                    <div class="font-medium truncate">{{ item.dictName }}</div>
                    <div class="text-xs text-gray-400 truncate">{{ item.dictType }}</div>
                  </div>
                  <NTag :type="item.status === 1 ? 'success' : 'error'" size="small">
                    {{ item.status === 1 ? '正常' : '禁用' }}
                  </NTag>
                </div>
                <div class="type-actions mt-2 flex gap-1">
                  <NButton v-if="hasPermission('system:dict:edit')" size="tiny" quaternary @click.stop="handleEditType(item)">
                    <template #icon><icon-mdi-pencil /></template>
                  </NButton>
                  <NPopconfirm v-if="hasPermission('system:dict:delete')" @positive-click="handleDeleteType(item)">
                    <template #trigger>
                      <NButton size="tiny" quaternary type="error" @click.stop>
                        <template #icon><icon-mdi-delete /></template>
                      </NButton>
                    </template>
                    确定删除该字典类型吗？
                  </NPopconfirm>
                </div>
              </div>
            </div>
            <div v-if="typeList.length === 0 && !typeLoading" class="text-center text-gray-400 py-8">
              暂无数据
            </div>
          </NScrollbar>
        </NSpin>
        <div class="mt-4 flex justify-center" v-if="typePagination.itemCount > (typeQueryForm.pageSize || 20)">
          <NPagination
            v-model:page="typeQueryForm.page"
            :page-size="typeQueryForm.pageSize || 20"
            :item-count="typePagination.itemCount"
            simple
            @update:page="loadTypeList"
          />
        </div>
      </div>

      <!-- 右侧：字典数据 -->
      <div class="flex-1 min-w-0">
        <div v-if="!selectedType" class="flex items-center justify-center h-64 text-gray-400">
          <div class="text-center">
            <icon-mdi-book-openVariant class="text-4xl mb-2" />
            <p>请在左侧选择字典类型</p>
          </div>
        </div>
        <template v-else>
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="text-lg font-medium">{{ selectedType.dictName }}</h3>
              <span class="text-xs text-gray-400">{{ selectedType.dictType }}</span>
            </div>
            <NSpace>
              <NInput v-model:value="dataQueryForm.dictLabel" placeholder="搜索数据标签" clearable style="width: 150px" @update:value="handleSearchData" />
              <NButton v-auth="'system:dict:add'" type="primary" @click="handleAddData">
                <template #icon><icon-mdi-plus /></template>
                新增数据
              </NButton>
            </NSpace>
          </div>

          <NDataTable
            :columns="dataColumns"
            :data="dataList"
            :loading="dataLoading"
            :pagination="dataPagination"
            :row-key="(row) => row.id"
            @update:page="onDataPageChange"
          />
        </template>
      </div>
    </div>

    <!-- 字典类型表单弹窗 -->
    <NModal v-model:show="showTypeModal" :title="typeModalTitle" preset="card" style="width: 500px;">
      <NForm ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100">
        <NFormItem label="字典名称" path="dictName">
          <NInput v-model:value="typeForm.dictName" placeholder="请输入字典名称" />
        </NFormItem>
        <NFormItem label="字典类型" path="dictType">
          <NInput
            v-model:value="typeForm.dictType"
            placeholder="请输入字典类型（如：user_status）"
            :disabled="isEditType"
          />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="typeForm.status">
            <NRadio :value="1">正常</NRadio>
            <NRadio :value="0">禁用</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="备注" path="remark">
          <NInput v-model:value="typeForm.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showTypeModal = false">取消</NButton>
          <NButton type="primary" :loading="typeSaving" @click="handleSaveType">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 字典数据表单弹窗 -->
    <NModal v-model:show="showDataModal" :title="dataModalTitle" preset="card" style="width: 500px;">
      <NForm ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100">
        <NFormItem label="数据标签" path="label">
          <NInput v-model:value="dataForm.label" placeholder="请输入数据标签" />
        </NFormItem>
        <NFormItem label="数据值" path="value">
          <NInput v-model:value="dataForm.value" placeholder="请输入数据值" />
        </NFormItem>
        <NFormItem label="排序" path="sortOrder">
          <NInputNumber v-model:value="dataForm.sortOrder" :min="0" placeholder="请输入排序" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="dataForm.status">
            <NRadio :value="1">正常</NRadio>
            <NRadio :value="0">禁用</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="备注" path="remark">
          <NInput v-model:value="dataForm.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDataModal = false">取消</NButton>
          <NButton type="primary" :loading="dataSaving" @click="handleSaveData">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { useMessage, useDialog, NDataTable, NTag, NButton, NSpace, NInput, NModal, NForm, NFormItem, NRadioGroup, NRadio, NInputNumber, NPopconfirm, NPagination, NSpin, NScrollbar, NCard, type DataTableColumns } from 'naive-ui'
import {
  getDictTypePageApi,
  getDictTypeDetailApi,
  createDictTypeApi,
  updateDictTypeApi,
  deleteDictTypeApi,
  getDictDataPageApi,
  getDictDataDetailApi,
  createDictDataApi,
  updateDictDataApi,
  deleteDictDataApi,
  refreshDictCacheApi,
} from '@/api/dict'
import type { DictTypeVO, DictDataVO, DictTypeDTO, DictDataDTO, DictTypeQuery, DictDataQuery } from '@/api/dict'

import { usePermission } from '@/composables'

const message = useMessage()
const dialog = useDialog()
const { hasPermission } = usePermission()

// 表单 ref
const typeFormRef = ref<any>(null)
const dataFormRef = ref<any>(null)

// 字典类型相关
const typeLoading = ref(false)
const typeList = ref<DictTypeVO[]>([])
const showTypeModal = ref(false)
const typeSaving = ref(false)
const isEditType = ref(false)
const typeModalTitle = ref('')
const typeQueryForm = reactive<DictTypeQuery>({ page: 1, pageSize: 20, dictName: undefined })

const typePagination = reactive({
  itemCount: 0,
})

// 字典数据相关
const selectedType = ref<DictTypeVO | null>(null)
const dataLoading = ref(false)
const dataList = ref<DictDataVO[]>([])
const showDataModal = ref(false)
const dataSaving = ref(false)
const isEditData = ref(false)
const dataModalTitle = ref('')
const dataQueryForm = reactive<DictDataQuery>({ page: 1, pageSize: 10, dictLabel: undefined })

const dataPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page: number) => {
    dataPagination.page = page
    dataQueryForm.page = page
    loadDataList()
  },
})

// 字典类型表单
const typeForm = reactive<DictTypeDTO>({
  dictName: '',
  dictType: '',
  status: 1,
  remark: '',
})

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
}

// 字典数据表单
const dataForm = reactive<DictDataDTO>({
  dictType: '',
  label: '',
  value: '',
  sortOrder: 0,
  status: 1,
  remark: '',
})

const dataRules = {
  label: [{ required: true, message: '请输入数据标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入数据值', trigger: 'blur' }],
}

// 字典数据表格列
const dataColumns: DataTableColumns<DictDataVO> = [
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => (dataPagination.page - 1) * dataPagination.pageSize + index + 1,
  },
  { title: '数据标签', key: 'label', width: 120 },
  { title: '数据值', key: 'value', width: 120 },
  { title: '排序', key: 'sortOrder', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, () => row.status === 1 ? '正常' : '禁用'),
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: (row) =>
      h('div', { style: { display: 'flex', gap: '8px' } }, [
        hasPermission('system:dict:edit') ? h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer' }, onClick: () => handleEditData(row) }, '编辑') : null,
        hasPermission('system:dict:delete') ? h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer' }, onClick: () => handleDeleteData(row) }, '删除') : null,
      ]),
  },
]

// 加载字典类型列表
const loadTypeList = async () => {
  typeLoading.value = true
  try {
    const res = await getDictTypePageApi(typeQueryForm)
    typeList.value = res.records || []
    typePagination.itemCount = res.totalRow || 0
  } catch (error: any) {
    message.error(error.message || '加载失败')
  } finally {
    typeLoading.value = false
  }
}

// 加载字典数据列表
const loadDataList = async () => {
  if (!selectedType.value) return

  dataLoading.value = true
  try {
    dataQueryForm.dictType = selectedType.value.dictType
    const res = await getDictDataPageApi(dataQueryForm)
    dataList.value = res.records || []
    dataPagination.itemCount = res.totalRow || 0
  } catch (error: any) {
    message.error(error.message || '加载失败')
  } finally {
    dataLoading.value = false
  }
}

// 搜索类型（防抖）
let searchTimer: number | null = null
const handleSearchType = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    typeQueryForm.page = 1
    loadTypeList()
  }, 300)
}

// 搜索数据（防抖）
let dataSearchTimer: number | null = null
const handleSearchData = () => {
  if (dataSearchTimer) clearTimeout(dataSearchTimer)
  dataSearchTimer = window.setTimeout(() => {
    dataQueryForm.page = 1
    dataPagination.page = 1
    loadDataList()
  }, 300)
}

// 数据分页
const onDataPageChange = (page: number) => {
  dataPagination.page = page
  dataQueryForm.page = page
  loadDataList()
}

// 选择字典类型
const handleSelectType = (row: DictTypeVO) => {
  selectedType.value = row
  dataQueryForm.page = 1
  dataPagination.page = 1
  dataQueryForm.dictLabel = undefined
  loadDataList()
}

// 新增字典类型
const handleAddType = () => {
  isEditType.value = false
  typeModalTitle.value = '新增字典类型'
  Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
  showTypeModal.value = true
}

// 编辑字典类型
const handleEditType = async (row: DictTypeVO) => {
  isEditType.value = true
  typeModalTitle.value = '编辑字典类型'
  try {
    const detail = await getDictTypeDetailApi(row.id)
    Object.assign(typeForm, {
      id: detail.id,
      dictName: detail.dictName || '',
      dictType: detail.dictType || '',
      status: detail.status ?? 1,
      remark: detail.remark || '',
    })
    showTypeModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取详情失败')
  }
}

// 保存字典类型
const handleSaveType = async () => {
  try {
    await typeFormRef.value?.validate()
    typeSaving.value = true

    if (isEditType.value) {
      await updateDictTypeApi(typeForm as DictTypeDTO)
    } else {
      await createDictTypeApi(typeForm as DictTypeDTO)
    }

    message.success('保存成功')
    showTypeModal.value = false
    loadTypeList()
  } catch (error: any) {
    if (error?.message) message.error(error.message)
  } finally {
    typeSaving.value = false
  }
}

// 删除字典类型
const handleDeleteType = async (row: DictTypeVO) => {
  try {
    await deleteDictTypeApi([row.id])
    message.success('删除成功')
    loadTypeList()
    if (selectedType.value?.id === row.id) {
      selectedType.value = null
      dataList.value = []
    }
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 刷新字典缓存
const handleRefreshCache = async () => {
  try {
    await refreshDictCacheApi()
    message.success('刷新成功')
  } catch (error: any) {
    message.error(error.message || '刷新失败')
  }
}

// 新增字典数据
const handleAddData = () => {
  isEditData.value = false
  dataModalTitle.value = '新增字典数据'
  Object.assign(dataForm, {
    id: undefined,
    dictType: selectedType.value?.dictType || '',
    label: '',
    value: '',
    sortOrder: 0,
    status: 1,
    remark: '',
  })
  showDataModal.value = true
}

// 编辑字典数据
const handleEditData = async (row: DictDataVO) => {
  isEditData.value = true
  dataModalTitle.value = '编辑字典数据'
  try {
    const detail = await getDictDataDetailApi(row.id)
    Object.assign(dataForm, {
      id: detail.id,
      dictType: detail.dictType || '',
      label: detail.label || '',
      value: detail.value || '',
      sortOrder: detail.sortOrder ?? 0,
      status: detail.status ?? 1,
      remark: detail.remark || '',
    })
    showDataModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取详情失败')
  }
}

// 保存字典数据
const handleSaveData = async () => {
  try {
    await dataFormRef.value?.validate()
    dataSaving.value = true

    if (isEditData.value) {
      await updateDictDataApi(dataForm as DictDataDTO)
    } else {
      await createDictDataApi(dataForm as DictDataDTO)
    }

    message.success('保存成功')
    showDataModal.value = false
    loadDataList()
  } catch (error: any) {
    if (error?.message) message.error(error.message)
  } finally {
    dataSaving.value = false
  }
}

// 删除字典数据
const handleDeleteData = (row: DictDataVO) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除数据 "${row.label}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteDictDataApi([row.id])
        message.success('删除成功')
        loadDataList()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    },
  })
}

onMounted(() => {
  loadTypeList()
})
</script>

<style scoped>
.h-full {
  height: 100%;
}

.flex {
  display: flex;
}

.gap-4 {
  gap: 16px;
}

.gap-2 {
  gap: 8px;
}

.w-80 {
  width: 320px;
}

.flex-shrink-0 {
  flex-shrink: 0;
}

.flex-1 {
  flex: 1;
}

.min-w-0 {
  min-width: 0;
}

.border-r {
  border-right: 1px solid #e0e0e0;
}

.pr-4 {
  padding-right: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-2 {
  margin-top: 8px;
}

.mt-4 {
  margin-top: 16px;
}

.type-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.type-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.type-item:hover {
  background-color: #f5f5f5;
}

.type-item.active {
  background-color: #e3f2fd;
  border-color: #2196f3;
}

.type-actions {
  display: flex;
  gap: 8px;
}

.font-medium {
  font-weight: 500;
}

.text-xs {
  font-size: 12px;
}

.text-sm {
  font-size: 14px;
}

.text-lg {
  font-size: 18px;
}

.text-4xl {
  font-size: 36px;
}

.text-gray-400 {
  color: #9ca3af;
}

.text-gray-500 {
  color: #6b7280;
}

.text-center {
  text-align: center;
}

.items-center {
  align-items: center;
}

.justify-center {
  justify-content: center;
}

.justify-between {
  justify-content: space-between;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.py-8 {
  padding-top: 32px;
  padding-bottom: 32px;
}
</style>