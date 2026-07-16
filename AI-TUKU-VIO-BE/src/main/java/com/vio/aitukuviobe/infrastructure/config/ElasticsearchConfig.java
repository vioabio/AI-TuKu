package com.vio.aitukuviobe.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;

import javax.annotation.PostConstruct;

/**
 * Elasticsearch 配置
 * <p>
 * 通过 aituku.elasticsearch.enabled=true 启用（默认 true）
 * <p>
 * 索引设置（通过 @Document 注解自动创建）：
 * <ul>
 *   <li>分词器：IK 中文分词（ik_max_word / ik_smart）</li>
 *   <li>拼音搜索：可通过 ES 插件 elasticsearch-analysis-pinyin 支持</li>
 *   <li>索引自动创建：@Document(createIndex = true)</li>
 * </ul>
 * <p>
 * ES 插件安装（服务器端）：
 * <pre>
 * bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.0/elasticsearch-analysis-ik-7.17.0.zip
 * bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v7.17.0/elasticsearch-analysis-pinyin-7.17.0.zip
 * </pre>
 *
 * @author vivin
 */
@Slf4j
@Configuration
@EnableElasticsearchAuditing
@ConditionalOnProperty(name = "aituku.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {

    @PostConstruct
    public void init() {
        log.info("Elasticsearch 搜索引擎模块已启用");
        log.info("  索引名称: picture_index");
        log.info("  分词器: IK 中文分词 (ik_max_word / ik_smart)");
        log.info("  搜索权重: name(3.0) > introduction(2.0) > tags(1.0)");
        log.info("  降级策略: ES 不可用时自动回退 MySQL LIKE 搜索");
    }
}
