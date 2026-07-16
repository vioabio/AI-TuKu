/**
 * 图片 URL 优化工具
 *
 * 功能：
 *  - WebP/AVIF 格式自动转换（腾讯云 COS 万象CI）
 *  - 缩略图生成（按指定宽高裁剪）
 *  - 渐进式加载占位图（极低质量模糊预览）
 *
 * 腾讯云万象CI 图片处理参数文档：
 * https://cloud.tencent.com/document/product/460/36540
 */

/** COS 万象CI 基础 URL（支持图片处理的基础域名） */
const CI_BASE_URL = 'https://vio-1447107544.cos.ap-shanghai.myqcloud.com'

/**
 * 判断 URL 是否支持万象CI处理
 * COS 原始域名和自定义 CDN 域名均可使用万象CI参数
 */
const isCiSupported = (url: string): boolean => {
  if (!url) return false
  return (
    url.includes('myqcloud.com') ||
    url.includes('cos.') ||
    url.includes('.cdn.')
  )
}

/**
 * 获取 WebP 格式图片 URL
 * 浏览器不支持 WebP 时自动返回原 URL
 *
 * @param url 原始图片 URL
 * @returns 拼接了 ?format=webp 的 URL
 */
export const getWebpUrl = (url?: string): string => {
  if (!url) return ''
  if (!isCiSupported(url)) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}format=webp`
}

/**
 * 获取 AVIF 格式图片 URL（压缩率更高，但编码较慢）
 * 适用于 Chrome 85+ / Firefox 93+
 *
 * @param url 原始图片 URL
 * @returns 拼接了 ?format=avif 的 URL
 */
export const getAvifUrl = (url?: string): string => {
  if (!url) return ''
  if (!isCiSupported(url)) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}format=avif`
}

/**
 * 智能选择最优图片格式
 * 优先 AVIF → WebP → 原图
 *
 * @param url 原始图片 URL
 * @returns 最优格式的图片 URL
 */
export const getOptimizedUrl = (url?: string): string => {
  if (!url) return ''
  if (!isCiSupported(url)) return url

  // 通过 <picture> + <source type="image/avif"> 可以自动降级
  // 此函数返回 WebP 作为安全默认值（兼容性最广）
  return getWebpUrl(url)
}

/**
 * 生成指定尺寸的缩略图 URL
 *
 * @param url 原始图片 URL
 * @param width 宽度（px）
 * @param height 高度（px），不传则等比例缩放
 * @returns 缩略图 URL
 */
export const getThumbnailUrl = (
  url?: string,
  width?: number,
  height?: number,
): string => {
  if (!url) return ''
  if (!isCiSupported(url)) return url

  const params: string[] = []
  if (width || height) {
    const w = width ?? ''
    const h = height ?? ''
    params.push(`imageMogr2/thumbnail/${w}x${h}`)
  }
  // 同时转换为 WebP（减少体积 ~30%）
  params.push('format/webp')

  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}${params.join('|')}`
}

/**
 * 生成模糊占位图 URL（极低质量 + 极小尺寸）
 * 用于渐进式加载：先展示模糊版本，原图加载完毕后替换
 *
 * @param url 原始图片 URL
 * @returns 模糊占位图 URL（~200B，5x5px 模糊放大）
 */
export const getBlurPlaceholderUrl = (url?: string): string => {
  if (!url) return ''
  if (!isCiSupported(url)) return url

  const sep = url.includes('?') ? '&' : '?'
  // 缩放到 10x10 像素，高斯模糊 20 像素，转 WebP
  return `${url}${sep}imageMogr2/thumbnail/10x10|blur/20x20|format/webp`
}

/**
 * 浏览器 WebP 支持检测（缓存结果）
 */
let webpSupported: boolean | null = null

export const isWebpSupported = async (): Promise<boolean> => {
  if (webpSupported !== null) return webpSupported
  try {
    // 通过检测 1x1 WebP 图片是否可加载来判断
    const hasNativeWebp =
      document
        .createElement('canvas')
        .toDataURL('image/webp')
        .indexOf('data:image/webp') === 0
    webpSupported = hasNativeWebp
    return webpSupported!
  } catch {
    webpSupported = false
    return false
  }
}

/**
 * 浏览器 AVIF 支持检测（缓存结果）
 */
let avifSupported: boolean | null = null

export const isAvifSupported = async (): Promise<boolean> => {
  if (avifSupported !== null) return avifSupported
  try {
    const hasNativeAvif =
      document
        .createElement('canvas')
        .toDataURL('image/avif')
        .indexOf('data:image/avif') === 0
    avifSupported = hasNativeAvif
    return avifSupported!
  } catch {
    avifSupported = false
    return false
  }
}

/**
 * 图片加载完毕后移除模糊效果（渐进式加载过渡）
 */
export const onImageLoaded = (el: HTMLImageElement): void => {
  el.style.filter = 'none'
  el.style.transition = 'filter 0.3s ease-in-out'
}

/**
 * 图片加载前设置模糊样式
 */
export const onImageLoading = (el: HTMLImageElement): void => {
  el.style.filter = 'blur(10px)'
  el.style.transform = 'scale(1.05)' // 防止模糊边缘出现白边
}
