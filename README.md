<p align="center">
  <img src="AI-TUKU-VIO-FE/public/logo.svg" alt="AI-TuKu Logo" width="120" />
</p>

<h1 align="center">AI-TuKu 智能云图库</h1>

<p align="center">
  <strong>AI 驱动的智能图片管理与团队协作平台</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="Java 17" />
  <img src="https://img.shields.io/badge/Vue-3.x-4FC08D?logo=vuedotjs" alt="Vue 3" />
  <img src="https://img.shields.io/badge/TypeScript-5.6-3178C6?logo=typescript" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker" alt="Docker" />
  <img src="https://img.shields.io/badge/LangChain4j-1.0--beta2-00B265?logo=openai" alt="LangChain4j" />
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="License" />
</p>

---

## 📖 项目简介

AI-TuKu 是一个从零搭建的 **AI 驱动的智能图片管理与团队协作平台**，支持个人图库、团队共享图库和公共图库三种模式。集成 AI 智能扩图、自动标签提取、NSFW 内容审核和自然语言搜索意图解析，提供 6 维度数据分析看板，支持 WebSocket 实时协同编辑。

> 🚀 **已上线**：一键 Docker Compose 部署 + GitHub Actions CI/CD 自动化流水线

## ✨ 核心特性

### 🖼️ 图片管理
- **三种空间模式**：公共图库（所有人可见）| 私有空间（个人）| 团队空间（多人协作）
- **批量上传**：文件上传 + URL 上传 + 必应图片批量抓取
- **图片编辑**：裁剪（vue-cropper）、旋转缩放、名称/标签/分类编辑
- **万象 CI 优化**：自动 WebP 压缩 + 缩略图生成，CI 不可用时自动降级

### 🤖 AI 能力（LangChain4j 驱动）
- **智能扩图**：AI 驱动的图片外扩（Outpainting），异步任务 + 前端轮询
- **自动标签**：AI 识别图片内容并生成标签
- **NSFW 审核**：上传图片自动内容审核，安全优先降级策略
- **搜索意图解析**：自然语言搜索词 → 结构化查询参数
- **模型热切换**：DashScope ⇄ OpenAI ⇄ OneAPI，改配置即可

### 🔍 全文搜索（Elasticsearch）
- **IK 中文分词** + **拼音插件** + **Fuzziness 拼写纠错**
- 多字段加权（name³ / introduction² / tags¹）
- ES 优先 + MySQL LIKE 自动降级，保证搜索 100% 可用

### 👥 团队协作
- **RBAC 权限体系**：viewer / editor / admin 三种空间角色 + JSON 配置化权限
- **实时协同编辑**：WebSocket + Disruptor 环形缓冲区异步处理，排除式广播
- **OAuth2 第三方登录**：GitHub / Google / 微信 / Gitee

### 📊 数据分析
- 6 维度分析看板：空间使用量 / 大小 / 分类 / 标签 / 用户 / 排行
- ECharts 图表可视化

### 🛡️ 安全纵深防御
- 6 层防线：CORS 白名单 → XSS 清洗 → Bucket4j 令牌桶限流 → Sa-Token RBAC → 参数校验 → 防 SQL 注入
- 9 类 HTTP 安全响应头（CSP / HSTS / X-Frame-Options / COOP / CORP 等）
- OWASP 依赖扫描（CVSS ≥ 8 阻断构建）

### 🐳 部署与运维
- **Docker Compose** 一键编排（MySQL + Redis + 后端 + 前端）
- **GitHub Actions CI/CD**：PR 自动测试 → 构建镜像 → 推送 ghcr.io → Webhook 部署
- **PWA** 离线支持（Workbox 4 种缓存策略）
- **可观测性**：Actuator + Micrometer + Prometheus + Zipkin 链路追踪

