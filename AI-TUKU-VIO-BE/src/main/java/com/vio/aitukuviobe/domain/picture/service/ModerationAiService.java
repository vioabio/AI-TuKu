package com.vio.aitukuviobe.domain.picture.service;

import com.vio.aitukuviobe.infrastructure.ai.model.ModerationResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 内容审核 AI 服务接口 — NSFW 检测 + 违规内容识别
 *
 * <p>对应优化文档 3.2 节（AI 图片审核）/ 第 20 章
 *
 * <p>审核流程：
 * <ol>
 *   <li>图片上传 → extractTags() 提取标签</li>
 *   <li>标签 + 图片信息 → moderateContent() 审核</li>
 *   <li>safe=false + confidence > 0.9 → 自动 REJECT</li>
 *   <li>safe=false + confidence < 0.9 → 标记 MANUAL_REVIEW</li>
 *   <li>safe=true → 自动 PASS</li>
 * </ol>
 *
 * @author vivin
 */
public interface ModerationAiService {

    /**
     * 图片内容安全审核
     *
     * @param name        图片名称
     * @param description 图片描述/简介
     * @param tags        图片标签 (JSON 数组字符串)
     * @param category    图片分类
     * @return 审核结果 (safe/reason/confidence/suggestedAction)
     */
    @SystemMessage("你是专业的内容安全审核专家。请严格审核图片信息是否包含违规内容。判断标准包括但不限于：色情、暴力、政治敏感、恐怖主义、垃圾广告。")
    @UserMessage("""
        请审核以下图片信息是否安全：
        名称: {{name}}
        描述: {{description}}
        标签: {{tags}}
        分类: {{category}}

        请返回 JSON:
        {
          "safe": true/false,
          "reason": "违规原因（safe=false时填写）",
          "confidence": 0.0-1.0,
          "category": "porn/violence/politics/terror/spam/other（safe=false时填写）",
          "suggestedAction": "PASS/REJECT/MANUAL_REVIEW"
        }

        判定规则:
        - safe=true: 内容完全合规 → suggestedAction=PASS
        - safe=false, confidence>=0.9: 明确违规 → suggestedAction=REJECT
        - safe=false, confidence<0.9: 疑似违规 → suggestedAction=MANUAL_REVIEW
        """)
    ModerationResult moderateContent(
        @V("name") String name,
        @V("description") String description,
        @V("tags") String tags,
        @V("category") String category
    );

    /**
     * 图片批量审核（轻量版，仅判断是否安全）
     *
     * @param name 图片名称
     * @param tags 图片标签
     * @return 审核结果
     */
    @SystemMessage("你是内容安全审核专家。请快速判断图片是否安全。")
    @UserMessage("图片名称: {{name}}, 标签: {{tags}}。仅返回 JSON: {\"safe\": true/false, \"confidence\": 0.0-1.0}")
    ModerationResult quickModerate(
        @V("name") String name,
        @V("tags") String tags
    );
}
