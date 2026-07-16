package com.vio.aitukuviobe.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
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
 * 自动注册 JVM 和系统级指标到 Prometheus：
 * <ul>
 *   <li>JVM 内存：堆内存、非堆内存、各内存池使用量</li>
 *   <li>JVM GC：GC 次数、GC 耗时（按 GC 类型分组）</li>
 *   <li>JVM 线程：活跃线程数、守护线程数、峰值线程数</li>
 *   <li>JVM 类加载：已加载类数、未加载类数</li>
 *   <li>系统指标：CPU 使用率、系统负载、进程运行时间</li>
 * </ul>
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

    /**
     * 注册 JVM 内存指标
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics(MeterRegistry registry) {
        JvmMemoryMetrics metrics = new JvmMemoryMetrics();
        metrics.bindTo(registry);
        log.debug("JVM 内存指标已注册");
        return metrics;
    }

    /**
     * 注册 JVM GC 指标
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics(MeterRegistry registry) {
        JvmGcMetrics metrics = new JvmGcMetrics();
        metrics.bindTo(registry);
        log.debug("JVM GC 指标已注册");
        return metrics;
    }

    /**
     * 注册 JVM 线程指标
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics(MeterRegistry registry) {
        JvmThreadMetrics metrics = new JvmThreadMetrics();
        metrics.bindTo(registry);
        log.debug("JVM 线程指标已注册");
        return metrics;
    }

    /**
     * 注册 JVM 类加载指标
     */
    @Bean
    public ClassLoaderMetrics classLoaderMetrics(MeterRegistry registry) {
        ClassLoaderMetrics metrics = new ClassLoaderMetrics();
        metrics.bindTo(registry);
        log.debug("JVM 类加载指标已注册");
        return metrics;
    }

    /**
     * 注册 CPU / 系统指标
     */
    @Bean
    public ProcessorMetrics processorMetrics(MeterRegistry registry) {
        ProcessorMetrics metrics = new ProcessorMetrics();
        metrics.bindTo(registry);
        log.debug("CPU 指标已注册");
        return metrics;
    }

    /**
     * 注册进程运行时间指标
     */
    @Bean
    public UptimeMetrics uptimeMetrics(MeterRegistry registry) {
        UptimeMetrics metrics = new UptimeMetrics();
        metrics.bindTo(registry);
        log.debug("运行时间指标已注册");
        return metrics;
    }
}