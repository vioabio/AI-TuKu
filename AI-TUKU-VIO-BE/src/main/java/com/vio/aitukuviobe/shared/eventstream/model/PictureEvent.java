package com.vio.aitukuviobe.shared.eventstream.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图片操作事件 — Kafka 消息体
 *
 * <p>对应优化文档 9.3 节：图片的所有操作记录为事件流
 *
 * <p>Topic: {@code picture-events} (4 分区, 复制因子 2)
 * <p>消息键: pictureId (保证同一图片的事件有序)
 * <p>消息体: JSON 序列化
 *
 * <p>事件溯源：完整的操作历史通过 Kafka 日志保留（默认 7 天），
 * 可随时重放事件流重建状态。
 *
 * @author vivin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PictureEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件唯一 ID (UUID) */
    private String eventId;

    /** 事件类型 */
    private PictureEventType eventType;

    /** 图片 ID */
    private Long pictureId;

    /** 图片名称 (用于审计日志展示) */
    private String pictureName;

    /** 操作者 ID */
    private Long operatorId;

    /** 操作者账号 */
    private String operatorAccount;

    /** 所属空间 ID (null=公共图库) */
    private Long spaceId;

    /** 事件发生时间 */
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    /** 操作者 IP */
    private String operatorIp;

    /** 操作者 User-Agent */
    private String userAgent;

    /** 额外元数据 (如审核意见、标签变更等) */
    private Map<String, String> metadata;

    /**
     * 创建事件的工厂方法
     */
    public static PictureEvent of(PictureEventType type, Long pictureId, String pictureName,
                                   Long operatorId, String operatorAccount, Long spaceId) {
        return PictureEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType(type)
            .pictureId(pictureId)
            .pictureName(pictureName)
            .operatorId(operatorId)
            .operatorAccount(operatorAccount)
            .spaceId(spaceId)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
