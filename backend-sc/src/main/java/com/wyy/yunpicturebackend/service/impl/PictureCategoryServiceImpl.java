package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.mapper.PictureCategoryMapper;
import com.wyy.yunpicturebackend.model.entity.PictureCategory;
import com.wyy.yunpicturebackend.service.PictureCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片分类服务实现
 */
@Service
public class PictureCategoryServiceImpl extends ServiceImpl<PictureCategoryMapper, PictureCategory>
        implements PictureCategoryService {

    // ==================== 用户端 ====================

    @Override
    public List<String> listCategoryNames() {
        return listActiveCategories().stream()
                .map(PictureCategory::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Long getByName(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        LambdaQueryWrapper<PictureCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PictureCategory::getName, name.trim());
        PictureCategory existing = getOne(wrapper);
        return existing != null ? existing.getId() : null;
    }

    // ==================== 管理端 ====================

    @Override
    public List<PictureCategory> listActiveCategories() {
        LambdaQueryWrapper<PictureCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PictureCategory::getIsActive, 1)
               .orderByAsc(PictureCategory::getSortOrder);
        return list(wrapper);
    }

    @Override
    public Long createCategory(String name, Integer sortOrder) {
        if (StrUtil.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称不能为空");
        }
        // 重名检查
        if (getByName(name.trim()) != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称已存在");
        }
        PictureCategory category = new PictureCategory();
        category.setName(name.trim());
        category.setSortOrder(sortOrder != null ? sortOrder : 99);
        category.setIsActive(1);
        save(category);
        return category.getId();
    }

    @Override
    public boolean updateCategory(Long id, String name, Integer sortOrder, Integer isActive) {
        PictureCategory category = getById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }
        if (StrUtil.isNotBlank(name)) {
            // 重名检查（排除自身）
            Long existId = getByName(name.trim());
            if (existId != null && !existId.equals(id)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类名称已存在");
            }
            category.setName(name.trim());
        }
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        if (isActive != null) {
            category.setIsActive(isActive);
        }
        return updateById(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        PictureCategory category = getById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在");
        }
        return removeById(id);
    }
}
