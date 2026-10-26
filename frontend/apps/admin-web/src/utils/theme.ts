import { ref, computed, watch } from 'vue'
import { darkTheme } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'

// 主题类型
export type ThemeMode = 'light' | 'dark'

// 当前主题
const currentTheme = ref<ThemeMode>('light')

// 是否暗色模式
const isDark = computed(() => currentTheme.value === 'dark')

// Naive UI 主题
const theme = computed(() => (isDark.value ? darkTheme : undefined))

// 主题覆盖配置
const themeOverrides = computed<GlobalThemeOverrides>(() => {
  if (isDark.value) {
    return {
      common: {
        primaryColor: '#1890ff',
        primaryColorHover: '#40a9ff',
        primaryColorPressed: '#096dd9',
        primaryColorSuppl: '#40a9ff',
        bodyColor: '#18181c',
        cardColor: '#1f1f23',
        tableColor: '#1f1f23',
        inputColor: '#1f1f23',
        actionColor: '#28282c',
        hoverColor: 'rgba(255, 255, 255, 0.09)',
        borderColor: '#3a3a3a',
        dividerColor: '#3a3a3a'
      }
    }
  }
  return {
    common: {
      primaryColor: '#1890ff',
      primaryColorHover: '#40a9ff',
      primaryColorPressed: '#096dd9',
      primaryColorSuppl: '#40a9ff'
    }
  }
})

// 初始化主题
export function initTheme() {
  const savedTheme = localStorage.getItem('theme') as ThemeMode | null
  if (savedTheme && (savedTheme === 'light' || savedTheme === 'dark')) {
    currentTheme.value = savedTheme
    applyTheme(savedTheme)
  }
}

// 切换主题
export function toggleTheme() {
  const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
  setTheme(newTheme)
}

// 设置主题
export function setTheme(mode: ThemeMode) {
  currentTheme.value = mode
  localStorage.setItem('theme', mode)
  applyTheme(mode)
}

// 应用主题到 HTML
function applyTheme(mode: ThemeMode) {
  if (mode === 'dark') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

// 监听主题变化
watch(currentTheme, (newMode) => {
  applyTheme(newMode)
})

export { currentTheme, isDark, theme, themeOverrides }
