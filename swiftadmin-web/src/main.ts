import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './styles/index.css'
import { setupPermissionDirective } from './directives/permission'
import { initTheme } from '@/utils/theme'

const app = createApp(App)
const pinia = createPinia()

// 初始化主题
initTheme()

app.use(pinia)
app.use(router)
// naive-ui 组件按需注册(由 unplugin-vue-components 的 NaiveUiResolver 驱动),
// 若这里再全局注册 naives 会把整个组件库拉进 vendor 包(约 1.3MB)
setupPermissionDirective(app)

app.mount('#app')
