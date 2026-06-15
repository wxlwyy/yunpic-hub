package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.mapper.PictureTagMapper;
import com.wyy.yunpicturebackend.mapper.PictureTagRelationMapper;
import com.wyy.yunpicturebackend.model.entity.PictureTag;
import com.wyy.yunpicturebackend.model.entity.PictureTagRelation;
import com.wyy.yunpicturebackend.service.PictureTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片标签服务实现
 */
@Service
public class PictureTagServiceImpl extends ServiceImpl<PictureTagMapper, PictureTag>
        implements PictureTagService {

    @Resource
    private PictureTagRelationMapper pictureTagRelationMapper;

    // ==================== 查询 ====================

    @Override
    public List<String> getTagNamesByPictureId(Long pictureId) {
        List<Long> tagIds = pictureTagRelationMapper.selectTagIdsByPictureId(pictureId);
        return getTagNamesByIds(tagIds);
    }

    @Override
    public List<String> listHotTagNames(int topN) {
        List<Long> hotTagIds = pictureTagRelationMapper.selectHotTagIds(topN);
        return getTagNamesByIds(hotTagIds);
    }

    @Override
    public List<String> getTagNamesByIds(List<Long> tagIds) {
        if (CollUtil.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        return listByIds(tagIds).stream()
                .map(PictureTag::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findPictureIdsByTagNames(List<String> tagNames) {
        if (CollUtil.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        List<PictureTag> tags = baseMapper.selectByNames(
                tagNames.stream()
                        .filter(StrUtil::isNotBlank)
                        .map(s -> s.trim().toLowerCase())
                        .collect(Collectors.toList())
        );
        if (CollUtil.isEmpty(tags)) {
            return Collections.emptyList();
        }
        List<Long> tagIds = tags.stream().map(PictureTag::getId).collect(Collectors.toList());
        return pictureTagRelationMapper.selectPictureIdsByTagIds(tagIds);
    }

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> getOrCreateByNames(List<String> tagNames) {
        if (CollUtil.isEmpty(tagNames)) {
            return Collections.emptyList();
        }
        List<String> normalized = tagNames.stream()
                .filter(StrUtil::isNotBlank)
                .map(s -> s.trim().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(normalized)) {
            return Collections.emptyList();
        }
        baseMapper.insertIgnoreBatch(normalized);
        List<PictureTag> tags = baseMapper.selectByNames(normalized);
        return tags.stream().map(PictureTag::getId).collect(Collectors.toList());
    }

    @Override
    public void deleteRelationsByPictureId(Long pictureId) {
        LambdaQueryWrapper<PictureTagRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PictureTagRelation::getPictureId, pictureId);
        pictureTagRelationMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRelations(Long pictureId, List<String> tagNames) {
        if (CollUtil.isEmpty(tagNames)) {
            return;
        }
        List<Long> tagIds = getOrCreateByNames(tagNames);
        if (CollUtil.isNotEmpty(tagIds)) {
            pictureTagRelationMapper.insertBatch(pictureId, tagIds);
        }
    }
}
