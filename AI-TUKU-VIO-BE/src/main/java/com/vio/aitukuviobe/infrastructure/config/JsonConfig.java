package com.vio.aitukuviobe.infrastructure.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置
 * <p>
 * 核心功能：将 Long 类型序列化为 String，避免 JavaScript Number 精度丢失。
 * <p>
 * 背景：MyBatis-Plus ASSIGN_ID 生成的 Snowflake ID 为 19 位数字，
 * 超出 JS Number 安全整数范围 (2^53-1 ≈ 9e15，仅 16 位)，
 * 导致前端 Number() 解析后 ID 末尾数字改变，查询失败。
 *
 * @author vivin
 */
@Configuration
public class JsonConfig {

    /**
     * 全局 Jackson 配置：Long → String（解决 JS 精度丢失）
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
