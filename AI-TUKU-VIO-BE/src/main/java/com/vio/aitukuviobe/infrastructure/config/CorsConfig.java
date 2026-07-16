package com.vio.aitukuviobe.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS 跨域配置（已收紧）
 * <p>
 * 安全规则：
 * <ul>
 *   <li>仅允许受信域跨域访问（生产环境必须配置具体域名）</li>
 *   <li>开发环境可放宽为 localhost</li>
 *   <li>禁止 allowCredentials(true) + allowedOriginPatterns("*") 的危险组合</li>
 * </ul>
 * <p>
 * 配置方式（application.yml）：
 * <pre>
 * cors:
 *   allowed-origins:
 *     - http://localhost:5173
 *     - https://your-domain.com
 * </pre>
 *
 * @author vivin
 */
@Slf4j
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.toArray(new String[0]);

        log.info("CORS 配置: allowedOrigins={}", allowedOrigins);

        // API 接口 — 仅允许配置的受信域名
        registry.addMapping("/api/**")
                .allowCredentials(true)
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(
                        "X-API-Version",
                        "X-Deprecation-Notice",
                        "Sunset",
                        "Content-Disposition"
                )
                .maxAge(3600);

        // WebSocket 端点 — 允许跨域握手
        registry.addMapping("/ws/**")
                .allowCredentials(true)
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        // 公共静态资源 — 允许任意域访问
        registry.addMapping("/public/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "OPTIONS")
                .maxAge(86400);
    }
}
