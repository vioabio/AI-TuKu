package com.vio.aitukuviobe.infrastructure.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 软删除数据归档定时任务 — 对应优化文档 8.2 节
 *
 * <p>策略：
 * <ul>
 *   <li>每天凌晨 3:00 执行</li>
 *   <li>将 isDelete=1 且 updateTime 超过 archiveDays 天的数据迁移到归档表</li>
 *   <li>归档后从业务表物理删除，业务表保持瘦身</li>
 * </ul>
 *
 * <p>归档表：
 * <ul>
 *   <li>picture → picture_archive</li>
 *   <li>space_user → space_user_archive</li>
 * </ul>
 *
 * <p>启用方式：{@code aituku.archive.enabled=true}（默认 false）
 * <p>使用前需先执行 sql/archive_table.sql 创建归档表
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aituku.archive.enabled", havingValue = "true")
public class DataArchiveScheduler {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 归档阈值（天）：isDelete=1 且超过此天数的数据将被归档
     */
    @Value("${aituku.archive.retention-days:30}")
    private int archiveDays;

    /**
     * 归档数据保留天数：归档表中的数据超过此天数将被彻底删除
     */
    @Value("${aituku.archive.cleanup-days:180}")
    private int cleanupDays;

    /**
     * 每次归档的批次大小
     */
    @Value("${aituku.archive.batch-size:1000}")
    private int batchSize;

    /**
     * 图片数据归档
     * <p>每天凌晨 3:05 执行（错开 3:00 整点的高峰）
     */
    @Scheduled(cron = "${aituku.archive.cron:0 5 3 * * ?}")
    public void archiveDeletedPictures() {
        log.info("[数据归档] 开始归档 picture 表 (isDelete=1, 超过 {} 天)", archiveDays);
        long start = System.currentTimeMillis();

        try {
            // 1. 将符合条件的已删除数据插入归档表
            String insertSql = """
                INSERT IGNORE INTO picture_archive
                    (id, url, thumbnailUrl, name, introduction, category, tags,
                     picSize, picWidth, picHeight, picScale, picFormat, picColor,
                     userId, spaceId, reviewStatus, reviewMessage, reviewerId,
                     reviewTime, createTime, editTime, updateTime, deleteTime)
                SELECT
                    id, url, thumbnailUrl, name, introduction, category, tags,
                    picSize, picWidth, picHeight, picScale, picFormat, picColor,
                    userId, spaceId, reviewStatus, reviewMessage, reviewerId,
                    reviewTime, createTime, editTime, updateTime, updateTime
                FROM picture
                WHERE isDelete = 1
                  AND updateTime < DATE_SUB(NOW(), INTERVAL ? DAY)
                LIMIT ?
                """;

            int archived = jdbcTemplate.update(insertSql, archiveDays, batchSize);

            // 2. 从原表删除已归档数据
            if (archived > 0) {
                String deleteSql = """
                    DELETE FROM picture
                    WHERE isDelete = 1
                      AND updateTime < DATE_SUB(NOW(), INTERVAL ? DAY)
                      AND id IN (SELECT id FROM picture_archive)
                    LIMIT ?
                    """;
                int deleted = jdbcTemplate.update(deleteSql, archiveDays, batchSize);

                long elapsed = System.currentTimeMillis() - start;
                log.info("[数据归档] picture 表归档完成: 归档 {} 条, 删除 {} 条, 耗时 {}ms",
                    archived, deleted, elapsed);
            } else {
                log.info("[数据归档] picture 表无需归档的数据");
            }
        } catch (Exception e) {
            log.error("[数据归档] picture 表归档失败", e);
        }
    }

    /**
     * 空间成员数据归档
     * <p>每天凌晨 3:15 执行
     */
    @Scheduled(cron = "${aituku.archive.space-user-cron:0 15 3 * * ?}")
    public void archiveDeletedSpaceUsers() {
        log.info("[数据归档] 开始归档 space_user 表 (isDelete=1, 超过 {} 天)", archiveDays);
        long start = System.currentTimeMillis();

        try {
            String insertSql = """
                INSERT IGNORE INTO space_user_archive
                    (id, spaceId, userId, spaceRole, createTime, updateTime, deleteTime)
                SELECT
                    id, spaceId, userId, spaceRole, createTime, updateTime, updateTime
                FROM space_user
                WHERE isDelete = 1
                  AND updateTime < DATE_SUB(NOW(), INTERVAL ? DAY)
                LIMIT ?
                """;

            int archived = jdbcTemplate.update(insertSql, archiveDays, batchSize);

            if (archived > 0) {
                String deleteSql = """
                    DELETE FROM space_user
                    WHERE isDelete = 1
                      AND updateTime < DATE_SUB(NOW(), INTERVAL ? DAY)
                      AND id IN (SELECT id FROM space_user_archive)
                    LIMIT ?
                    """;
                int deleted = jdbcTemplate.update(deleteSql, archiveDays, batchSize);

                long elapsed = System.currentTimeMillis() - start;
                log.info("[数据归档] space_user 表归档完成: 归档 {} 条, 删除 {} 条, 耗时 {}ms",
                    archived, deleted, elapsed);
            } else {
                log.info("[数据归档] space_user 表无需归档的数据");
            }
        } catch (Exception e) {
            log.error("[数据归档] space_user 表归档失败", e);
        }
    }

    /**
     * 清理超期归档数据
     * <p>每周日凌晨 4:00 执行
     * <p>归档超过 cleanupDays 天的数据彻底删除（不可恢复）
     */
    @Scheduled(cron = "${aituku.archive.cleanup-cron:0 0 4 * * SUN}")
    public void cleanupExpiredArchives() {
        log.info("[数据归档] 开始清理超过 {} 天的归档数据", cleanupDays);
        long start = System.currentTimeMillis();

        try {
            String deletePictureArchive = """
                DELETE FROM picture_archive
                WHERE archiveTime < DATE_SUB(NOW(), INTERVAL ? DAY)
                LIMIT ?
                """;
            int deletedPictures = jdbcTemplate.update(deletePictureArchive, cleanupDays, batchSize);

            String deleteSpaceUserArchive = """
                DELETE FROM space_user_archive
                WHERE archiveTime < DATE_SUB(NOW(), INTERVAL ? DAY)
                LIMIT ?
                """;
            int deletedSpaceUsers = jdbcTemplate.update(deleteSpaceUserArchive, cleanupDays, batchSize);

            long elapsed = System.currentTimeMillis() - start;
            log.info("[数据归档] 归档清理完成: picture_archive {} 条, space_user_archive {} 条, 总计耗时 {}ms",
                deletedPictures, deletedSpaceUsers, elapsed);
        } catch (Exception e) {
            log.error("[数据归档] 归档清理失败", e);
        }
    }

    /**
     * 打印归档统计信息（每周一早上 8:00）
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void logArchiveStatistics() {
        try {
            Long pictureCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM picture_archive", Long.class);
            Long spaceUserCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM space_user_archive", Long.class);
            Long deletedPictureCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM picture WHERE isDelete = 1", Long.class);
            Long deletedSpaceUserCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM space_user WHERE isDelete = 1", Long.class);

            log.info("[归档统计] 归档表: picture_archive={}, space_user_archive={} | "
                    + "待归档: picture_isDelete={}, space_user_isDelete={}",
                pictureCount, spaceUserCount, deletedPictureCount, deletedSpaceUserCount);
        } catch (Exception e) {
            log.debug("[归档统计] 统计查询失败（归档表可能尚未创建）: {}", e.getMessage());
        }
    }
}
