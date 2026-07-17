package com.vio.aitukuviobe.shared.eventstream.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vio.aitukuviobe.shared.eventstream.model.PictureEvent;
import com.vio.aitukuviobe.shared.eventstream.model.PictureEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 图片事件生产者 — 将图片操作发布到 Kafka，解耦业务逻辑和审计/缓存刷新
 *
 * <p>对应优化文档 9.3 节
 *
 * <p>使用方式（在 DomainService 中注入）：
 * <pre>{@code
 * @Resource
 * private PictureEventProducer eventProducer;
 *
 * // 图片上传后
 * pictureRepository.save(picture);
 * eventProducer.publish(PictureEvent.of(
 *     PictureEventType.UPLOADED, picture.getId(), picture.getName(),
 *     loginUser.getId(), loginUser.getUserAccount(), picture.getSpaceId()
 * ));
 * }</pre>
 *
 * <p>Topic: {@code picture-events} (4 分区, 复制因子 2)
 * <p>消息键: pictureId（保证同一图片事件有序）
 *
 * @author vivin
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aituku.kafka.streams.enabled", havingValue = "true")
public class PictureEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "picture-events";

    /**
     * 发布图片事件（异步非阻塞）
     *
     * <p>发送失败仅记录 ERROR 日志，不影响主业务流程
     *
     * @param event 图片事件
     */
    public void publish(PictureEvent event) {
        String key = String.valueOf(event.getPictureId());
        String value;

        try {
            value = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("[事件发布] JSON 序列化失败, eventId={}", event.getEventId(), e);
            return;
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
        // 添加自定义头信息
        record.headers().add("eventType", event.getEventType().name().getBytes());
        record.headers().add("timestamp", event.getTimestamp().toString().getBytes());

        CompletableFuture<?> future = kafkaTemplate.send(record)
            .thenAccept(result -> {
                if (log.isDebugEnabled()) {
                    log.debug("[事件发布] eventId={}, type={}, pictureId={}, partition={}, offset={}",
                        event.getEventId(), event.getEventType(),
                        event.getPictureId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            })
            .exceptionally(ex -> {
                log.error("[事件发布] 发送失败, eventId={}, type={}, pictureId={}",
                    event.getEventId(), event.getEventType(), event.getPictureId(), ex);
                return null;
            });
    }

    /**
     * 同步发布（用于需要确认的场景，如审核操作）
     */
    public boolean publishSync(PictureEvent event) {
        try {
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getPictureId()), value).get();
            return true;
        } catch (Exception e) {
            log.error("[事件发布] 同步发送失败, eventId={}", event.getEventId(), e);
            return false;
        }
    }

    /**
     * 便捷方法：上传事件
     */
    public void publishUploaded(Long pictureId, String pictureName,
                                 Long operatorId, String operatorAccount, Long spaceId) {
        publish(PictureEvent.of(PictureEventType.UPLOADED,
            pictureId, pictureName, operatorId, operatorAccount, spaceId));
    }

    /**
     * 便捷方法：编辑事件
     */
    public void publishEdited(Long pictureId, String pictureName,
                               Long operatorId, String operatorAccount, Long spaceId) {
        publish(PictureEvent.of(PictureEventType.EDITED,
            pictureId, pictureName, operatorId, operatorAccount, spaceId));
    }

    /**
     * 便捷方法：删除事件
     */
    public void publishDeleted(Long pictureId, String pictureName,
                                Long operatorId, String operatorAccount, Long spaceId) {
        publish(PictureEvent.of(PictureEventType.DELETED,
            pictureId, pictureName, operatorId, operatorAccount, spaceId));
    }

    /**
     * 便捷方法：审核事件
     */
    public void publishReviewed(Long pictureId, String pictureName, boolean approved,
                                 Long operatorId, String operatorAccount, Long spaceId,
                                 String reviewMessage) {
        PictureEvent event = PictureEvent.of(
            approved ? PictureEventType.REVIEW_APPROVED : PictureEventType.REVIEW_REJECTED,
            pictureId, pictureName, operatorId, operatorAccount, spaceId
        );
        event.setMetadata(java.util.Map.of("reviewMessage",
            reviewMessage != null ? reviewMessage : ""));
        publish(event);
    }
}
