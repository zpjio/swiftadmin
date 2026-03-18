<template>
  <div class="w-full h-full flex items-center justify-center bg-gradient-to-br from-[#667eea] to-[#764ba2]">
    <div class="w-[450px] p-10 bg-white rounded-2xl shadow-[0_20px_60px_rgba(0,0,0,0.3)]">
      <h1 class="text-center text-3xl font-bold text-blue-500 mb-2">修改密码</h1>
      <p class="text-center text-gray-400 text-sm mb-8">为了您的账号安全，首次登录需要修改密码</p>

      <NForm :model="form" :rules="rules" class="flex flex-col gap-5" ref="formRef">
        <NFormItem label="新密码" path="newPassword">
          <NInput
            v-model:value="form.newPassword"
            type="password"
            placeholder="请输入新密码（至少 6 位）"
            size="large"
            show-password-on="click"
          />
        </NFormItem>

        <NFormItem label="确认密码" path="confirmPassword">
          <NInput
            v-model:value="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            show-password-on="click"
          />
        </NFormItem>

        <NAlert type="info" title="提示">
          管理员已重置您的密码，请直接设置新密码，无需输入旧密码。
        </NAlert>

        <NButton type="primary" size="large" block :loading="loading" @click="handleSubmit">
          确认修改
        </NButton>
      </NForm>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { forceChangePasswordApi } from '@/api/auth'
import type { FormInst, FormRules } from 'naive-ui'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const formRef = ref<FormInst | null>(null)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
  ],
}

const handleSubmit = async () => {
  // 先手动验证确认密码
  if (!form.confirmPassword) {
    message.error('请再次输入密码')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  
  // 表单验证
  await formRef.value?.validate(async (errors) => {
    if (errors) {
      return
    }
    
    loading.value = true
    try {
      // 获取临时 Token
      const tempToken = localStorage.getItem('tempToken')
      if (!tempToken) {
        message.error('未找到临时 Token，请重新登录')
        router.push('/login')
        return
      }
      
      const res = await forceChangePasswordApi({
        tempToken,
        newPassword: form.newPassword,
      })
      
      // 保存新的 Token
      if (res && res.accessToken) {
        localStorage.setItem('token', res.accessToken)
        if (res.refreshToken) {
          localStorage.setItem('refreshToken', res.refreshToken)
        }
      }
      localStorage.removeItem('tempToken')
      
      message.success('密码修改成功')
      
      // 跳转到首页
      setTimeout(() => {
        router.push('/dashboard')
      }, 1500)
    } catch (error: any) {
      console.error('修改密码失败:', error)
      message.error(error.message || '修改密码失败')
    } finally {
      loading.value = false
    }
  })
}
</script>