---

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite 6)                  │
│  Ant Design Vue 4 / Pinia / ECharts / PWA / Rust-WASM   │
│               Nginx 反向代理 (生产)                        │
├─────────────────────────────────────────────────────────┤
│              RESTful HTTP / GraphQL / WebSocket           │
├─────────────────────────────────────────────────────────┤
│               后端 DDD 四层架构 (Spring Boot)              │
│  interfaces → application → domain ← infrastructure     │
│  ┌──────────────────────────────────────────────────┐   │
│  │ AI 层: LangChain4j @AiService (4 种 AI 能力)      │   │
│  │ 事件层: Kafka Streams (事件溯源 + 审计日志)        │   │
│  │ 搜索层: Elasticsearch (IK + 拼音 + 纠错)           │   │
│  │ 安全层: Sa-Token + OAuth2 + Bucket4j + XSS        │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│          MySQL 8.0  Redis 7  Elasticsearch  Kafka        │
│          Caffeine  Disruptor  ShardingSphere (就绪)       │
├─────────────────────────────────────────────────────────┤
│       腾讯云 COS + 万象CI  │  阿里云百炼 / OpenAI         │
└─────────────────────────────────────────────────────────┘
```

### 技术栈一览

| 层级 | 技术 |
|------|------|
| **前端** | Vue 3 + TypeScript 5.6 + Vite 6 + Ant Design Vue 4 + Pinia + ECharts + PWA |
| **后端** | Spring Boot 2.7.6 + Java 17 + Maven + DDD 四层架构 |
| **AI** | LangChain4j 1.0-beta2（DashScope / OpenAI / OneAPI） |
| **ORM** | MyBatis-Plus 3.5.15 |
| **鉴权** | Sa-Token 1.39 + JustAuth OAuth2 + Session-Cookie |
| **搜索** | Elasticsearch（IK 分词 + 拼音插件 + Fuzziness 纠错） |
| **缓存** | Caffeine 本地缓存 + Redis 分布式缓存（两级） |
| **并发** | LMAX Disruptor 3.4 + Kafka Streams |
| **限流** | Bucket4j 7.6（令牌桶算法） |
| **存储** | 腾讯云 COS 5.6 + 万象 CI（WebP 压缩/缩略图） |
| **监控** | Actuator + Micrometer + Prometheus + Zipkin |
| **测试** | JUnit 5 + Testcontainers + AssertJ + JaCoCo + Vitest + Playwright + K6 |
| **部署** | Docker Compose + GitHub Actions CI/CD + ghcr.io |

### 设计模式（11 种）

`DDD 四层架构` `Repository（仓储）` `模板方法` `门面（Facade）` `AOP 切面` `策略` `Builder` `生产者-消费者` `观察者` `Read-Through` `依赖倒置`

---

## 🚀 快速开始

### 前置条件

- [Docker](https://docs.docker.com/get-docker/) & [Docker Compose](https://docs.docker.com/compose/install/) v2.0+
- 腾讯云 COS 存储桶（[注册](https://cloud.tencent.com/product/cos)）
- 阿里云百炼 API Key（[获取](https://bailian.console.aliyun.com/)）或 OpenAI API Key

### 一键启动

```bash
# 1. 克隆项目
git clone https://github.com/your-username/AI-TuKu.git
cd AI-TuKu

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入 COS/AI 相关密钥

# 3. 一键启动全部服务
docker compose up -d

# 4. 查看运行状态
docker compose ps

# 5. 访问
# 前端: http://localhost
# 后端 API: http://localhost:8123/api
# Knife4j 接口文档: http://localhost:8123/api/doc.html
# GraphiQL: http://localhost:8123/api/graphiql
```

### 本地开发

```bash
# 后端 (需要 JDK 17 + Maven)
cd AI-TUKU-VIO-BE
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 编辑 application-local.yml 填入本地数据库和密钥配置
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 前端 (需要 Node.js 22+)
cd AI-TUKU-VIO-FE
npm install
npm run dev
# 访问 http://localhost:5173
```

---

## 📁 项目结构

```
AI-TuKu/
├── AI-TUKU-VIO-BE/                  # 后端 Spring Boot 项目
│   ├── src/main/java/com/vio/aitukuviobe/
│   │   ├── interfaces/              # 接口层 (Controller / DTO / VO / GraphQL)
│   │   ├── application/service/     # 应用层 (业务流程编排)
│   │   ├── domain/                  # 领域层 (Entity / Repository / DomainService)
│   │   │   ├── picture/             # 图片领域（含 AI 服务接口）
│   │   │   ├── space/               # 空间领域
│   │   │   └── user/                # 用户领域
│   │   ├── infrastructure/          # 基础设施层 (Mapper / COS / ES / AI / 限流)
│   │   └── shared/                  # 横切层 (Auth / WebSocket / Kafka / Sharding)
│   ├── src/main/resources/
│   │   ├── graphql/                 # GraphQL Schema
│   │   └── sql/                     # 数据库脚本 + 诊断 SQL
│   └── Dockerfile
│
├── AI-TUKU-VIO-FE/                  # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                     # API 调用层 (@umijs/openapi 自动生成)
│   │   ├── components/              # 通用组件 (10 个)
│   │   ├── pages/                   # 页面组件 (14 个)
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── utils/                   # 工具 (WebSocket / 图片优化 / WASM)
│   │   └── router/                  # Vue Router 配置
│   ├── wasm/                        # Rust/WASM 图片处理引擎
│   ├── k6/                          # K6 性能测试脚本
│   ├── e2e/                         # Playwright E2E 测试
│   └── Dockerfile + nginx.conf
│
├── docker-compose.yml               # 一键编排 (MySQL + Redis + BE + FE)
├── .env.example                     # 环境变量模板
├── .github/workflows/               # CI/CD 流水线
│   ├── test.yml                     # 测试流水线 (6 个 Job)
│   └── deploy.yml                   # 部署流水线
└── 开发步骤/                        # 14 阶段开发指南（本地文档）
```

---

## 📊 项目数据

| 指标 | 数据 |
|------|------|
| 开发周期 | 2025.09 ~ 2026.07（11 个月持续迭代） |
| 后端文件 | ~200 个 Java 文件（DDD 四层 + AI 层 + 事件层 + GraphQL 层） |
| 前端页面 | 14 个页面 + 10 个通用组件 |
| API 接口 | 40+ RESTful + GraphQL 端点 + WebSocket |
| AI 能力 | 4 种（扩图 / 标签 / 审核 / 搜索意图）+ 3 模型后端 |
| 数据库表 | 7 张（4 业务表 + 2 归档表 + 1 审计日志表） |
| 中间件 | MySQL + Redis + Elasticsearch + Kafka + ShardingSphere |
| 设计模式 | 11 种 |

---

## 🔒 安全特性

```
HTTP 请求
  → 第 1 层: CORS 白名单校验
  → 第 2 层: XSS 清洗 (Jsoup.clean)
  → 第 3 层: Bucket4j 令牌桶限流 (注册 3次/分 | 上传 20次/分 | 搜索 10次/秒)
  → 第 4 层: Sa-Token RBAC 鉴权 + @AuthCheck 注解
  → 第 5 层: Controller 参数校验
  → 第 6 层: MyBatis-Plus #{} 防 SQL 注入
