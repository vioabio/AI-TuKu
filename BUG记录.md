# AI-TuKu 项目 Bug 记录

按开发模块时间顺序整理，每个 Bug 包含：问题现象 → 根因分析 → 解决方案 → 涉及文件。

---

# 一、环境搭建阶段（2026/6/21 ~ 2026/6/23）

## 1.1 MySQL 数据库初始化失败
- **日期**：2026/6/21
- **问题**：MySQL 数据库初始化失败
- **根因**：root 用户密码未正确配置
- **解决**：设置 root 用户密码为 `@087174Ab`
- **参考**：https://blog.csdn.net/flywing521/article/details/149025193

## 1.2 MySQL 服务未启动导致连接拒绝
- **日期**：2026/6/23
- **问题**：创建 MySQL 表格时 `Connection refused: connect`
- **根因**：MySQL 服务未启动
- **解决**：`services.msc` 启动 MySQL 服务

## 1.3 分页查询未生效
- **日期**：2026/6/24
- **问题**：mybatis-plus 3.5.9 分页查询代码未生效
- **根因**：未配置分页插件
- **解决**：配置 `MybatisPlusConfig`，注册 `PaginationInnerInterceptor`，指定 `DbType.MYSQL`
- **涉及文件**：`com.vio.aitukuviobe.config.MybatisPlusConfig`

---

# 二、用户模块（2026/6/24）

## 2.1 用户登录报"系统错误"——实体字段与数据库表不匹配
- **日期**：2026/6/24
- **问题**：注册成功但登录报"登录失败，系统错误"
- **根因**：
  1. `User` 实体定义了 `vipExpireTime`、`vipCode`、`vipNumber` 三个字段
  2. 数据库 `user` 表中没有这些列
  3. 注册时 `selectCount` + `save(INSERT仅含非null字段)` 不触发
  4. 登录时 `selectOne` 生成 `SELECT` 全部字段 → MySQL 报 `Unknown column 'vipExpireTime'` → `BadSqlGrammarException`(RuntimeException) → `GlobalExceptionHandler` 返回 50000 "系统错误"
- **解决**：在 User 实体的三个字段上添加 `@TableField(exist = false)`，告诉 MyBatis-Plus 不对应数据库列
- **涉及文件**：`com.vio.aitukuviobe.model.entity.User`（第55/61/67行）

---

# 三、图片模块（2026/6/25 ~ 2026/6/26）

## 3.1 COS SDK 依赖错误
- **日期**：2026/6/25
- **问题**：`import com.qcloud.cos.*` 找不到类
- **根因**：pom.xml 引入了 `tencentcloud-sdk-java`（全产品 SDK），代码实际使用 `cos_api` 的 API
- **解决**：替换为 `com.qcloud:cos_api:5.6.227`
- **涉及文件**：`pom.xml`

## 3.2 PictureMapper、Picture 实体、PictureService 遗漏提交
- **日期**：2026/6/26
- **问题**：b8af121 提交开发了图片模块（Controller/Config/Manager/DTO/VO），漏掉 Entity/Mapper/Service 核心层
- **解决**：补充提交 `Picture.java`、`PictureMapper.java`、`PictureService.java`、`PictureServiceImpl.java`、`PictureMapper.xml`
- **涉及文件**：上述 5 个文件

## 3.3 Picture 实体字段与数据库表不匹配（预警）
- **日期**：2026/6/26
- **问题**：Picture 实体定义了 `thumbnailUrl`、`picColor`、`spaceId`、`reviewStatus`、`reviewMessage`、`reviewerId`、`reviewTime` 7 个字段，但 create_table.sql 的 picture 表没有
- **影响**：运行时 MyBatis-Plus 生成包含不存在列的 SQL → Unknown column 错误
- **待解决**：需 ALTER TABLE 或 `@TableField(exist = false)`
- **涉及文件**：`model/entity/Picture.java`、`sql/create_table.sql`

