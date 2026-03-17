<template>
  <NLayout has-sider class="h-full">
    <NLayoutSider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="collapsed"
      show-trigger="bar"
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="sider-inner">
        <div class="logo" @click="router.push('/')">
          <span v-if="!collapsed">SwiftAdmin</span>
          <span v-else>SA</span>
        </div>
        <div class="menu-wrapper">
          <NScrollbar>
            <NMenu
              :options="(menuOptions as any)"
              :collapsed="collapsed"
              :collapsed-width="64"
              :collapsed-icon-size="22"
              :indent="18"
              :value="currentRoute"
              :expanded-keys="expandedKeys"
              @update:value="handleMenuClick"
              @update:expanded-keys="handleExpandKeysChange"
            />
          </NScrollbar>
        </div>
      </div>
    </NLayoutSider>
    
    <NLayout>
      <NLayoutHeader bordered class="h-16 flex items-center justify-between px-4">
        <div class="flex items-center">
          <NButton quaternary @click="collapsed = !collapsed">
            <template #icon>
              <icon-mdi-menu />
            </template>
          </NButton>
        </div>
        <div class="flex items-center gap-2">
          <NButton quaternary @click="toggleTheme" :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
            <template #icon>
              <icon-mdi-white-balance-sunny v-if="isDark" />
              <icon-mdi-weather-night v-else />
            </template>
          </NButton>
          <Notification />
          <NDropdown :options="userDropdownOptions" @select="handleUserAction">
            <NButton quaternary class="user-info">
              <img v-if="userAvatar" :src="userAvatar" class="w-6 h-6 rounded-full object-cover" />
              <NAvatar v-else :size="24" round>
                {{ avatarText }}
              </NAvatar>
              <span class="ml-2">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
            </NButton>
          </NDropdown>
        </div>
      </NLayoutHeader>
      
      <!-- 标签页 -->
      <TagsView />
      
      <NLayoutContent class="p-4 flex-1 overflow-auto">
        <RouterView v-slot="{ Component, route }">
          <KeepAlive :include="cachedViews">
            <component :is="Component" :key="route.fullPath" v-if="isAlive" />
          </KeepAlive>
        </RouterView>
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>

<script setup lang="ts">
import { ref, computed, h, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useUserStore } from '@/store'
import { useTagsStore } from '@/store/modules/tags'
import { logoutApi } from '@/api/auth'
import { getCurrentUserInfoApi } from '@/api/user'
import { toggleTheme, isDark } from '@/utils/theme'
import { normalizeIconName } from '@/utils/icon'
import { getMenuName } from '@/utils/router-helper'
import { Icon } from '@iconify/vue'
import TagsView from '@/components/TagsView/index.vue'
import Notification from '@/components/Notification/index.vue'
import type { MenuOption, DropdownOption } from 'naive-ui'
import type { MenuVO } from '@/types'

/** 扩展的菜单选项，包含 path 属性 */
interface ExtendedMenuOption {
  label: string
  key: string
  icon?: (() => import('vue').VNodeChild) | null
  path?: string
  children?: ExtendedMenuOption[]
}

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()
const tagsStore = useTagsStore()
const collapsed = ref(false)
const menuOptions = ref<ExtendedMenuOption[]>([])
const cachedViews = computed(() => tagsStore.cachedViews)
const isAlive = ref(true)

const currentRoute = computed(() => route.name as string)

// 展开的菜单项 keys
const expandedKeys = ref<string[]>([])

// 获取所有需要展开的菜单项 key（有子菜单的项）
const getAllExpandKeys = (options: ExtendedMenuOption[]): string[] => {
  const keys: string[] = []
  const traverse = (items: ExtendedMenuOption[]) => {
    items.forEach((item) => {
      if (item.children && item.children.length > 0) {
        keys.push(item.key)
        traverse(item.children)
      }
    })
  }
  traverse(options)
  return keys
}

// 监听菜单选项变化，自动展开所有菜单
watch(menuOptions, (newOptions) => {
  if (newOptions.length > 0) {
    expandedKeys.value = getAllExpandKeys(newOptions)
  }
}, { immediate: true })

// 用户头像（优先使用用户配置的头像）
const userAvatar = computed(() => {
  return userStore.userInfo?.avatar || ''
})

// 头像文字（无头像时显示用户名首字母）
const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  return name ? name.charAt(0).toUpperCase() : 'U'
})

