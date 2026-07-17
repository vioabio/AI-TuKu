package com.vio.aitukuviobe.shared.eventstream.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审计事件 — Kafka Streams 拓扑处理后写入 audit_log 表
 *
 * <p>对应优化文档 9.3 节：完整审计日志，满足合规审计要求
 *
 * <p>数据流：
 * <pre>
 *   picture-events (topic)
 *       → KafkaStreams audit-topology
 *       → audit-events (topic)
 *       → audit-events-consumer
 *       → audit_log 表
 * </pre>
 *
 * <p>审计字段（who/what/when/where/how）：
 * <ul>
 *   <li>who:    operatorId + operatorAccount</li>
 *   <li>what:   eventType + entityType + entityId</li>
 *   <li>when:   timestamp</li>
 *   <li>where:  operatorIp</li>
 *   <li>how:    userAgent</li>
 * </ul>
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审计记录 ID */
    private String auditId;

    /** 原始事件 ID */
    private String sourceEventId;

    /** 操作类型 */
    private String action;

    /** 实体类型 */
    private String entityType;

    /** 实体 ID */
    private Long entityId;

    /** 实体名称 */
    private String entityName;

    /** 操作者 ID */
    private Long operatorId;

    /** 操作者账号 */
    private String operatorAccount;

    /** 所属空间 */
    private Long spaceId;

    /** 操作时间 */
    private LocalDateTime timestamp;

    /** 操作者 IP */
    private String operatorIp;

    /** 操作者 User-Agent */
    private String userAgent;

    /** 变更详情 (JSON) */
    private Map<String, String> details;

    /** 审计级别: INFO / WARN / CRITICAL */
    @Builder.Default
    private String severity = "INFO";

    /**
     * 从 PictureEvent 创建 AuditEvent
     */
    public static AuditEvent fromPictureEvent(PictureEvent event) {
        return AuditEvent.builder()
            .auditId(java.util.UUID.randomUUID().toString())
            .sourceEventId(event.getEventId())
            .action(event.getEventType().name())
            .entityType("PICTURE")
            .entityId(event.getPictureId())
            .entityName(event.getPictureName())
            .operatorId(event.getOperatorId())
            .operatorAccount(event.getOperatorAccount())
            .spaceId(event.getSpaceId())
            .timestamp(event.getTimestamp())
            .operatorIp(event.getOperatorIp())
            .userAgent(event.getUserAgent())
            .details(event.getMetadata())
            .severity(determineSeverity(event.getEventType()))
            .build();
    }

    /**
     * 根据事件类型判定审计级别
     */
    private static String determineSeverity(PictureEventType type) {
        switch (type) {
            case DELETED:
            case REVIEW_REJECTED:
                return "WARN";
            case REVIEW_APPROVED:
                return "INFO";
            default:
                return "INFO";
        }
    }
}
