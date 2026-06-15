-- ============================================================
-- 迁移: 删除 picture 表中已被拆分的旧字段
-- 注意: 此脚本必须在 003 的数据迁移完成且应用代码切换后执行
--       否则代码还在读写旧字段，删掉会出问题！
-- ============================================================

-- 先在测试环境跑一遍确认数据完整：
-- SELECT COUNT(*) FROM picture WHERE categoryId IS NULL AND category IS NOT NULL AND category != '';
-- SELECT COUNT(*) FROM picture_tag_relation;

-- 如果上述查询结果正常，再执行：

ALTER TABLE `picture` DROP COLUMN `category`;
ALTER TABLE `picture` DROP COLUMN `tags`;

-- 如果 DROP COLUMN 没有自动清理索引，手动删：
-- DROP INDEX `picture_category_index` ON `picture`;
-- DROP INDEX `picture_tags_index` ON `picture`;
