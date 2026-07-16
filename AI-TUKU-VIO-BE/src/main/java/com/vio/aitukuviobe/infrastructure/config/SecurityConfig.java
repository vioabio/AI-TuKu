package com.vio.aitukuviobe.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 安全配置汇总
 * <p>
 * 集中管理安全相关配置：
 * <ul>
 *   <li>CORS 跨域收紧（CorsConfig）</li>
 *   <li>HTTP 安全响应头（SecurityHeadersFilter）</li>
 *   <li>XSS 输入清洗（XssFilter）</li>
 *   <li>接口限流（RateLimitInterceptor）</li>
 *   <li>HTTPS 强制跳转（Nginx/生产环境）</li>
 * </ul>
 * <p>
 * 安全头策略摘要：
 * <pre>
 *   X-XSS-Protection: 1; mode=block           → 防 XSS 反射
 *   X-Content-Type-Options: nosniff            → 防 MIME 嗅探
 *   X-Frame-Options: DENY                      → 防点击劫持
 *   Referrer-Policy: strict-origin-when-cross-origin → 防 Referer 泄露
 *   CSP: default-src 'self' ...               → 内容安全策略
 *   Permissions-Policy: camera=(), ...        → 浏览器特性限制
 *   Cross-Origin-Opener-Policy: same-origin    → 跨域窗口隔离
 * </pre>
 * <p>
 * 安全检查清单：
 * <ul>
 *   <li>[x] SQL 注入 → MyBatis-Plus 参数化查询（#{} 占位符，非 ${}）</li>
 *   <li>[x] XSS 防护 → Jsoup Safelist 清洗 + CSP 头</li>
 *   <li>[x] CSRF 防护 → Sa-Token 会话令牌 + CORS 白名单</li>
 *   <li>[x] 点击劫持 → X-Frame-Options: DENY</li>
 *   <li>[x] HTTPS → 生产环境 Nginx 强制跳转</li>
 *   <li>[ ] 文件上传魔数校验 → 待实施（11.5 节）</li>
 *   <li>[ ] 密码加密升级 BCrypt → 待实施（11.1 节）</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Configuration
public class SecurityConfig {

    @Value("${aituku.security.https-only:false}")
    private boolean httpsOnly;

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("安全加固模块已启用");
        log.info("  HTTP 安全头: 已注入 (XSS/MIME/Frame/CSP)");
        log.info("  XSS 清洗: 已启用 (Jsoup Safelist)");
        log.info("  CORS 白名单: 已收紧 (仅受信域名)");
        log.info("  接口限流: 已启用 (Bucket4j 令牌桶)");
        log.info("  HTTPS 强制: {}", httpsOnly ? "已启用" : "关闭（开发环境）");
        log.info("  安全头详情:");
        log.info("    X-XSS-Protection: 1; mode=block");
        log.info("    X-Content-Type-Options: nosniff");
        log.info("    X-Frame-Options: DENY");
        log.info("    Referrer-Policy: strict-origin-when-cross-origin");
        log.info("    Content-Security-Policy: 已配置");
        log.info("    Permissions-Policy: 已限制");
        log.info("    Cross-Origin-Opener-Policy: same-origin");
        log.info("========================================");
    }
}
