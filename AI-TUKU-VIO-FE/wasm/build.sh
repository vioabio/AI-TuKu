#!/bin/bash
# ============================================================
# AI-TuKu WASM 构建脚本 (第 9.2 节)
#
# 编译 Rust → WebAssembly 并输出到前端 public/ 目录
# 使用方式: cd wasm && ./build.sh
# ============================================================
set -euo pipefail

echo "============================================"
echo "  AI-TuKu WASM 图片处理器 — 构建"
echo "============================================"

# 1. 检查环境
if ! command -v wasm-pack &> /dev/null; then
    echo "[ERROR] wasm-pack 未安装，请运行: cargo install wasm-pack"
    exit 1
fi

if ! command -v cargo &> /dev/null; then
    echo "[ERROR] Rust/Cargo 未安装，请访问 https://rustup.rs"
    exit 1
fi

echo "[OK] Rust $(rustc --version)"
echo "[OK] wasm-pack $(wasm-pack --version)"

# 2. 编译（release 模式 — 体积优化）
echo ""
echo "[BUILD] 编译 Rust → WASM (release)..."
wasm-pack build \
    --target web \
    --out-dir ../public/wasm \
    --out-name aituku-image-processor \
    --release

# 3. 输出结果
echo ""
echo "============================================"
echo "  构建完成!"
echo "============================================"
echo "  输出目录: public/wasm/"
ls -lh ../public/wasm/aituku-image-processor* 2>/dev/null || echo "  (文件列表获取失败)"
echo ""
echo "  前端使用方式:"
echo "    import init, { processImage, generateThumbnail } from '@/utils/wasmLoader'"
echo ""
echo "  调用示例:"
echo "    await init()"
echo "    const result = await processImage(fileBytes, 1920, 1080)"
echo "============================================"
