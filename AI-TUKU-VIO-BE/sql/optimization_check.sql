-- ============================================================
-- AI-TuKu 数据库优化诊断脚本 (第 8.1 / 8.2 节)
--
-- 用途: 定期检查数据库健康状况
-- 执行: mysql -u root -p vio_tuku < optimization_check.sql
-- ============================================================

USE vio_tuku;

SELECT '============================================' AS '';
SELECT '  AI-TuKu 数据库优化诊断报告' AS '';
SELECT CONCAT('  检查时间: ', NOW()) AS '';
SELECT '============================================' AS '';

-- ============================================================
-- 1. 表大小统计 (数据量 + 索引量)
-- ============================================================
SELECT '--- 1. 表大小统计 ---' AS '';
SELECT
    TABLE_NAME AS '表名',
    TABLE_ROWS AS '行数(估算)',
    ROUND(DATA_LENGTH / 1024 / 1024, 2) AS '数据大小(MB)',
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) AS '索引大小(MB)',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS '总大小(MB)',
    TABLE_COMMENT AS '注释'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'vio_tuku'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ============================================================
-- 2. 索引使用情况 (检查冗余/未使用索引)
-- ============================================================
SELECT '--- 2. 索引统计 (检查冗余索引) ---' AS '';
SELECT
    TABLE_NAME AS '表名',
    INDEX_NAME AS '索引名',
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS '索引列',
    NOT NON_UNIQUE AS '唯一',
    CARDINALITY AS '基数'
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'vio_tuku'
  AND TABLE_NAME IN ('picture', 'space_user', 'user')
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE, CARDINALITY
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================================
-- 3. 慢查询分析 (需要 slow_query_log 开启)
-- ============================================================
SELECT '--- 3. 典型查询 EXPLAIN 分析 ---' AS '';

-- 场景1: 空间图片列表 (最频繁)
SELECT '>>> 场景1: 空间图片列表查询 (频次最高)' AS '';
EXPLAIN
SELECT id, name, url, thumbnailUrl, category, tags, createTime
FROM picture
WHERE spaceId = 1 AND reviewStatus = 1 AND isDelete = 0
ORDER BY createTime DESC
LIMIT 20;

-- 场景2: 我的图片 (用户个人中心)
SELECT '>>> 场景2: 我的图片查询' AS '';
EXPLAIN
SELECT id, name, url, thumbnailUrl, createTime
FROM picture
WHERE userId = 10001 AND isDelete = 0
ORDER BY createTime DESC
LIMIT 20;

-- 场景3: 空间成员权限校验 (超高频)
SELECT '>>> 场景3: 空间成员权限校验 (超高频)' AS '';
EXPLAIN
SELECT id, spaceId, userId, spaceRole
FROM space_user
WHERE spaceId = 1 AND userId = 10001 AND isDelete = 0;

-- 场景4: 模糊搜索 (当前使用 LIKE)
SELECT '>>> 场景4: 图片名称模糊搜索 (全表扫描风险)' AS '';
EXPLAIN
SELECT id, name, url
FROM picture
WHERE name LIKE '%风景%' AND isDelete = 0
LIMIT 20;

-- 场景5: 审核列表 (管理员)
SELECT '>>> 场景5: 待审核图片列表' AS '';
EXPLAIN
SELECT id, name, url, userId, reviewStatus, createTime
FROM picture
WHERE reviewStatus = 0 AND isDelete = 0
ORDER BY createTime ASC
LIMIT 20;

-- ============================================================
-- 4. 软删除数据统计 (判断是否需要归档)
-- ============================================================
SELECT '--- 4. 软删除数据统计 ---' AS '';
SELECT
    'picture' AS '表名',
    COUNT(*) AS '已删除(>=30天)',
    COUNT(CASE WHEN updateTime >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) AS '已删除(<30天)',
    COUNT(CASE WHEN updateTime < DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END) AS '待归档(>=30天)'
FROM picture WHERE isDelete = 1
UNION ALL
SELECT
    'space_user',
    COUNT(*),
    COUNT(CASE WHEN updateTime >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END),
    COUNT(CASE WHEN updateTime < DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 END)
FROM space_user WHERE isDelete = 1;

-- ============================================================
-- 5. 归档表统计 (如果已创建)
-- ============================================================
SELECT '--- 5. 归档表统计 ---' AS '';
SELECT
    'picture_archive' AS '归档表',
    COUNT(*) AS '归档条数',
    MIN(archiveTime) AS '最早归档时间',
    MAX(archiveTime) AS '最近归档时间'
FROM picture_archive
UNION ALL
SELECT
    'space_user_archive',
    COUNT(*),
    MIN(archiveTime),
    MAX(archiveTime)
FROM space_user_archive;

-- ============================================================
-- 6. 连接池状态 (MySQL 运行时)
-- ============================================================
SELECT '--- 6. 连接池状态 ---' AS '';
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Threads_running';
SHOW STATUS LIKE 'Max_used_connections';
SHOW VARIABLES LIKE 'max_connections';

-- ============================================================
-- 7. InnoDB 缓冲池命中率 (关键性能指标)
-- ============================================================
SELECT '--- 7. InnoDB 缓冲池命中率 ---' AS '';
SELECT
    ROUND((
        (SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests') -
        (SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'Innodb_buffer_pool_reads')
    ) * 100.0 / (
        SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME = 'Innodb_buffer_pool_read_requests'
    ), 2) AS '缓冲池命中率(%)';
-- 目标: > 99.5%
-- < 99% 表示内存不足, 数据无法常驻内存, 需增大 innodb_buffer_pool_size

SELECT '============================================' AS '';
SELECT '  诊断完成' AS '';
SELECT '============================================' AS '';
