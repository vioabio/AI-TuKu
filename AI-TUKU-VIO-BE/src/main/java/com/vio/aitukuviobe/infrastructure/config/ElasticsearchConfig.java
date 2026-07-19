package com.vio.aitukuviobe.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

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
 * 注：未启用 @EnableElasticsearchAuditing，因为实体未使用 @CreatedDate / @LastModifiedDate 等审计注解。
 *     如需审计功能，需要先引入 @EnableElasticsearchAuditing 并确保 MappingElasticsearchConverter bean 存在。
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
@ConditionalOnProperty(name = "aituku.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {

    /**
     * 显式注册 MappingElasticsearchConverter（具体类型），
     * 覆盖 Spring Boot 2.7.x 自动配置中返回 ElasticsearchConverter（接口类型）的默认行为。
     * <p>
     * 同时启用 Elasticsearch 审计功能时需要此 bean（若添加 @EnableElasticsearchAuditing 需要保留），
     * 与 ShardingSphere 配合使用时也需确保该 Bean 正确存在。
     */
    @Bean
    public MappingElasticsearchConverter mappingElasticsearchConverter() {
        SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
        return new MappingElasticsearchConverter(mappingContext);
    }

    @PostConstruct
    public void init() {
        log.info("Elasticsearch 搜索引擎模块已启用");
        log.info("  索引名称: picture_index");
        log.info("  分词器: IK 中文分词 (ik_max_word / ik_smart)");
        log.info("  搜索权重: name(3.0) > introduction(2.0) > tags(1.0)");
        log.info("  降级策略: ES 不可用时自动回退 MySQL LIKE 搜索");
    }
}
