package com.vio.aitukuviobe.infrastructure.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 内容审核结果 — LangChain4j Structured Output
 *
 * <p>对应优化文档 3.2 节（NSFW 检测）/ 20 章
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    /** 内容是否安全 */
    @JsonProperty("safe")
    private Boolean safe;

    /** 违规原因（safe=false 时） */
    @JsonProperty("reason")
    private String reason;

    /** 审核置信度 (0.0-1.0) */
    @JsonProperty("confidence")
    private Double confidence;

    /** 违规类别: porn/violence/politics/terror/spam/other */
    @JsonProperty("category")
    private String category;

    /** 建议操作: PASS / REJECT / MANUAL_REVIEW */
    @JsonProperty("suggestedAction")
    private String suggestedAction;
}