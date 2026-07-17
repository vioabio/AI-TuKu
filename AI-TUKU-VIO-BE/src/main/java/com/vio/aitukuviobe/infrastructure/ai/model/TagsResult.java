package com.vio.aitukuviobe.infrastructure.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 标签提取结果 — LangChain4j Structured Output
 *
 * <p>对应优化文档 3.1 节 / 20 章
 * <p>由 @AiService 自动将 LLM 返回的 JSON 反序列化为此对象
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagsResult {

    /** 提取的中文标签列表 (3-5 个) */
    @JsonProperty("tags")
    private List<String> tags;

    /** 建议的分类 (如 "自然"、"人物"、"设计") */
    @JsonProperty("suggestedCategory")
    private String suggestedCategory;

    /** 标签置信度 (0.0-1.0) */
    @JsonProperty("confidence")
    private Double confidence;
}