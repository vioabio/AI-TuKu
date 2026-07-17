package com.vio.aitukuviobe.shared.eventstream.model;

/**
 * 图片操作事件类型 — 对应优化文档 9.3 节 (Kafka Streams 事件驱动)
 *
 * <p>每种图片操作都产生一个事件，由 Kafka Streams 拓扑处理后：
 * <ul>
 *   <li>写入审计日志（audit_log topic → 审计表）</li>
 *   <li>更新缓存/搜索索引（trigger cache invalidation / ES reindex）</li>
 *   <li>触发下游通知（如审核结果通知用户）</li>
 * </ul>
 *
 * @author vivin
 */
public enum PictureEventType {

    /** 图片上传 */
    UPLOADED,

    /** 图片信息编辑 */
    EDITED,

    /** 图片软删除 */
    DELETED,

    /** 审核通过 */
    REVIEW_APPROVED,

    /** 审核拒绝 */
    REVIEW_REJECTED,

    /** AI 扩图任务创建 */
    OUTPAINTING_CREATED,

    /** AI 扩图任务完成 */
    OUTPAINTING_COMPLETED,

    /** 图片被添加到空间 */
    ADDED_TO_SPACE,

    /** 图片从空间移除 */
    REMOVED_FROM_SPACE
}
