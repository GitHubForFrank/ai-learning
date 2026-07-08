// [SDD-TASK: Task001]
// [SDD-SPEC: 03-技术方案.md §9.6 + §10] Vite dev:5173;/api 代理到 10197/app
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:10197/app',
        changeOrigin: true,
      },
    },
  },
})
