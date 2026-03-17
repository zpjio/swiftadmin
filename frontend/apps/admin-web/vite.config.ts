import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import { fileURLToPath } from 'url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import Icons from 'unplugin-icons/vite'
import IconsResolver from 'unplugin-icons/resolver'

const __dirname = resolve(fileURLToPath(import.meta.url), '..')

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const config: any = ({ mode }: { mode: string }) => {
  const env = loadEnv(mode, process.cwd())

  const plugins = [
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
  ]

  return {
    plugins,
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 5173,
      host: true,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path: string) => {
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
          manualChunks: (id: string) => {
            // 将 store 相关模块打包到一起，避免循环依赖
            if (id.includes('/store/')) {
              return 'store'
            }
            if (id.includes('naive-ui')) {
              return 'naive-ui'
            }
            if (id.includes('vue-router')) {
              return 'vue-router'
            }
            if (id.includes('pinia')) {
              return 'pinia'
            }
            if (id.includes('axios')) {
              return 'axios'
            }
            if (id.includes('echarts')) {
              return 'echarts'
            }
            if (id.includes('@vueuse/core')) {
              return 'vueuse'
            }
          },
        },
      },
    },
  }
}

export default defineConfig(config)
