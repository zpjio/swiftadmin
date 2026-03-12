<template>
  <div class="profile-container">
    <NCard title="个人设置" :bordered="false" class="w-full max-w-2xl">
      <NTabs type="line" animated>
        <NTabPane name="basic" tab="基本信息" display-directive="show">
          <NForm :model="basicForm" :rules="basicRules" label-placement="left" label-width="100px">
            <NFormItem label="用户名" path="username">
              <NInput v-model:value="basicForm.username" disabled placeholder="用户名不可修改" />
            </NFormItem>
            
            <NFormItem label="昵称" path="nickname">
              <NInput v-model:value="basicForm.nickname" placeholder="请输入昵称" />
            </NFormItem>
            
            <NFormItem label="头像" path="avatar">
              <div class="flex items-center gap-4">
                <NAvatar :size="64" :src="basicForm.avatar || undefined" />
                <NButton @click="handleUpdateAvatar">更换头像</NButton>
              </div>
            </NFormItem>
            
            <NFormItem label="性别" path="sex">
              <NRadioGroup v-model:value="basicForm.sex">
                <NRadio :value="0">未知</NRadio>
                <NRadio :value="1">男</NRadio>
                <NRadio :value="2">女</NRadio>
              </NRadioGroup>
            </NFormItem>
            
            <NFormItem label="手机号" path="phone">
              <NInput v-model:value="basicForm.phone" placeholder="请输入手机号" />
            </NFormItem>
            
            <NFormItem label="邮箱" path="email">
              <NInput v-model:value="basicForm.email" placeholder="请输入邮箱" />
            </NFormItem>
            
            <NFormItem label="部门" path="deptName">
              <NInput v-model:value="basicForm.deptName" disabled placeholder="部门不可修改" />
            </NFormItem>
            
            <NFormItem label="角色" path="roleNames">
              <NDynamicTags v-model:value="basicForm.roleNames" :max="5" disabled />
            </NFormItem>
            
            <NFormItem>
              <NButton type="primary" @click="handleSaveBasic" :loading="saving">保存修改</NButton>
            </NFormItem>
          </NForm>
        </NTabPane>
        
        <NTabPane name="security" tab="安全设置" display-directive="show">
          <NForm :model="securityForm" :rules="securityRules" label-placement="left" label-width="100px">
            <NFormItem label="旧密码" path="oldPassword">
              <NInput 
                v-model:value="securityForm.oldPassword" 
                type="password" 
                placeholder="请输入旧密码" 
                show-password-on="click"
              />
            </NFormItem>
            
            <NFormItem label="新密码" path="newPassword">
              <NInput 
                v-model:value="securityForm.newPassword" 
                type="password" 
                placeholder="请输入新密码（至少 6 位）" 
                show-password-on="click"
              />
            </NFormItem>
            
            <NFormItem label="确认密码" path="confirmPassword">
              <NInput 
                v-model:value="securityForm.confirmPassword" 
                type="password" 
                placeholder="请再次输入新密码" 
                show-password-on="click"
              />
            </NFormItem>
            
            <NFormItem>
              <NButton type="primary" @click="handleUpdatePassword" :loading="updatingPassword">修改密码</NButton>
            </NFormItem>
          </NForm>
        </NTabPane>
      </NTabs>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useMessage } from 'naive-ui'
import { useUserStore } from '@/store'
import { getCurrentUserInfoApi, updateCurrentUserInfoApi } from '@/api/user'
import { changePasswordApi } from '@/api/auth'
import type { FormRules } from 'naive-ui'

const message = useMessage()
const userStore = useUserStore()

const saving = ref(false)
const updatingPassword = ref(false)

// 上传配置
const uploadUrl = computed(() => `${import.meta.env.VITE_API_BASE_URL}/system/file/upload`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
}))

// 基本信息
const basicForm = reactive({
  id: 0,
  username: '',
  nickname: '',
  avatar: '',
  sex: 0,
  phone: '',
  email: '',
  deptName: '',
  roleNames: [] as string[],
})

// 安全设置
const securityForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const basicRules: FormRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '请输入正确的邮箱', trigger: 'blur' }],
}

const securityRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请再次输入密码', trigger: 'blur' }],
}

// 加载用户信息
onMounted(async () => {
  try {
    // 从 API 获取最新的用户信息
    const userInfo = await getCurrentUserInfoApi()

    basicForm.id = userInfo.id
    basicForm.username = userInfo.username || ''
    basicForm.nickname = userInfo.nickname || ''
    basicForm.avatar = userInfo.avatar || ''
    basicForm.sex = userInfo.sex ?? 0
    basicForm.phone = userInfo.phone || ''
    basicForm.email = userInfo.email || ''
    basicForm.deptName = userInfo.deptName || ''
    basicForm.roleNames = userInfo.roleNames || []

    // 更新 store 中的用户信息
    userStore.setUserInfo({
      id: userInfo.id,
      username: userInfo.username || '',
      nickname: userInfo.nickname || '',
      avatar: userInfo.avatar || '',
      roles: userInfo.roleNames || [],
      permissions: userStore.permissions,
      sex: userInfo.sex,
      phone: userInfo.phone,
      email: userInfo.email,
      deptName: userInfo.deptName,
    })
  } catch (error: any) {
    message.error(error.message || '获取用户信息失败')
  }
})

// 保存基本信息
const handleSaveBasic = async () => {
  saving.value = true
  try {
    await updateCurrentUserInfoApi({
      id: basicForm.id,
      nickname: basicForm.nickname,
      avatar: basicForm.avatar,
      sex: basicForm.sex,
      phone: basicForm.phone,
      email: basicForm.email,
    })
    
    // 更新本地用户信息
    const userInfo = userStore.userInfo
    if (userInfo) {
      userStore.setUserInfo({
        ...userInfo,
        nickname: basicForm.nickname,
        avatar: basicForm.avatar,
        phone: basicForm.phone,
        email: basicForm.email,
        sex: basicForm.sex,
      })
    }
    
    message.success('保存成功')
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 更换头像
const handleUpdateAvatar = (_e: MouseEvent) => {
  // 点击按钮时触发文件选择
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  input.onchange = async (event) => {
    const target = event.target as HTMLInputElement
    const file = target.files?.[0]
    if (!file) return

    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      message.error('请选择图片文件')
      return
    }

    // 验证文件大小（最大 2MB）
    if (file.size > 2 * 1024 * 1024) {
      message.error('图片大小不能超过 2MB')
      return
    }

    // 上传文件
    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await fetch(uploadUrl.value, {
        method: 'POST',
        headers: uploadHeaders.value,
        body: formData,
      })

      const result = await response.json()
      if (result.code === 0 && result.data) {
        // 获取 OSS 访问地址
        let avatarUrl = result.data.accessUrl || result.data.fileUrl || ''
        // 确保 URL 有协议头
        if (avatarUrl && !avatarUrl.startsWith('http://') && !avatarUrl.startsWith('https://')) {
          avatarUrl = 'https://' + avatarUrl
        }
        basicForm.avatar = avatarUrl
        message.success('头像上传成功')
      } else {
        message.error(result.message || '上传失败')
      }
    } catch (error) {
      message.error('上传失败，请重试')
    }
  }
  input.click()
}

// 修改密码
const handleUpdatePassword = async () => {
  // 验证确认密码
  if (securityForm.newPassword !== securityForm.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  
  updatingPassword.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) {
      message.error('未获取到用户信息，请重新登录')
      return
    }
    
    await changePasswordApi({
      userId,
      oldPassword: securityForm.oldPassword,
      newPassword: securityForm.newPassword,
    })
    
    // 清空表单
    securityForm.oldPassword = ''
    securityForm.newPassword = ''
    securityForm.confirmPassword = ''
    
    message.success('密码修改成功，请重新登录')
    
    // 退出登录
    setTimeout(() => {
      userStore.logout()
      window.location.href = '/login'
    }, 1500)
  } catch (error: any) {
    message.error(error.message || '修改密码失败')
  } finally {
    updatingPassword.value = false
  }
}
</script>

<style scoped>
.profile-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 20px;
}
</style>
