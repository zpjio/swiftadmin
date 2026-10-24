<template>
  <NCard title="部门管理">
    <div class="flex justify-between items-center mb-4">
      <NSpace>
        <NButton v-auth="'system:dept:add'" type="primary" @click="handleAdd()">
          <template #icon><icon-mdi-plus /></template>
          新增部门
        </NButton>
        <NButton @click="toggleExpandAll">
          {{ isAllExpanded ? '收起全部' : '展开全部' }}
        </NButton>
      </NSpace>
    </div>

    <NDataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :row-key="(row) => row.id"
      :expanded-row-keys="expandedRowKeys"
      @update:expanded-row-keys="onExpandedRowKeysChange"
      :scroll-x="1000"
    />

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 600px;">
      <NForm :model="formData" :rules="formRules" ref="formRef" label-placement="left" label-width="100">
        <NFormItem label="上级部门" path="parentId">
          <NTreeSelect
            v-model:value="formData.parentId"
            :options="deptSelectTreeData"
            placeholder="请选择上级部门"
            clearable
            default-expand-all
          />
        </NFormItem>
        <NFormItem label="部门名称" path="name">
          <NInput v-model:value="formData.name" placeholder="请输入部门名称" />
        </NFormItem>
        <NFormItem label="负责人" path="leader">
          <NInput v-model:value="formData.leader" placeholder="请输入负责人" />
        </NFormItem>
        <NFormItem label="手机号" path="phone">
          <NInput v-model:value="formData.phone" placeholder="请输入手机号" />
        </NFormItem>
        <NFormItem label="邮箱" path="email">
          <NInput v-model:value="formData.email" placeholder="请输入邮箱" />
        </NFormItem>
        <NFormItem label="排序" path="sortOrder">
          <NInputNumber v-model:value="formData.sortOrder" :min="0" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="formData.status">
            <NRadio :value="1">正常</NRadio>
            <NRadio :value="0">禁用</NRadio>
          </NRadioGroup>
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { useMessage, type DataTableColumns, type FormInst, type FormRules, type TreeSelectOption } from 'naive-ui'
import { getDeptTreeApi, getDeptDetailApi, createDeptApi, updateDeptApi, deleteDeptApi } from '@/api/dept'
import type { DeptVO, DeptDTO } from '@/api/dept'

import { usePermission, useConfirm, useTableActions, useFormReset } from '@/composables'
import { convertToTreeSelectOptions, getParentIds } from '@/utils/tree'
import { renderStatus } from '@/utils/constants'

const message = useMessage()
const { hasPermission } = usePermission()
const { confirmDelete } = useConfirm()
const { renderActions, edit, del } = useTableActions()

const loading = ref(false)
const tableData = ref<DeptVO[]>([])
const expandedRowKeys = ref<string[]>([])
const allDeptIds = ref<string[]>([])

// 是否全部展开
const isAllExpanded = computed(() => expandedRowKeys.value.length === allDeptIds.value.length)

// 展开/收起切换
const toggleExpandAll = () => {
  if (isAllExpanded.value) {
    expandedRowKeys.value = []
  } else {
    expandedRowKeys.value = [...allDeptIds.value]
  }
}

// 展开行变化
const onExpandedRowKeysChange = (keys: (string | number)[]) => {
  expandedRowKeys.value = keys.map(String)
}

// 表格列定义
const columns: DataTableColumns<DeptVO> = [
  { title: '部门名称', key: 'name', width: 250 },
  { title: '负责人', key: 'leader', width: 120 },
  { title: '手机号', key: 'phone', width: 140 },
  { title: '邮箱', key: 'email', width: 200, ellipsis: { tooltip: true } },
  { title: '排序', key: 'sortOrder', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      const { text, color } = renderStatus(row.status)
      return h('span', { style: { color } }, text)
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    fixed: 'right',
    render: (row) => renderActions([
      {
        label: '新增',
        color: '#1890ff',
        onClick: () => handleAdd(row.id),
        show: hasPermission('system:dept:add'),
      },
      edit(() => handleEdit(row), hasPermission('system:dept:edit')),
      del(() => handleDelete(row), hasPermission('system:dept:delete')),
    ]),
  },
]

// 弹窗相关
const showModal = ref(false)
const modalTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInst | null>(null)
const deptSelectTreeData = ref<TreeSelectOption[]>([])

// 使用 useFormReset 管理表单状态
const { formData, resetForm } = useFormReset<DeptDTO>({
  parentId: '',
  name: '',
  sortOrder: 0,
  leader: '',
  phone: '',
  email: '',
  status: 1,
})

const formRules: FormRules = {
  name: { required: true, message: '请输入部门名称', trigger: 'blur' },
}

// 加载部门树
const loadDeptTree = async () => {
  loading.value = true
  try {
    const res = await getDeptTreeApi()
    const treeData = res as DeptVO[]
    tableData.value = treeData
    deptSelectTreeData.value = convertToTreeSelectOptions(treeData, {
      rootLabel: '顶级部门',
      rootKey: '',
    })
    allDeptIds.value = getParentIds(treeData) as string[]
    // 默认展开全部
    expandedRowKeys.value = [...allDeptIds.value]
  } catch (error) {
    console.error('加载部门树失败:', error)
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = (parentId?: string) => {
  modalTitle.value = parentId ? '新增子部门' : '新增顶级部门'
  resetForm({ parentId: parentId || '' })
  showModal.value = true
}

// 编辑
const handleEdit = async (row: DeptVO) => {
  modalTitle.value = '编辑部门'
  try {
    const detail = await getDeptDetailApi(row.id)
    resetForm({
      id: detail.id,
      parentId: detail.parentId,
      name: detail.name,
      sortOrder: detail.sortOrder,
      leader: detail.leader,
      phone: detail.phone,
      email: detail.email,
      status: detail.status,
    })
    showModal.value = true
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '获取部门详情失败')
  }
}

// 删除
const handleDelete = (row: DeptVO) => {
  confirmDelete(`确定要删除部门 "${row.name}" 吗？如有子部门将一并删除。`, async () => {
    await deleteDeptApi(row.id)
    message.success('删除成功')
    loadDeptTree()
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    submitLoading.value = true
    try {
      if (formData.id) {
        await updateDeptApi(formData)
        message.success('更新成功')
      } else {
        await createDeptApi(formData)
        message.success('创建成功')
      }
      showModal.value = false
      loadDeptTree()
    } catch (error: unknown) {
      message.error(error instanceof Error ? error.message : '操作失败')
    } finally {
      submitLoading.value = false
    }
  } catch {
    // 验证失败
  }
}

onMounted(() => {
  loadDeptTree()
})
</script>