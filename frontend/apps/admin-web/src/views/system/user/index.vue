<template>
  <NCard title="用户管理">
    <div class="flex justify-between items-start flex-wrap gap-4 mb-4">
      <NForm :model="queryForm" inline class="flex flex-wrap gap-4">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="queryForm.username" placeholder="请输入用户名" clearable style="width: 180px" />
        </NFormItem>
        <NFormItem label="手机号" path="phone">
          <NInput v-model:value="queryForm.phone" placeholder="请输入手机号" clearable style="width: 180px" />
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
        <NFormItem label="部门" path="deptId">
          <NTreeSelect
            v-model:value="queryForm.deptId"
            :options="deptSelectTreeData"
            placeholder="请选择部门"
            clearable
            style="width: 180px"
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
          <NButton v-auth="'system:user:add'" type="primary" @click="handleAdd">
            <template #icon><icon-mdi-plus /></template>
            新增用户
          </NButton>
          <NButton v-auth="'system:user:delete'" type="error" :disabled="!hasSelection" @click="handleBatchDelete">
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
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 700px;">
      <NAlert v-if="isSuperAdminEditingSelf" type="info" style="margin-bottom: 16px;">
        您正在编辑自己的基本信息，用户名、角色、状态不可修改
      </NAlert>
      <NForm :model="formData" :rules="computedFormRules" ref="formRef" label-placement="left" label-width="80">
        <NFormItem label="用户名" path="username">
          <NInput v-model:value="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </NFormItem>
        <NFormItem label="昵称" path="nickname">
          <NInput v-model:value="formData.nickname" placeholder="请输入昵称" />
        </NFormItem>
        <NFormItem label="部门" path="deptId">
          <NTreeSelect
            v-model:value="formData.deptId"
            :options="deptSelectTreeData"
            placeholder="请选择部门"
            clearable
            default-expand-all
            :disabled="isSuperAdminEditingSelf"
          />
        </NFormItem>
        <NFormItem label="角色" path="roleIds">
          <NSelect
            v-model:value="formData.roleIds"
            :options="roleOptions"
            placeholder="请选择角色"
            multiple
            clearable
            :disabled="isSuperAdmin"
          />
        </NFormItem>
        <NFormItem label="手机号" path="phone">
          <NInput v-model:value="formData.phone" placeholder="请输入手机号" />
        </NFormItem>
        <NFormItem label="邮箱" path="email">
          <NInput v-model:value="formData.email" placeholder="请输入邮箱" />
        </NFormItem>
        <NFormItem label="性别" path="sex">
          <NRadioGroup v-model:value="formData.sex">
            <NRadio :value="0">未知</NRadio>
            <NRadio :value="1">男</NRadio>
            <NRadio :value="2">女</NRadio>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSwitch v-model:value="formData.status" :checked-value="1" :unchecked-value="0" :disabled="isSuperAdmin" />
          <NText v-if="isSuperAdmin" depth="3" style="margin-left: 8px;">超级管理员不可禁用</NText>
        </NFormItem>
        <NFormItem v-if="!isEdit" label="密码" path="password">
          <NInput v-model:value="formData.password" type="password" placeholder="请输入密码" show-password-on="click" />
        </NFormItem>
        <NFormItem label="备注" path="remark">
          <NInput v-model:value="formData.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 重置密码弹窗 -->
    <NModal v-model:show="showResetPasswordModal" preset="dialog" title="重置密码" style="width: 400px;">
      <NForm :model="resetPasswordForm" :rules="resetPasswordRules" ref="resetPasswordFormRef">
        <NFormItem label="新密码" path="password">
          <NInput
            v-model:value="resetPasswordForm.password"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password-on="click"
          />
        </NFormItem>
        <NFormItem label="确认密码" path="confirmPassword">
          <NInput
            v-model:value="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password-on="click"
          />
        </NFormItem>
      </NForm>
      <template #action>
        <NSpace justify="end">
          <NButton @click="showResetPasswordModal = false">取消</NButton>
          <NButton type="primary" :loading="resetPasswordLoading" @click="handleConfirmResetPassword">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { reactive, ref, h, computed, onMounted } from 'vue'
import { useMessage, useDialog, type DataTableColumns, type FormInst, type FormRules, type TreeSelectOption } from 'naive-ui'
import { getUserPageApi, getUserDetailApi, createUserApi, updateUserApi, deleteUserApi, resetPasswordApi } from '@/api/user'
import { getDeptTreeApi } from '@/api/dept'
import { getRoleListApi } from '@/api/role'
import type { UserVO, UserDTO, UserQuery } from '@/api/user'
import type { DeptVO } from '@/api/dept'
import type { RoleVO } from '@/api/role'

