<template>
  <NCard title="角色管理">
    <div class="flex justify-between items-start flex-wrap gap-4 mb-4">
      <NForm :model="queryForm" inline class="flex flex-wrap gap-4">
        <NFormItem label="角色名称" path="name">
          <NInput
            v-model:value="queryForm.name"
            placeholder="请输入角色名称"
            clearable
            style="width: 180px"
          />
        </NFormItem>
        <NFormItem label="角色编码" path="code">
          <NInput
            v-model:value="queryForm.code"
            placeholder="请输入角色编码"
            clearable
            style="width: 180px"
          />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSelect
            v-model:value="queryForm.status"
            :options="StatusOptions"
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

      <div>
        <NSpace>
          <NButton v-auth="'system:role:add'" type="primary" @click="handleAdd">
            <template #icon><icon-mdi-plus /></template>
            新增角色
          </NButton>
          <NButton
            v-auth="'system:role:delete'"
            type="error"
            :disabled="!hasSelection"
            @click="handleBatchDelete"
          >
            <template #icon><icon-mdi-delete /></template>
            批量删除
          </NButton>
        </NSpace>
      </div>
    </div>

    <NDataTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="paginationProps"
      :row-key="(row) => row.id"
      :checked-row-keys="selectedKeys"
      @update:checked-row-keys="onSelectionChange"
    />

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 600px">
      <NAlert v-if="isAdminRole" type="warning" style="margin-bottom: 16px">
        超级管理员角色拥有系统全部权限和数据范围，无需配置权限
      </NAlert>
      <NForm
        ref="formRef"
        :model="formData"
        :rules="currentFormRules"
        label-placement="left"
        label-width="100"
      >
        <NFormItem label="角色名称" path="name">
          <NInput v-model:value="formData.name" placeholder="请输入角色名称" />
        </NFormItem>
        <NFormItem v-if="isEdit" label="角色编码" path="code">
          <NInput v-model:value="formData.code" disabled />
        </NFormItem>
        <NFormItem v-else label="角色编码" path="code">
          <div class="flex items-center gap-4 w-full">
            <NSwitch v-model:value="autoGenerateCode">
              <template #checked>自动生成</template>
              <template #unchecked>手动输入</template>
            </NSwitch>
            <NInput
              v-if="!autoGenerateCode"
              v-model:value="formData.code"
              placeholder="请输入角色编码（小写字母开头）"
              style="flex: 1"
            />
            <div v-else style="flex: 1">
              <NText v-if="generatedCodePreview !== 'role_'" code>{{ generatedCodePreview }}</NText>
              <NText v-else depth="3">输入角色名称后自动生成</NText>
            </div>
          </div>
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NRadioGroup v-model:value="formData.status" :disabled="isAdminRole">
            <NRadio :value="1">正常</NRadio>
            <NRadio :value="0">禁用</NRadio>
          </NRadioGroup>
          <NText v-if="isAdminRole" depth="3" style="margin-left: 8px">超级管理员不可禁用</NText>
        </NFormItem>
        <NFormItem v-if="!isAdminRole" label="数据范围" path="dataScope">
          <NSelect v-model:value="formData.dataScope" :options="dataScopeOptions" />
        </NFormItem>
        <!-- 自定义数据权限 - 部门选择 -->
        <NFormItem v-if="!isAdminRole && formData.dataScope === 2" label="数据权限" path="deptIds">
          <div class="w-full">
            <NSpace style="margin-bottom: 8px">
              <NButton size="small" @click="handleSelectAllDept">全选</NButton>
              <NButton size="small" @click="handleUnselectAllDept">取消全选</NButton>
            </NSpace>
            <div class="max-h-[300px] overflow-y-auto border border-gray-300 rounded p-2">
              <NTree
                ref="deptTreeRef"
                :data="deptTreeData"
                :checked-keys="formData.deptIds"
                checkable
                block-line
                cascade
                key-field="key"
                label-field="label"
                @update:checked-keys="handleDeptCheckedChange"
              />
            </div>
          </div>
        </NFormItem>
        <NFormItem label="备注" path="description">
          <NInput v-model:value="formData.description" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 分配权限弹窗 -->
    <NModal v-model:show="showPermissionModal" title="分配权限" preset="card" style="width: 600px">
      <NAlert type="info" style="margin-bottom: 16px">
        当前角色：<strong>{{ currentRole?.name }}</strong>
      </NAlert>
      <NSpace style="margin-bottom: 16px">
        <NButton size="small" @click="handleSelectAllMenu">全选</NButton>
        <NButton size="small" @click="handleUnselectAllMenu">取消全选</NButton>
        <NButton size="small" @click="handleExpandAllMenu">展开全部</NButton>
        <NButton size="small" @click="handleCollapseAllMenu">收起全部</NButton>
      </NSpace>
      <div class="max-h-[400px] overflow-y-auto border border-gray-300 rounded p-2">
        <NTree
          ref="menuTreeRef"
          :data="menuTreeData"
          :checked-keys="permissionForm.menuIds"
          :expanded-keys="expandedMenuKeys"
          checkable
          block-line
          cascade
          key-field="key"
          label-field="label"
          @update:checked-keys="handleMenuCheckedChange"
          @update:expanded-keys="handleMenuExpandedChange"
        />
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showPermissionModal = false">取消</NButton>
          <NButton type="primary" :loading="permissionLoading" @click="handleSavePermission"
            >确定</NButton
          >
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { reactive, ref, h, computed, watch } from 'vue'
import {
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type TreeOption
} from 'naive-ui'
import {
  getRolePageApi,
  getRoleDetailApi,
  createRoleApi,
  updateRoleApi,
  deleteRoleApi,
  assignMenusApi
} from '@/api/role'
import { getMenuTreeApi } from '@/api/menu'
import { getDeptTreeApi } from '@/api/dept'
import type { RoleVO, RoleQuery } from '@/api/role'
import type { MenuVO } from '@/api/menu'
import type { DeptVO } from '@/api/dept'
import { generateRoleCode } from '@/utils/codeGenerator'
import {
  usePermission,
  useTablePage,
  useConfirm,
  useTableActions,
  useFormReset
} from '@/composables'
import { formatDateTime } from '@/utils/date'
import { convertToTreeOptions, getAllIds, getParentIds } from '@/utils/tree'
import { StatusOptions, renderStatus } from '@/utils/constants'

