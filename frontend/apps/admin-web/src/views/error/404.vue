<template>
  <div class="w-full h-full flex items-center justify-center bg-gray-100">
    <div class="text-center">
      <h1 class="text-[120px] font-bold text-blue-500 mb-4">404</h1>
      <p class="text-xl text-gray-500 mb-8">抱歉，您访问的页面不存在</p>
      <NButton type="primary" @click="goHome">返回首页</NButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>
