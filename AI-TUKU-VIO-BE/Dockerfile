# ============================================================
# AI-TuKu 后端 Dockerfile — 多阶段构建
# 阶段 1: Maven 编译打包
# 阶段 2: JRE 瘦身运行
# ============================================================

# ---------- 阶段 1: 构建 ----------
FROM maven:3.8.6-openjdk-11-slim AS builder

WORKDIR /build

# 先复制 pom.xml 单独下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# 复制源码并构建
COPY src ./src
RUN mvn package -DskipTests -B -q

# ---------- 阶段 2: 运行 ----------
FROM openjdk:11-jre-slim

WORKDIR /app

# 创建非 root 用户（安全最佳实践）
RUN groupadd -r aituku && useradd -r -g aituku aituku

# 复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# 切换到非 root 用户
USER aituku

# 应用端口
EXPOSE 8123

# JVM 参数（可根据服务器内存调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD java -jar /app/app.jar health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]