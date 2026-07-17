package com.vio.aitukuviobe.domain.picture.service;

import com.vio.aitukuviobe.infrastructure.ai.model.OutPaintingResult;
import com.vio.aitukuviobe.infrastructure.ai.model.TagsResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 图片 AI 服务接口 — 扩图 + 标签提取
 *
 * <p>对应优化文档 3.1/3.4 节 / 第 20 章
 *
 * <p>使用 LangChain4j Builder 模式创建代理（见 {@link com.vio.aitukuviobe.infrastructure.ai.config.LangChain4jConfig}）
 * <p>Spring Boot 3 升级后可改为 @AiService 注解模式
 *
 * <p>在 DomainService 中注入:
 * <pre>
 * @Resource
 * private PictureAiService pictureAiService;
 * </pre>
 *
 * @author vivin
 */
public interface PictureAiService {

    /**
     * AI 扩图 — 替换原 AliYunAiApi.createOutPaintingTask()
     *
     * @param imageUrl 图片 URL
     * @param xScale   水平扩展比例 (1.0-3.0)
     * @param yScale   垂直扩展比例 (1.0-3.0)
     * @return 扩图任务结果
     */
    @SystemMessage("你是一个专业的图像处理助手。请根据参数调用图像扩图功能。")
    @UserMessage("""
        请对以下图片进行 AI 扩图处理：
        图片URL: {{imageUrl}}
        水平扩展比例: {{xScale}}
        垂直扩展比例: {{yScale}}
        请返回 JSON: {"taskId":"...", "taskStatus":"PENDING", "outputImageUrl":null, ...}
        """)
    OutPaintingResult outPainting(
        @V("imageUrl") String imageUrl,
        @V("xScale") double xScale,
        @V("yScale") double yScale
    );

    /**
     * AI 自动标签提取
     *
     * @param name        图片名称
     * @param category    当前分类 (可为空)
     * @param description 图片简介/描述
     * @return 标签列表 + 建议分类 + 置信度
     */
    @SystemMessage("你是专业的图片标签提取专家。请根据图片信息提取 3-5 个中文标签，并建议一个最合适的分类。")
    @UserMessage("""
        图片名称: {{name}}
        当前分类: {{category}}
        图片描述: {{description}}
        请返回 JSON: {"tags":["标签1","标签2",...], "suggestedCategory":"分类", "confidence":0.95}
        """)
    TagsResult extractTags(
        @V("name") String name,
        @V("category") String category,
        @V("description") String description
    );
}
