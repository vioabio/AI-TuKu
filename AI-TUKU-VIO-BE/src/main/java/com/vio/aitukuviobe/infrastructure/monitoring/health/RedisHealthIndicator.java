package com.vio.aitukuviobe.infrastructure.monitoring.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis 健康检查指示器
 * <p>
 * 验证 Redis 连接是否正常，可通过 /actuator/health 查看。
 *
 * @author vivin
 */
@Slf4j
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Override
    public Health health() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String pong = conn.ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "connected")
                        .withDetail("ping", pong)
                        .build();
            }
            return Health.down()
                    .withDetail("redis", "unexpected response")
                    .withDetail("response", pong)
                    .build();
        } catch (Exception e) {
            log.error("Redis 健康检查失败: {}", e.getMessage());
            return Health.down()
                    .withDetail("redis", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}