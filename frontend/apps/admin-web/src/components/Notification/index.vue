<template>
  <NPopover trigger="click" placement="bottom-end" :width="380" @update:show="handlePopoverChange">
    <template #trigger>
      <NButton quaternary class="notification-btn">
        <template #icon>
          <NBadge :value="unreadCount" :max="99" :show="unreadCount > 0">
            <icon-mdi-bell :size="20" />
          </NBadge>
        </template>
      </NButton>
    </template>

    <div class="notification-panel">
      <!-- 头部 -->
      <div class="notification-header">
        <span class="title">消息通知</span>
        <NSpace>
          <NButton text size="small" @click="handleRefresh" :loading="loading">
            <template #icon><icon-mdi-refresh :size="14" /></template>
          </NButton>
          <NButton text size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">
            全部已读
          </NButton>
          <NButton text size="small" @click="handleGoInbox">
            查看全部
          </NButton>
        </NSpace>
      </div>

      <NDivider style="margin: 8px 0" />

      <!-- 消息列表 -->
      <NScrollbar style="max-height: 400px">
        <div v-if="loading" class="loading-container">
          <NSpin size="small" />
        </div>
        <div v-else-if="messageList.length === 0" class="empty-container">
          <NEmpty description="暂无消息" />
        </div>
        <div v-else>
          <div
            v-for="msg in messageList"
            :key="msg.id"
            class="message-item"
            :class="{ unread: msg.isRead === 0 }"
            @click="handleMessageClick(msg)"
          >
            <div class="message-header">
              <NTag :type="getMsgTypeTag(msg.msgType)" size="small">
                {{ getMsgTypeLabel(msg.msgType) }}
              </NTag>
              <span class="message-time">{{ formatTime(msg.createTime) }}</span>
            </div>
            <div class="message-title">
              <span v-if="msg.isRead === 0" class="unread-dot"></span>
              {{ msg.title }}
            </div>
            <div class="message-content">{{ truncateContent(msg.content) }}</div>
          </div>
        </div>
      </NScrollbar>
    </div>
  </NPopover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import {
  getInboxPageApi,
  getUnreadCountApi,
  markInboxReadApi,
  markAllInboxReadApi,
  type InboxMessage,
} from '@/api/message'

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const messageList = ref<InboxMessage[]>([])
const unreadCount = ref(0)

// 消息类型标签映射
const msgTypeTagMap: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  system: 'info',
  security: 'error',
  order: 'warning',
  promotion: 'success',
  task: 'info',
  notice: 'default',
  alert: 'error',
}

const msgTypeLabelMap: Record<string, string> = {
  system: '系统',
  security: '安全',
  order: '订单',
  promotion: '营销',
  task: '任务',
  notice: '公告',
  alert: '告警',
}

function getMsgTypeTag(type?: string) {
  return msgTypeTagMap[type || 'system'] || 'default'
}

function getMsgTypeLabel(type?: string) {
  return msgTypeLabelMap[type || 'system'] || type || '通知'
}

// 格式化时间
function formatTime(time?: string) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 小于1分钟
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))}分钟前`
  }
  // 小于24小时
  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  }
  // 小于7天
  if (diff < 7 * 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (24 * 60 * 60 * 1000))}天前`
  }
  // 其他显示日期
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 截断内容
function truncateContent(content?: string) {
  if (!content) return ''
  // 移除HTML标签
  const text = content.replace(/<[^>]*>/g, '')
  return text.length > 50 ? text.slice(0, 50) + '...' : text
}

// 加载消息列表
async function loadMessages() {
  loading.value = true
  try {
    const res = await getInboxPageApi({
      page: 1,
      pageSize: 10,
    })
    messageList.value = res?.records || []
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载未读数量
async function loadUnreadCount() {
  try {
    const res = await getUnreadCountApi()
    unreadCount.value = res?.total || 0
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

// 刷新消息
async function handleRefresh() {
  await Promise.all([loadMessages(), loadUnreadCount()])
}

// 弹窗显示状态变化
function handlePopoverChange(show: boolean) {
  if (show) {
    loadMessages()
    loadUnreadCount()
  }
}

// 点击消息
async function handleMessageClick(msg: InboxMessage) {
  // 标记已读
  if (msg.isRead === 0) {
    try {
      await markInboxReadApi([msg.id])
      msg.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  // 跳转到站内信页面
  router.push('/message/inbox')
}

// 全部已读
async function handleMarkAllRead() {
  try {
    await markAllInboxReadApi()
    messageList.value.forEach(msg => msg.isRead = 1)
    unreadCount.value = 0
    message.success('已全部标记为已读')
  } catch (error) {
    message.error('操作失败')
  }
}

// 查看全部
function handleGoInbox() {
  router.push('/message/inbox')
}

// 定时刷新未读数量
let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadUnreadCount()
  // 每30秒刷新一次未读数量
  timer = setInterval(loadUnreadCount, 30 * 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.notification-btn {
  padding: 0 8px;
}

.notification-panel {
  min-height: 200px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.notification-header .title {
  font-weight: 500;
  font-size: 14px;
}

.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.message-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.message-item:hover {
  background-color: var(--n-color-hover);
}

.message-item.unread {
  background-color: rgba(24, 144, 255, 0.08);
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-title {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #18a058;
  flex-shrink: 0;
}

.message-content {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}
</style>