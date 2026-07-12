import { defineConfig } from 'vitest/config'
import viteConfig from './vite.config'

export default defineConfig({
  ...viteConfig,
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'src/api/typings.d.ts',
        'src/api/mainController.ts',
        'src/api/userController.ts',
        'src/api/pictureController.ts',
        'src/api/spaceController.ts',
        'src/api/spaceAnalyzeController.ts',
        'src/api/spaceUserController.ts',
        'src/api/fileController.ts',
        'src/api/index.ts',
        'src/main.ts',
        'src/access.ts',
        'src/request.ts',
      ],
      thresholds: {
        lines: 60,
        functions: 60,
        branches: 50,
        statements: 60,
      },
    },
  },
})
