import { defineConfig, loadEnv, type ConfigEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'

export default defineConfig(({ mode }: ConfigEnv) => {
  const env = loadEnv(mode, process.cwd())
  
  return {
    plugins: [
      vue(),
      AutoImport({
        imports: [
          'vue',
          {
            'naive-ui': ['useDialog', 'useMessage', 'useNotification', 'useLoadingBar']
          }
        ],
        dts: 'src/auto-imports.d.ts',
        eslintrc: {
          enabled: true,
        },
      }),
      Components({
        resolvers: [
          NaiveUiResolver(),
          IconsResolver({
            prefix: 'icon',
          }),
        ],
        dts: 'src/components.d.ts',
      }),
      Icons({
        autoInstall: true,
      }),
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5173,
      host: true,
      // 支持 Vue Router 的 history 模式，刷新时返回 index.html
      historyApiFallback: {
        rewrites: [
          { from: /.*/, to: '/index.html' },
        ],
      },
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          // 如果 baseURL 已包含 /api，需要重写路径
          rewrite: (path) => {
            // 如果目标 URL 已包含 /api，则去掉请求路径中的 /api
            if (env.VITE_API_BASE_URL?.includes('/api')) {
              return path.replace(/^\/api/, '')
            }
            return path
          },
        },
      },
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 2000,
      rollupOptions: {
        output: {
          manualChunks: {
            'naive-ui': ['naive-ui'],
            'vue-router': ['vue-router'],
            'pinia': ['pinia'],
            'axios': ['axios'],
            'echarts': ['echarts'],
            'vueuse': ['@vueuse/core'],
          },
        },
      },
    },
  }
})
