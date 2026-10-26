<template>
  <NPopover trigger="click" :width="400" placement="bottom-start">
    <template #trigger>
      <div class="icon-select-trigger">
        <div class="icon-preview">
          <Icon v-if="modelValue" :icon="modelValue" class="text-xl" />
          <span v-else class="placeholder">请选择图标</span>
        </div>
        <icon-mdi-chevron-down class="arrow" />
      </div>
    </template>
    <div class="icon-select-content">
      <NInput
        v-model:value="searchKey"
        placeholder="搜索图标..."
        clearable
        size="small"
        class="mb-3"
      >
        <template #prefix>
          <icon-mdi-magnify />
        </template>
      </NInput>
      <NScrollbar style="max-height: 300px">
        <div class="icon-grid">
          <div
            v-for="icon in filteredIcons"
            :key="icon"
            class="icon-item"
            :class="{ active: modelValue === icon }"
            @click="handleSelect(icon)"
          >
            <Icon :icon="icon" class="text-2xl" />
            <span class="icon-name">{{ getIconLabel(icon) }}</span>
          </div>
        </div>
      </NScrollbar>
    </div>
  </NPopover>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon } from '@iconify/vue'

defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const searchKey = ref('')

// 获取图标显示名称
const getIconLabel = (icon: string) => {
  return icon.replace('mdi:', '')
}

// 常用图标列表（存储格式：mdi:xxx，与 @iconify 格式一致）
const commonIcons = [
  // 系统管理
  'mdi:account',
  'mdi:account-group',
  'mdi:account-cog',
  'mdi:shield-account',
  'mdi:cog',
  'mdi:cogs',
  'mdi:menu',
  'mdi:file-tree',
  'mdi:folder',
  'mdi:database',
  'mdi:cloud-upload',
  'mdi:cloud-download',
  // 监控
  'mdi:monitor',
  'mdi:chart-line',
  'mdi:chart-bar',
  'mdi:pulse',
  'mdi:history',
  'mdi:login',
  'mdi:logout',
  // 操作
  'mdi:plus',
  'mdi:pencil',
  'mdi:delete',
  'mdi:magnify',
  'mdi:refresh',
  'mdi:check',
  'mdi:close',
  'mdi:download',
  'mdi:upload',
  // 状态
  'mdi:check-circle',
  'mdi:close-circle',
  'mdi:alert',
  'mdi:information',
  'mdi:warning',
  'mdi:error',
  // 文件
  'mdi:file',
  'mdi:file-document',
  'mdi:file-pdf-box',
  'mdi:file-excel',
  'mdi:file-word',
  'mdi:file-image',
  'mdi:file-video',
  'mdi:file-music',
  // 导航
  'mdi:home',
  'mdi:dashboard',
  'mdi:view-dashboard',
  'mdi:apps',
  'mdi:view-list',
  'mdi:view-grid',
  'mdi:table',
  // 用户相关
  'mdi:user',
  'mdi:user-circle',
  'mdi:account-circle',
  'mdi:face',
  'mdi:lock',
  'mdi:key',
  'mdi:key-variant',
  // 设置
  'mdi:wrench',
  'mdi:tools',
  'mdi:toggle-switch',
  'mdi:tune',
  // 其他常用
  'mdi:star',
  'mdi:heart',
  'mdi:bell',
  'mdi:email',
  'mdi:phone',
  'mdi:calendar',
  'mdi:clock',
  'mdi:map-marker',
  'mdi:link',
  'mdi:code-tags',
  'mdi:palette',
  'mdi:brush',
  'mdi:image',
  'mdi:camera',
  'mdi:video',
  'mdi:music',
  'mdi:microphone',
  'mdi:gift',
  'mdi:shopping',
  'mdi:cart',
  'mdi:credit-card',
  'mdi:weather-sunny',
  'mdi:weather-cloudy',
  'mdi:weather-rainy',
  'mdi:car',
  'mdi:bike',
  'mdi:walk',
  'mdi:airplane',
  'mdi:rocket',
  'mdi:leaf',
  'mdi:fire',
  'mdi:water',
  'mdi:bug',
  'mdi:robot',
  'mdi:android',
  'mdi:apple'
]

const filteredIcons = computed(() => {
  if (!searchKey.value) return commonIcons
  const key = searchKey.value.toLowerCase()
  return commonIcons.filter((icon) => icon.toLowerCase().includes(key))
})

const handleSelect = (icon: string) => {
  emit('update:modelValue', icon)
}
</script>

<style scoped>
.icon-select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
  min-width: 180px;
  background: #fff;
}

.icon-select-trigger:hover {
  border-color: #1890ff;
}

.icon-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}

.placeholder {
  color: #9ca3af;
}

.arrow {
  color: #9ca3af;
}

.icon-select-content {
  padding: 8px;
}

.mb-3 {
  margin-bottom: 12px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-item:hover {
  background: #f0f0f0;
}

.icon-item.active {
  background: #e6f7ff;
  color: #1890ff;
}

.text-2xl {
  font-size: 24px;
}

.text-xl {
  font-size: 20px;
}

.icon-name {
  font-size: 10px;
  color: #666;
  margin-top: 4px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}
</style>
