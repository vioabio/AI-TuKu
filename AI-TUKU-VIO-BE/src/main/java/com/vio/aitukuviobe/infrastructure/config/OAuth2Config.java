package com.vio.aitukuviobe.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 / 第三方登录配置
 * <p>
 * 支持平台：微信开放平台、GitHub、Google
 * <p>
 * 配置方式（application.yml）：
 * <pre>
 * oauth2:
 *   enabled: true
 *   platforms:
 *     github:
 *       client-id: your-github-client-id
 *       client-secret: your-github-client-secret
 *       redirect-uri: http://localhost:8123/api/oauth/github/callback
 *     google:
 *       client-id: your-google-client-id
 *       client-secret: your-google-client-secret
 *       redirect-uri: http://localhost:8123/api/oauth/google/callback
 *     wechat:
 *       client-id: your-wechat-app-id
 *       client-secret: your-wechat-app-secret
 *       redirect-uri: http://localhost:8123/api/oauth/wechat/callback
 * </pre>
 *
 * @author vivin
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Config {

    /** 是否启用 OAuth2（开发环境可关闭） */
    private boolean enabled = false;

    /** 各平台配置 */
    private Map<String, OAuth2PlatformConfig> platforms = new HashMap<>();

    @Data
    public static class OAuth2PlatformConfig {
        /** 客户端 ID / App ID */
        private String clientId;
        /** 客户端密钥 / App Secret */
        private String clientSecret;
        /** 回调地址 */
        private String redirectUri;
    }
}
