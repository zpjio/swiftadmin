<template>
  <div class="login-container">
    <div class="login-box">
      <h1 class="title">SwiftAdmin</h1>
      <p class="subtitle">高效、简洁、美观的后台管理系统</p>
      
      <NForm :model="form" :rules="rules" class="login-form">
        <NFormItem path="username">
          <NInput
            v-model:value="form.username"
            placeholder="请输入用户名"
            size="large"
            clearable
          >
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
          <div class="captcha-input">
            <NInput
              v-model:value="form.captchaCode"
              placeholder="请输入验证码"
              size="large"
            />
            <div class="captcha-image" @click="refreshCaptcha">
              <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" />
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
  remember: false,
})

const rules: FormRules = {
  username: {
    required: true,
    message: '请输入用户名',
    trigger: 'blur',
  },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur',
  },
}

// 获取验证码
const getCaptcha = async () => {
  try {
    // TODO: 调用后端验证码接口
    // const res = await getCaptchaApi()
    // captchaId.value = res.data.captchaId
    // captchaUrl.value = res.data.captchaImage
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
      captchaCode: captchaEnabled.value ? form.captchaCode : undefined,
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
          permissions: loginRes.userInfo.permissions || [],
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

<style scoped>
.login-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.title {
  text-align: center;
  font-size: 32px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 32px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.captcha-input {
  display: flex;
  gap: 12px;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  border-radius: 4px;
}
</style>
