package com.vio.aitukuviobe.domain.picture.service;

import com.vio.aitukuviobe.infrastructure.ai.model.SearchIntent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 搜索意图 AI 服务接口 — 自然语言 → 结构化查询参数
 *
 * <p>对应优化文档 3.1 节 / 第 20 章
 *
 * <p>将用户的自然语言搜索转为结构化参数，替代传统的 LIKE 模糊匹配：
 * <ul>
 *   <li>"找一些蓝色调的海洋风景照" → keywords=["海洋","风景"], colorFilter="蓝色", category="自然"</li>
 *   <li>"去年冬天的雪景" → keywords=["雪景"], timeRange="2025-12/2026-02"</li>
 *   <li>"小红书的风格的设计素材" → keywords=["设计","素材"], sortPreference="relevance"</li>
 * </ul>
 *
 * @author vivin
 */
public interface SearchAiService {

    /**
     * 解析用户搜索意图
     *
     * @param query 用户原始搜索文本（自然语言）
     * @return 结构化的搜索意图（关键词/分类/颜色/时间范围/排序）
     */
    @SystemMessage("""
        你是图片搜索引擎的意图理解专家。将用户的自然语言搜索转为结构化的查询参数。

        规则:
        1. keywords: 提取 2-5 个核心关键词（中文）
        2. category: 推断分类（自然/人物/设计/动物/科技/美食/建筑/其他），不确定则为 null
        3. timeRange: 识别时间表达（"去年夏天"→年份-1的6-8月，"上周"→7天前至今），无则为 null
        4. colorFilter: 识别颜色词（红/蓝/绿/黑白/暖色/冷色），无则为 null
        5. sortPreference: relevance（默认）/ newest / oldest
        """)
    @UserMessage("用户搜索: {{query}}")
    SearchIntent parseSearchIntent(
        @V("query") String query
    );
}