```

- **OAuth2 第三方登录**：GitHub / Google / 微信 / Gitee（JustAuth + state 防 CSRF）
- **HTTP 安全头**：CSP / HSTS / X-Frame-Options / X-Content-Type-Options / COOP / CORP / Referrer-Policy
- **OWASP 依赖扫描**：CI 自动检测，CVSS ≥ 8 阻断构建

---

## 🧪 测试体系

```
      ╱ E2E ╲           Playwright (用户旅程)
     ╱ K6 压测 ╲         K6 30s 冒烟 (CI) + 11min 全量 (手动)
    ╱ API 契约测试 ╲     TestRestTemplate + AssertJ
   ╱ Repository 集成 ╲   Testcontainers (真实 MySQL/Redis)
  ╱ DomainService 单测 ╲ Mockito + AssertJ
 ╱ Entity 纯单测       ╲ JUnit 5
```

| 测试类型 | 工具 | 覆盖范围 |
|---------|------|---------|
| 单元测试 | JUnit 5 + Mockito | DomainService / Entity |
| 集成测试 | Testcontainers（MySQL/Redis） | Repository / Mapper |
| 前端测试 | Vitest | Pinia Store / 组件 |
| E2E 测试 | Playwright（Chromium） | 完整用户旅程 |
| 性能测试 | K6（阶梯 50→500 VU） | API 压力测试 |
| 安全扫描 | OWASP Dependency-Check | 依赖漏洞检测 |
| 覆盖率 | JaCoCo（70% 阈值） | 后端代码覆盖 |

---

## 🤖 AI 能力详解

### LangChain4j 声明式 AI Service

```java
// 标签提取 — 一行接口定义，无需手写 HTTP
@AiService
public interface PictureAiService {
    @UserMessage("为这张图片生成 3-10 个标签: {{imageUrl}}")
    Result<TagsResult> generateTags(@V("imageUrl") String imageUrl);
}

// NSFW 审核 — Structured Output 自动反序列化
@AiService
public interface ModerationAiService {
    @UserMessage("审核这张图片是否包含违规内容: {{imageUrl}}")
    Result<ModerationResult> moderate(@V("imageUrl") String imageUrl);
}
```

**对比手写 HTTP**：5 个 AI 功能从 1295 行胶水代码 → ~150 行声明式接口，代码量减少 88%。

### 模型热切换

```yaml
# DashScope (默认)
aituku.ai.provider: dashscope
# 切换到 OpenAI
aituku.ai.provider: openai
aituku.ai.openai-base-url: https://api.openai.com/v1
```

---

## 📄 API 文档

启动后端后访问：

- **Knife4j / Swagger**：http://localhost:8123/api/doc.html
- **GraphiQL IDE**：http://localhost:8123/api/graphiql
- **Prometheus Metrics**：http://localhost:8123/api/actuator/prometheus
- **Health Check**：http://localhost:8123/api/actuator/health

---

## 🤝 贡献指南

本项目为个人全栈独立开发项目，但欢迎提 Issue 和 PR。

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

提交前请确保：
- 后端：`mvn test` 通过
- 前端：`npm run test:unit` 通过
- JaCoCo 覆盖率 ≥ 70%

---

## 📝 License

MIT License © 2025-2026 VIO

---

## 🙏 致谢

- [LangChain4j](https://github.com/langchain4j/langchain4j) — Java AI 框架
- [Sa-Token](https://sa-token.dev33.cn/) — 轻量级鉴权框架
- [JustAuth](https://www.justauth.cn/) — 第三方登录 SDK
- [Ant Design Vue](https://antdv.com/) — 企业级 UI 组件库
- [MyBatis-Plus](https://baomidou.com/) — 增强版 MyBatis

---

<p align="center">
  <sub>Built with ❤️ by VIO | 全栈独立开发</sub>
</p>
