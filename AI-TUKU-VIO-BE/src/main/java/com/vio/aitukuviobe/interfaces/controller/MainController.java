package com.vio.aitukuviobe.interfaces.controller;

import com.vio.aitukuviobe.infrastructure.common.BaseResponse;
import com.vio.aitukuviobe.infrastructure.common.ResultUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主控制器（健康检查、系统信息）
 */
@RestController
@RequestMapping
public class MainController {

    @Value("${spring.application.name:vio_tuku}")
    private String appName;

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    /**
     * 健康检查（增强版）
     * <p>
     * 返回应用基本信息 + JVM 运行状态。
     * 详细健康检查（MySQL/Redis/COS 连通性）请访问：
     * GET /actuator/health
     */
    @GetMapping("/health")
    public BaseResponse<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("application", appName);
        info.put("profile", activeProfile);
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // JVM 基本信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("availableProcessors", runtime.availableProcessors());
        jvm.put("maxMemory", formatBytes(runtime.maxMemory()));
        jvm.put("totalMemory", formatBytes(runtime.totalMemory()));
        jvm.put("freeMemory", formatBytes(runtime.freeMemory()));
        jvm.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));

        // JVM 运行时间
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);
        jvm.put("uptime", String.format("%dd %dh %dm %ds",
                uptime.toDays(),
                uptime.toHours() % 24,
                uptime.toMinutes() % 60,
                uptime.getSeconds() % 60));

        info.put("jvm", jvm);
        return ResultUtils.success(info);
    }

    /**
     * Prometheus 健康检查指标说明
     */
    @GetMapping("/actuator-info")
    public BaseResponse<Map<String, Object>> actuatorInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("health", "/actuator/health — 健康检查（MySQL/Redis/COS 连通性）");
        info.put("metrics", "/actuator/metrics — 所有可用指标列表");
        info.put("prometheus", "/actuator/prometheus — Prometheus 抓取端点");
        info.put("info", "/actuator/info — 应用信息");
        return ResultUtils.success(info);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
