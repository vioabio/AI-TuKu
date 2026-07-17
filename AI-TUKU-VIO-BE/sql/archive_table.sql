-- ============================================================
-- AI-TuKu 软删除数据归档 (第 8.2 节)
--
-- 策略: isDelete=1 且超过 30 天的数据自动迁移到归档表
-- 收益: 业务表瘦身, 查询性能提升, 数据不丢失
--
-- 归档表命名: {原表名}_archive
-- 归档触发: 定时任务 (每天凌晨 3 点执行)
-- ============================================================

USE vio_tuku;

-- ============================================================
-- 1. 归档表定义
-- ============================================================

-- 图片归档表 (结构与 picture 一致 + 归档时间)
CREATE TABLE IF NOT EXISTS picture_archive
(
    id             bigint                                   not null comment '原表 id',
    url            varchar(512)                             not null comment '图片 url',
    thumbnailUrl   varchar(512)                             null comment '缩略图 url',
    name           varchar(128)                             not null comment '图片名称',
    introduction   varchar(512)                             null comment '简介',
    category       varchar(64)                              null comment '分类',
    tags           varchar(512)                             null comment '标签（JSON 数组）',
    picSize        bigint                                   null comment '图片体积',
    picWidth       int                                      null comment '图片宽度',
    picHeight      int                                      null comment '图片高度',
    picScale       double                                   null comment '图片宽高比例',
    picFormat      varchar(32)                              null comment '图片格式',
    picColor       varchar(16)                              null comment '图片主色调',
    userId         bigint                                   not null comment '创建用户 id',
    spaceId        bigint                                   null comment '空间 id',
    reviewStatus   int        default 0                     null comment '审核状态',
    reviewMessage  varchar(512)                             null comment '审核信息',
    reviewerId     bigint                                   null comment '审核人 ID',
    reviewTime     datetime                                 null comment '审核时间',
    createTime     datetime                                 not null comment '创建时间',
    editTime       datetime                                 null comment '编辑时间',
    updateTime     datetime                                 null comment '更新时间',
    deleteTime     datetime                                 not null comment '原表删除时间',
    archiveTime    datetime   default CURRENT_TIMESTAMP     not null comment '归档时间',
    PRIMARY KEY (id),
    INDEX idx_archive_userId (userId),
    INDEX idx_archive_deleteTime (deleteTime),
    INDEX idx_archive_archiveTime (archiveTime)
) comment '图片归档表 — isDelete=1 超过30天的数据自动迁移至此' collate = utf8mb4_unicode_ci;

-- 空间成员归档表
CREATE TABLE IF NOT EXISTS space_user_archive
(
    id          bigint                                 not null comment '原表 id',
    spaceId     bigint                                 not null comment '空间 id',
    userId      bigint                                 not null comment '用户 id',
    spaceRole   varchar(64)                            not null comment '空间角色',
    createTime  datetime                               not null comment '创建时间',
    updateTime  datetime                               null comment '更新时间',
    deleteTime  datetime                               not null comment '原表删除时间',
    archiveTime datetime default CURRENT_TIMESTAMP     not null comment '归档时间',
    PRIMARY KEY (id),
    INDEX idx_su_archive_spaceId (spaceId),
    INDEX idx_su_archive_deleteTime (deleteTime)
) comment '空间成员归档表' collate = utf8mb4_unicode_ci;

-- ============================================================
-- 2. 归档存储过程 (可选: 也可以由后端 Java 定时任务执行)
-- ============================================================

DELIMITER //

-- 图片归档存储过程
CREATE PROCEDURE IF NOT EXISTS archive_deleted_pictures(IN archive_days INT)
BEGIN
    DECLARE archived_count INT DEFAULT 0;

    -- 将 isDelete=1 且超过 archive_days 天的数据迁移到归档表
    INSERT INTO picture_archive
        (id, url, thumbnailUrl, name, introduction, category, tags,
         picSize, picWidth, picHeight, picScale, picFormat, picColor,
         userId, spaceId, reviewStatus, reviewMessage, reviewerId,
         reviewTime, createTime, editTime, updateTime, deleteTime)
    SELECT
        id, url, thumbnailUrl, name, introduction, category, tags,
        picSize, picWidth, picHeight, picScale, picFormat, picColor,
        userId, spaceId, reviewStatus, reviewMessage, reviewerId,
        reviewTime, createTime, editTime, updateTime, updateTime as deleteTime
    FROM picture
    WHERE isDelete = 1
      AND updateTime < DATE_SUB(NOW(), INTERVAL archive_days DAY)
      AND id NOT IN (SELECT id FROM picture_archive);

    SET archived_count = ROW_COUNT();

    -- 从原表删除已归档数据
    DELETE FROM picture
    WHERE isDelete = 1
      AND updateTime < DATE_SUB(NOW(), INTERVAL archive_days DAY)
      AND id IN (SELECT id FROM picture_archive);

    SELECT archived_count AS 'archived_pictures';
END //

-- 空间成员归档存储过程
CREATE PROCEDURE IF NOT EXISTS archive_deleted_space_users(IN archive_days INT)
BEGIN
    DECLARE archived_count INT DEFAULT 0;

    INSERT INTO space_user_archive
        (id, spaceId, userId, spaceRole, createTime, updateTime, deleteTime)
    SELECT
        id, spaceId, userId, spaceRole, createTime, updateTime, updateTime as deleteTime
    FROM space_user
    WHERE isDelete = 1
      AND updateTime < DATE_SUB(NOW(), INTERVAL archive_days DAY)
      AND id NOT IN (SELECT id FROM space_user_archive);

    SET archived_count = ROW_COUNT();

    DELETE FROM space_user
    WHERE isDelete = 1
      AND updateTime < DATE_SUB(NOW(), INTERVAL archive_days DAY)
      AND id IN (SELECT id FROM space_user_archive);

    SELECT archived_count AS 'archived_space_users';
END //

DELIMITER ;

-- ============================================================
-- 3. 手动测试归档
-- ============================================================

-- 调用存储过程 (归档 30 天前删除的数据):
-- CALL archive_deleted_pictures(30);
-- CALL archive_deleted_space_users(30);

-- 查看归档统计:
-- SELECT COUNT(*) AS total_archived FROM picture_archive;
-- SELECT COUNT(*) AS total_archived FROM space_user_archive;

-- ============================================================
-- 4. 清理归档数据 (归档超过 180 天可彻底删除)
-- ============================================================
-- DELETE FROM picture_archive WHERE archiveTime < DATE_SUB(NOW(), INTERVAL 180 DAY);
-- DELETE FROM space_user_archive WHERE archiveTime < DATE_SUB(NOW(), INTERVAL 180 DAY);
