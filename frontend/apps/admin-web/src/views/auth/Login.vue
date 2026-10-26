<template>
  <div
    class="w-full h-full flex items-center justify-center bg-gradient-to-br from-[#667eea] to-[#764ba2]"
  >
    <div class="w-[400px] p-10 bg-white rounded-2xl shadow-[0_20px_60px_rgba(0,0,0,0.3)]">
      <h1 class="text-center text-3xl font-bold text-blue-500 mb-2">SwiftAdmin</h1>
      <p class="text-center text-gray-400 mb-8">高效、简洁、美观的后台管理系统</p>

      <NForm :model="form" :rules="rules" class="flex flex-col gap-5">
        <NFormItem path="username">
          <NInput v-model:value="form.username" placeholder="请输入用户名" size="large" clearable>
            <template #prefix>
              <icon-mdi-account />
            </template>
          </NInput>
        </NFormItem>

        <NFormItem path="password">
          <NInput
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password-on="click"
          >
            <template #prefix>
              <icon-mdi-lock />
            </template>
          </NInput>
        </NFormItem>

        <NFormItem v-if="captchaEnabled" path="captchaCode">
          <div class="flex gap-3">
            <NInput v-model:value="form.captchaCode" placeholder="请输入验证码" size="large" />
            <div
              class="w-[120px] h-10 border border-gray-300 rounded cursor-pointer flex items-center justify-center bg-gray-100"
              @click="refreshCaptcha"
            >
              <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" class="w-full h-full rounded" />
              <span v-else>点击获取</span>
            </div>
          </div>
        </NFormItem>

        <NFormItem>
          <NCheckbox v-model:checked="form.remember">记住密码</NCheckbox>
        </NFormItem>

        <NButton type="primary" size="large" block :loading="loading" @click="handleLogin">
          登录
        </NButton>
      </NForm>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useUserStore } from '@/store'
import { loginApi, type LoginResult } from '@/api/auth'
import { BusinessError } from '@/types'
import type { FormRules } from 'naive-ui'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const loading = ref(false)
const captchaEnabled = ref(false)
const captchaUrl = ref('')
const captchaId = ref('')

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
  remember: false
})

const rules: FormRules = {
  username: {
    required: true,
    message: '请输入用户名',
    trigger: 'blur'
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur'
  }
}

// 获取验证码（预留功能，默认关闭）
const getCaptcha = async () => {
  try {
    // 验证码功能暂未启用，后续可通过 /api/auth/captcha 接口实现
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 刷新验证码
const refreshCaptcha = () => {
  getCaptcha()
}

const handleLogin = async () => {
  loading.value = true
  try {
    const res = await loginApi({
      type: 'password',
      username: form.username,
      password: form.password,
      captchaId: captchaEnabled.value ? captchaId.value : undefined,
      captchaCode: captchaEnabled.value ? form.captchaCode : undefined
    })

    // 检查是否需要修改密码
    const loginRes = res as LoginResult
    if (loginRes.passwordExpired) {
      // 保存临时 Token
      if (loginRes.tempToken) {
        localStorage.setItem('tempToken', loginRes.tempToken)
      }
      message.warning('密码已过期，需要修改密码')
      // 跳转到修改密码页面
      router.push('/change-password')
      return
    }

    // 后端返回 accessToken 或 tempToken
    const token = loginRes.accessToken || loginRes.tempToken
    if (token) {
      // 保存到 store 和 localStorage
      userStore.setToken(token, loginRes.refreshToken)

      // 获取并保存用户信息
      if (loginRes.userInfo) {
        userStore.setUserInfo({
          id: loginRes.userInfo.id,
          username: loginRes.userInfo.username || '',
          nickname: loginRes.userInfo.nickname || '',
          avatar: loginRes.userInfo.avatar || '',
          roles: loginRes.userInfo.roles || [],
          permissions: loginRes.userInfo.permissions || []
        })
      }

      message.success('登录成功')
      router.push('/dashboard')
    } else {
      message.error('登录失败：未获取到 Token')
    }
  } catch (error) {
    console.error('登录失败:', error)
    // 如果是需要验证码的错误码，刷新验证码
    if (error instanceof BusinessError && error.code === 402) {
      captchaEnabled.value = true
      refreshCaptcha()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 检查是否需要验证码
  // getCaptcha()
})
</script>
