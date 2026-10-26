<template>
  <NCard title="用户管理">
    <div class="toolbar mb-4">
      <NForm :model="queryForm" inline class="search-form">
        <NFormItem label="用户名" path="username">
          <NInput
            v-model:value="queryForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 180px"
          />
        </NFormItem>
        <NFormItem label="手机号" path="phone">
          <NInput
            v-model:value="queryForm.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 180px"
          />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSelect
            v-model:value="queryForm.status"
            :options="[
              { label: '全部', value: undefined },
              { label: '正常', value: 1 },
              { label: '禁用', value: 0 }
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

      <div class="actions">
        <NSpace>
          <NButton type="primary" @click="handleAdd">
            <template #icon><icon-mdi-plus /></template>
            新增用户
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
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 700px">
      <NForm
        ref="formRef"
        :model="formData"
        :rules="computedFormRules"
        label-placement="left"
        label-width="80"
      >
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
          />
        </NFormItem>
        <NFormItem label="角色" path="roleIds">
          <NSelect
            v-model:value="formData.roleIds"
            :options="roleOptions"
            placeholder="请选择角色"
            multiple
            clearable
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
          <NSwitch v-model:value="formData.status" :checked-value="1" :unchecked-value="0" />
        </NFormItem>
        <NFormItem v-if="!isEdit" label="密码" path="password">
          <NInput
            v-model:value="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password-on="click"
          />
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
    <NModal
      v-model:show="showResetPasswordModal"
      preset="dialog"
      title="重置密码"
      style="width: 400px"
    >
      <NForm ref="resetPasswordFormRef" :model="resetPasswordForm" :rules="resetPasswordRules">
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
          <NButton
            type="primary"
            :loading="resetPasswordLoading"
            @click="handleConfirmResetPassword"
            >确定</NButton
          >
        </NSpace>
      </template>
    </NModal>
  </NCard>
</template>

<script setup lang="ts">
import { reactive, ref, h, computed, onMounted } from 'vue'
import {
  useMessage,
  useDialog,
  type DataTableColumns,
  type FormInst,
  type FormRules,
  type TreeSelectOption
} from 'naive-ui'
import {
  getUserPageApi,
  getUserDetailApi,
  createUserApi,
  updateUserApi,
  deleteUserApi,
  resetPasswordApi
} from '@/api/user'
import { getDeptTreeApi } from '@/api/dept'
import { getRoleListApi } from '@/api/role'
import type { UserVO, UserDTO, UserQuery } from '@/api/user'
import type { DeptVO } from '@/api/dept'
import type { RoleVO } from '@/api/role'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tableData = ref<UserVO[]>([])
const selectedRowKeys = ref<number[]>([])
const queryForm = reactive<UserQuery>({
  page: 1,
  size: 10
})

// 部门选择树数据
const deptSelectTreeData = ref<TreeSelectOption[]>([])
// 角色选项
const roleOptions = ref<{ label: string; value: number }[]>([])

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
    queryForm.size = pageSize
    loadData()
  }
})

// 表格列定义
const columns: DataTableColumns<UserVO> = [
  { type: 'selection' },
  { title: 'ID', key: 'id', width: 80 },
  { title: '用户名', key: 'username', width: 120 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '手机号', key: 'phone', width: 130 },
  { title: '邮箱', key: 'email', width: 180, ellipsis: { tooltip: true } },
  {
    title: '性别',
    key: 'sex',
    width: 80,
    render: (row) => ({ 0: '未知', 1: '男', 2: '女' })[row.sex] || '未知'
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) =>
      h(
        'span',
        { style: { color: row.status === 1 ? '#52c41a' : '#ff4d4f' } },
        row.status === 1 ? '正常' : '禁用'
      )
  },
  { title: '部门', key: 'deptName', width: 120 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) =>
      h('div', { style: { display: 'flex', gap: '8px' } }, [
        h(
          'button',
          {
            style: {
              padding: '0 8px',
              background: 'none',
              border: 'none',
              color: '#1890ff',
              cursor: 'pointer'
            },
            onClick: () => handleEdit(row)
          },
          '编辑'
        ),
        h(
          'button',
          {
            style: {
              padding: '0 8px',
              background: 'none',
              border: 'none',
              color: '#faad14',
              cursor: 'pointer'
            },
            onClick: () => handleResetPassword(row)
          },
          '重置密码'
        ),
        h(
          'button',
          {
            style: {
              padding: '0 8px',
              background: 'none',
              border: 'none',
              color: '#ff4d4f',
              cursor: 'pointer'
            },
            onClick: () => handleDelete(row)
          },
          '删除'
        )
      ])
  }
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
  remark: ''
})

// 动态表单验证规则
const computedFormRules = computed<FormRules>(() => ({
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  password: isEdit.value ? {} : { required: true, message: '请输入密码', trigger: 'blur' }
}))

// 重置密码相关
const showResetPasswordModal = ref(false)
const resetPasswordLoading = ref(false)
const resetPasswordFormRef = ref<FormInst | null>(null)
const resetPasswordForm = reactive({
  userId: 0,
  password: '',
  confirmPassword: ''
})
const resetPasswordRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value) => {
        return value === resetPasswordForm.password ? true : new Error('两次密码不一致')
      },
      trigger: 'blur'
    }
  ]
}

// 加载部门树
const loadDeptTree = async () => {
  try {
    const res = await getDeptTreeApi()
    deptSelectTreeData.value = convertDeptTree(res as DeptVO[])
  } catch (error) {
    console.error('加载部门树失败:', error)
  }
}

// 转换部门树数据
const convertDeptTree = (items: DeptVO[]): TreeSelectOption[] => {
  return items.map((item) => ({
    key: item.id,
    label: item.name,
    children: item.children ? convertDeptTree(item.children) : undefined
  }))
}

// 加载角色列表
const loadRoleList = async () => {
  try {
    const res = await getRoleListApi()
    roleOptions.value = (res as RoleVO[]).map((item) => ({ label: item.name, value: item.id }))
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserPageApi(queryForm)
    tableData.value = res.records
    pagination.page = res.current
    pagination.pageSize = res.size
    pagination.itemCount = res.total
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
  queryForm.username = undefined
  queryForm.phone = undefined
  queryForm.status = undefined
  queryForm.deptId = undefined
  handleSearch()
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
    remark: ''
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
      remark: detail.remark
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
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的用户')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个用户吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteUserApi(selectedRowKeys.value)
        message.success('删除成功')
        selectedRowKeys.value = []
        loadData()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    }
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
        const errors = errorMsg
          .split(';')
          .filter(Boolean)
          .map((e: string) => e.trim())
          .join('\n')
        message.error(errors, { duration: 5000 })
      } else {
        message.error(errorMsg)
      }
    } finally {
      submitLoading.value = false
    }
  } catch {
    // 验证失败
  }
}

// 选择
const onChecked = (rowKeys: (string | number)[]) => {
  selectedRowKeys.value = rowKeys as number[]
}

onMounted(() => {
  loadData()
  loadDeptTree()
  loadRoleList()
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
