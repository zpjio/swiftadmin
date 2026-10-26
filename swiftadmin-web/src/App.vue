<template>
  <NConfigProvider
    :theme="theme"
    :theme-overrides="themeOverrides"
    :locale="zhCN"
    :date-locale="dateZhCN"
  >
    <NMessageProvider>
      <NNotificationProvider>
        <NDialogProvider>
          <RouterView />
        </NDialogProvider>
      </NNotificationProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>

<script setup lang="ts">
import { zhCN, dateZhCN } from 'naive-ui'
import { theme, themeOverrides } from '@/utils/theme'
</script>
