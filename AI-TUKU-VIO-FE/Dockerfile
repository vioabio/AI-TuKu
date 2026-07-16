# ============================================================
# AI-TuKu 前端 Dockerfile — 多阶段构建
# 阶段 1: Node.js 构建
# 阶段 2: Nginx 静态服务
# ============================================================

# ---------- 阶段 1: 构建 ----------
FROM node:22-alpine AS builder

WORKDIR /build

# 先复制依赖文件（利用 Docker 缓存层）
COPY package*.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build-only

# ---------- 阶段 2: Nginx 托管 ----------
FROM nginx:1.27-alpine

# 复制构建产物到 Nginx 目录
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]