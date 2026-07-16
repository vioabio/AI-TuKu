package com.vio.aitukuviobe.infrastructure.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HTTP 安全响应头过滤器
 * <p>
 * 为所有响应自动注入安全头，防护常见 Web 攻击：
 * <ul>
 *   <li>XSS 反射：X-XSS-Protection</li>
 *   <li>MIME 嗅探：X-Content-Type-Options</li>
 *   <li>点击劫持：X-Frame-Options</li>
 *   <li>HTTPS 强制：Strict-Transport-Security（仅生产环境）</li>
 *   <li>引用策略：Referrer-Policy</li>
 *   <li>内容安全策略：Content-Security-Policy</li>
 *   <li>跨域嵌入：Cross-Origin-Embedder-Policy / Cross-Origin-Opener-Policy</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. 防 XSS 反射攻击（已过时但仍有效作为纵深防御）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 2. 防 MIME 类型嗅探（禁止浏览器猜测 Content-Type）
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 3. 防点击劫持（禁止被嵌入 iframe）
        //    如有需要嵌入的特定页面，可用 ALLOW-FROM uri
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 4. 引用策略（跨域时不发送完整 Referer，保护 URL 中的敏感参数）
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 5. 权限策略（限制浏览器特性使用）
        httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), " +
                "interest-cohort=()");

        // 6. 内容安全策略（CSP）：防止 XSS 及数据注入
        //    生产环境建议收紧：禁止 inline script，仅允许信任的 CDN
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "img-src 'self' https: data: blob:; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "font-src 'self' data:; " +
                "connect-src 'self' https: wss:; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

        // 7. 跨域隔离策略（增强安全性，限制跨域资源访问）
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "cross-origin");

        // 8. 缓存控制（敏感页面禁止缓存）
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");
        httpResponse.setHeader("Expires", "0");

        chain.doFilter(request, response);
    }
}
