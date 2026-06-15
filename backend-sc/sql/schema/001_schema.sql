-- ============================================================
-- 完整建表语句快照（仅供参考，非执行脚本）
-- 实际部署请按 migration/ 目录顺序执行增量迁移
-- 最后更新: 2025-06-15
-- ============================================================

-- ============================
-- 1. 用户表
-- ============================
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            bigint auto_increment COMMENT '用户id' PRIMARY KEY,
    `userAccount`   varchar(256) NOT NULL COMMENT '账号',
    `userPassword`  varchar(512) NOT NULL COMMENT '密码',
    `userName`      varchar(256) NULL COMMENT '用户昵称',
    `userAvatar`    varchar(1024) NULL COMMENT '用户头像',
    `userProfile`   varchar(512) NULL COMMENT '用户简介',
    `userRole`      varchar(256) NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
    `vipExpireTime` datetime NULL COMMENT 'VIP过期时间（NULL=非VIP）',
    `editTime`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `createTime`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`      tinyint  NOT NULL DEFAULT 0 COMMENT '是否删除',
    CONSTRAINT `user_userAccount_uindex` UNIQUE (`userAccount`),
    KEY `idx_userName` (`userName`),
    KEY `idx_user_role_expire_time` (`userRole`, `vipExpireTime`, `id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ============================
-- 2. 图片表
-- ============================
CREATE TABLE IF NOT EXISTS `picture`
(
    `id`             bigint auto_increment COMMENT '主键' PRIMARY KEY,
    `url`            varchar(512) NOT NULL COMMENT '图片URL',
    `thumbnailUrl`   varchar(512) NULL COMMENT '缩略图URL',
    `name`           varchar(128) NOT NULL COMMENT '图片名称',
    `introduction`   varchar(512) NULL COMMENT '图片简介',
    `categoryId`     bigint NULL COMMENT '分类ID（关联 picture_category.id）',
    `picSize`        bigint NULL COMMENT '图片体积（字节）',
    `picWidth`       int NULL COMMENT '图片宽度（px）',
    `picHeight`      int NULL COMMENT '图片高度（px）',
    `picScale`       double NULL COMMENT '宽高比',
    `picFormat`      varchar(64) NULL COMMENT '文件格式',
    `picColor`       varchar(16) NULL COMMENT '主色调（16进制RGB，如#FF0000）',
    `userId`         bigint NOT NULL COMMENT '上传用户ID',
    `spaceId`        bigint NULL COMMENT '空间ID（null=公共图库）',
    `reviewerId`     bigint NULL COMMENT '审核人ID',
    `reviewTime`     datetime NULL COMMENT '审核时间',
    `reviewStatus`   int NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核 1-通过 2-拒绝',
    `reviewMessage`  varchar(512) NULL COMMENT '审核备注',
    `createTime`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `editTime`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `updateTime`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`       tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY `idx_name` (`name`),
    KEY `idx_introduction` (`introduction`),
    KEY `idx_categoryId` (`categoryId`),
    KEY `idx_reviewStatus` (`reviewStatus`),
    KEY `idx_userId` (`userId`),
    KEY `idx_spaceId` (`spaceId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片表';

-- ============================
-- 3. 图片分类表
-- ============================
CREATE TABLE IF NOT EXISTS `picture_category`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`       varchar(50) NOT NULL COMMENT '分类名称',
    `sortOrder`  int         NOT NULL DEFAULT 0 COMMENT '排序权重（越小越靠前）',
    `isActive`   tinyint(1)  NOT NULL DEFAULT 1 COMMENT '是否启用（0:禁用 1:启用）',
    `createTime` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片分类表';

-- ============================
-- 4. 图片标签表
-- ============================
CREATE TABLE IF NOT EXISTS `picture_tag`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(50) NOT NULL COMMENT '标签名称',
    `createTime`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
    `updateTime`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片标签表';

-- ============================
-- 5. 图片-标签关联表
-- ============================
CREATE TABLE IF NOT EXISTS `picture_tag_relation`
(
    `pictureId`  bigint   NOT NULL COMMENT '图片ID',
    `tagId`      bigint   NOT NULL COMMENT '标签ID',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (`pictureId`, `tagId`),
    KEY `idx_tag_picture` (`tagId`, `pictureId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片-标签关联表';

-- ============================
-- 6. 空间表
-- ============================
CREATE TABLE IF NOT EXISTS `space`
(
    `id`         bigint auto_increment COMMENT 'id' PRIMARY KEY,
    `spaceName`  varchar(128) NULL COMMENT '空间名称',
    `spaceLevel` int NULL DEFAULT 0 COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    `maxSize`    bigint NULL DEFAULT 0 COMMENT '最大总大小',
    `maxCount`   bigint NULL DEFAULT 0 COMMENT '最大图片数量',
    `totalSize`  bigint NULL DEFAULT 0 COMMENT '当前总大小',
    `totalCount` bigint NULL DEFAULT 0 COMMENT '当前图片数量',
    `userId`     bigint NOT NULL COMMENT '创建用户ID',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `editTime`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`   tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY `idx_userId` (`userId`),
    KEY `idx_spaceName` (`spaceName`),
    KEY `idx_spaceLevel` (`spaceLevel`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '空间表';

-- ============================
-- 7. 空间成员表
-- ============================
CREATE TABLE IF NOT EXISTS `space_user`
(
    `id`         bigint AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    `spaceId`    bigint NOT NULL COMMENT '空间ID',
    `userId`     bigint NOT NULL COMMENT '用户ID',
    `spaceRole`  varchar(50) NOT NULL COMMENT '空间角色：viewer/editor/admin',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成员加入时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    KEY `idx_spaceId` (`spaceId`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '空间成员表';

-- ============================
-- 8. VIP 兑换码表
-- ============================
CREATE TABLE IF NOT EXISTS `vip_code`
(
    `id`             bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `code`           varchar(32)  NOT NULL COMMENT '兑换码（唯一）',
    `vipDuration`    int          NOT NULL COMMENT 'VIP时长（天）',
    `status`         tinyint      NOT NULL DEFAULT 0 COMMENT '状态：0-未使用 1-已使用 2-已禁用',
    `expirationTime` datetime     NULL COMMENT '兑换码过期时间',
    `description`    varchar(256) NULL COMMENT '描述',
    `createTime`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`       tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'VIP兑换码表';

-- ============================
-- 9. VIP 兑换记录表
-- ============================
CREATE TABLE IF NOT EXISTS `vip_order`
(
    `id`            bigint AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId`        bigint       NOT NULL COMMENT '用户ID',
    `codeId`        bigint       NOT NULL COMMENT '兑换码ID',
    `code`          varchar(32)  NOT NULL COMMENT '兑换码（冗余）',
    `vipDuration`   int          NOT NULL COMMENT 'VIP时长（天）',
    `oldExpireTime` datetime     NULL COMMENT '兑换前VIP到期时间',
    `newExpireTime` datetime     NOT NULL COMMENT '兑换后VIP到期时间',
    `createTime`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
    `updateTime`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`      tinyint      NOT NULL DEFAULT 0 COMMENT '是否删除',
    UNIQUE KEY `uk_user_code` (`userId`, `code`),
    KEY `idx_user` (`userId`),
    KEY `idx_code` (`code`),
    KEY `idx_createTime` (`createTime`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'VIP兑换记录表';
