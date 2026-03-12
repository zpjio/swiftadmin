<template>
  <NCard title="角色管理">
    <div class="toolbar mb-4">
      <NForm :model="queryForm" inline class="search-form">
        <NFormItem label="角色名称" path="name">
          <NInput v-model:value="queryForm.name" placeholder="请输入角色名称" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="角色编码" path="code">
          <NInput v-model:value="queryForm.code" placeholder="请输入角色编码" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSelect
            v-model:value="queryForm.status"
            :options="[
              { label: '全部', value: undefined },
              { label: '正常', value: 1 },
              { label: '禁用', value: 0 },
            ]"
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

      <div class="actions">
        <NSpace>
          <NButton type="primary" @click="handleAdd">
            <template #icon><icon-mdi-plus /></template>
            新增角色
          </NButton>
          <NButton type="error" :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
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
      :pagination="pagination"
      :row-key="(row) => row.id"
      :checked-row-keys="selectedRowKeys"
      @update:checked-row-keys="onChecked"
    />

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 500px;">
      <NForm :model="formData" :rules="currentFormRules" ref="formRef" label-placement="left" label-width="100">
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
          <NRadioGroup v-model:value="formData.status">
            <NRadio :value="1">正常</NRadio>
            <NRadio :value="0">禁用</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="数据范围" path="dataScope">
          <NSelect
            v-model:value="formData.dataScope"
            :options="[
              { label: '全部数据', value: 1 },
              { label: '本部门及以下', value: 2 },
              { label: '本部门', value: 3 },
              { label: '仅本人', value: 4 },
            ]"
          />
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
    <NModal v-model:show="showPermissionModal" title="分配权限" preset="card" style="width: 600px;">
      <NAlert type="info" style="margin-bottom: 16px;">
        当前角色：<strong>{{ currentRole?.name }}</strong>
      </NAlert>
      <NSpace style="margin-bottom: 16px;">
        <NButton size="small" @click="handleSelectAllMenu">全选</NButton>
        <NButton size="small" @click="handleUnselectAllMenu">取消全选</NButton>
        <NButton size="small" @click="handleExpandAllMenu">展开全部</NButton>
        <NButton size="small" @click="handleCollapseAllMenu">收起全部</NButton>
      </NSpace>
      <div class="menu-tree-container">
        <NTree
          ref="menuTreeRef"
          :data="menuTreeData"
          :checked-keys="permissionForm.menuIds"
          @update:checked-keys="handleMenuCheckedChange"
          :expanded-keys="expandedMenuKeys"
          @update:expanded-keys="handleMenuExpandedChange"
          checkable
          block-line
          cascade
          key-field="key"
          label-field="label"
        />
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showPermissionModal = false">取消</NButton>
          <NButton type="primary" :loading="permissionLoading" @click="handleSavePermission">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { reactive, ref, h, onMounted, computed, watch } from 'vue'
import { useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeOption } from 'naive-ui'
import { getRolePageApi, getRoleDetailApi, createRoleApi, updateRoleApi, deleteRoleApi, assignMenusApi } from '@/api/role'
import { getMenuTreeApi } from '@/api/menu'
import type { RoleVO, RoleDTO, RoleQuery } from '@/api/role'
import type { MenuVO } from '@/api/menu'
import { generateRoleCode } from '@/utils/codeGenerator'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tableData = ref<RoleVO[]>([])
const selectedRowKeys = ref<number[]>([])
const queryForm = reactive<RoleQuery>({
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

// 表格列定义
const columns: DataTableColumns<RoleVO> = [
  { type: 'selection' },
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色名称', key: 'name', width: 150 },
  { title: '角色编码', key: 'code', width: 150 },
  { title: '描述', key: 'description', width: 200, ellipsis: { tooltip: true } },
  {
    title: '数据范围',
    key: 'dataScope',
    width: 120,
    render: (row) => ({ 1: '全部数据', 2: '本部门及以下', 3: '本部门', 4: '仅本人' }[row.dataScope] || '未知'),
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => h('span', { style: { color: row.status === 1 ? '#52c41a' : '#ff4d4f' } }, row.status === 1 ? '正常' : '禁用'),
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right',
    render: (row) => h('div', { style: { display: 'flex', gap: '8px' } }, [
      h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#1890ff', cursor: 'pointer' }, onClick: () => handleEdit(row) }, '编辑'),
      h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#52c41a', cursor: 'pointer' }, onClick: () => handlePermission(row) }, '分配权限'),
      h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#ff4d4f', cursor: 'pointer' }, onClick: () => handleDelete(row) }, '删除'),
    ]),
  },
]

// 新增/编辑弹窗相关
const showModal = ref(false)
const modalTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInst | null>(null)
const autoGenerateCode = ref(true) // 默认自动生成编码

const formData = reactive<RoleDTO>({
  name: '',
  code: '',
  description: '',
  status: 1,
  dataScope: 1,
})

// 计算生成的编码预览
const generatedCodePreview = computed(() => {
  return generateRoleCode(formData.name || '')
})

// 监听角色名称变化，自动生成编码
watch(() => formData.name, (newName) => {
  if (autoGenerateCode.value && !isEdit.value && newName) {
    formData.code = generatedCodePreview.value
  }
})

const formRules: FormRules = {
  name: { required: true, message: '请输入角色名称', trigger: 'blur' },
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '角色编码必须以小写字母开头，只能包含小写字母、数字和下划线', trigger: 'blur' },
  ],
}

