package com.vio.aitukuviobe.infrastructure.monitoring;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 监控配置
 * <p>
 * 注册 JVM 和系统级指标到 Prometheus：
 * <ul>
 *   <li>JVM 内存：堆内存、非堆内存、各内存池使用量</li>
 *   <li>JVM GC：GC 次数、GC 耗时（按 GC 类型分组）</li>
 *   <li>JVM 线程：活跃线程数、守护线程数、峰值线程数</li>
 *   <li>JVM 类加载：已加载类数、未加载类数</li>
 *   <li>系统指标：CPU 使用率、系统负载、进程运行时间</li>
 * </ul>
 * <p>
 * 注意：Bean 方法不直接注入 MeterRegistry，而是返回 MeterBinder 实例。
 * Spring Boot 会在 MeterRegistry 就绪后自动调用 bindTo()，避免循环依赖。
 * <p>
 * HTTP 请求指标（自动采集，无需额外配置）：
 * <ul>
 *   <li>http.server.requests — QPS / 延迟分布 / 错误率</li>
 *   <li>按 URI、方法、状态码分组</li>
 * </ul>
 * <p>
 * 数据库连接池指标（HikariCP 自动上报）：
 * <ul>
 *   <li>hikaricp_connections_active — 活跃连接数</li>
 *   <li>hikaricp_connections_idle — 空闲连接数</li>
 *   <li>hikaricp_connections_pending — 等待连接数</li>
 * </ul>
 * <p>
 * Prometheus 抓取端点：GET /actuator/prometheus
 * <p>
 * Grafana 仪表板推荐：
 * <ul>
 *   <li>JVM Dashboard: https://grafana.com/grafana/dashboards/4701-jvm-micrometer/</li>
 *   <li>Spring Boot Dashboard: https://grafana.com/grafana/dashboards/12900-spring-boot/</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Configuration
public class MonitoringConfig {

    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        log.debug("JVM 内存指标已注册");
        return new JvmMemoryMetrics();
    }

    @Bean
    public JvmGcMetrics jvmGcMetrics() {
        log.debug("JVM GC 指标已注册");
        return new JvmGcMetrics();
    }

    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        log.debug("JVM 线程指标已注册");
        return new JvmThreadMetrics();
    }

    @Bean
    public ClassLoaderMetrics classLoaderMetrics() {
        log.debug("JVM 类加载指标已注册");
        return new ClassLoaderMetrics();
    }

    @Bean
    public ProcessorMetrics processorMetrics() {
        log.debug("CPU 指标已注册");
        return new ProcessorMetrics();
    }

    @Bean
    public UptimeMetrics uptimeMetrics() {
        log.debug("运行时间指标已注册");
        return new UptimeMetrics();
    }
}