const message = useMessage()
const { confirmDelete, confirmBatchDelete } = useConfirm()
const { hasPermission } = usePermission()
const { renderActions, edit, del } = useTableActions()

// 数据范围选项
const dataScopeOptions = [
  { label: '全部数据', value: 1 },
  { label: '自定义', value: 2 },
  { label: '本部门', value: 3 },
  { label: '本部门及以下', value: 4 },
  { label: '仅本人', value: 5 }
]

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
  selection
} = useTablePage<RoleVO, RoleQuery>({
  fetchFn: getRolePageApi,
  defaultQuery: {
    name: undefined,
    code: undefined,
    status: undefined
  },
  enableSelection: true,
  immediate: true
})

// 选择相关的便捷访问
const selectedKeys = selection!.selectedKeys
const hasSelection = selection!.hasSelection
const clearSelection = selection!.clearSelection
const onSelectionChange = selection!.onSelectionChange

// 表格列定义
const columns: DataTableColumns<RoleVO> = [
  { type: 'selection' },
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => getIndex(index)
  },
  {
    title: '角色名称',
    key: 'name',
    width: 180,
    render: (row) => {
      if (row.code === 'admin') {
        return h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
          h('span', row.name),
          h(
            'span',
            {
              style: {
                background: '#ff9800',
                color: '#fff',
                padding: '2px 6px',
                borderRadius: '4px',
                fontSize: '12px'
              }
            },
            '超管'
          )
        ])
      }
      return row.name
    }
  },
  { title: '角色编码', key: 'code', width: 150 },
  { title: '描述', key: 'description', width: 200, ellipsis: { tooltip: true } },
  {
    title: '数据范围',
    key: 'dataScope',
    width: 120,
    render: (row) => dataScopeOptions.find((o) => o.value === row.dataScope)?.label || '未知'
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      const { text, color } = renderStatus(row.status)
      return h('span', { style: { color } }, text)
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 160,
    render: (row) => formatDateTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      const isAdmin = row.code === 'admin'
      return renderActions([
        edit(() => handleEdit(row), hasPermission('system:role:edit') && !isAdmin),
        {
          label: '分配权限',
          color: '#059669',
          onClick: () => handlePermission(row),
          show: hasPermission('system:role:edit') && !isAdmin
        },
        del(() => handleDelete(row), hasPermission('system:role:delete') && !isAdmin)
      ])
    }
  }
]

