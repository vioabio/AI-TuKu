<template>
  <div ref="containerRef" class="lazy-image-container" :style="containerStyle">
    <!-- 模糊占位层（使用极低质量预览图） -->
    <img
      v-if="placeholderSrc"
      :src="placeholderSrc"
      class="lazy-image-placeholder"
      :class="{ loaded: isLoaded }"
      :style="{ objectFit: fit }"
      alt=""
      aria-hidden="true"
    />

    <!-- 变色骨架屏（无 placeholder URL 时显示） -->
    <div v-else-if="!isLoaded" class="lazy-image-skeleton" />

    <!-- 实际图片 -->
    <picture v-if="isVisible">
      <!-- AVIF 格式（浏览器支持时优先） -->
      <source v-if="avifSrc" :srcset="avifSrc" type="image/avif" />
      <!-- WebP 格式（兜底） -->
      <source v-if="webpSrc" :srcset="webpSrc" type="image/webp" />
      <!-- 原图 -->
      <img
        ref="imgRef"
        :src="src"
        :alt="alt"
        class="lazy-image-real"
        :class="{ loaded: isLoaded }"
        :style="{
          objectFit: fit,
          aspectRatio: aspectRatio,
        }"
        loading="lazy"
        @load="onLoad"
        @error="onError"
      />
    </picture>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  getWebpUrl,
  getAvifUrl,
  getBlurPlaceholderUrl,
  getThumbnailUrl,
} from '@/utils/imageOptimizer'

/**
 * LazyImage — 支持懒加载、WebP/AVIF、模糊占位的图片组件
 *
 * 功能：
 *  1. IntersectionObserver 懒加载：仅加载可视区域内的图片
 *  2. WebP/AVIF 自动转换：通过 <picture> + <source> 实现格式降级
 *  3. 模糊占位符：先展示极低质量模糊预览，原图加载后过渡切换
 *  4. 固定宽高比：防止图片加载时的布局抖动（CLS）
 *
 * 使用示例：
 *   <LazyImage
 *     src="https://cos.example.com/pic.jpg"
 *     alt="图片"
 *     :width="360"
 *     :height="240"
 *   />
 */

interface Props {
  /** 图片源 URL */
  src?: string
  /** 图片 alt 文本 */
  alt?: string
  /** 容器宽度（px），防止 CLS */
  width?: number
  /** 容器高度（px），防止 CLS */
  height?: number
  /** 图片适应方式，同 CSS object-fit */
  fit?: 'cover' | 'contain' | 'fill' | 'none' | 'scale-down'
  /** 是否启用 WebP 优化（默认 true） */
  webp?: boolean
  /** 是否启用 AVIF 优化（默认 true） */
  avif?: boolean
  /** 是否启用模糊占位（默认 true） */
  blurPlaceholder?: boolean
  /** 懒加载根边距（px），提前加载即将进入视口的图片 */
  rootMargin?: string
}

const props = withDefaults(defineProps<Props>(), {
  src: '',
  alt: '',
  fit: 'cover',
  webp: true,
  avif: true,
  blurPlaceholder: true,
  rootMargin: '200px',
})

const emit = defineEmits<{
  loaded: []
  error: []
}>()

// ---- 状态 ----
const containerRef = ref<HTMLElement>()
const imgRef = ref<HTMLImageElement>()
const isVisible = ref(false)
const isLoaded = ref(false)
const hasError = ref(false)

// ---- 计算属性 ----
const aspectRatio = computed(() => {
  if (props.width && props.height) {
    return `${props.width} / ${props.height}`
  }
  return undefined
})

const containerStyle = computed(() => {
  return {
    width: props.width ? `${props.width}px` : '100%',
    height: props.height ? `${props.height}px` : undefined,
    aspectRatio: aspectRatio.value,
    overflow: 'hidden',
    position: 'relative' as const,
    backgroundColor: '#f0f0f0',
  }
})

const webpSrc = computed(() => {
  if (!props.webp || !props.src) return undefined
  return getWebpUrl(props.src)
})

const avifSrc = computed(() => {
  if (!props.avif || !props.src) return undefined
  return getAvifUrl(props.src)
})

const placeholderSrc = computed(() => {
  if (!props.blurPlaceholder || !props.src) return undefined
  // 使用 COS 万象CI 生成 10x10 模糊预览图（仅几百字节）
  return getBlurPlaceholderUrl(props.src)
})

// ---- Intersection Observer 懒加载 ----
let observer: IntersectionObserver | null = null

const setupObserver = () => {
  if (!containerRef.value) return

  // 如果浏览器不支持 IntersectionObserver，直接加载
  if (!('IntersectionObserver' in window)) {
    isVisible.value = true
    return
  }

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          isVisible.value = true
          observer?.unobserve(entry.target)
          observer?.disconnect()
          observer = null
        }
      })
    },
    {
      rootMargin: props.rootMargin, // 提前 200px 开始加载
      threshold: 0.01,
    },
  )

  observer.observe(containerRef.value)
}

// ---- 事件处理 ----
const onLoad = () => {
  isLoaded.value = true
  emit('loaded')
}

const onError = () => {
  hasError.value = true
  emit('error')
}

// ---- 生命周期 ----
onMounted(() => {
  setupObserver()
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})

// 如果 src 变化且已可见，重置状态
watch(
  () => props.src,
  () => {
    if (isVisible.value && props.src) {
      isLoaded.value = false
      hasError.value = false
    }
  },
)
</script>

<style scoped>
.lazy-image-container {
  position: relative;
  overflow: hidden;
  background: #f5f5f5;
}

.lazy-image-placeholder,
.lazy-image-real {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

/* 占位图：始终模糊 */
.lazy-image-placeholder {
  filter: blur(20px);
  transform: scale(1.1);
  transition: opacity 0.4s ease-in-out;
  z-index: 1;
}

/* 占位图在原图加载后淡出 */
.lazy-image-placeholder.loaded {
  opacity: 0;
  pointer-events: none;
}

/* 原图：加载后淡入 + 去模糊 */
.lazy-image-real {
  z-index: 2;
  opacity: 0;
  filter: blur(10px);
  transform: scale(1.02);
  transition:
    opacity 0.4s ease-in-out,
    filter 0.3s ease-in-out;
}

.lazy-image-real.loaded {
  opacity: 1;
  filter: blur(0);
  transform: scale(1);
}

/* 骨架屏动画（无 placeholder URL 时使用） */
.lazy-image-skeleton {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
