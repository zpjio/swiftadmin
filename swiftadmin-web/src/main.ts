import { createApp } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'
import router from './router'
import App from './App.vue'
import './styles/index.css'
import { setupPermissionDirective } from './directives/permission'
import { initTheme, theme, themeOverrides } from '@/utils/theme'

const app = createApp(App)
const pinia = createPinia()

// 初始化主题
initTheme()

app.use(pinia)
app.use(router)
app.use(naive, {
  theme: theme.value,
  themeOverrides: themeOverrides.value,
})
setupPermissionDirective(app)

app.mount('#app')
