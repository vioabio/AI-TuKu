package com.vio.aitukuviobe.infrastructure.monitoring.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * MySQL 数据库健康检查指示器
 * <p>
 * 验证数据库连接是否正常，可通过 /actuator/health 查看。
 * 健康状态：
 * <ul>
 *   <li>UP：数据库连接正常，可执行 SELECT 1</li>
 *   <li>DOWN：连接失败或执行超时</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Component
public class DataSourceHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DataSourceHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(3);
            stmt.execute("SELECT 1");
            return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            log.error("MySQL 健康检查失败: {}", e.getMessage());
            return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}