import { usePermission, usePagination, useTableSelection } from '@/composables'
import { useUserStore } from '@/store'
import { formatDateTime } from '@/utils/date'
import { convertToTreeSelectOptions } from '@/utils/tree'

const message = useMessage()
const dialog = useDialog()
const { hasPermission } = usePermission()
const userStore = useUserStore()

// 当前登录用户ID
const currentUserId = computed(() => userStore.userInfo?.id)

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const { selectedKeys, hasSelection, clearSelection, onSelectionChange } = useTableSelection<UserVO>()
const queryForm = reactive<UserQuery>({})

// 部门选择树数据
const deptSelectTreeData = ref<TreeSelectOption[]>([])
// 角色选项
const roleOptions = ref<{ label: string; value: string }[]>([])

// 分页
const { paginationProps, setTotal, getIndex } = usePagination({
  onChange: () => loadData(),
  onPageSizeChange: () => loadData(),
})

// 表格列定义
const columns: DataTableColumns<UserVO> = [
  { type: 'selection' },
  {
    title: '序号',
    key: 'index',
    width: 60,
    render: (_row, index) => getIndex(index),
  },
  {
    title: '用户名',
    key: 'username',
    width: 140,
    render: (row) => {
      if (row.username === 'admin') {
        return h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
          h('span', row.username),
          h('span', { style: { background: '#ff9800', color: '#fff', padding: '2px 6px', borderRadius: '4px', fontSize: '12px' } }, '超管')
        ])
      }
      return row.username
    }
  },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '手机号', key: 'phone', width: 130 },
  { title: '邮箱', key: 'email', width: 180, ellipsis: { tooltip: true } },
  {
    title: '性别',
    key: 'sex',
    width: 80,
    render: (row) => ({ 0: '未知', 1: '男', 2: '女' }[row.sex] || '未知'),
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => h('span', { style: { color: row.status === 1 ? '#52c41a' : '#ff4d4f' } }, row.status === 1 ? '正常' : '禁用'),
  },
  { title: '部门', key: 'deptName', width: 120 },
  { title: '创建时间', key: 'createTime', width: 180, render: (row) => formatDateTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      const isSuperAdmin = row.username === 'admin'
      const isSelf = row.id === currentUserId.value
      const buttons: any[] = []
      // 超级管理员可以编辑自己（基本信息的），其他用户不能编辑超级管理员
      if (hasPermission('system:user:edit') && (!isSuperAdmin || isSelf)) {
        buttons.push(h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#1890ff', cursor: 'pointer' }, onClick: () => handleEdit(row) }, '编辑'))
      }
      // 重置密码：超级管理员不能被重置（包括自己），其他人正常
      if (hasPermission('system:user:resetPwd') && !isSuperAdmin) {
        buttons.push(h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#faad14', cursor: 'pointer' }, onClick: () => handleResetPassword(row) }, '重置密码'))
      }
      // 删除：超级管理员不能被删除
      if (hasPermission('system:user:delete') && !isSuperAdmin) {
        buttons.push(h('button', { style: { padding: '0 8px', background: 'none', border: 'none', color: '#ff4d4f', cursor: 'pointer' }, onClick: () => handleDelete(row) }, '删除'))
      }
      return h('div', { style: { display: 'flex', gap: '8px' } }, buttons)
    },
  },
]

// 弹窗相关
const showModal = ref(false)
const modalTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInst | null>(null)

const formData = reactive<UserDTO>({
  username: '',
  nickname: '',
  password: '',
  phone: '',
  email: '',
  sex: 0,
  status: 1,
  deptId: undefined,
  roleIds: [],
  remark: '',
})

// 判断是否为超级管理员
const isSuperAdmin = computed(() => formData.username === 'admin')

// 判断是否是超级管理员编辑自己
const isSuperAdminEditingSelf = computed(() => {
  return isSuperAdmin.value && formData.id === currentUserId.value
})

// 动态表单验证规则
const computedFormRules = computed<FormRules>(() => ({
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  password: isEdit.value ? {} : { required: true, message: '请输入密码', trigger: 'blur' },
}))

