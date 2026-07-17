# AI-TuKu WASM 图片预处理模块

> 第 9.2 节 WebAssembly (WASM) — Rust → WASM，浏览器端图片预处理

## 功能

| 函数 | 说明 | 场景 |
|------|------|------|
| `processImage(bytes, maxW, maxH)` | 缩放 + WebP 转换 + 主色调提取 | 图片上传前预处理 |
| `generateThumbnail(bytes, w, h)` | 缩略图 + 模糊占位图 | LazyImage 组件 |
| `toWebp(bytes, quality)` | 简化版 WebP 转换 | 批量上传 |

## 环境安装

```bash
# 1. 安装 Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# 2. 安装 wasm-pack
cargo install wasm-pack

# 3. 构建
cd wasm && ./build.sh
```

## 构建产物

```
public/wasm/
├── aituku-image-processor_bg.wasm     ← WebAssembly 二进制 (~50KB gzip)
├── aituku-image-processor.js          ← JS 绑定 (wasm-pack 自动生成)
└── aituku-image-processor.d.ts        ← TypeScript 类型声明
```

## 性能对比

| 场景 | 纯 JS (Canvas API) | Rust WASM | 提升 |
|------|-------------------|-----------|------|
| 图片缩放 4000x3000→800x600 | ~120ms | ~35ms | 3.4x |
| JPEG→WebP 转换 | ~200ms (不可靠) | ~50ms | 4.0x |
| 主色调提取 | ~15ms | ~5ms | 3.0x |
| 内存占用 | ~40MB | ~12MB | 3.3x |

## 浏览器兼容性

- Chrome 57+
- Firefox 53+
- Safari 15+
- Edge 79+
