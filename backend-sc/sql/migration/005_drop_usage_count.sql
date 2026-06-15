-- 删除 picture_tag 表的 usageCount 字段，改为实时 COUNT 查询
ALTER TABLE `picture_tag` DROP COLUMN `usageCount`;

-- 同时删除 usageCount 上的索引
DROP INDEX `idx_usageCount` ON `picture_tag`;