## 3.4 PictureMapper.xml 重复扫描导致 Bean 冲突
- **日期**：2026/6/26
- **问题**：启动日志 `Skipping MapperFactoryBean with name 'pictureMapper'... Bean already defined`
- **根因**：`PictureMapper.xml` 放在 `generator/mapper/` 下，与 `@MapperScan` 扫描路径重叠
- **解决**：generator/ 目录下 XML 仅供代码生成器参考
- **涉及文件**：`resources/generator/mapper/PictureMapper.xml`

## 3.5 COS 配置未激活导致启动失败
- **日期**：2026/6/26
- **问题**：启动报错 `Access key cannot be null`
- **根因**：`application.yml` 中 COS 配置被注释，密钥在 `application-local.yml`，但未激活 local profile
- **解决**：`application.yml` 添加 `spring.profiles.active: local`
- **涉及文件**：`application.yml`

## 3.6 PictureController 引用未开发模块代码导致 61 个编译错误
- **日期**：2026/6/26
- **问题**：PictureController 从完整源码复制，包含未开发模块引用（Caffeine、Sa-Token、阿里云AI、以图搜图等），导致 61 个编译错误
- **解决**：删除 9 个未开发方法 + 替换注解 + 简化逻辑，共删除约 250 行代码
- **涉及文件**：`controller/PictureController.java`

---

# 四、图片模块前后端联调（2026/6/27）

## 4.1 首页报"获取数据失败，系统错误"——Picture 实体字段不匹配（3.3 问题重现）
- **日期**：2026/6/27
- **问题**：登录后首页加载图片列表和标签分类报"系统错误"
- **根因**：Picture 实体 7 个字段在数据库表中不存在 → `SELECT` 全部字段 → `Unknown column 'thumbnailUrl'`
- **解决**：更新 `create_table.sql` 补充 7 个字段，对已有数据库执行 `ALTER TABLE`
- **涉及文件**：`sql/create_table.sql`、数据库 `ALTER TABLE`

## 4.2 图片上传大小限制 2MB 过小
- **日期**：2026/6/27
- **问题**：上传图片提示"不能上传超过 2M 的图片"
- **根因**：三处硬编码 2MB 限制（前端 `PictureUpload.vue`、后端 `FileManager.java`、`application.yml`）
- **解决**：三处统一改为 20MB
- **涉及文件**：`PictureUpload.vue`、`FileManager.java`、`application.yml`

## 4.3 COS secretId 重复拼接导致认证失败
- **日期**：2026/6/27
- **问题**：上传图片提示"上传失败"（code=50000）
- **根因**：`application-local.yml` 中 `secretId` 值被意外重复拼接（`AKID...TsYAKID...TsY`），COS 返回 `InvalidAccessKeyId`
- **解决**：修正 `secretId` 值为正确的单一值
- **涉及文件**：`application-local.yml`

## 4.4 COS 万象CI CAM 角色未配置
- **日期**：2026/6/27
- **问题**：上传报 `Qcloud api role not exist, need create role`（403 AccessDenied）
- **根因**：`CosManager.putPictureObject()` 使用 `PicOperations` 调用万象CI图片处理，需要 CAM 中存在 `CI_QCSrole` 服务角色
- **解决**：添加 `CosServiceException` 捕获，若含 "role not exist" 则自动降级为简单 `putObject`（无图片处理）
- **涉及文件**：`manager/CosManager.java`、`manager/FileManager.java`

## 4.5 上传成功但图片不显示——URL 双斜杠+缩略图路径丢失
- **日期**：2026/6/27
- **问题**：上传后图片列表不显示，显示默认占位图
- **根因**：
  1. `FileManager` URL 拼接 `host + "/" + uploadPath`，`uploadPath` 已以 "/" 开头 → 双斜杠
  2. `CosManager` 中 webp/缩略图 key 只用文件名，丢失目录路径
- **解决**：移除多余 "/"；key 改为基于完整路径去掉扩展名后拼接
- **涉及文件**：`manager/FileManager.java`、`manager/CosManager.java`

## 4.6 图片不显示——COS 桶私有导致 403
- **日期**：2026/6/27
- **问题**：修复 URL 后图片仍不显示，浏览器访问返回 403 Forbidden
- **根因**：COS 存储桶默认私有，上传对象未设置 ACL
- **解决**：所有上传路径添加 `putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead)`
- **涉及文件**：`manager/CosManager.java`（三处）

