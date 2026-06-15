package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.entity.PictureTag;

import java.util.List;

/**
 * 图片标签服务
 */
public interface PictureTagService extends IService<PictureTag> {

    // ==================== 查询 ====================

    /**
     * 根据图片ID查询该图片的标签名称列表
     */
    List<String> getTagNamesByPictureId(Long pictureId);

    /**
     * 热门标签 TOP N（实时 COUNT 排序）
     */
    List<String> listHotTagNames(int topN);

    /**
     * 标签名称 → 图片ID（标签表 + 关联表联查）
     */
    List<Long> findPictureIdsByTagNames(List<String> tagNames);

    /**
     * 标签ID → 标签名称
     */
    List<String> getTagNamesByIds(List<Long> tagIds);

    // ==================== 写入 ====================

    /**
     * 标签名称 → 标签ID，不存在则自动创建
     */
    List<Long> getOrCreateByNames(List<String> tagNames);

    /**
     * 删除图片的所有标签关联
     */
    void deleteRelationsByPictureId(Long pictureId);

    /**
     * 为图片添加标签关联（标签不存在则自动创建）
     */
    void addRelations(Long pictureId, List<String> tagNames);
}
