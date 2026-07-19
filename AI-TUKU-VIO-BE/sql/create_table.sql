-- ============================================================
-- AI-TuKu 数据库建表脚本
-- ============================================================
-- 使用方法：在 IDEA 中右键 → Run 或在 mysql 命令行 source 此文件
-- 数据库会自动创建（若已存在则跳过）
-- ============================================================

CREATE DATABASE IF NOT EXISTS vio_tuku
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE vio_tuku;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS user (
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    userAccount VARCHAR(191)                            NOT NULL COMMENT '账号',
    userPassword VARCHAR(512)                           NOT NULL COMMENT '密码',
    userName    VARCHAR(191)                            NULL COMMENT '用户昵称',
    userAvatar  VARCHAR(1024)                           NULL COMMENT '用户头像',
    userProfile VARCHAR(512)                            NULL COMMENT '用户简介',
    userRole    VARCHAR(256) DEFAULT 'user'             NOT NULL COMMENT '用户角色: user/admin',
    editTime    DATETIME     DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '编辑时间',
    createTime  DATETIME     DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updateTime  DATETIME     DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT      DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) COMMENT '用户' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. 空间表（⚠ 之前缺失，现已补充）
-- ============================================================
CREATE TABLE IF NOT EXISTS space (
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    spaceName   VARCHAR(128)                            NOT NULL COMMENT '空间名称',
    spaceLevel  INT         DEFAULT 0                   NOT NULL COMMENT '空间级别: 0-普通 1-专业 2-旗舰',
    spaceType   INT         DEFAULT 0                   NOT NULL COMMENT '空间类型: 0-私有 1-团队',
    maxSize     BIGINT      DEFAULT 104857600           NOT NULL COMMENT '最大容量(字节), 默认100MB',
    maxCount    BIGINT      DEFAULT 100                 NOT NULL COMMENT '最大图片数',
    totalSize   BIGINT      DEFAULT 0                   NOT NULL COMMENT '已使用容量(字节)',
    totalCount  BIGINT      DEFAULT 0                   NOT NULL COMMENT '已使用图片数',
    userId      BIGINT                                  NOT NULL COMMENT '创建用户 id',
    createTime  DATETIME    DEFAULT CURRENT_TIMESTAMP   NOT NULL COMMENT '创建时间',
    editTime    DATETIME    DEFAULT CURRENT_TIMESTAMP   NOT NULL COMMENT '编辑时间',
    updateTime  DATETIME    DEFAULT CURRENT_TIMESTAMP   NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT     DEFAULT 0                   NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_spaceName (spaceName)
) COMMENT '空间' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. 图片表
-- ============================================================
CREATE TABLE IF NOT EXISTS picture (
    id             BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    url            VARCHAR(512)                         NOT NULL COMMENT '图片 url',
    thumbnailUrl   VARCHAR(512)                         NULL COMMENT '缩略图 url',
    name           VARCHAR(128)                         NOT NULL COMMENT '图片名称',
    introduction   VARCHAR(512)                         NULL COMMENT '简介',
    category       VARCHAR(64)                          NULL COMMENT '分类',
    tags           VARCHAR(512)                         NULL COMMENT '标签（JSON 数组）',
    picSize        BIGINT                               NULL COMMENT '图片体积',
    picWidth       INT                                  NULL COMMENT '图片宽度',
    picHeight      INT                                  NULL COMMENT '图片高度',
    picScale       DOUBLE                               NULL COMMENT '图片宽高比例',
    picFormat      VARCHAR(32)                          NULL COMMENT '图片格式',
    picColor       VARCHAR(16)                          NULL COMMENT '图片主色调',
    userId         BIGINT                               NOT NULL COMMENT '创建用户 id',
    spaceId        BIGINT                               NULL COMMENT '空间 id（NULL 表示公共空间）',
    reviewStatus   INT        DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage  VARCHAR(512)                         NULL COMMENT '审核信息',
    reviewerId     BIGINT                               NULL COMMENT '审核人 ID',
    reviewTime     DATETIME                             NULL COMMENT '审核时间',
    createTime     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime       DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime     DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT    DEFAULT 0                 NOT NULL COMMENT '是否删除',
    INDEX idx_name (name),
    INDEX idx_introduction (introduction(191)),
    INDEX idx_category (category),
    INDEX idx_tags (tags(191)),
    INDEX idx_userId (userId),
    INDEX idx_reviewStatus (reviewStatus),
    INDEX idx_spaceId_reviewStatus (spaceId, reviewStatus),
    INDEX idx_userId_createTime (userId, createTime)
) COMMENT '图片' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. 空间成员表
-- ============================================================
CREATE TABLE IF NOT EXISTS space_user (
    id          BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    spaceId     BIGINT                                  NOT NULL COMMENT '空间 id',
    userId      BIGINT                                  NOT NULL COMMENT '用户 id',
    spaceRole   VARCHAR(64)  DEFAULT 'viewer'           NOT NULL COMMENT '空间角色: viewer/editor/admin',
    createTime  DATETIME     DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updateTime  DATETIME     DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete    TINYINT      DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE INDEX uk_spaceId_userId (spaceId, userId)
) COMMENT '空间成员' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