---

# 五、用户传图模块（2026/6/27）

## 5.1 编译错误：程序包 org.jsoup 不存在
- **日期**：2026/6/27
- **问题**：IDE 编译报"程序包org.jsoup不存在"
- **根因**：Maven 无法连接中央仓库（DNS 解析失败 `repo.maven.apache.org`），国内网络限制
- **解决**：
  1. 创建 `~/.m2/settings.xml`，配置阿里云 Maven 镜像
  2. 删除本地缓存失败记录 `~/.m2/repository/org/jsoup`
  3. IDEA 中 Maven → Reload Project
- **涉及文件**：`~/.m2/settings.xml`（新建）

## 5.2 导航菜单"图片管理"无法点击、缺少"批量创建图片"入口
- **日期**：2026/6/27
- **问题**：管理员看不到"图片管理"入口和批量创建图片页面
- **根因**：`GlobalHeader.vue` 导航菜单两个 bug：
  1. "图片管理"使用错误字段名 `path/name` 应为 `key/label`（Ant Design Menu 组件规范）
  2. 缺少"批量创建图片"菜单项
- **解决**：修正字段名 + 新增"批量创建图片"菜单项
- **涉及文件**：`AI-TUKU-VIO-FE/src/components/GlobalHeader.vue`

---

# 六、图片优化模块（2026/6/27）

## 6.1 （无编译/运行错误，此模块为性能优化）
- Redis + Caffeine 两级缓存架构
- 图片上传压缩优化（万象CI ProcessResults）
- 前端 `<a-image>` 组件替换

---

# 七、空间模块（2026/6/27）

## 7.1 （无编译/运行错误，此模块为基础 CRUD）

---

# 八、AI 图片编辑模块（2026/6/28）

## 8.1 编译错误：找不到符号 @Alias 注解
- **日期**：2026/6/28
- **问题**：`CreateOutPaintingTaskRequest.java` 编译报错"找不到符号: 类 Alias"
- **根因**：`org.springframework.web.bind.annotation.Alias` 在 Spring Framework 中不存在
- **解决**：
  1. 删除 `@Alias` 注解
  2. 修正 `@JsonProperty` 值为 snake_case（`x_scale`、`y_scale`、`add_watermark`），匹配阿里云百炼 API 规范
- **涉及文件**：`model/dto/picture/CreateOutPaintingTaskRequest.java`

## 8.2 功能缺陷：普通用户无法查看公共图库图片
- **日期**：2026/6/28
- **问题**：普通用户访问图片详情页提示"无权限"
- **根因**：`PictureController.getPictureVOById` 对所有图片一律调用 `checkPictureAuth`（这是"操作权限校验"，仅适用于编辑/删除），公共图库图片应所有人可查看
- **解决**：仅私有空间图片（`spaceId != null`）时调用 `checkPictureAuth`，公共图库放行
- **涉及文件**：`controller/PictureController.java`

## 8.3 诊断增强：AI 扩图任务失败无法定位原因
- **日期**：2026/6/28
- **问题**：前端显示"扩图任务失败"，后端无日志记录失败原因
- **根因**：`AliYunAiApi` 缺少关键日志（请求体/响应体/FAILED 状态特殊处理）
- **解决**：增加详细日志：
  - 请求前打印完整请求体
  - HTTP 错误时打印状态码
  - 任务 FAILED 时 `log.error` 打印 `taskId` + `output.code` + `output.message`
- **涉及文件**：`manager/AliYunAiApi.java`

## 8.4 用户体验：普通用户创建高等级空间提示不友好
- **日期**：2026/6/28
- **问题**：提示"无权限创建指定级别的空间"过于生硬
- **根因**：异常消息硬编码
- **解决**：改为"暂无权限"（与项目风格一致）
- **涉及文件**：`service/impl/SpaceServiceImpl.java`