// 新增/编辑弹窗相关
const showModal = ref(false)
const modalTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInst | null>(null)
const autoGenerateCode = ref(true)

// 使用 useFormReset 管理表单
const { formData, resetForm, setForm } = useFormReset({
  id: undefined as string | undefined,
  name: '',
  code: '',
  description: '',
  status: 1,
  dataScope: 5,
  deptIds: [] as string[]
})

// 计算生成的编码预览
const generatedCodePreview = computed(() => {
  return generateRoleCode(formData.name || '')
})

// 判断是否为超级管理员角色
const isAdminRole = computed(() => formData.code === 'admin')

// 监听角色名称变化，自动生成编码
watch(
  () => formData.name,
  (newName) => {
    if (autoGenerateCode.value && !isEdit.value && newName) {
      formData.code = generatedCodePreview.value
    }
  }
)

const formRules: FormRules = {
  name: { required: true, message: '请输入角色名称', trigger: 'blur' },
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: '角色编码必须以小写字母开头，只能包含小写字母、数字和下划线',
      trigger: 'blur'
    }
  ]
}

// 动态表单验证规则
const currentFormRules = computed<FormRules>(() => {
  if (isEdit.value || !autoGenerateCode.value) {
    return formRules
  }
  return {
    name: { required: true, message: '请输入角色名称', trigger: 'blur' }
  }
})

// 分配权限相关
const showPermissionModal = ref(false)
const permissionLoading = ref(false)
const currentRole = ref<RoleVO | null>(null)
const menuTreeRef = ref<any>(null)
const menuTreeData = ref<TreeOption[]>([])
const allMenuIds = ref<string[]>([])
const expandedMenuKeys = ref<string[]>([])

const permissionForm = reactive({
  roleId: '' as string,
  menuIds: [] as string[]
})

// 部门树相关
const deptTreeRef = ref<any>(null)
const deptTreeData = ref<TreeOption[]>([])
const allDeptIds = ref<string[]>([])

// 加载部门树
const loadDeptTree = async () => {
  try {
    const res = await getDeptTreeApi()
    const deptData = res as DeptVO[]
    deptTreeData.value = convertToTreeOptions(deptData)
    allDeptIds.value = getAllIds(deptData).map(String)
  } catch (error) {
    console.error('加载部门树失败:', error)
  }
}

// 部门选中变化
const handleDeptCheckedChange = (keys: Array<string | number>) => {
  formData.deptIds = keys.map(String)
}

// 全选部门
const handleSelectAllDept = () => {
  formData.deptIds = [...allDeptIds.value]
}

// 取消全选部门
const handleUnselectAllDept = () => {
  formData.deptIds = []
}

