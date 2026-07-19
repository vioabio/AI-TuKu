package com.vio.aitukuviobe.shared.eventstream.topology;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vio.aitukuviobe.shared.eventstream.model.AuditEvent;
import com.vio.aitukuviobe.shared.eventstream.model.PictureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Kafka Streams 事件处理拓扑 — 对应优化文档 9.3 节
 *
 * <p>核心处理流水线：
 * <pre>
 *   picture-events (source)
 *       │
 *       ├── [分支 1] → 转换为审计事件 → audit-events (sink)
 *       │
 *       ├── [分支 2] → 按 pictureId 聚合 → 图片操作计数 → picture-stats-store (state store)
 *       │
 *       └── [分支 3] → 过滤 DELETE 事件 → 触发缓存/ES 索引刷新 → cache-invalidation (sink)
 * </pre>
 *
 * <p>关键设计：
 * <ul>
 *   <li>Exactly-once 语义：通过 Kafka 事务保证</li>
 *   <li>状态存储：RocksDB 本地状态 + Kafka changelog 备份</li>
 *   <li>窗口聚合：5 分钟滑动窗口统计操作频率（异常检测）</li>
 *   <li>DLQ：处理失败的事件路由到 picture-events-dlq</li>
 * </ul>
 *
 * @author vivin
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aituku.kafka.streams.enabled", havingValue = "true")
public class PictureEventTopology {

    private final ObjectMapper objectMapper;

    private static final String SOURCE_TOPIC = "picture-events";
    private static final String AUDIT_SINK_TOPIC = "audit-events";
    private static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation";
    private static final String DLQ_TOPIC = "picture-events-dlq";
    private static final String STATS_STORE = "picture-stats-store";

    /**
     * 定义 Kafka Streams 拓扑
     *
     * @param builder StreamsBuilder（Spring 自动注入）
     * @return 拓扑定义
     */
    @Bean
    public KStream<String, String> pictureEventStream(StreamsBuilder builder) {
        // 1. 从源 topic 读取事件流
        KStream<String, String> sourceStream = builder.stream(
            SOURCE_TOPIC,
            Consumed.with(Serdes.String(), Serdes.String())
                .withName("picture-events-source")
        );

        // 2. 反序列化 JSON → PictureEvent
        KStream<String, PictureEvent> eventStream = sourceStream
            .mapValues(this::deserializeEvent)
            .filter((key, event) -> event != null);

        // ============================================================
        // 分支 1: 转换为审计事件 (所有事件都审计)
        // ============================================================
        eventStream
            .mapValues(this::toAuditEventJson)
            .filter((key, auditJson) -> auditJson != null)
            .to(AUDIT_SINK_TOPIC,
                Produced.with(Serdes.String(), Serdes.String())
                    .withName("audit-events-sink"));

        // ============================================================
        // 分支 2: 按操作者聚合 — 异常操作检测
        // ============================================================
        eventStream
            .selectKey((key, event) -> String.valueOf(event.getOperatorId()))
            .groupByKey(Grouped.with(Serdes.String(), new EventSerde()))
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofSeconds(30)))
            .aggregate(
                () -> 0L,
                (operatorId, event, count) -> count + 1,
                Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(STATS_STORE)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Long())
            );

        // ============================================================
        // 分支 3: 缓存/ES 索引失效 (仅 UPLOADED/EDITED/DELETED)
        // ============================================================
        eventStream
            .filter((key, event) -> isCacheInvalidationEvent(event))
            .map((key, event) -> KeyValue.pair(
                String.valueOf(event.getPictureId()),
                toCacheInvalidationJson(event)
            ))
            .to(CACHE_INVALIDATION_TOPIC,
                Produced.with(Serdes.String(), Serdes.String())
                    .withName("cache-invalidation-sink"));

        // ============================================================
        // 错误处理: 不可反序列化的消息 → DLQ
        // ============================================================
        sourceStream
            .filter((key, value) -> deserializeEvent(value) == null)
            .to(DLQ_TOPIC,
                Produced.with(Serdes.String(), Serdes.String())
                    .withName("dlq-sink"));

        log.info("[Kafka Streams] 事件处理拓扑已注册:");
        log.info("  源:       {}", SOURCE_TOPIC);
        log.info("  审计:     {} → {}", SOURCE_TOPIC, AUDIT_SINK_TOPIC);
        log.info("  统计:     {} → {}(RocksDB)", SOURCE_TOPIC, STATS_STORE);
        log.info("  缓存失效: {} → {}", SOURCE_TOPIC, CACHE_INVALIDATION_TOPIC);
        log.info("  死信:     {} → {}", SOURCE_TOPIC, DLQ_TOPIC);

        return sourceStream;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private PictureEvent deserializeEvent(String json) {
        try {
            return objectMapper.readValue(json, PictureEvent.class);
        } catch (JsonProcessingException e) {
            log.error("[Kafka Streams] 事件反序列化失败: {}", json, e);
            return null;
        }
    }

    private String toAuditEventJson(PictureEvent event) {
        try {
            AuditEvent audit = AuditEvent.fromPictureEvent(event);
            return objectMapper.writeValueAsString(audit);
        } catch (JsonProcessingException e) {
            log.error("[Kafka Streams] 审计事件序列化失败", e);
            return null;
        }
    }

    private boolean isCacheInvalidationEvent(PictureEvent event) {
        if (event == null || event.getEventType() == null) return false;
        return switch (event.getEventType()) {
            case UPLOADED, EDITED, DELETED, REVIEW_APPROVED, REVIEW_REJECTED -> true;
            default -> false;
        };
    }

    private String toCacheInvalidationJson(PictureEvent event) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                "pictureId", String.valueOf(event.getPictureId()),
                "spaceId", event.getSpaceId() != null ? String.valueOf(event.getSpaceId()) : "",
                "action", event.getEventType().name(),
                "timestamp", event.getTimestamp().toString()
            ));
        } catch (JsonProcessingException e) {
            log.error("[Kafka Streams] 缓存失效事件序列化失败", e);
            return "";
        }
    }

    /**
     * Kafka Streams 自定义 Serde（基于 JSON）
     */
    private static class EventSerde extends Serdes.WrapperSerde<PictureEvent> {
        EventSerde() {
            super(new PictureEventSerializer(), new PictureEventDeserializer());
        }
    }

    // 内联序列化器（简单实现，生产环境建议用 Confluent Avro / Protobuf）
    private static class PictureEventSerializer implements org.apache.kafka.common.serialization.Serializer<PictureEvent> {
        private final ObjectMapper mapper = new ObjectMapper();
        @Override
        public byte[] serialize(String topic, PictureEvent data) {
            try { return mapper.writeValueAsBytes(data); }
            catch (Exception e) { throw new RuntimeException("序列化失败", e); }
        }
    }

    private static class PictureEventDeserializer implements org.apache.kafka.common.serialization.Deserializer<PictureEvent> {
        private final ObjectMapper mapper = new ObjectMapper();
        @Override
        public PictureEvent deserialize(String topic, byte[] data) {
            try { return mapper.readValue(data, PictureEvent.class); }
            catch (Exception e) { throw new RuntimeException("反序列化失败", e); }
        }
    }
}
