/**
 * AI-TuKu WebAssembly 加载器 (第 9.2 节)
 *
 * 封装 wasm-pack 生成的 WASM 模块，提供类型安全的 API
 *
 * 使用方式:
 *   import { initWasm, processImage, generateThumbnail } from '@/utils/wasmLoader'
 *   await initWasm()
 *   const result = await processImage(fileBytes, 1920, 1080)
 */

// wasm-pack 生成的模块类型
interface WasmModule {
  init(): Promise<void>
  process_image(
    inputBytes: Uint8Array,
    maxWidth: number,
    maxHeight: number,
  ): ProcessResult
  generate_thumbnail(
    inputBytes: Uint8Array,
    thumbWidth: number,
    thumbHeight: number,
  ): ThumbnailResult
  to_webp(inputBytes: Uint8Array, quality: number): Uint8Array
}

interface ProcessResult {
  data: Uint8Array
  width: number
  height: number
  aspect_ratio: number
  dominant_color: string
  size: number
}

interface ThumbnailResult {
  thumbnail_data: Uint8Array
  blur_placeholder: Uint8Array
}

/** WASM 模块缓存 */
let wasmModule: WasmModule | null = null

/** WASM 是否已初始化 */
let initialized = false

/** 当前浏览器是否支持 WASM */
const wasmSupported =
  typeof WebAssembly === 'object' &&
  WebAssembly.validate !== undefined

/**
 * 初始化 WASM 模块
 *
 * 延迟加载 — 仅在上传图片等功能触发时才加载
 * 首次调用约 50ms（加载 + 编译），后续调用零开销
 */
export async function initWasm(): Promise<boolean> {
  if (initialized) return true
  if (!wasmSupported) {
    console.warn('[WASM] 浏览器不支持 WebAssembly，回退到纯 JS 处理')
    return false
  }

  try {
    // 动态导入 wasm-pack 生成的模块
    const module = await import(
      /* webpackChunkName: "wasm-image-processor" */
      '../../public/wasm/aituku-image-processor.js'
    )
    wasmModule = module as unknown as WasmModule
    await wasmModule.init()
    initialized = true
    console.log('[WASM] AI-TuKu 图片处理器已加载')
    return true
  } catch (e) {
    console.warn('[WASM] 加载失败，回退到纯 JS 处理:', e)
    return false
  }
}

// ============================================================
// 公开 API — 类型安全的封装
// ============================================================

/** 图片处理结果 */
export interface ImageProcessResult {
  /** WebP 格式的图片字节 */
  data: Uint8Array
  /** 处理后宽度 */
  width: number
  /** 处理后高度 */
  height: number
  /** 宽高比 */
  aspectRatio: number
  /** 主色调 (HEX) */
  dominantColor: string
  /** 文件大小 (字节) */
  size: number
}

/**
 * 图片预处理：缩放 + WebP 转换 + 主色调提取
 *
 * @param fileBytes - 原始图片字节
 * @param maxWidth - 最大宽度 (0 = 不限制)
 * @param maxHeight - 最大高度 (0 = 不限制)
 * @returns 处理后的图片 + 元数据，WASM 不可用时返回 null
 */
export async function processImage(
  fileBytes: Uint8Array,
  maxWidth: number = 1920,
  maxHeight: number = 1080,
): Promise<ImageProcessResult | null> {
  if (!initialized || !wasmModule) return null

  try {
    const result = wasmModule.process_image(fileBytes, maxWidth, maxHeight)
    return {
      data: result.data,
      width: result.width,
      height: result.height,
      aspectRatio: result.aspect_ratio,
      dominantColor: result.dominant_color,
      size: result.size,
    }
  } catch (e) {
    console.error('[WASM] processImage 失败:', e)
    return null
  }
}

/** 缩略图生成结果 */
export interface ThumbnailGenerateResult {
  /** WebP 缩略图字节 */
  thumbnailData: Uint8Array
  /** 模糊占位图字节 (10×10 blur) */
  blurPlaceholder: Uint8Array
}

/**
 * 生成缩略图 + 模糊占位图
 *
 * @param fileBytes - 原始图片字节
 * @param thumbWidth - 缩略图宽度 (默认 400)
 * @param thumbHeight - 缩略图高度 (默认 300)
 */
export async function generateThumbnail(
  fileBytes: Uint8Array,
  thumbWidth: number = 400,
  thumbHeight: number = 300,
): Promise<ThumbnailGenerateResult | null> {
  if (!initialized || !wasmModule) return null

  try {
    const result = wasmModule.generate_thumbnail(fileBytes, thumbWidth, thumbHeight)
    return {
      thumbnailData: result.thumbnail_data,
      blurPlaceholder: result.blur_placeholder,
    }
  } catch (e) {
    console.error('[WASM] generateThumbnail 失败:', e)
    return null
  }
}

/**
 * 简化版 WebP 转换（不需要缩放和主色调）
 *
 * @param fileBytes - 原始图片字节
 * @returns WebP 格式的图片字节，失败时返回 null
 */
export async function toWebp(fileBytes: Uint8Array): Promise<Uint8Array | null> {
  if (!initialized || !wasmModule) return null

  try {
    return wasmModule.to_webp(fileBytes, 80)
  } catch (e) {
    console.error('[WASM] toWebp 失败:', e)
    return null
  }
}

/**
 * 检测 WASM 是否可用
 */
export function isWasmAvailable(): boolean {
  return initialized && wasmModule !== null
}

/**
 * WASM 不可用时的降级方案：使用 Canvas API 进行纯 JS 图片处理
 *
 * @param file - 原始 File 对象
 * @param maxWidth - 最大宽度
 * @param maxHeight - 最大高度
 * @param format - 输出格式 (默认 'image/webp')
 * @param quality - 输出质量 0.0-1.0 (默认 0.8)
 */
export async function processImageFallback(
  file: File,
  maxWidth: number = 1920,
  maxHeight: number = 1080,
  format: string = 'image/webp',
  quality: number = 0.8,
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)

    img.onload = () => {
      URL.revokeObjectURL(url)

      // 计算缩放尺寸
      let { width, height } = img
      if (maxWidth > 0 && width > maxWidth) {
        height = Math.round((height * maxWidth) / width)
        width = maxWidth
      }
      if (maxHeight > 0 && height > maxHeight) {
        width = Math.round((width * maxHeight) / height)
        height = maxHeight
      }

      // Canvas 绘制
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')!
      ctx.drawImage(img, 0, 0, width, height)

      canvas.toBlob(
        (blob) => {
          if (blob) resolve(blob)
          else reject(new Error('Canvas toBlob 失败'))
        },
        format,
        quality,
      )
    }

    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = url
  })
}
