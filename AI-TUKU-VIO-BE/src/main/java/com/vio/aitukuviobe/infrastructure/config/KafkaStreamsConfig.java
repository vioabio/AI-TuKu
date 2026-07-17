package com.vio.aitukuviobe.infrastructure.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Streams 配置 — 对应优化文档 9.3 节（事件驱动架构）
 *
 * <p>启用方式：{@code aituku.kafka.streams.enabled=true}（默认 false）
 * <p>依赖：Apache Kafka 3.1.x（Spring Boot 2.7.6 内置的 Spring Kafka 2.8.x 基线）
 *
 * <p>核心组件：
 * <ul>
 *   <li>Kafka Streams Topology — 事件处理拓扑（PictureEventTopology）</li>
 *   <li>Kafka Producer — 事件发布（PictureEventProducer）</li>
 *   <li>Kafka Consumer — 审计事件消费（AuditEventConsumer）</li>
 * </ul>
 *
 * <p>Topic 设计：
 * <pre>
 *   picture-events         (4 分区, RF=2) — 图片操作事件源
 *   audit-events           (2 分区, RF=2) — 审计事件（Kafka Streams 输出）
 *   cache-invalidation     (2 分区, RF=2) — 缓存/ES 索引失效通知
 *   picture-events-dlq     (1 分区, RF=2) — 死信队列
 * </pre>
 *
 * @author vivin
 */
@Slf4j
@Configuration
@EnableKafka
@EnableKafkaStreams
@ConditionalOnProperty(name = "aituku.kafka.streams.enabled", havingValue = "true")
public class KafkaStreamsConfig {

    /**
     * Kafka Streams 基础配置
     */
    @Bean
    @ConfigurationProperties(prefix = "aituku.kafka.streams")
    public KafkaStreamsProperties kafkaStreamsProperties() {
        return new KafkaStreamsProperties();
    }

    /**
     * 组装 Kafka Streams 配置
     */
    @Bean
    public StreamsConfig streamsConfig(KafkaStreamsProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, props.getApplicationId());
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers());
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // Exactly-once 语义
        config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        // 状态存储: RocksDB
        config.put(StreamsConfig.STATE_DIR_CONFIG, props.getStateDir());
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024); // 10MB

        // 生产环境线程数
        config.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, props.getNumThreads());

        // 反序列化异常处理: 记录日志并继续
        config.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
            LogAndContinueExceptionHandler.class.getName());

        // 副本因子（生产环境建议 3）
        config.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, props.getReplicationFactor());

        return new StreamsConfig(config);
    }

    /**
     * 自动创建 Topics（开发环境，生产环境建议手动管理）
     */
    @Bean
    public NewTopic pictureEventsTopic(KafkaStreamsProperties props) {
        return TopicBuilder.name("picture-events")
            .partitions(props.getPictureEventsPartitions())
            .replicas(props.getReplicationFactor())
            .build();
    }

    @Bean
    public NewTopic auditEventsTopic(KafkaStreamsProperties props) {
        return TopicBuilder.name("audit-events")
            .partitions(2)
            .replicas(props.getReplicationFactor())
            .build();
    }

    @Bean
    public NewTopic cacheInvalidationTopic(KafkaStreamsProperties props) {
        return TopicBuilder.name("cache-invalidation")
            .partitions(2)
            .replicas(props.getReplicationFactor())
            .build();
    }

    @Bean
    public NewTopic dlqTopic(KafkaStreamsProperties props) {
        return TopicBuilder.name("picture-events-dlq")
            .partitions(1)
            .replicas(props.getReplicationFactor())
            .build();
    }

    @PostConstruct
    public void init() {
        log.info("Kafka Streams 事件驱动模块已启用");
        log.info("  Application ID: {}", kafkaStreamsProperties().getApplicationId());
        log.info("  Bootstrap: {}", kafkaStreamsProperties().getBootstrapServers());
        log.info("  Threads: {}", kafkaStreamsProperties().getNumThreads());
        log.info("  Topics:");
        log.info("    picture-events     ({} 分区)", kafkaStreamsProperties().getPictureEventsPartitions());
        log.info("    audit-events       (2 分区)");
        log.info("    cache-invalidation (2 分区)");
        log.info("    picture-events-dlq (1 分区, 死信)");
    }

    /**
     * Kafka Streams 配置属性
     */
    @Data
    public static class KafkaStreamsProperties {
        private String applicationId = "aituku-picture-streams";
        private String bootstrapServers = "localhost:9092";
        private String stateDir = "/tmp/aituku-kafka-streams";
        private int numThreads = 2;
        private int replicationFactor = 1;
        private int pictureEventsPartitions = 4;
    }
}
