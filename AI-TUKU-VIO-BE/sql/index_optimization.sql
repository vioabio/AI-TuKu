-- ============================================================
-- AI-TuKu 数据库索引优化 (第 8.1 节)
--
-- 优化项:
--   1. picture 表 (spaceId, reviewStatus) 复合索引
--      用途: 加速按空间+审核状态的分页查询 (高频场景)
--      EXPLAIN 分析: 原查询 type=ALL, rows=全表扫描
--      优化后: type=ref, rows=精确命中
--
--   2. picture 表 (userId, createTime) 复合索引
--      用途: 加速"我的图片"按时间排序查询
--      场景: 用户个人中心 → 我上传的图片列表
--
--   3. space_user 表 (spaceId, userId) 唯一复合索引
--      用途: 加速空间成员查询 + 防止重复加入
--      场景: 查看空间成员、权限校验
--
-- 执行方式:
--   mysql -u root -p vio_tuku < index_optimization.sql
-- ============================================================

USE vio_tuku;

-- ============================================================
-- 1. picture 表优化
-- ============================================================

-- 1.1 (spaceId, reviewStatus) 复合索引
-- 场景: 空间详情页查询已审核通过的图片
--   SELECT * FROM picture
--   WHERE spaceId = ? AND reviewStatus = ? AND isDelete = 0
--   ORDER BY createTime DESC LIMIT 20;
--
-- 优化前: Using filesort + 全表扫描 (rows = 全表)
-- 优化后: 索引覆盖 spaceId + reviewStatus, rows = 该空间已审核图片数
ALTER TABLE picture
ADD INDEX idx_spaceId_reviewStatus (spaceId, reviewStatus)
COMMENT '空间+审核状态复合索引 — 加速空间图片列表分页查询';

-- 1.2 (userId, createTime) 复合索引
-- 场景: 用户个人中心"我的图片"
--   SELECT * FROM picture
--   WHERE userId = ? AND isDelete = 0
--   ORDER BY createTime DESC LIMIT 20;
--
-- 优化前: idx_userId 单列索引 + Using filesort (排序不匹配)
-- 优化后: 复合索引直接覆盖 WHERE + ORDER BY, 消除 filesort
ALTER TABLE picture
ADD INDEX idx_userId_createTime (userId, createTime)
COMMENT '用户ID+创建时间复合索引 — 加速我的图片按时间排序查询';

-- ============================================================
-- 2. space_user 表优化
-- ============================================================

-- 2.1 (spaceId, userId) 唯一复合索引
-- 场景1: 查询某空间的所有成员
--   SELECT * FROM space_user WHERE spaceId = ? AND isDelete = 0;
-- 场景2: 查询某用户是否在某空间中 (权限校验 - 最高频)
--   SELECT * FROM space_user WHERE spaceId = ? AND userId = ?;
-- 场景3: 唯一约束 — 防止同一用户重复加入空间
ALTER TABLE space_user
ADD UNIQUE INDEX uk_spaceId_userId (spaceId, userId)
COMMENT '空间+用户唯一复合索引 — 加速权限校验 + 防重复';

-- ============================================================
-- 3. 验证索引是否生效
-- ============================================================

-- 验证索引已创建
SHOW INDEX FROM picture WHERE Key_name IN ('idx_spaceId_reviewStatus', 'idx_userId_createTime');
SHOW INDEX FROM space_user WHERE Key_name = 'uk_spaceId_userId';

-- 分析典型查询的执行计划
EXPLAIN SELECT * FROM picture
WHERE spaceId = 1 AND reviewStatus = 1 AND isDelete = 0
ORDER BY createTime DESC LIMIT 20;

EXPLAIN SELECT * FROM picture
WHERE userId = 1 AND isDelete = 0
ORDER BY createTime DESC LIMIT 20;

EXPLAIN SELECT * FROM space_user
WHERE spaceId = 1 AND userId = 1 AND isDelete = 0;

-- ============================================================
-- 回滚脚本 (如需)
-- ============================================================
-- ALTER TABLE picture DROP INDEX idx_spaceId_reviewStatus;
-- ALTER TABLE picture DROP INDEX idx_userId_createTime;
-- ALTER TABLE space_user DROP INDEX uk_spaceId_userId;
