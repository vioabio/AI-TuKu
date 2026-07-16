import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { VitePWA } from 'vite-plugin-pwa'

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
    VitePWA({
      registerType: 'autoUpdate',
      // 开发环境也生成 SW（方便调试）
      devOptions: {
        enabled: false, // 设为 true 可在 dev 模式下测试 PWA
      },
      // Manifest 配置
      manifest: {
        name: 'AI-TuKu - 智能图库',
        short_name: 'AI-TuKu',
        description: 'AI 驱动的智能图片管理与分享平台',
        theme_color: '#1677ff',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'any',
        start_url: '/',
        scope: '/',
        lang: 'zh-CN',
        icons: [
          {
            src: '/pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
          },
          {
            src: '/pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
        // 快捷方式
        shortcuts: [
          {
            name: '上传图片',
            short_name: '上传',
            description: '上传一张新图片',
            url: '/add_picture',
            icons: [{ src: '/pwa-192x192.png', sizes: '192x192' }],
          },
        ],
        // 关联的 Web 应用
        related_applications: [],
        prefer_related_applications: false,
      },
      // Workbox 配置（Service Worker 缓存策略）
      workbox: {
        // 预缓存：构建产物自动预缓存
        globPatterns: [
          '**/*.{js,css,html,ico,png,svg,woff2}',
        ],
        // 运行时缓存策略
        runtimeCaching: [
          // 1. API 请求：Network First（优先网络，离线时使用缓存）
          {
            urlPattern: /^\/api\/.*/,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              networkTimeoutSeconds: 5,
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60, // 1 小时
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
          // 2. COS 图片：Stale While Revalidate（先用缓存，后台更新）
          {
            urlPattern: /^https:\/\/.*\.(myqcloud\.com|cos\..*)\/.*\.(jpg|jpeg|png|webp|gif|svg)/i,
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'image-cache',
              expiration: {
                maxEntries: 200,
                maxAgeSeconds: 60 * 60 * 24 * 7, // 1 周
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
          // 3. 图片缩略图（COS 代理）：Stale While Revalidate
          {
            urlPattern: /^\/(public|space)\/.*/,
            handler: 'StaleWhileRevalidate',
            options: {
              cacheName: 'image-proxy-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24, // 1 天
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
          // 4. 静态资源（字体、图标等）：Cache First
          {
            urlPattern: /\.(woff2?|ttf|eot|svg|png|ico)$/,
            handler: 'CacheFirst',
            options: {
              cacheName: 'static-assets-cache',
              expiration: {
                maxEntries: 50,
                maxAgeSeconds: 60 * 60 * 24 * 30, // 30 天
              },
            },
          },
        ],
        // 客户端声明 SW 更新已就绪（由 registerType: 'autoUpdate' 自动化处理）
        skipWaiting: true,
        clientsClaim: true,
      },
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
