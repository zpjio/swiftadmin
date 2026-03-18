<template>
  <NCard title="菜单管理" class="h-full">
    <div class="flex gap-4 h-full">
      <!-- 左侧树形结构 -->
      <div class="w-80 flex-shrink-0 border-r border-gray-300 pr-4">
        <div class="mb-4">
          <NInput v-model:value="searchKey" placeholder="搜索菜单名称" clearable>
            <template #prefix>
              <icon-mdi-magnify />
            </template>
          </NInput>
        </div>
        <div class="mb-4">
          <NSpace>
            <NButton v-auth="'system:menu:add'" type="primary" size="small" @click="handleAdd()">
              <template #icon><icon-mdi-plus /></template>
              新增
            </NButton>
            <NButton size="small" @click="toggleExpandAll">
              {{ isAllExpanded ? '收起' : '展开' }}
            </NButton>
          </NSpace>
        </div>
        <NSpin :show="loading">
          <NScrollbar style="max-height: calc(100vh - 280px)">
            <NTree
              :data="treeData"
              :expanded-keys="expandedKeys"
              :selected-keys="selectedKeys"
              :pattern="searchKey"
              :render-label="renderTreeLabel"
              block-line
              expand-on-click
              @update:selected-keys="onSelect"
              @update:expanded-keys="onExpand"
            />
          </NScrollbar>
        </NSpin>
      </div>

      <!-- 右侧详情面板 -->
      <div class="flex-1 min-w-0">
        <div v-if="!selectedMenu" class="flex items-center justify-center h-64 text-gray-400">
          <div class="text-center">
            <icon-mdi-menu class="text-4xl mb-2" />
            <p>请在左侧选择菜单</p>
          </div>
        </div>
        <template v-else>
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-medium">{{ isEditing ? '编辑菜单' : '菜单详情' }}</h3>
            <NSpace>
              <NButton v-if="!isEditing && hasPermission('system:menu:edit')" type="primary" @click="startEdit">
                <template #icon><icon-mdi-pencil /></template>
                编辑
              </NButton>
              <NButton v-if="!isEditing && hasPermission('system:menu:add')" type="primary" @click="handleAdd(selectedMenu.id)">
                <template #icon><icon-mdi-plus /></template>
                新增子菜单
              </NButton>
              <template v-else-if="isEditing">
                <NButton @click="cancelEdit">取消</NButton>
                <NButton type="primary" :loading="submitLoading" @click="handleSubmit">保存</NButton>
              </template>
            </NSpace>
          </div>

          <!-- 详情展示 -->
          <NDescriptions v-if="!isEditing" :column="2" bordered label-placement="left">
            <NDescriptionsItem label="菜单名称">
              <div class="flex items-center gap-2">
                <Icon v-if="selectedMenu.icon" :icon="normalizeIconName(selectedMenu.icon)" />
                {{ selectedMenu.name }}
              </div>
            </NDescriptionsItem>
            <NDescriptionsItem label="菜单类型">
              <NTag v-if="selectedMenu.type" :type="getTypeTagType(selectedMenu.type)" size="small">
                {{ getTypeLabel(selectedMenu.type) }}
              </NTag>
              <span v-else>-</span>
            </NDescriptionsItem>
            <NDescriptionsItem label="路由地址">{{ selectedMenu.path || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="组件路径">{{ selectedMenu.component || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="权限标识">{{ selectedMenu.permission || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="排序">{{ selectedMenu.sortOrder }}</NDescriptionsItem>
            <NDescriptionsItem label="状态">
              <NTag :type="selectedMenu.status === 1 ? 'success' : 'error'" size="small">
                {{ selectedMenu.status === 1 ? '正常' : '禁用' }}
              </NTag>
            </NDescriptionsItem>
            <NDescriptionsItem label="创建时间">{{ selectedMenu.createTime || '-' }}</NDescriptionsItem>
            <NDescriptionsItem label="操作" :span="2">
              <NSpace>
                <NButton v-if="hasPermission('system:menu:add')" type="primary" size="small" @click="handleAdd(selectedMenu.id)">
                  新增子菜单
                </NButton>
                <NPopconfirm v-if="hasPermission('system:menu:delete')" @positive-click="handleDelete">
                  <template #trigger>
                    <NButton type="error" size="small">删除</NButton>
                  </template>
                  确定要删除该菜单吗？如有子菜单将一并删除。
                </NPopconfirm>
              </NSpace>
            </NDescriptionsItem>
          </NDescriptions>

          <!-- 编辑表单 -->
          <MenuForm
            v-if="isEditing"
            ref="editFormRef"
            v-model="formData"
            :menu-options="menuSelectTreeData"
          />
        </template>
      </div>
    </div>

    <!-- 新增弹窗 -->
    <NModal v-model:show="showAddModal" title="新增菜单" preset="card" style="width: 600px;">
      <MenuForm
        ref="addFormRef"
        v-model="formData"
        :menu-options="menuSelectTreeData"
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showAddModal = false">取消</NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { reactive, ref, h, computed, onMounted, nextTick } from 'vue'
import { useMessage, NTag, type FormInst, type TreeSelectOption, type TreeOption } from 'naive-ui'
import { getMenuTreeApi, getMenuSelectTreeApi, getMenuDetailApi, createMenuApi, updateMenuApi, deleteMenuApi } from '@/api/menu'
import type { MenuVO, MenuDTO, MenuTreeVO } from '@/api/menu'
import MenuForm from './MenuForm.vue'
import { Icon } from '@iconify/vue'
import { normalizeIconName } from '@/utils/icon'
import { getParentIds, convertToTreeSelectOptions } from '@/utils/tree'

import { usePermission } from '@/composables'

const message = useMessage()
const { hasPermission } = usePermission()

const loading = ref(false)
const submitLoading = ref(false)
const searchKey = ref('')

// 原始数据
const rawTreeData = ref<MenuVO[]>([])
const menuSelectTreeData = ref<TreeSelectOption[]>([])

// 选中的菜单
const selectedKeys = ref<string[]>([])
const selectedMenu = ref<MenuVO | null>(null)
const isEditing = ref(false)

// 展开
const expandedKeys = ref<string[]>([])
const parentMenuIds = ref<string[]>([])

// 新增弹窗
const showAddModal = ref(false)
const editFormRef = ref<FormInst | null>(null)  // 编辑表单 ref
const addFormRef = ref<FormInst | null>(null)   // 新增表单 ref

// 是否全部展开
const isAllExpanded = computed(() => {
  if (parentMenuIds.value.length === 0) return false
  return parentMenuIds.value.every(id => expandedKeys.value.includes(id))
})

// 转换为 NTree 数据格式（带图标和类型标签）
const convertMenuToTreeOptions = (menus: MenuVO[]): TreeOption[] => {
  return menus.map(menu => ({
    key: menu.id,
    label: menu.name,
    icon: normalizeIconName(menu.icon) || undefined,
    type: menu.type,
    children: menu.children && menu.children.length > 0 ? convertMenuToTreeOptions(menu.children) : undefined
  }))
}

// 树形数据（计算属性）
const treeData = computed<TreeOption[]>(() => {
  return convertMenuToTreeOptions(rawTreeData.value)
})


// 渲染树节点标签
const renderTreeLabel = ({ option }: { option: TreeOption }) => {
  return h('div', { class: 'flex items-center gap-2 w-full py-1' }, [
    option.icon ? h(Icon, { icon: option.icon as string, style: { fontSize: '16px' } }) : null,
    h('span', null, option.label),
    option.type ? h(NTag, { type: getTypeTagType(option.type as number), size: 'small', round: true }, () => getTypeLabel(option.type as number)) : null
  ])
}

// 展开/收起
const toggleExpandAll = () => {
  if (isAllExpanded.value) {
    expandedKeys.value = []
  } else {
    expandedKeys.value = [...parentMenuIds.value]
  }
}

const onExpand = (keys: (string | number)[]) => {
  expandedKeys.value = keys.map(String)
}

// 选择节点
const onSelect = async (keys: (string | number)[]) => {
  selectedKeys.value = keys.map(String)
  if (keys.length > 0) {
    const id = String(keys[0])
    try {
      const detail = await getMenuDetailApi(id)
      selectedMenu.value = detail as MenuVO
      isEditing.value = false
    } catch (error: any) {
      message.error(error.message || '获取菜单详情失败')
    }
  } else {
    selectedMenu.value = null
    isEditing.value = false
  }
}

// 表单数据
const formData = reactive<MenuDTO & { isExternal?: boolean; isCache?: boolean; isVisible?: boolean }>({
  parentId: '0',
  name: '',
  type: 1,
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  permission: '',
  status: 1,
  isExternal: false,
  isCache: false,
  isVisible: true,
})

// 类型相关
const getTypeLabel = (type: number) => {
  const map: Record<number, string> = { 1: '目录', 2: '菜单', 3: '按钮' }
  return map[type] || '-'
}

const getTypeTagType = (type: number): 'default' | 'info' | 'success' | 'warning' | 'error' => {
  const map: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    1: 'info',
    2: 'success',
    3: 'warning'
  }
  return map[type] || 'default'
}

// 新增
const handleAdd = (parentId?: string) => {
  Object.assign(formData, {
    id: undefined,
    parentId: parentId || '0',
    name: '',
    type: parentId ? 2 : 1,
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    permission: '',
    status: 1,
    isExternal: false,
    isCache: false,
    isVisible: true,
  })
  showAddModal.value = true
}

// 编辑
const startEdit = () => {
  if (!selectedMenu.value) return

  // 先重置表单数据
  Object.assign(formData, {
    id: selectedMenu.value.id,
    parentId: selectedMenu.value.parentId || '0',
    name: selectedMenu.value.name || '',
    type: selectedMenu.value.type ?? 1,
    path: selectedMenu.value.path || '',
    component: selectedMenu.value.component || '',
    icon: selectedMenu.value.icon || '',
    sortOrder: selectedMenu.value.sortOrder ?? 0,
    permission: selectedMenu.value.permission || '',
    status: selectedMenu.value.status ?? 1,
    isExternal: false,
    isCache: false,
    isVisible: true,
  })

  isEditing.value = true

  // 重置表单验证状态
  nextTick(() => {
    editFormRef.value?.restoreValidation()
  })
}

const cancelEdit = () => {
  isEditing.value = false
}

// 删除
const handleDelete = async () => {
  if (!selectedMenu.value) return
  try {
    await deleteMenuApi(selectedMenu.value.id)
    message.success('删除成功')
    selectedMenu.value = null
    selectedKeys.value = []
    loadMenuTree()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 提交
const handleSubmit = async () => {
  // 根据是编辑还是新增，使用不同的 formRef
  const currentFormRef = formData.id ? editFormRef.value : addFormRef.value

  try {
    await currentFormRef?.validate()
    submitLoading.value = true
    try {
      if (formData.id) {
        await updateMenuApi(formData)
        message.success('更新成功')
        isEditing.value = false
        // 重新获取详情
        const detail = await getMenuDetailApi(formData.id)
        selectedMenu.value = detail as MenuVO
      } else {
        await createMenuApi(formData)
        message.success('创建成功')
        showAddModal.value = false
      }
      loadMenuTree()
    } catch (error: any) {
      message.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  } catch (errors: any) {
    // 验证失败
  }
}

// 加载菜单树
const loadMenuTree = async () => {
  loading.value = true
  try {
    const [treeRes, selectTreeRes] = await Promise.all([getMenuTreeApi(), getMenuSelectTreeApi()])
    const treeData = treeRes as MenuVO[]
    const selectTreeData = selectTreeRes as MenuTreeVO[]
    rawTreeData.value = treeData
    menuSelectTreeData.value = convertToTreeSelectOptions(selectTreeData, {
      labelField: 'label',
      rootLabel: '顶级菜单',
      rootKey: '0',
    })
    parentMenuIds.value = getParentIds(treeData)
    expandedKeys.value = [...parentMenuIds.value]
  } catch (error) {
    console.error('加载菜单树失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadMenuTree()
})