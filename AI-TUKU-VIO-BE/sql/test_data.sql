-- ============================================
-- 用户模块测试数据
-- 数据库: vio_tuku
-- 密码加密方式: MD5("vio" + 原始密码)
-- ============================================
-- 所有普通用户密码: 12345678
-- 管理员密码:       admin123456
-- ============================================
# 创建库
create database if not exists vio_tuku;
USE vio_tuku;

-- 密码哈希对照:
-- "vio12345678"      -> MD5: 245eda831cc8521c310a1af26ccf07bb
-- "vioadmin123456"   -> MD5: b79b8fa0666e1a2862afd64c33232c81

-- ============================================
-- 1. 管理员账号 (2个)
-- ============================================
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole, editTime, createTime, updateTime, isDelete)
VALUES
(10001, 'admin',    'b79b8fa0666e1a2862afd64c33232c81', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '我是系统管理员，负责平台运维', 'admin', NOW(), NOW(), NOW(), 0),
(10002, 'admin2',   'b79b8fa0666e1a2862afd64c33232c81', '管理员小王', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin2', '内容审核管理员', 'admin', NOW(), NOW(), NOW(), 0);

-- ============================================
-- 2. 普通用户账号 (6个)
-- ============================================
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole, editTime, createTime, updateTime, isDelete)
VALUES
(10003, 'wangwu',   '245eda831cc8521c310a1af26ccf07bb', '王五',   'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu',   '摄影爱好者',           'user', NOW(), NOW(), NOW(), 0),
(10004, 'zhaoliu',  '245eda831cc8521c310a1af26ccf07bb', '赵六',   'https://api.dicebear.com/7.x/avataaars/svg?seed=zhaoliu',  '平面设计师',           'user', NOW(), NOW(), NOW(), 0),
(10005, 'sunqi',    '245eda831cc8521c310a1af26ccf07bb', '孙七',   'https://api.dicebear.com/7.x/avataaars/svg?seed=sunqi',    '插画师，寻找灵感中',    'user', NOW(), NOW(), NOW(), 0),
(10006, 'zhouba',   '245eda831cc8521c310a1af26ccf07bb', '周八',   'https://api.dicebear.com/7.x/avataaars/svg?seed=zhouba',   'UI/UX 设计师',         'user', NOW(), NOW(), NOW(), 0),
(10007, 'wujiu',    '245eda831cc8521c310a1af26ccf07bb', '吴九',   'https://api.dicebear.com/7.x/avataaars/svg?seed=wujiu',    '新媒体运营，经常找配图', 'user', NOW(), NOW(), NOW(), 0),
(10008, 'zhengshi', '245eda831cc8521c310a1af26ccf07bb', '郑十',   'https://api.dicebear.com/7.x/avataaars/svg?seed=zhengshi', '自由职业者，偶尔找素材', 'user', NOW(), NOW(), NOW(), 0);

-- ============================================
-- 验证数据
-- ============================================
SELECT id, userAccount, userName, userRole FROM user WHERE isDelete = 0;