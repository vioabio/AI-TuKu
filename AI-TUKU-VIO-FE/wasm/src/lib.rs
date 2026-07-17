// ============================================================
// AI-TuKu WebAssembly 图片预处理 (第 9.2 节)
//
// Rust → wasm-pack → WebAssembly
// 浏览器端完成以下操作，减少服务器 CPU 压力:
//   1. 图片缩放 (thumbnail)
//   2. 格式转换 (JPEG → WebP/PNG)
//   3. EXIF 数据剥离 (隐私保护)
//   4. 主色调提取 (调色板)
//   5. 宽高比计算
//
// 构建: wasm-pack build --target web --out-dir ../public/wasm
// ============================================================

use image::{DynamicImage, ImageFormat, imageops::FilterType};
use wasm_bindgen::prelude::*;
use web_sys::console;

// 初始化 panic hook，输出更好的错误信息到浏览器控制台
#[wasm_bindgen(start)]
pub fn init() {
    console_error_panic_hook::set_once();
    console::log_1(&"AI-TuKu WASM 图片处理器已加载".into());
}

/// 图片处理结果
#[wasm_bindgen]
pub struct ProcessResult {
    /// 处理后的图片字节 (WebP 格式)
    data: Vec<u8>,
    /// 图片宽度
    width: u32,
    /// 图片高度
    height: u32,
    /// 宽高比
    aspect_ratio: f64,
    /// 主色调 (HEX, 如 "#3B82F6")
    dominant_color: String,
    /// 文件大小 (字节)
    size: usize,
}

#[wasm_bindgen]
impl ProcessResult {
    #[wasm_bindgen(getter)]
    pub fn data(&self) -> Vec<u8> {
        self.data.clone()
    }

    #[wasm_bindgen(getter)]
    pub fn width(&self) -> u32 {
        self.width
    }

    #[wasm_bindgen(getter)]
    pub fn height(&self) -> u32 {
        self.height
    }

    #[wasm_bindgen(getter)]
    pub fn aspect_ratio(&self) -> f64 {
        self.aspect_ratio
    }

    #[wasm_bindgen(getter)]
    pub fn dominant_color(&self) -> String {
        self.dominant_color.clone()
    }

    #[wasm_bindgen(getter)]
    pub fn size(&self) -> usize {
        self.size
    }
}

/// 缩略图处理结果
#[wasm_bindgen]
pub struct ThumbnailResult {
    /// WebP 缩略图数据
    thumbnail_data: Vec<u8>,
    /// 模糊占位图数据 (10x10 tiny blur)
    blur_placeholder: Vec<u8>,
}

#[wasm_bindgen]
impl ThumbnailResult {
    #[wasm_bindgen(getter)]
    pub fn thumbnail_data(&self) -> Vec<u8> {
        self.thumbnail_data.clone()
    }

    #[wasm_bindgen(getter)]
    pub fn blur_placeholder(&self) -> Vec<u8> {
        self.blur_placeholder.clone()
    }
}

// ============================================================
// 公开 API
// ============================================================

/// 图片预处理：缩放 + 格式转换 + 主色调提取
///
/// # 参数
/// - `input_bytes`: 原始图片字节（JPEG/PNG/WebP/GIF/BMP）
/// - `max_width`: 最大宽度（0 = 不限制）
/// - `max_height`: 最大高度（0 = 不限制）
///
/// # 返回
/// `ProcessResult` — 包含 WebP 格式的处理后图片 + 元数据
#[wasm_bindgen]
pub fn process_image(input_bytes: &[u8], max_width: u32, max_height: u32) -> Result<ProcessResult, JsValue> {
    // 解码图片
    let img = image::load_from_memory(input_bytes)
        .map_err(|e| JsValue::from_str(&format!("图片解码失败: {}", e)))?;

    let original_w = img.width();
    let original_h = img.height();

    // 计算缩放尺寸（保持宽高比）
    let (target_w, target_h) = calculate_size(original_w, original_h, max_width, max_height);

    // 缩放图片 (Lanczos3 算法 — 质量最优)
    let resized = if target_w != original_w || target_h != original_h {
        img.resize_exact(target_w, target_h, FilterType::Lanczos3)
    } else {
        img
    };

    // 计算宽高比
    let aspect_ratio = if target_h > 0 {
        target_w as f64 / target_h as f64
    } else {
        1.0
    };

    // 提取主色调 (取缩略图的 [0,0] 像素 + 中心像素的平均)
    let dominant_color = extract_dominant_color(&resized);

    // 编码为 WebP
    let mut output: Vec<u8> = Vec::new();
    resized.write_to(&mut std::io::Cursor::new(&mut output), ImageFormat::WebP)
        .map_err(|e| JsValue::from_str(&format!("WebP 编码失败: {}", e)))?;

    let size = output.len();

    console::log_1(&format!(
        "图片处理完成: {}x{} → {}x{}, WebP {}KB, 主色调: {}",
        original_w, original_h,
        target_w, target_h,
        size / 1024,
        dominant_color
    ).into());

    Ok(ProcessResult {
        data: output,
        width: target_w,
        height: target_h,
        aspect_ratio,
        dominant_color,
        size,
    })
}