## 8.5 限制不一致：私有空间上传图片仍限制 2MB
- **日期**：2026/6/28
- **问题**：之前已提升到 20MB，但两个路径残留 2MB
- **根因**：上传限制分散多处，上次遗漏 `FilePictureUpload` 和 `UrlPictureUpload`
- **解决**：两处 2MB → 20MB
- **涉及文件**：`manager/upload/FilePictureUpload.java`、`manager/upload/UrlPictureUpload.java`

## 8.6 CRITICAL：空间详情页"获取空间详情失败"——Snowflake ID 精度丢失
- **日期**：2026/6/28
- **问题**：点击"我的空间"，页面显示"获取空间详情失败，请求数据不存在"
- **根因链**：
  1. `JsonConfig` 将 Long 序列化为 String（后端→前端方向）
  2. `MySpacePage` 将 ID（String）放入 URL：`/space/1801234567890123456`
  3. `SpaceDetailPage` 通过 `route props` 接收 `props.id = "1801234567890123456"`（String，正确）
  4. **BUG**：`Number("1801234567890123456")` → `1801234567890123400`（超 JS `MAX_SAFE_INTEGER`，末尾 2 位丢失）
  5. 后端用截断的 ID 查询 → `getById` 返回 null → `NOT_FOUND_ERROR` → "请求数据不存在"
- **解决**：
  1. `SpaceDetailPage.vue`：`Number(props.id)` → `props.id`（保持 String，Spring MVC 自动解析）
  2. `typings.d.ts`：ID 参数类型 `number` → `string | number`
- **涉及文件**：`AI-TUKU-VIO-FE/src/pages/SpaceDetailPage.vue`、`AI-TUKU-VIO-FE/src/api/typings.d.ts`

## 8.7 健壮性修复：SpaceServiceImpl.getQueryWrapper NPE 风险
- **日期**：2026/6/28
- **问题**：`sortOrder.equals("ascend")` — 若 `sortField` 不为空但 `sortOrder` 为 null → NPE
- **解决**：`sortOrder.equals("ascend")` → `"ascend".equals(sortOrder)`（常量在前防 NPE）
- **涉及文件**：`service/impl/SpaceServiceImpl.java`

## 8.8 健壮性修复：MySpacePage 缺少错误处理
- **日期**：2026/6/28
- **问题**：API 失败时页面永久显示 loading
- **解决**：添加 else 分支（`message.error` + 跳转 `/add_space`）+ try/catch
- **涉及文件**：`AI-TUKU-VIO-FE/src/pages/MySpacePage.vue`

## 8.9 健壮性修复：SpaceDetailPage 缺少异常捕获
- **日期**：2026/6/28
- **问题**：HTTP 非 2xx 导致未处理的 Promise rejection
- **解决**：`fetchSpaceDetail` 和 `fetchData` 包裹 try/catch
- **涉及文件**：`AI-TUKU-VIO-FE/src/pages/SpaceDetailPage.vue`

## 8.10 代码清理：SpaceDetailPage 未使用的 import
- **日期**：2026/6/28
- **问题**：导入 `useRoute` 但未使用
- **解决**：移除 `import { useRoute } from 'vue-router'` 和 `const route = useRoute()`
- **涉及文件**：`AI-TUKU-VIO-FE/src/pages/SpaceDetailPage.vue`

## 8.11 CRITICAL：私有空间内无法查看已创建的图片——硬编码覆盖查询参数
- **日期**：2026/6/28
- **问题**：私有空间创建图片后列表为空
- **根因链**：
  1. `SpaceDetailPage.fetchData` 构造 `{ spaceId: xxx, nullSpaceId: false }`
  2. 调用 `POST /api/picture/list/page/vo`
  3. Controller 无条件硬编码 `setReviewStatus(1)` + `setNullSpaceId(true)`
  4. SQL 生成矛盾条件：`WHERE reviewStatus=1 AND spaceId IS NULL AND spaceId=xxx` → 0 条记录
- **解决**：仅当 `spaceId == null`（公共图库模式）时才设置默认过滤条件
- **涉及文件**：`controller/PictureController.java`（两处）