// 从菜单树生成菜单选项
function convertMenuTreeToOptions(menus: MenuVO[]): ExtendedMenuOption[] {
  const options: ExtendedMenuOption[] = []

  menus.forEach((menu) => {
    // 跳过按钮类型（type=3）- 按钮只用于权限控制，不显示在菜单中
    if (menu.type === 3) {
      return
    }

    // 跳过禁用的菜单
    if (menu.status === 0) {
      return
    }

    // 跳过没有 path 的菜单
    if (!menu.path) {
      return
    }

    const option: ExtendedMenuOption = {
      label: menu.name,
      key: getMenuName(menu.path),
      icon: menu.icon ? () => h(Icon, { icon: normalizeIconName(menu.icon), style: { fontSize: '18px' } }) : null,
      path: menu.path,
    }

    // 处理子菜单（递归过滤掉按钮类型）
    if (menu.children && menu.children.length > 0) {
      const childrenOptions = convertMenuTreeToOptions(menu.children)
      if (childrenOptions.length > 0) {
        option.children = childrenOptions
      }
    }

    options.push(option)
  })

  return options
}

// 更新菜单选项
const updateMenuOptions = () => {
  // 直接从 userStore 获取菜单树生成菜单
  const menus = userStore.userMenus || []

  if (menus.length === 0) {
    menuOptions.value = []
    return
  }

  const options = convertMenuTreeToOptions(menus)
  menuOptions.value = options
}

// 监听路由变化
watch(() => router.getRoutes().length, () => {
  updateMenuOptions()
})

// 监听路由变化，添加标签页
watch(
  () => route.fullPath,
  () => {
    tagsStore.addView({
      name: route.name,
      path: route.path,
      fullPath: route.fullPath,
      query: route.query,
      params: route.params,
      meta: route.meta,
    })
    tagsStore.setActiveView({
      name: route.name,
      path: route.path,
      fullPath: route.fullPath,
      query: route.query,
      params: route.params,
      meta: route.meta,
    })
  },
  { immediate: true }
)

// 组件挂载时初始化菜单
onMounted(async () => {
  // 如果没有用户信息，从 API 获取
  if (!userStore.userInfo) {
    try {
      const userInfo = await getCurrentUserInfoApi()
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
    } catch (error) {
      console.error('[Layout] 获取用户信息失败:', error)
    }
  }

  // 延迟确保路由已经加载
  setTimeout(() => {
    updateMenuOptions()
  }, 300)
})

// 用户下拉菜单
const userDropdownOptions: DropdownOption[] = [
  {
    label: '个人设置',
    key: 'profile',
    icon: () => h(Icon, { icon: 'mdi:account-settings', style: { fontSize: '18px' } }),
  },
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(Icon, { icon: 'mdi:logout', style: { fontSize: '18px' } }),
  },
]

// 从菜单选项中查找路径
const findPathByKey = (options: ExtendedMenuOption[], key: string): string | undefined => {
  for (const option of options) {
    if (option.key === key) {
      return option.path
    }
    if (option.children) {
      const path = findPathByKey(option.children, key)
      if (path) return path
    }
  }
  return undefined
}

// 处理菜单点击
const handleMenuClick = (key: string) => {
  // 从菜单选项中查找路径
  const path = findPathByKey(menuOptions.value, key)
  if (path) {
    router.push(path)
  }
}

// 处理展开状态变化
const handleExpandKeysChange = (keys: string[]) => {
  expandedKeys.value = keys
}

// 处理用户操作
const handleUserAction = async (key: string) => {
  if (key === 'profile') {
    router.push('/profile')
  } else if (key === 'logout') {
    try {
      await logoutApi()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      // 重置标签页状态
      tagsStore.resetViews()
      // 清除用户状态和路由
      userStore.logout()
      router.push('/login')
      message.success('已退出登录')
    }
  }
}
</script>

<style scoped>
.sider-inner {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.logo {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: #1890ff;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  user-select: none;
}

.menu-wrapper {
  flex: 1;
  overflow: hidden;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.w-6 {
  width: 24px;
}

.h-6 {
  height: 24px;
}

.rounded-full {
  border-radius: 50%;
}

.object-cover {
  object-fit: cover;
}

.ml-2 {
  margin-left: 8px;
}
</style>
