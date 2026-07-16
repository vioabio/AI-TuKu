package com.vio.aitukuviobe.infrastructure.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解
 * <p>
 * 使用令牌桶算法（Bucket4j），超过限流阈值时返回 HTTP 429 Too Many Requests。
 * <p>
 * 使用示例：
 * <pre>
 * // 每秒最多 10 次请求
 * &#64;RateLimit(rate = 10, interval = 1, timeUnit = TimeUnit.SECONDS)
 * &#64;PostMapping("/search")
 * public BaseResponse<?> search() { ... }
 *
 * // 每分钟最多 3 次登录请求
 * &#64;RateLimit(rate = 3, interval = 1, timeUnit = TimeUnit.MINUTES)
 * &#64;PostMapping("/login")
 * public BaseResponse<?> login() { ... }
 *
 * // 每秒最多 20 次上传请求
 * &#64;RateLimit(rate = 20, interval = 1, timeUnit = TimeUnit.SECONDS)
 * &#64;PostMapping("/upload")
 * public BaseResponse<?> upload() { ... }
 * </pre>
 *
 * @author vivin
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流速率（令牌数） */
    int rate() default 10;

    /** 时间间隔 */
    int interval() default 1;

    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /** 限流维度 key（默认基于 IP + 方法名，也可指定自定义 key） */
    String key() default "";

    /** 限流提示消息 */
    String message() default "请求过于频繁，请稍后再试";
}
