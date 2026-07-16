package com.vio.aitukuviobe.infrastructure.filter;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

/**
 * XSS 防护过滤器
 * <p>
 * 对请求参数进行 HTML 转义，防止存储型/反射型 XSS 攻击。
 * 使用 Jsoup 的 Safelist 清洗用户输入，移除 &lt;script&gt;、onclick 等危险内容。
 * <p>
 * 清洗规则：
 * <ul>
 *   <li>移除所有 HTML 标签和属性</li>
 *   <li>保留纯文本内容</li>
 *   <li>&lt;script&gt;alert('xss')&lt;/script&gt; → 完全移除</li>
 *   <li>&lt;img onerror="alert(1)"&gt; → 移除</li>
 * </ul>
 * <p>
 * 注意：如果某些字段需要允许富文本（如简介），可在白名单中配置跳过清洗的字段路径。
 *
 * @author vivin
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
public class XssFilter implements Filter {

    /**
     * 不需要 XSS 清洗的路径（白名单）
     * 例如图片 URL、文件上传等场景
     */
    private static final Set<String> SKIP_PATHS = new HashSet<>(Arrays.asList(
            "/api/file/upload",
            "/api/picture/upload",
            "/api/picture/upload/url"
    ));

    /**
     * 不需要 XSS 清洗的参数名（白名单）
     * 例如图片 URL、JSON 数据等
     */
    private static final Set<String> SKIP_PARAMS = new HashSet<>(Arrays.asList(
            "fileUrl", "url", "thumbnailUrl", "tags", "parameters"
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();

        // 白名单路径跳过清洗
        if (SKIP_PATHS.contains(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 请求体已在 HttpRequestWrapperFilter 中包装支持重复读取
        // 此处只对查询参数和表单参数进行 XSS 清洗
        chain.doFilter(new XssRequestWrapper(httpRequest), response);
    }

    /**
     * HttpServletRequest 包装器：对参数值进行 XSS 清洗
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanXss(name, value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            return Arrays.stream(values)
                    .map(v -> cleanXss(name, v))
                    .toArray(String[]::new);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> paramMap = super.getParameterMap();
            Map<String, String[]> cleanedMap = new LinkedHashMap<>();
            paramMap.forEach((key, values) -> {
                String[] cleaned = Arrays.stream(values)
                        .map(v -> cleanXss(key, v))
                        .toArray(String[]::new);
                cleanedMap.put(key, cleaned);
            });
            return cleanedMap;
        }

        /**
         * 对参数值进行 XSS 清洗
         */
        private String cleanXss(String paramName, String value) {
            if (StrUtil.isBlank(value)) return value;

            // 白名单参数跳过清洗
            if (SKIP_PARAMS.contains(paramName)) return value;

            // 使用 Jsoup 清洗（仅保留纯文本，移除所有 HTML 标签）
            String cleaned = Jsoup.clean(value, Safelist.none());

            if (!value.equals(cleaned)) {
                log.debug("XSS 清洗: param={}, original length={}, cleaned length={}",
                        paramName, value.length(), cleaned.length());
            }
            return cleaned;
        }
    }
}
