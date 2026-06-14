-- VIP 系统相关表

-- 1. VIP 兑换码表（简化版：一码一用）
CREATE TABLE IF NOT EXISTS vip_code
(
    id            BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL COMMENT '兑换码（唯一）',
    vipDuration   INT          NOT NULL COMMENT 'VIP 时长（天）',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-未使用，1-已使用，2-已禁用',
    expirationTime DATETIME    NULL COMMENT '兑换码过期时间',
    description   VARCHAR(256) NULL COMMENT '兑换码描述（如活动名称）',
    createTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    INDEX idx_code (code),
    UNIQUE KEY uk_code (code)
) COMMENT 'VIP 兑换码表' COLLATE = utf8mb4_unicode_ci;

-- 2. VIP 兑换记录表
CREATE TABLE IF NOT EXISTS vip_order
(
    id            BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    userId        BIGINT       NOT NULL COMMENT '用户 ID',
    codeId        BIGINT       NOT NULL COMMENT '兑换码 ID',
    code          VARCHAR(32)  NOT NULL COMMENT '兑换码（冗余，方便查询）',
    vipDuration   INT          NOT NULL COMMENT 'VIP 时长（天）',
    oldExpireTime DATETIME     NULL COMMENT '用户使用兑换码前，他的VIP到期时间',
    newExpireTime DATETIME     NOT NULL COMMENT '用户使用兑换码后，他的VIP到期时间',
    createTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
    updateTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除',
    INDEX idx_user (userId),
    INDEX idx_code (code),
    INDEX idx_create_time (createTime),
    UNIQUE KEY uk_user_code (userId, code)
) COMMENT 'VIP 兑换记录表' COLLATE = utf8mb4_unicode_ci;

-- 3. 修改 user 表，增加 VIP 过期时间字段
-- NULL = 非 VIP 用户，有值 = VIP 用户（过期时间）
ALTER TABLE user ADD COLUMN vipExpireTime DATETIME NULL COMMENT 'VIP 过期时间（NULL=非VIP，有值=VIP过期时间）' AFTER userRole;

-- 3.1 为 VIP 过期降级定时任务添加索引（性能优化）
-- 查询条件：WHERE userRole = 'vip' AND vipExpireTime < NOW() ORDER BY id
-- 联合索引覆盖查询，避免全表扫描
CREATE INDEX idx_user_role_expire_time ON user (userRole, vipExpireTime, id);

-- 4. 插入测试兑换码（一码一用）
INSERT INTO vip_code (code, vipDuration, expirationTime, description)
VALUES
    ('TUKU2024VIP30A1', 30, DATE_ADD(NOW(), INTERVAL 1 YEAR), '30天VIP体验码'),
    ('TUKU2024VIP30A2', 30, DATE_ADD(NOW(), INTERVAL 1 YEAR), '30天VIP体验码'),
    ('TUKU2024VIP90B1', 90, DATE_ADD(NOW(), INTERVAL 1 YEAR), '90天VIP季度码'),
    ('TUKU2024VIP90B2', 90, DATE_ADD(NOW(), INTERVAL 1 YEAR), '90天VIP季度码'),
    ('TUKU2024VIP365C1', 365, DATE_ADD(NOW(), INTERVAL 1 YEAR), '365天VIP年度码');