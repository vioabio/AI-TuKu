package com.vio.aitukuviobe.infrastructure.ai.config;

import com.vio.aitukuviobe.domain.picture.service.ModerationAiService;
import com.vio.aitukuviobe.domain.picture.service.PictureAiService;
import com.vio.aitukuviobe.domain.picture.service.SearchAiService;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@EnableConfigurationProperties(AiProperties.class)
@ConditionalOnProperty(name = "aituku.ai.enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jConfig {

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
            .temperature((float) props.getTemperature())
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
        return WanxImageModel.builder()
            .apiKey(props.getDashscopeApiKey())
            .modelName(props.getImageModelName())
            .build();
    }

    // ============================================================
    // AI Service Bean (Builder 模式创建代理)
    // ============================================================

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
    public PictureAiService pictureAiService(ChatLanguageModel chatModel, AiProperties props) {
        log.info("[LangChain4j] 创建 PictureAiService 代理 (Builder 模式)");
        logStartupBanner(props);
        return AiServices.builder(PictureAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }

    /**
     * 内容审核 AI 服务
     */
    @Bean
    public ModerationAiService moderationAiService(ChatLanguageModel chatModel, AiProperties props) {
        log.info("[LangChain4j] 创建 ModerationAiService 代理 (Builder 模式)");
        return AiServices.builder(ModerationAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }

    /**
     * 搜索意图 AI 服务
     */
    @Bean
    public SearchAiService searchAiService(ChatLanguageModel chatModel, AiProperties props) {
        log.info("[LangChain4j] 创建 SearchAiService 代理 (Builder 模式)");
        return AiServices.builder(SearchAiService.class)
            .chatLanguageModel(chatModel)
            .build();
    }

    // ============================================================
    // 启动日志 (idempotent — 只打印一次)
    // ============================================================

    private static volatile boolean startupBannerPrinted = false;

    private void logStartupBanner(AiProperties props) {
        if (startupBannerPrinted) return;
        synchronized (LangChain4jConfig.class) {
            if (startupBannerPrinted) return;
            startupBannerPrinted = true;
        }
        log.info("========================================");
        log.info("  LangChain4j AI 框架已启用");
        log.info("========================================");
        log.info("  版本:      1.0.0-beta2");
        log.info("  提供商:    {}", props.getProvider());
        log.info("  聊天模型:  {}", props.getChatModelName());
        log.info("  图像模型:  {}", props.getImageModelName());
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