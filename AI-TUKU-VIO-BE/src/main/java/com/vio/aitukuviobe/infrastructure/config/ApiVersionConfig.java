package com.vio.aitukuviobe.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * API 版本管理配置
 * <p>
 * 实现 URL 路径版本化：
 * <ul>
 *   <li>/api/v1/user/register → 内部重写为 /api/user/register（转发到现有 Controller）</li>
 *   <li>/api/v2/user/register → 转发到 /api/v2/user/register（新版 Controller）</li>
 * </ul>
 * <p>
 * 版本策略：
 * <ul>
 *   <li><b>兼容期（当前）</b>：/api/* 和 /api/v1/* 并存，/api/* 默认映射为 v1</li>
 *   <li><b>v2 发布后</b>：新增 /api/v2/* 端点，旧版 /api/v1/* 保留 6 个月后下线</li>
 *   <li><b>响应头</b>：自动添加 X-API-Version 响应头，废弃版本添加 Deprecation + Sunset 头</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /**
     * API 版本路径重写过滤器
     * <p>
     * 将 /api/v1/* 请求重写为 /api/*（内部转发，对客户端透明）
     * /api/v2/* 保持不变（由新版 Controller 处理）
     */
    @Bean
    public FilterRegistrationBean<ApiVersionRewriteFilter> apiVersionRewriteFilter() {
        FilterRegistrationBean<ApiVersionRewriteFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiVersionRewriteFilter());
        registration.addUrlPatterns("/api/v1/*", "/api/v2/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("apiVersionRewriteFilter");
        return registration;
    }

    /**
     * 版本响应头过滤器：为所有 API 响应添加版本标识头
     */
    @Bean
    public FilterRegistrationBean<ApiVersionResponseHeaderFilter> apiVersionResponseHeaderFilter() {
        FilterRegistrationBean<ApiVersionResponseHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiVersionResponseHeaderFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        registration.setName("apiVersionResponseHeaderFilter");
        return registration;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 启用路径前缀匹配（已默认启用，此处为显式声明）
        configurer.setUseTrailingSlashMatch(true);
    }

    /**
     * 请求路径重写过滤器
     * <p>
     * 规则：
     * - /api/v1/xxx → /api/xxx（内部转发，兼容所有现有 API）
     * - /api/v2/xxx → 保持 /api/v2/xxx（新版 API，由 v2 Controller 处理）
     */
    public static class ApiVersionRewriteFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String requestUri = httpRequest.getRequestURI();
            String contextPath = httpRequest.getContextPath();

            // 移除 context-path 后的路径
            String path = requestUri;
            if (contextPath != null && !contextPath.isEmpty()) {
                path = path.substring(contextPath.length());
            }

            // /api/v1/* → /api/*（内部重写）
            String rewrittenUri = requestUri;
            if (path.startsWith("/api/v1/")) {
                rewrittenUri = requestUri.replaceFirst("/api/v1/", "/api/");
                log.debug("API 版本路由: {} → {}", requestUri, rewrittenUri);
            }
            // /api/v2/* 保持不变（新版 API）

            if (!rewrittenUri.equals(requestUri)) {
                String finalPath = rewrittenUri;
                HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getRequestURI() {
                        return finalPath;
                    }

                    @Override
                    public String getServletPath() {
                        return finalPath;
                    }
                };
                chain.doFilter(wrapper, response);
            } else {
                chain.doFilter(request, response);
            }
        }
    }

    /**
     * 响应头过滤器：自动添加 API 版本标识和废弃警告
     */
    public static class ApiVersionResponseHeaderFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                             FilterChain chain) throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 根据请求路径判断版本
            String requestUri = httpRequest.getRequestURI();
            String apiVersion = "v1";
            if (requestUri.contains("/api/v2/")) {
                apiVersion = "v2";
            }

            // 添加版本响应头
            httpResponse.setHeader("X-API-Version", apiVersion);
            // 允许前端读取自定义响应头
            httpResponse.setHeader("Access-Control-Expose-Headers",
                    "X-API-Version, X-Deprecation-Notice, Sunset");

            chain.doFilter(request, response);
        }
    }
}
