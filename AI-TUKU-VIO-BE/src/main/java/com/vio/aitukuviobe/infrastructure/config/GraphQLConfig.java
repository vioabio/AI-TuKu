package com.vio.aitukuviobe.infrastructure.config;

import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import javax.annotation.PostConstruct;

/**
 * GraphQL 配置 — 对应优化文档 9.1 节
 *
 * <p>启用方式：{@code aituku.graphql.enabled=true}（默认 false）
 * <p>端点：POST /api/graphql
 * <p>GraphiQL IDE：GET /api/graphiql（开发环境）
 *
 * <p>技术栈：
 * <ul>
 *   <li>Spring GraphQL — Spring Boot 2.7+ 原生支持</li>
 *   <li>GraphQL Java Extended Scalars — DateTime / JSON 等扩展标量</li>
 *   <li>Apollo Client — 前端 GraphQL 客户端</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "aituku.graphql.enabled", havingValue = "true")
public class GraphQLConfig {

    /**
     * 注册 GraphQL 扩展标量类型
     *
     * <p>DateTime: ISO 8601 格式的日期时间
     * <p>JSON: 任意 JSON 对象/数组
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.Json);
    }

    @PostConstruct
    public void init() {
        log.info("GraphQL 模块已启用");
        log.info("  端点: POST /api/graphql");
        log.info("  IDE:  GET /api/graphiql  (开发环境)");
        log.info("  Schema: classpath:graphql/*.graphqls");
        log.info("  标量类型: DateTime, JSON");
    }
}