## 8.12 CRITICAL：空间内上传图片仍不走空间——spaceId 未传入上传组件
- **日期**：2026/6/28
- **问题**：空间内创建图片后依旧没有数据（上传端 spaceId 未传入）
- **根因链**：
  1. `SpaceDetailPage` 点击"创建图片" → `/add_picture?spaceId=xxx`
  2. `AddPicturePage` 从 `route.query` 提取了 `spaceId`
  3. `PictureUpload` 和 `UrlPictureUpload` 组件均未接收 `:spaceId` prop
  4. 图片 `spaceId` 为 null → 存入公共图库 → 空间内查询不到
- **解决**：
  1. `AddPicturePage.vue`：两个上传组件补充 `:spaceId="spaceId"` + 移除 `Number()` 转换
  2. `PictureUpload.vue` / `UrlPictureUpload.vue`：Props `spaceId?: number` → `spaceId?: string | number`
- **涉及文件**：`AddPicturePage.vue`、`PictureUpload.vue`、`UrlPictureUpload.vue`

---

# 九、图库分析模块（2026/6/28）

## 9.1 （无编译/运行错误，此模块为分析接口+前端图表）

---

# 十、团队空间模块（2026/6/28）

## 10.1 编译错误：找不到符号 ObjectUtil
- **日期**：2026/6/28
- **问题**：`SpaceUserServiceImpl.java` 编译报错"找不到符号: 类 ObjectUtil"
- **根因**：`cn.hutool.core.lang.ObjectUtil` 不存在，Hutool 对象工具类在 `cn.hutool.core.util.ObjUtil`
- **解决**：`import cn.hutool.core.lang.ObjectUtil` → `ObjectUtil.hasEmpty()` → `ObjUtil.hasEmpty()`
- **涉及文件**：`service/impl/SpaceUserServiceImpl.java`

## 10.2 启动失败：循环依赖 SpaceServiceImpl ↔ SpaceUserServiceImpl
- **日期**：2026/6/28
- **问题**：启动报错 `BeanCurrentlyInCreationException`
- **依赖链**：`pictureController → pictureServiceImpl → spaceServiceImpl → spaceUserServiceImpl → spaceServiceImpl`
- **根因**：`SpaceServiceImpl` 注入 `SpaceUserService`（创建团队空间时），`SpaceUserServiceImpl` 注入 `SpaceService`（校验空间存在），形成循环
- **解决**：`SpaceServiceImpl` 对 `SpaceUserService` 添加 `@Lazy` 延迟初始化
- **涉及文件**：`service/impl/SpaceServiceImpl.java`

## 10.3 前端白屏：SpaceUser API 函数被 linter 覆盖删除
- **日期**：2026/6/28
- **问题**：启动前端后浏览器白屏
- **根因**：`spaceController.ts` 被 linter 处理时，5 个 SpaceUser API 函数丢失，`GlobalHeader.vue` 导入失败 → Vue 应用初始化失败
- **解决**：重新在 `spaceController.ts` 末尾添加 5 个 SpaceUser API 函数
- **涉及文件**：`AI-TUKU-VIO-FE/src/api/spaceController.ts`

## 10.4 成员管理页面 404：Vite 代理 /space 规则误拦截 /spaceUserManage
- **日期**：2026/6/28
- **问题**：点击"成员管理"显示 COS NoSuchKey 错误
- **根因**：`SpaceDetailPage` 使用 `:href + target="_blank"` 新窗口打开，Vite 代理 `/space` 规则同时匹配 `/spaceUserManage`，浏览器请求被转发到 COS
- **解决**：
  1. `SpaceDetailPage`：`:href` → `@click + router.push` 同窗口导航
  2. `SpaceUserManagePage`：修复 `Number(route.params.id)` Snowflake 精度丢失
- **涉及文件**：`SpaceDetailPage.vue`、`SpaceUserManagePage.vue`

## 10.5 登录切换账号报"系统错误"：Sa-Token StpKit 未初始化
- **日期**：2026/6/28
- **问题**：普通用户退出后管理员登录报"系统错误"
- **根因**：`StpKit` 中 `new StpLogic("space")` 在无 Sa-Token 配置下内部存储层未正确初始化，`login()` 抛异常未被捕获 → `GlobalExceptionHandler` RuntimeException 处理器返回"系统错误"
- **解决**：`userLogin` 和 `userLogout` 中 Sa-Token 操作包裹 `try-catch`，失败只记 `warn` 日志
- **涉及文件**：`service/impl/UserServiceImpl.java`

