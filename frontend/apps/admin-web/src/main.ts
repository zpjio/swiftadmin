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
setupPermissionDirective(app)

app.mount('#app')