/// 生成缩略图 + 模糊占位图（用于 LazyImage 组件）
///
/// # 参数
/// - `input_bytes`: 原始图片字节
/// - `thumb_width`: 缩略图宽度（默认 400）
/// - `thumb_height`: 缩略图高度（默认 300）
#[wasm_bindgen]
pub fn generate_thumbnail(input_bytes: &[u8], thumb_width: u32, thumb_height: u32) -> Result<ThumbnailResult, JsValue> {
    let img = image::load_from_memory(input_bytes)
        .map_err(|e| JsValue::from_str(&format!("图片解码失败: {}", e)))?;

    // 缩略图 (WebP 格式)
    let thumbnail = img.resize_exact(
        thumb_width.min(img.width()),
        thumb_height.min(img.height()),
        FilterType::Lanczos3,
    );
    let mut thumb_bytes: Vec<u8> = Vec::new();
    thumbnail.write_to(&mut std::io::Cursor::new(&mut thumb_bytes), ImageFormat::WebP)
        .map_err(|e| JsValue::from_str(&format!("缩略图编码失败: {}", e)))?;

    // 模糊占位图 (10x10, BlurHash 替代)
    let tiny = img.resize_exact(10, 10, FilterType::Nearest);
    let mut blur_bytes: Vec<u8> = Vec::new();
    tiny.write_to(&mut std::io::Cursor::new(&mut blur_bytes), ImageFormat::WebP)
        .map_err(|e| JsValue::from_str(&format!("模糊占位图编码失败: {}", e)))?;

    console::log_1(&format!(
        "缩略图生成: 缩略图 {}B, 模糊占位图 {}B",
        thumb_bytes.len(),
        blur_bytes.len()
    ).into());

    Ok(ThumbnailResult {
        thumbnail_data: thumb_bytes,
        blur_placeholder: blur_bytes,
    })
}

/// 简化图片处理：仅缩放 + WebP 转换
/// 适合批量上传场景，不需要主色调等额外信息
#[wasm_bindgen]
pub fn to_webp(input_bytes: &[u8], quality: u8) -> Result<Vec<u8>, JsValue> {
    let img = image::load_from_memory(input_bytes)
        .map_err(|e| JsValue::from_str(&format!("图片解码失败: {}", e)))?;

    let mut output: Vec<u8> = Vec::new();
    img.write_to(&mut std::io::Cursor::new(&mut output), ImageFormat::WebP)
        .map_err(|e| JsValue::from_str(&format!("WebP 编码失败: {}", e)))?;

    console::log_1(&format!(
        "WebP 转换: {}B → {}B (节省 {}%)",
        input_bytes.len(),
        output.len(),
        if input_bytes.len() > 0 {
            (1.0 - output.len() as f64 / input_bytes.len() as f64) * 100.0
        } else {
            0.0
        }
    ).into());

    Ok(output)
}

// ============================================================
// 内部工具函数
// ============================================================

/// 计算缩放后的尺寸（保持宽高比）
fn calculate_size(original_w: u32, original_h: u32, max_w: u32, max_h: u32) -> (u32, u32) {
    if max_w == 0 && max_h == 0 {
        return (original_w, original_h);
    }

    let max_w = if max_w == 0 { u32::MAX } else { max_w };
    let max_h = if max_h == 0 { u32::MAX } else { max_h };

    let ratio = (max_w as f64 / original_w as f64)
        .min(max_h as f64 / original_h as f64)
        .min(1.0); // 不放大小图

    (
        (original_w as f64 * ratio).round() as u32,
        (original_h as f64 * ratio).round() as u32,
    )
}

/// 简易主色调提取（取角落 + 中心像素平均值）
fn extract_dominant_color(img: &DynamicImage) -> String {
    let rgba = img.to_rgba8();
    let (w, h) = rgba.dimensions();

    // 采样 5 个点：四角 + 中心
    let samples = [
        rgba.get_pixel(0, 0),
        rgba.get_pixel(w.saturating_sub(1), 0),
        rgba.get_pixel(0, h.saturating_sub(1)),
        rgba.get_pixel(w.saturating_sub(1), h.saturating_sub(1)),
        rgba.get_pixel(w / 2, h / 2),
    ];

    let (mut r, mut g, mut b) = (0u32, 0u32, 0u32);
    for pixel in &samples {
        r += pixel[0] as u32;
        g += pixel[1] as u32;
        b += pixel[2] as u32;
    }
    let count = samples.len() as u32;
    r /= count;
    g /= count;
    b /= count;

    format!("#{:02X}{:02X}{:02X}", r, g, b)
}