// 重置密码相关
const showResetPasswordModal = ref(false)
const resetPasswordLoading = ref(false)
const resetPasswordFormRef = ref<FormInst | null>(null)
const resetPasswordForm = reactive({
  userId: '' as string,
  password: '',
  confirmPassword: '',
})
const resetPasswordRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value) => {
        return value === resetPasswordForm.password ? true : new Error('两次密码不一致')
      },
      trigger: 'blur',
    },
  ],
}

// 加载部门树
const loadDeptTree = async () => {
  try {
    // 使用静默模式，权限不足时不显示错误提示
    const res = await getDeptTreeApi(undefined, true)
    deptSelectTreeData.value = convertToTreeSelectOptions(res as DeptVO[])
  } catch (error) {
    // 权限不足时静默处理
    console.debug('加载部门树失败，可能缺少权限')
  }
}

// 加载角色列表
const loadRoleList = async () => {
  try {
    // 使用静默模式，权限不足时不显示错误提示
    const res = await getRoleListApi(true)
    roleOptions.value = (res as RoleVO[]).map((item) => ({ label: item.name, value: item.id }))
  } catch (error) {
    // 权限不足时静默处理
    console.debug('加载角色列表失败，可能缺少权限')
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserPageApi(queryForm)
    tableData.value = res.records
    setTotal(res.totalRow)
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  loadData()
}

// 重置
const handleReset = () => {
  queryForm.username = undefined
  queryForm.phone = undefined
  queryForm.status = undefined
  queryForm.deptId = undefined
  loadData()
}

// 新增
const handleAdd = () => {
  modalTitle.value = '新增用户'
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    username: '',
    nickname: '',
    password: '',
    phone: '',
    email: '',
    sex: 0,
    status: 1,
    deptId: undefined,
    roleIds: [],
    remark: '',
  })
  showModal.value = true
}

// 编辑
const handleEdit = async (row: UserVO) => {
  modalTitle.value = '编辑用户'
  isEdit.value = true
  try {
    const detail = await getUserDetailApi(row.id)
    Object.assign(formData, {
      id: detail.id,
      username: detail.username,
      nickname: detail.nickname,
      phone: detail.phone,
      email: detail.email,
      sex: detail.sex,
      status: detail.status,
      deptId: detail.deptId,
      roleIds: detail.roleIds || [],
      remark: detail.remark,
    })
    showModal.value = true
  } catch (error: any) {
    message.error(error.message || '获取用户详情失败')
  }
}

// 重置密码
const handleResetPassword = (row: UserVO) => {
  resetPasswordForm.userId = row.id
  resetPasswordForm.password = ''
  resetPasswordForm.confirmPassword = ''
  showResetPasswordModal.value = true
}

// 确认重置密码
const handleConfirmResetPassword = async () => {
  try {
    await resetPasswordFormRef.value?.validate()
    resetPasswordLoading.value = true
    await resetPasswordApi(resetPasswordForm.userId, resetPasswordForm.password)
    message.success('密码重置成功')
    showResetPasswordModal.value = false
  } catch (error: any) {
    if (error?.message) message.error(error.message)
  } finally {
    resetPasswordLoading.value = false
  }
}

// 删除
const handleDelete = (row: UserVO) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除用户 "${row.username}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteUserApi([row.id])
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
  if (!hasSelection.value) {
    message.warning('请选择要删除的用户')
    return
  }
  // 过滤掉超级管理员用户
  const adminUsers = tableData.value.filter(u => selectedKeys.value.includes(u.id) && u.username === 'admin')
  if (adminUsers.length > 0) {
    message.warning('超级管理员用户不可删除')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedKeys.value.length} 个用户吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteUserApi(selectedKeys.value as string[])
        message.success('删除成功')
        clearSelection()
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
      if (isEdit.value) {
        await updateUserApi(formData)
        message.success('更新成功')
      } else {
        await createUserApi(formData)
        message.success('创建成功')
      }
      showModal.value = false
      loadData()
    } catch (error: any) {
      const errorMsg = error.message || error.response?.data?.message || '操作失败'
      // 将分号分隔的错误信息转换为换行显示
      if (errorMsg.includes(';')) {
        const errors = errorMsg.split(';').filter(Boolean).map((e: string) => e.trim()).join('\n')
        message.error(errors, { duration: 5000 })
      } else {
        message.error(errorMsg)
      }
    } finally {
      submitLoading.value = false
    }
  } catch (errors: any) {
    // 验证失败
  }
}

onMounted(() => {
  loadData()
  loadDeptTree()
  loadRoleList()
})
