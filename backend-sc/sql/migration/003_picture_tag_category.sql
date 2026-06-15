-- ============================================================
-- 迁移: 图片分类和标签规范化
-- 将 picture 表中的 category (varchar) 和 tags (JSON数组字符串)
-- 拆分为独立的 category/tag 表，支持分类管理和热门标签
-- 注意: 字段使用驼峰命名，与现有表风格保持一致
--       (mybatis-plus.map-underscore-to-camel-case = false)
-- ============================================================

-- 1. 新建图片分类表
CREATE TABLE IF NOT EXISTS `picture_category`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`       varchar(50) NOT NULL COMMENT '分类名称（如：风景、人物、动漫）',
    `sortOrder`  int         NOT NULL DEFAULT 0  COMMENT '排序权重（数字越小越靠前）',
    `isActive`   tinyint(1)  NOT NULL DEFAULT 1  COMMENT '是否启用（0:禁用 1:启用）',
    `createTime` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='图片分类表';

-- 2. 新建图片标签表
CREATE TABLE IF NOT EXISTS `picture_tag`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(50) NOT NULL COMMENT '标签名称（统一小写、去前后空格存储）',
    `createTime`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
    `updateTime`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='图片标签表';

-- 3. 新建图片-标签关联表
CREATE TABLE IF NOT EXISTS `picture_tag_relation`
(
    `pictureId`  bigint   NOT NULL COMMENT '图片ID',
    `tagId`      bigint   NOT NULL COMMENT '标签ID',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (`pictureId`, `tagId`),
    KEY `idx_tag_picture` (`tagId`, `pictureId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='图片-标签关联表';

-- 4. 将 picture 表中已有的 category 值导入 picture_category 表
INSERT IGNORE INTO `picture_category` (`name`)
SELECT DISTINCT `category`
FROM `picture`
WHERE `category` IS NOT NULL
  AND `category` != '';

-- 5. picture 表新增 categoryId 字段
ALTER TABLE `picture`
    ADD COLUMN `categoryId` bigint DEFAULT NULL COMMENT '分类ID（关联 picture_category.id）' AFTER `category`;

-- 6. 回填 categoryId（用原 category 字符串匹配 picture_category.name）
UPDATE `picture` p
    INNER JOIN `picture_category` pc ON p.`category` = pc.`name`
SET p.`categoryId` = pc.`id`;

-- 7. 为 categoryId 建索引
ALTER TABLE `picture`
    ADD INDEX `idx_categoryId` (`categoryId`);
