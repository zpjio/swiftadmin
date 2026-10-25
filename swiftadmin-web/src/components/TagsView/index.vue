<template>
  <div class="tags-view-container">
    <NScrollbar ref="scrollbarRef" class="tags-view-scrollbar">
      <div class="tags-view-wrapper">
        <div
          v-for="tag in visitedViews"
          :key="tag.path"
          :class="['tags-view-item', { active: isActive(tag) }]"
          @click="handleClick(tag)"
          @contextmenu.prevent="handleContextmenu($event, tag)"
        >
          <i v-if="tag.icon" :class="getIconClass(tag.icon)" class="tags-view-icon" />
          <span class="tags-view-title">{{ tag.title }}</span>
          <icon-mdi-close
            v-if="tag.closable"
            class="tags-view-close"
            @click.stop="handleClose(tag)"
          />
        </div>
      </div>
    </NScrollbar>

    <!-- 右键菜单 -->
    <NDropdown
      v-model:show="showDropdown"
      :options="dropdownOptions"
      :x="dropdownX"
      :y="dropdownY"
      placement="bottom-start"
      @select="handleDropdownSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTagsStore, type TagView } from '@/store/modules/tags'
import { getIconClass } from '@/utils/icon'
import type { DropdownOption } from 'naive-ui'

const router = useRouter()
const route = useRoute()
const tagsStore = useTagsStore()

const visitedViews = computed(() => tagsStore.visitedViews)
const activeView = computed(() => tagsStore.activeView)

const scrollbarRef = ref<any>(null)
const showDropdown = ref(false)
const dropdownX = ref(0)
const dropdownY = ref(0)
const currentTag = ref<TagView | null>(null)

// 判断是否是当前激活的标签页
const isActive = (tag: TagView) => {
  return tag.path === route.path
}

// 点击标签页
const handleClick = (tag: TagView) => {
  tagsStore.setActiveView(tag)
  router.push(tag.path)
}

// 关闭标签页
const handleClose = (tag: TagView) => {
  tagsStore.closeView(tag)
}

// 右键菜单
const handleContextmenu = (event: MouseEvent, tag: TagView) => {
  showDropdown.value = true
  dropdownX.value = event.clientX
  dropdownY.value = event.clientY
  currentTag.value = tag
}

// 右键菜单选项
const dropdownOptions = computed<DropdownOption[]>(() => {
  const options: DropdownOption[] = [
    {
      label: '关闭',
      key: 'close',
      disabled: !currentTag.value?.closable,
      icon: () => h('i', { class: `${getIconClass('mdi-close')} text-lg` }),
    },
    {
      label: '关闭其他',
      key: 'close-others',
      icon: () => h('i', { class: `${getIconClass('mdi-close-circle-outline')} text-lg` }),
    },
    {
      label: '关闭全部',
      key: 'close-all',
      icon: () => h('i', { class: `${getIconClass('mdi-close-network-outline')} text-lg` }),
    },
    {
      type: 'divider',
      key: 'd1',
    },
    {
      label: '刷新',
      key: 'refresh',
      disabled: !currentTag.value?.closable,
      icon: () => h('i', { class: `${getIconClass('mdi-refresh')} text-lg` }),
    },
  ]
  return options
})

// 处理右键菜单选择
const handleDropdownSelect = (key: string) => {
  const tag = currentTag.value
  if (!tag) return

  switch (key) {
    case 'close':
      tagsStore.closeView(tag)
      break
    case 'close-others':
      tagsStore.closeOthers(tag)
      router.push(tag.path)
      break
    case 'close-all':
      tagsStore.closeAll()
      if (visitedViews.value.length > 0 && visitedViews.value[0]) {
        router.push(visitedViews.value[0].path)
      }
      break
    case 'refresh':
      handleRefresh(tag)
      break
  }

  showDropdown.value = false
}

// 刷新标签页
const handleRefresh = (tag: TagView) => {
  // 从缓存中移除
  if (tag.name) {
    const index = tagsStore.cachedViews.indexOf(tag.name as string)
    if (index > -1) {
      tagsStore.cachedViews.splice(index, 1)
    }
  }

  // 重新添加以触发重新渲染
  tagsStore.addView(tag)

  // 如果刷新的是当前页面，重新导航
  if (tag.path === route.path) {
    router.replace({ path: '/redirect' + tag.path, query: tag.query })
  }
}
</script>

<style scoped>
.tags-view-container {
  height: 32px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.tags-view-scrollbar {
  height: 100%;
}

.tags-view-wrapper {
  display: flex;
  align-items: center;
  padding: 0 8px;
  height: 100%;
  white-space: nowrap;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  margin-right: 4px;
  height: 26px;
  font-size: 13px;
  color: #666;
  background: #f5f5f5;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.tags-view-item:hover {
  color: #1890ff;
}

.tags-view-item.active {
  color: #fff;
  background: #1890ff;
}

.tags-view-item.active .tags-view-close:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.3);
}

.tags-view-icon {
  margin-right: 6px;
  font-size: 14px;
}

.tags-view-title {
  line-height: 1;
}

.tags-view-close {
  margin-left: 6px;
  font-size: 14px;
  border-radius: 50%;
  padding: 2px;
  transition: all 0.2s;
}

.tags-view-close:hover {
  color: #fff;
  background: rgba(0, 0, 0, 0.2);
}

/* 暗色模式 */
.dark .tags-view-container {
  background: #242428;
  border-bottom-color: #3a3a3a;
}

.dark .tags-view-item {
  color: #ccc;
  background: #2a2a30;
}

.dark .tags-view-item:hover {
  color: #1890ff;
}

.dark .tags-view-item.active {
  color: #fff;
  background: #1890ff;
}
</style>
