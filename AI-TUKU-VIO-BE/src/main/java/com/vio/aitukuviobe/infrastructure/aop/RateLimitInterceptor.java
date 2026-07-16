package com.vio.aitukuviobe.infrastructure.aop;

import cn.hutool.core.util.StrUtil;
import com.vio.aitukuviobe.infrastructure.annotation.RateLimit;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口限流拦截器（AOP）
 * <p>
 * 使用 Bucket4j 令牌桶算法实现内存级限流。
 * 每个限流 key 对应一个独立的令牌桶，桶容量 = rate，填充速率 = rate per interval。
 * <p>
 * 限流维度：默认按 [IP + 方法签名] 限流，可通过 @RateLimit(key) 指定自定义维度。
 * <p>
 * 分布式部署说明：
 * 当前为单机内存级限流。多实例部署时如需全局限流，
 * 可将 Bucket 存储迁移至 Redis（使用 bucket4j-redis 扩展）。
 *
 * @author vivin
 */
@Slf4j
@Aspect
@Component
public class RateLimitInterceptor {

    /** 令牌桶缓存（ConcurrentHashMap 线程安全） */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * 拦截带 @RateLimit 注解的方法
     */
    @Around("@annotation(com.vio.aitukuviobe.infrastructure.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 构建限流 key
        String key = buildRateLimitKey(rateLimit, method);
        Bucket bucket = bucketCache.computeIfAbsent(key, k -> createBucket(rateLimit));

        // 尝试消费一个令牌
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        }

        // 限流触发
        log.warn("接口限流触发: key={}, rate={}/{}{}",
                key, rateLimit.rate(), rateLimit.interval(),
                rateLimit.timeUnit().name().toLowerCase());
        throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
    }

    /**
     * 构建限流 key：默认 [来源IP:方法全限定名]，可通过注解指定
     */
    private String buildRateLimitKey(RateLimit rateLimit, Method method) {
        if (StrUtil.isNotBlank(rateLimit.key())) {
            return rateLimit.key();
        }

        String ip = getClientIp();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        return ip + ":" + methodName;
    }

    /**
     * 创建令牌桶
     */
    private Bucket createBucket(RateLimit rateLimit) {
        long intervalNanos = rateLimit.timeUnit().toNanos(rateLimit.interval());
        Bandwidth limit = Bandwidth.classic(
                rateLimit.rate(),
                Refill.intervally(rateLimit.rate(), Duration.ofNanos(intervalNanos))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For 可能包含多个 IP（代理链），取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
