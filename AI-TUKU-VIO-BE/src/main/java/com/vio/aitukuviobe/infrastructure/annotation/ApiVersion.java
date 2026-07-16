package com.vio.aitukuviobe.infrastructure.annotation;

import java.lang.annotation.*;

/**
 * API 版本标记注解
 * <p>
 * 用于标记 Controller 或方法的 API 版本号，配合版本路由自动映射。
 * 同时支持 URL 路径版本化：/api/v1/user/... 和 /api/v2/user/...
 * <p>
 * 版本策略：
 * <ul>
 *   <li>v1（当前默认）：所有现有接口</li>
 *   <li>v2+：后续重大变更时新增，与 v1 并存 6 个月过渡期</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * // 标记整个 Controller 为 v1
 * &#64;ApiVersion("v1")
 * &#64;RestController
 * public class UserController { ... }
 *
 * // 单独标记某个方法为 v2（覆盖类级别）
 * &#64;ApiVersion("v2")
 * &#64;GetMapping("/profile")
 * public BaseResponse<?> getProfileV2() { ... }
 * </pre>
 *
 * @author vivin
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {

    /** API 版本号，如 "v1"、"v2" */
    String value() default "v1";

    /** 是否为废弃版本（标记后前端会收到 Deprecation 警告头） */
    boolean deprecated() default false;

    /** 废弃说明 */
    String deprecatedMessage() default "";

    /** 计划移除日期 */
    String sunsetDate() default "";
}
