#!/bin/bash
# ============================================================
# AI-TuKu 服务器端部署脚本
# 由 GitHub Actions Webhook 触发执行
#
# 部署方式:
#   1. 在服务器上配置一个简单的 Webhook 接收器
#   2. Webhook 收到 GitHub 通知后执行此脚本
#
# 使用:
#   chmod +x deploy.sh
#   ./deploy.sh [image_tag]
# ============================================================
set -e

cd "$(dirname "$0")/.."

IMAGE_TAG="${1:-latest}"
COMPOSE_FILE="docker-compose.yml"

echo "============================================"
echo "  AI-TuKu 部署开始"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  镜像标签: ${IMAGE_TAG}"
echo "============================================"

# 拉取最新镜像
echo "[1/4] 拉取最新镜像..."
export BACKEND_IMAGE_TAG="${IMAGE_TAG}"
export FRONTEND_IMAGE_TAG="${IMAGE_TAG}"
docker compose -f "${COMPOSE_FILE}" pull

# 重启服务
echo "[2/4] 滚动更新服务..."
docker compose -f "${COMPOSE_FILE}" up -d --remove-orphans

# 等待健康检查
echo "[3/4] 等待服务健康检查..."
sleep 10
docker compose -f "${COMPOSE_FILE}" ps

# 清理旧镜像
echo "[4/4] 清理旧镜像..."
docker image prune -f

echo "============================================"
echo "  AI-TuKu 部署完成"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"
