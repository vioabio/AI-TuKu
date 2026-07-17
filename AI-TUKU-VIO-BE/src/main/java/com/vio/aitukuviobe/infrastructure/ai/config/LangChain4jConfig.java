package com.vio.aitukuviobe.infrastructure.ai.config;

import com.vio.aitukuviobe.domain.picture.service.ModerationAiService;
import com.vio.aitukuviobe.domain.picture.service.PictureAiService;
import com.vio.aitukuviobe.domain.picture.service.SearchAiService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenModelName;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.retry.RetryPolicy;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;

/**
 * LangChain4j AI 框架配置 — 对应优化文档第 20 章
 *
 * <p>使用 Builder 模式创建 AI Service 代理（非 @AiService 注解）
 * <p>原因：@AiService 注解需要 Spring Boot 3.x，项目当前为 2.7.6
 * <p>Spring Boot 3 升级（第 1.1 节）后可切换为注解模式
 *
 * <p>核心 Bean：
 * <ul>
 *   <li>{@link ChatLanguageModel} — 文本对话模型（Qwen-Turbo）</li>
 *   <li>{@link ImageModel} — 图像模型（通义万相）</li>
 *   <li>{@link PictureAiService} — 图片 AI 服务（扩图 + 标签）</li>
 *   <li>{@link ModerationAiService} — 内容审核 AI 服务</li>
 *   <li>{@link SearchAiService} — 搜索意图 AI 服务</li>
 * </ul>
 *
 * <p>模型切换：
 * <pre>
 *   aituku.ai.provider=dashscope  → 阿里云百炼 (默认)
 *   aituku.ai.provider=openai     → OpenAI / OneAPI 中转
 * </pre>
 *
 * @author vivin
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aituku.ai.enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jConfig {

    // ============================================================
    // 配置属性
    // ============================================================

    @Bean
    @ConfigurationProperties(prefix = "aituku.ai")
    public AiProperties aiProperties() {
        return new AiProperties();
    }

    @Data
    public static class AiProperties {
        /** 模型提供商: dashscope / openai */
        private String provider = "dashscope";
        /** 阿里云 DashScope API Key */
        private String dashscopeApiKey;
        /** 聊天模型名称 */
        private String chatModelName = QwenModelName.QWEN_TURBO;
        /** 图像模型名称 */
        private String imageModelName = "image-out-painting";
        /** 重试最大次数 */
        private int maxRetries = 3;
        /** 请求超时 (秒) */
        private int timeout = 60;
        /** 温度 (0.0-1.0, 越高越随机) */
        private double temperature = 0.7;
        /** 最大 Token */
        private int maxTokens = 2000;
        /** OpenAI 兼容 API Base URL (provider=openai 时使用) */
        private String openaiBaseUrl;
        /** OpenAI API Key */
        private String openaiApiKey;
    }

    // ============================================================
    // 模型 Bean
    // ============================================================

    /**
     * 聊天模型 (ChatLanguageModel)
     *
     * <p>根据 aituku.ai.provider 自动切换：
     * <ul>
     *   <li>dashscope → QwenChatModel (阿里云通义千问)</li>
     *   <li>openai   → OpenAiChatModel (OpenAI / OneAPI 中转)</li>
     * </ul>
     */
    @Bean
    public ChatLanguageModel chatLanguageModel(AiProperties props) {
        if ("openai".equalsIgnoreCase(props.getProvider())) {
            log.info("[LangChain4j] 使用 OpenAI 兼容模型: baseUrl={}, model={}",
                props.getOpenaiBaseUrl(), props.getChatModelName());
            return OpenAiChatModel.builder()
                .baseUrl(props.getOpenaiBaseUrl())
                .apiKey(props.getOpenaiApiKey())
                .modelName(props.getChatModelName())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(Duration.ofSeconds(props.getTimeout()))
                .build();
        }

        // 默认: 阿里云 DashScope
        String apiKey = props.getDashscopeApiKey();
        if (apiKey == null || apiKey.isBlank() || "your-api-key".equals(apiKey)) {
            log.warn("[LangChain4j] DashScope API Key 未配置，AI 功能将不可用");
            log.warn("  请设置环境变量 ALIYUN_AI_API_KEY 或 application.yml 中 aituku.ai.dashscope-api-key");
        }

        log.info("[LangChain4j] 使用阿里云 DashScope: model={}", props.getChatModelName());
        return QwenChatModel.builder()
            .apiKey(apiKey)
            .modelName(props.getChatModelName())
            .temperature(props.getTemperature())
            .maxTokens(props.getMaxTokens())
            .build();
    }

    /**
     * 图像模型 (ImageModel)
     */
    @Bean
    public ImageModel imageModel(AiProperties props) {
        if ("openai".equalsIgnoreCase(props.getProvider())) {
            log.info("[LangChain4j] 使用 OpenAI 图像模型");
            return OpenAiImageModel.builder()
                .apiKey(props.getOpenaiApiKey())
                .modelName(props.getImageModelName())
                .build();
        }

        // DashScope 图片模型（通义万相）
        log.info("[LangChain4j] 使用 DashScope 图像模型: {}", props.getImageModelName());
        return dev.langchain4j.model.dashscope.QwenImageModel.builder()
            .apiKey(props.getDashscopeApiKey())
            .modelName(props.getImageModelName())
            .build();
    }

    // ============================================================
    // AI Service Bean (Builder 模式创建代理)
    // ============================================================

    /**
     * 重试策略: 指数退避, 最多 3 次
     */
    @Bean
    public RetryPolicy aiRetryPolicy(AiProperties props) {
        return RetryPolicy.builder()
            .maxRetries(props.getMaxRetries())
            .delay(Duration.ofSeconds(1))
            .maxDelay(Duration.ofSeconds(10))
            .exponentialBackoff(2.0)
            .onRetry((e) -> log.warn("[LangChain4j] AI 调用失败，正在重试: {}", e.getMessage()))
            .onMaxRetries((e) -> log.error("[LangChain4j] 已达最大重试次数 {}", props.getMaxRetries()))
            .build();
    }

    /**
     * 聊天内存 (滑动窗口, 保留最近 10 轮对话)
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    /**
     * 图片 AI 服务 — 扩图 + 标签提取
     *
     * <p>Spring Boot 3 升级后可改为 @AiService 注解模式：
     * <pre>
     * @AiService
     * public interface PictureAiService {
     *     @SystemMessage("...")
     *     @UserMessage("...")
     *     Result&lt;TagsResult&gt; extractTags(...);
     * }
     * </pre>
     */
    @Bean
    public PictureAiService pictureAiService(
        ChatLanguageModel chatModel,
        ImageModel imageModel,
        RetryPolicy retryPolicy
    ) {
        log.info("[LangChain4j] 创建 PictureAiService 代理 (Builder 模式)");
        return AiServices.builder(PictureAiService.class)
            .chatLanguageModel(chatModel)
            .retryPolicy(retryPolicy)
            .build();
    }

    /**
     * 内容审核 AI 服务
     */
    @Bean
    public ModerationAiService moderationAiService(
        ChatLanguageModel chatModel,
        RetryPolicy retryPolicy
    ) {
        log.info("[LangChain4j] 创建 ModerationAiService 代理 (Builder 模式)");
        return AiServices.builder(ModerationAiService.class)
            .chatLanguageModel(chatModel)
            .retryPolicy(retryPolicy)
            .build();
    }

    /**
     * 搜索意图 AI 服务
     */
    @Bean
    public SearchAiService searchAiService(
        ChatLanguageModel chatModel,
        RetryPolicy retryPolicy
    ) {
        log.info("[LangChain4j] 创建 SearchAiService 代理 (Builder 模式)");
        return AiServices.builder(SearchAiService.class)
            .chatLanguageModel(chatModel)
            .retryPolicy(retryPolicy)
            .build();
    }

    // ============================================================
    // 启动日志
    // ============================================================

    @PostConstruct
    public void init() {
        AiProperties props = aiProperties();
        log.info("========================================");
        log.info("  LangChain4j AI 框架已启用");
        log.info("========================================");
        log.info("  版本:      1.0.0-beta2");
        log.info("  提供商:    {}", props.getProvider());
        log.info("  聊天模型:  {}", props.getChatModelName());
        log.info("  图像模型:  {}", props.getImageModelName());
        log.info("  重试:      最多 {} 次 (指数退避)", props.getMaxRetries());
        log.info("  API Key:   {}", (props.getDashscopeApiKey() != null
            && !props.getDashscopeApiKey().isBlank()
            && !"your-api-key".equals(props.getDashscopeApiKey()))
            ? "已配置 ✓" : "⚠ 未配置 (请设置 ALIYUN_AI_API_KEY)");
        log.info("  AI Services:");
        log.info("    - PictureAiService     (扩图 + 标签提取)");
        log.info("    - ModerationAiService  (内容审核 NSFW)");
        log.info("    - SearchAiService      (搜索意图解析)");
        log.info("========================================");
    }
}
