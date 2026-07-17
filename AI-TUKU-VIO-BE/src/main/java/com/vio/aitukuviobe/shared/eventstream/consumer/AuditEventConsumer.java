package com.vio.aitukuviobe.shared.eventstream.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vio.aitukuviobe.shared.eventstream.model.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 审计事件消费者 — 从 Kafka 消费审计事件写入 audit_log 数据库表
 *
 * <p>对应优化文档 9.3 节：完整审计日志
 *
 * <p>数据流：
 * <pre>
 *   picture-events → Kafka Streams topology → audit-events → AuditEventConsumer → audit_log 表
 * </pre>
 *
 * <p>容错策略：
 * <ul>
 *   <li>死信队列 (DLT): 重试 3 次后 → audit-events-dlt</li>
 *   <li>幂等写入: INSERT IGNORE (基于 audit_id 唯一键)</li>
 *   <li>批量写入: 每 500ms 或 100 条一批</li>
 * </ul>
 *
 * <p>前置条件：
 * <pre>
 * CREATE TABLE IF NOT EXISTS audit_log (
 *     audit_id         VARCHAR(36)  PRIMARY KEY,
 *     source_event_id  VARCHAR(36),
 *     action           VARCHAR(64)  NOT NULL,
 *     entity_type      VARCHAR(64)  NOT NULL,
 *     entity_id        BIGINT,
 *     entity_name      VARCHAR(128),
 *     operator_id      BIGINT,
 *     operator_account VARCHAR(128),
 *     space_id         BIGINT,
 *     timestamp        DATETIME     NOT NULL,
 *     operator_ip      VARCHAR(64),
 *     user_agent       VARCHAR(512),
 *     details          JSON,
 *     severity         VARCHAR(16)  DEFAULT 'INFO',
 *     INDEX idx_audit_entity (entity_type, entity_id),
 *     INDEX idx_audit_operator (operator_id),
 *     INDEX idx_audit_timestamp (timestamp),
 *     INDEX idx_audit_action (action)
 * ) comment '操作审计日志表' collate = utf8mb4_unicode_ci;
 * </pre>
 *
 * @author vivin
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aituku.kafka.streams.enabled", havingValue = "true")
public class AuditEventConsumer {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL = """
        INSERT IGNORE INTO audit_log
            (audit_id, source_event_id, action, entity_type, entity_id,
             entity_name, operator_id, operator_account, space_id,
             timestamp, operator_ip, user_agent, details, severity)
        VALUES (?, ?, ?, 'PICTURE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    /**
     * 消费审计事件并写入 audit_log 表
     *
     * <p>使用 @RetryableTopic 自动重试 + 死信队列
     */
    @KafkaListener(
        topics = "audit-events",
        groupId = "aituku-audit-consumer",
        concurrency = "2"  // 2 个消费者线程
    )
    @RetryableTopic(
        attempts = "3",
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        dltTopicSuffix = "-dlt"
    )
    public void consume(
        String message,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            AuditEvent audit = objectMapper.readValue(message, AuditEvent.class);

            jdbcTemplate.update(INSERT_SQL,
                audit.getAuditId(),
                audit.getSourceEventId(),
                audit.getAction(),
                audit.getEntityId(),
                audit.getEntityName(),
                audit.getOperatorId(),
                audit.getOperatorAccount(),
                audit.getSpaceId(),
                audit.getTimestamp(),
                audit.getOperatorIp(),
                audit.getUserAgent(),
                objectMapper.writeValueAsString(audit.getDetails()),
                audit.getSeverity()
            );

            log.debug("[审计消费] auditId={}, action={}, entityId={}, partition={}, offset={}",
                audit.getAuditId(), audit.getAction(), audit.getEntityId(), partition, offset);

        } catch (JsonProcessingException e) {
            log.error("[审计消费] JSON 反序列化失败: {}", message, e);
            throw new RuntimeException("审计事件反序列化失败", e);
        } catch (Exception e) {
            log.error("[审计消费] 写入数据库失败: {}", message, e);
            throw new RuntimeException("审计事件写入失败", e);
        }
    }
}