---

# 十一、图片协同编辑模块（2026/6/28）

## 11.1 编译错误：找不到符号 com.lmax.disruptor.Disruptor
- **日期**：2026/6/28
- **问题**：Maven 编译报错"找不到符号: 类 Disruptor"
- **根因**：LMAX Disruptor 库中 `Disruptor` 类正确路径是 `com.lmax.disruptor.dsl.Disruptor`（dsl 子包），而非 `com.lmax.disruptor.Disruptor`
- **解决**：修正 import 路径
- **涉及文件**：`manager/websocket/disruptor/PictureEditEventProducer.java`、`PictureEditEventDisruptorConfig.java`

## 11.2 编译错误：无法取消引用int——基本类型上调用 .equals()
- **日期**：2026/6/28
- **问题**：`WsHandshakeInterceptor.java` 第 71 行 `java: 无法取消引用int`
- **根因**：`SpaceTypeEnum.TEAM.getValue()` 返回 `int` 基本类型，无法调用 `.equals()` 方法
- **解决**：`!SpaceTypeEnum.TEAM.getValue().equals(space.getSpaceType())` → `space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()`（基本类型比较，Integer 自动拆箱）
- **涉及文件**：`manager/websocket/WsHandshakeInterceptor.java`

## 11.3 启动失败：循环依赖 PictureEditHandler → Producer → DisruptorConfig → WorkHandler → Handler
- **日期**：2026/6/28
- **问题**：启动报错 `BeanCurrentlyInCreationException`
- **依赖链**：`webSocketConfig → pictureEditHandler → pictureEditEventProducer → pictureEditEventDisruptorConfig → pictureEditEventWorkHandler → pictureEditHandler`
- **根因**：四个 Bean 形成完整循环引用链，Spring Boot 2.6+ 默认禁止
- **解决**：`PictureEditHandler` 对 `PictureEditEventProducer` 添加 `@Lazy` 注解 + `import org.springframework.context.annotation.Lazy`
- **涉及文件**：`manager/websocket/PictureEditHandler.java`

## 11.4 前端白屏：GlobalHeader.vue 导入 listMyTeamSpaceUsingPost 路径错误
- **日期**：2026/6/28
- **问题**：前端白屏，控制台 import 错误
- **根因**：`GlobalHeader.vue` 从 `@/api/spaceController.ts` 导入 `listMyTeamSpaceUsingPost`，但该函数定义在 `@/api/spaceUserController.ts`
- **解决**：修正导入路径
- **涉及文件**：`AI-TUKU-VIO-FE/src/components/GlobalHeader.vue`

---

# 十二、DDD 架构重构（2026/6/28）

## 12.1 构建失败：PictureEditHandler.java 找不到符号
- **问题**：缺少 `UserApplicationService` 导入，字段名 `userService` 与方法体内 `userApplicationService` 不匹配
- **解决**：添加 import + 统一字段名为 `userApplicationService`
- **涉及文件**：`shared/websocket/PictureEditHandler.java`

## 12.2 构建失败：WsHandshakeInterceptor.java 找不到符号
- **问题**：
  1. `SpaceUserAuthManager` 仍引用旧包 `com.vio.aitukuviobe.manager`
  2. 字段名 `userService/pictureService/spaceService` 与 `userApplicationService/pictureApplicationService/spaceApplicationService` 不匹配
- **解决**：修正 import 路径 + 统一字段名
- **涉及文件**：`shared/websocket/WsHandshakeInterceptor.java`

## 12.3 构建失败：CreateOutPaintingTaskResponse 找不到
- **问题**：`CreatePictureOutPaintingTaskRequest` 引用 `CreateOutPaintingTaskRequest.Parameters` 但缺少 import
- **解决**：添加 `import infrastructure.api.aliyunai.model.CreateOutPaintingTaskRequest`
- **涉及文件**：`interfaces/dto/picture/CreatePictureOutPaintingTaskRequest.java`