// 加载菜单树
const loadMenuTree = async () => {
  try {
    const res = await getMenuTreeApi()
    const menuData = res as MenuVO[]
    menuTreeData.value = convertToTreeOptions(menuData)
    allMenuIds.value = getAllIds(menuData).map(String)
  } catch (error) {
    console.error('加载菜单树失败:', error)
  }
}

// 菜单选中变化
const handleMenuCheckedChange = (keys: Array<string | number>) => {
  permissionForm.menuIds = keys.map(String)
}

// 菜单展开变化
const handleMenuExpandedChange = (keys: Array<string | number>) => {
  expandedMenuKeys.value = keys.map(String)
}

// 全选菜单
const handleSelectAllMenu = () => {
  permissionForm.menuIds = [...allMenuIds.value]
}

// 取消全选菜单
const handleUnselectAllMenu = () => {
  permissionForm.menuIds = []
}

// 展开全部菜单
const handleExpandAllMenu = () => {
  getMenuTreeApi().then((data) => {
    expandedMenuKeys.value = getParentIds(data as MenuVO[]).map(String)
  })
}

// 收起全部菜单
const handleCollapseAllMenu = () => {
  expandedMenuKeys.value = []
}

// 新增
const handleAdd = () => {
  modalTitle.value = '新增角色'
  isEdit.value = false
  autoGenerateCode.value = true
  resetForm()
  showModal.value = true
}

// 编辑
const handleEdit = async (row: RoleVO) => {
  modalTitle.value = '编辑角色'
  isEdit.value = true
  try {
    const detail = await getRoleDetailApi(row.id)
    setForm({
      id: detail.id,
      name: detail.name,
      code: detail.code,
      description: detail.description,
      status: detail.status ?? 1,
      dataScope: detail.dataScope ?? 5,
      deptIds: detail.deptIds || []
    })
    showModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取角色详情失败')
  }
}

// 分配权限
const handlePermission = async (row: RoleVO) => {
  currentRole.value = row
  try {
    const detail = await getRoleDetailApi(row.id)
    permissionForm.roleId = detail.id
    permissionForm.menuIds = detail.menuIds || []
    expandedMenuKeys.value = getParentIds((await getMenuTreeApi()) as MenuVO[]).map(String)
    showPermissionModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取角色详情失败')
  }
}

// 保存权限
const handleSavePermission = async () => {
  permissionLoading.value = true
  try {
    await assignMenusApi(permissionForm.roleId, permissionForm.menuIds)
    message.success('分配权限成功')
    showPermissionModal.value = false
    loadData()
  } catch (error: any) {
    message.error(error.message || '分配权限失败')
  } finally {
    permissionLoading.value = false
  }
}

// 删除
const handleDelete = (row: RoleVO) => {
  confirmDelete(`确定要删除角色 "${row.name}" 吗？`, async () => {
    await deleteRoleApi([row.id])
    message.success('删除成功')
    loadData()
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (!hasSelection.value) {
    message.warning('请选择要删除的角色')
    return
  }
  // 过滤掉超级管理员角色
  const adminRoles = tableData.value.filter(
    (r) => selectedKeys.value.includes(r.id) && r.code === 'admin'
  )
  if (adminRoles.length > 0) {
    message.warning('超级管理员角色不可删除')
    return
  }
  confirmBatchDelete(selectedKeys.value.length, async () => {
    await deleteRoleApi(selectedKeys.value as string[])
    message.success('删除成功')
    clearSelection()
    loadData()
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    submitLoading.value = true
    try {
      const submitData = { ...formData }
      if (!isEdit.value && autoGenerateCode.value) {
        submitData.code = generatedCodePreview.value
      }

      if (isEdit.value) {
        await updateRoleApi(submitData)
        message.success('更新成功')
      } else {
        await createRoleApi(submitData)
        message.success('创建成功')
      }
      showModal.value = false
      loadData()
    } catch (error: any) {
      message.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  } catch {
    // 验证失败
  }
}

// 初始化
loadMenuTree()
loadDeptTree()
</script>
