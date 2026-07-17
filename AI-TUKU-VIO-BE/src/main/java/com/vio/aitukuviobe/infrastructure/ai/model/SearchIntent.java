package com.vio.aitukuviobe.infrastructure.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 搜索意图解析结果 — LangChain4j Structured Output
 *
 * <p>对应优化文档 3.1/3.3 节 / 20 章
 * <p>将用户的自然语言搜索转为结构化的查询参数
 *
 * <p>示例: "找一些去年夏天拍的风景照" →
 * <pre>
 *   keywords: ["风景", "夏天"]
 *   category: "自然"
 *   timeRange: "2025-06/2025-08"
 *   colorFilter: null
 * </pre>
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchIntent {

    /** 提取的关键词 */
    @JsonProperty("keywords")
    private List<String> keywords;

    /** 意图分类 (如 "自然"、"人物"、"设计") */
    @JsonProperty("category")
    private String category;

    /** 时间范围过滤 (如 "2025-06/2025-08"，无则为 null) */
    @JsonProperty("timeRange")
    private String timeRange;

    /** 颜色过滤 (如 "蓝色"、"黑白"，无则为 null) */
    @JsonProperty("colorFilter")
    private String colorFilter;

    /** 排序偏好: relevance / newest / oldest */
    @JsonProperty("sortPreference")
    private String sortPreference;

    /** 原始查询文本 */
    @JsonProperty("originalQuery")
    private String originalQuery;
}