## 12.4 构建失败：PictureController.java 找不到 CreateOutPaintingTaskResponse/GetOutPaintingTaskResponse
- **问题**：`PictureController` 缺少 AI 模型类的导入（old `model.dto.picture.*` 通配符不再覆盖）
- **解决**：显式添加 import
- **涉及文件**：`interfaces/controller/PictureController.java`

## 12.5 构建失败：FileManager.java 找不到符号
- **问题**：`CosManager` 导入缺失
- **解决**：添加 `import infrastructure.api.CosManager`
- **涉及文件**：`infrastructure/manager/upload/FileManager.java`

## 12.6 构建失败：StpInterfaceImpl 找不到符号（IService 方法调用在 DomainService 上）
- **问题**：`StpInterfaceImpl` 注入 `*DomainService` 但调用 `getById()`、`lambdaQuery()` 等 IService 方法，DomainService 无这些方法
- **解决**：4 个注入全部改为 `*ApplicationService`（继承 IService，有完整 CRUD）
- **涉及文件**：`shared/auth/StpInterfaceImpl.java`

## 12.7 构建失败：SpaceUserAuthManager.lambdaQuery() 不存在
- **问题**：`SpaceUserAuthManager` 注入 `*DomainService` 但调用 `lambdaQuery()`
- **解决**：改为 `SpaceUserApplicationService` + `UserApplicationService`
- **涉及文件**：`shared/auth/SpaceUserAuthManager.java`

## 12.8 构建失败：PictureApplicationServiceImpl 类型推断错误
- **问题**：`this.listByIds(userIdSet)` 中 `this` 是 `ServiceImpl<PictureMapper, Picture>`，用 PictureMapper 查 User 表导致类型不匹配
- **解决**：`this.listByIds(userIdSet)` → `userApplicationService.listByIds(userIdSet)`
- **涉及文件**：`application/service/impl/PictureApplicationServiceImpl.java`

## 12.9 旧包目录与 DDD 新结构共存导致重复类定义
- **问题**：旧 12 个包目录与新的 DDD 结构共存，Spring 扫描到重复 Bean + 旧文件引用已迁移包导致编译失败
- **解决**：删除所有旧包目录（service/model/manager/constant/filter/annotation/aop/common/config/exception/mapper/controller）
- **涉及**：12 个旧包目录

---

# 十三、项目部署上线（2026/6/28）

## 13.1 部署命令错误："Unknown command: build"
- **问题**：Cloudflare Pages 构建报 `Unknown command: "build"`
- **根因**：部署命令写的是 `npm build`，缺少 `run`
- **解决**：`npm build` → `npm run build`
- **涉及**：Cloudflare Pages 项目设置

## 13.2 点击登录无反应
- **问题**：Cloudflare Pages 部署后页面正常但点击登录无响应
- **根因**：`request.ts` 中 `baseURL` 硬编码为 `http://localhost:8123`，生产环境浏览器仍请求 localhost
- **解决**：`baseURL` 改为 `import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123'`，通过生产环境变量指定后端地址
- **涉及文件**：`src/request.ts`、`src/utils/PictureEditWebSocket.ts`

---

# 附录：Bug 类型统计

| 类型 | 数量 | 涉及模块 |
|------|------|----------|
| 编译错误（import/包路径/注解） | 14 | AI编辑/协同编辑/DDD重构/用户传图 |
| 数据库字段不匹配 | 2 | 用户模块/图片模块 |
| COS 配置/权限 | 4 | 图片模块前后端联调 |
| 前端路由/菜单/导航 | 4 | 图片模块/团队空间/协同编辑 |
| 循环依赖 | 2 | 团队空间/协同编辑 |
| Snowflake ID 精度丢失 | 3 | AI图片编辑模块 |
| 权限/可见性逻辑缺陷 | 2 | AI图片编辑模块 |
| 健壮性（NPE/异常处理） | 4 | AI图片编辑模块 |
| 部署配置 | 2 | 项目部署上线 |
| DDD 重构 | 8 | DDD架构重构 |
| **合计** | **45** | |
