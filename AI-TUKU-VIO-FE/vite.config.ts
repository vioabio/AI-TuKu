import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  server:{
    host: 'localhost',
    proxy:{
      '/api': 'http://localhost:8123',
      // COS 图片代理，解决跨域问题
      '/public': {
        target: 'https://vio-1447107544.cos.ap-shanghai.myqcloud.com',
        changeOrigin: true,
      },
      '/space': {
        target: 'https://vio-1447107544.cos.ap-shanghai.myqcloud.com',
        changeOrigin: true,
      },
    }
  },

  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