// 动态表单验证规则（根据是否自动生成编码）
const currentFormRules = computed<FormRules>(() => {
  if (isEdit.value || !autoGenerateCode.value) {
    return formRules
  }
  // 自动生成时不需要验证 code
  return {
    name: { required: true, message: '请输入角色名称', trigger: 'blur' },
  }
})

// 分配权限相关
const showPermissionModal = ref(false)
const permissionLoading = ref(false)
const currentRole = ref<RoleVO | null>(null)
const menuTreeRef = ref<any>(null)
const menuTreeData = ref<TreeOption[]>([])
const allMenuIds = ref<number[]>([])
const expandedMenuKeys = ref<number[]>([])

const permissionForm = reactive({
  roleId: 0,
  menuIds: [] as number[],
})

// 加载菜单树
const loadMenuTree = async () => {
  try {
    const res = await getMenuTreeApi()
    const menuData = res as MenuVO[]
    menuTreeData.value = convertMenuToTree(menuData)
    allMenuIds.value = getAllMenuIds(menuData)
  } catch (error) {
    console.error('加载菜单树失败:', error)
  }
}

// 获取所有菜单ID
const getAllMenuIds = (menus: MenuVO[]): number[] => {
  const ids: number[] = []
  const traverse = (items: MenuVO[]) => {
    items.forEach((item) => {
      ids.push(item.id)
      if (item.children) traverse(item.children)
    })
  }
  traverse(menus)
  return ids
}

// 获取所有菜单ID用于展开
const getAllMenuKeysForExpand = (menus: MenuVO[]): number[] => {
  const keys: number[] = []
  const traverse = (items: MenuVO[]) => {
    items.forEach((item) => {
      if (item.children && item.children.length > 0) {
        keys.push(item.id)
        traverse(item.children)
      }
    })
  }
  traverse(menus)
  return keys
}

// 转换菜单数据为树形结构
const convertMenuToTree = (menus: MenuVO[]): TreeOption[] => {
  return menus.map((menu) => ({
    key: menu.id,
    label: menu.name,
    children: menu.children ? convertMenuToTree(menu.children) : undefined,
  }))
}

// 菜单选中变化
const handleMenuCheckedChange = (keys: Array<string | number>) => {
  permissionForm.menuIds = keys as number[]
}

// 菜单展开变化
const handleMenuExpandedChange = (keys: Array<string | number>) => {
  expandedMenuKeys.value = keys as number[]
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
  const res = menuTreeData.value
  // 需要从原始菜单数据获取所有有子节点的ID
  getMenuTreeApi().then((data) => {
    expandedMenuKeys.value = getAllMenuKeysForExpand(data as MenuVO[])
  })
}

// 收起全部菜单
const handleCollapseAllMenu = () => {
  expandedMenuKeys.value = []
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getRolePageApi(queryForm)
    tableData.value = res.records
    pagination.page = res.pageNumber
    pagination.pageSize = res.pageSize
    pagination.itemCount = res.totalRow
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  queryForm.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.name = undefined
  queryForm.code = undefined
  queryForm.status = undefined
  handleSearch()
}

// 新增
const handleAdd = () => {
  modalTitle.value = '新增角色'
  isEdit.value = false
  autoGenerateCode.value = true // 默认自动生成
  Object.assign(formData, {
    id: undefined,
    name: '',
    code: '',
    description: '',
    status: 1,
    dataScope: 1,
  })
  showModal.value = true
}

// 编辑
const handleEdit = async (row: RoleVO) => {
  modalTitle.value = '编辑角色'
  isEdit.value = true
  try {
    const detail = await getRoleDetailApi(row.id)
    Object.assign(formData, {
      id: detail.id,
      name: detail.name,
      code: detail.code,
      description: detail.description,
      status: detail.status,
      dataScope: detail.dataScope,
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
    // 展开已选中的菜单
    expandedMenuKeys.value = getAllMenuKeysForExpand(await getMenuTreeApi() as MenuVO[])
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
  dialog.warning({
    title: '确认删除',
    content: `确定要删除角色 "${row.name}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteRoleApi([row.id])
        message.success('删除成功')
        loadData()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    },
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的角色')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个角色吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteRoleApi(selectedRowKeys.value)
        message.success('删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    },
  })
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    submitLoading.value = true
    try {
      const submitData = { ...formData }
      // 新增且自动生成时，使用前端生成的编码
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
  } catch (errors: any) {
    // 验证失败
  }
}

// 选择
const onChecked = (rowKeys: (string | number)[]) => {
  selectedRowKeys.value = rowKeys as number[]
}

onMounted(() => {
  loadData()
  loadMenuTree()
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

.menu-tree-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 8px;
}

.flex {
  display: flex;
}

.items-center {
  align-items: center;
}

.gap-4 {
  gap: 16px;
}

.w-full {
  width: 100%;
}
</style>