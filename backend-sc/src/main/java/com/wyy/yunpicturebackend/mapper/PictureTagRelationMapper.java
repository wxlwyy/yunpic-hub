package com.wyy.yunpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wyy.yunpicturebackend.model.entity.PictureTagRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @description 针对表【picture_tag_relation(图片-标签关联表)】的数据库操作Mapper
* @Entity com.wyy.yunpicturebackend.model.entity.PictureTagRelation
*/
public interface PictureTagRelationMapper extends BaseMapper<PictureTagRelation> {

    /**
     * 根据图片ID查询关联的标签ID列表（只查自己的表）
     */
    List<Long> selectTagIdsByPictureId(@Param("pictureId") Long pictureId);

    /**
     * 批量插入关联（批量编辑）
     */
    int insertBatch(@Param("pictureId") Long pictureId, @Param("tagIds") List<Long> tagIds);

    /**
     * 根据标签ID列表，查询包含这些标签的图片ID（OR 逻辑：命中任一标签即返回）
     */
    List<Long> selectPictureIdsByTagIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 热门标签 TOP N：统计每个 tagId 出现次数，按降序返回 tagId（只查自己的表）
     */
    List<Long> selectHotTagIds(@Param("topN") int topN);
}
