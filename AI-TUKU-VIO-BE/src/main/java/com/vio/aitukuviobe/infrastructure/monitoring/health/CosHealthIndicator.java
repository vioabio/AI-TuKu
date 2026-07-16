package com.vio.aitukuviobe.infrastructure.monitoring.health;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 腾讯云 COS 对象存储健康检查指示器
 * <p>
 * 验证 COS 连接是否正常（列出 Bucket 列表）。
 * 仅在 COSClient Bean 存在时激活（即配置了 COS 密钥的环境）。
 *
 * @author vivin
 */
@Slf4j
@Component
@ConditionalOnBean(COSClient.class)
public class CosHealthIndicator implements HealthIndicator {

    private final COSClient cosClient;

    public CosHealthIndicator(COSClient cosClient) {
        this.cosClient = cosClient;
    }

    @Override
    public Health health() {
        try {
            List<Bucket> buckets = cosClient.listBuckets();
            return Health.up()
                    .withDetail("cos", "connected")
                    .withDetail("bucketCount", buckets.size())
                    .build();
        } catch (Exception e) {
            log.error("COS 健康检查失败: {}", e.getMessage());
            return Health.down()
                    .withDetail("cos", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}