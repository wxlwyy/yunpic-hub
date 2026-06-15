package com.wyy.yunpicturebackend.manager.migration;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wyy.yunpicturebackend.mapper.PictureMapper;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.service.PictureTagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 一次性数据迁移：将 picture.tags (JSON 数组字符串) 迁移到 picture_tag + picture_tag_relation
 *
 * 迁移完成后请删除此类，或添加标记避免重复执行。
 * 执行条件：应用启动时自动检测，仅当 picture_tag 表为空时执行。
 */
//@Component
@Slf4j
public class TagDataMigrationRunner implements CommandLineRunner {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private PictureTagService pictureTagService;

    /**
     * 标记：是否已经完成迁移。执行一次后手动改为 true，避免每次启动都跑。
     */
    private static final boolean MIGRATION_DONE = false;

    @Override
    public void run(String... args) {
        if (MIGRATION_DONE) {
            log.info("标签数据迁移已完成，跳过");
            return;
        }

        log.info("========================================");
        log.info("开始迁移图片标签数据...");

        int totalMigrated = 0;
        int totalTags = 0;

        // 分批查询所有有 tags 的图片（一次 500 条，避免内存溢出）
        int pageSize = 500;
        int offset = 0;
        while (true) {
            List<Picture> pictures = pictureMapper.selectList(
                    new LambdaQueryWrapper<Picture>()
                            .isNotNull(Picture::getTags)
                            .ne(Picture::getTags, "")
                            .ne(Picture::getTags, "[]")
                            .last("LIMIT " + offset + ", " + pageSize)
            );

            if (CollUtil.isEmpty(pictures)) {
                break;
            }

            for (Picture picture : pictures) {
                try {
                    List<String> tagNames = JSONUtil.toList(picture.getTags(), String.class);
                    if (CollUtil.isNotEmpty(tagNames)) {
                        pictureTagService.addRelations(picture.getId(), tagNames);
                        totalMigrated++;
                        totalTags += tagNames.size();
                    }
                } catch (Exception e) {
                    log.error("迁移图片标签失败, pictureId={}: {}", picture.getId(), e.getMessage());
                }
            }

            log.info("已迁移 {} 张图片...", totalMigrated);
            offset += pageSize;
        }

        log.info("标签数据迁移完成！共迁移 {} 张图片，{} 个标签关联", totalMigrated, totalTags);
        log.info("========================================");
    }
}
