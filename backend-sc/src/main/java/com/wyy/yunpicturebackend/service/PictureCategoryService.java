package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.entity.PictureCategory;

import java.util.List;

/**
 * 图片分类服务
 */
public interface PictureCategoryService extends IService<PictureCategory> {

    // ==================== 查询 ====================

    /**
     * 分类列表（isActive: 1=仅启用, null=全部）
     */
    List<PictureCategory> listCategories(Integer isActive);

    /**
     * 所有分类名称（用户下拉框）
     */
    List<String> listCategoryNames();

    /**
     * 根据名称查找分类ID，不存在返回 null
     */
    Long getByName(String name);

    // ==================== 管理端 ====================

    /**
     * 新增分类
     */
    Long createCategory(String name, Integer sortOrder);

    /**
     * 更新分类（名称、排序、启用禁用）
     */
    boolean updateCategory(Long id, String name, Integer sortOrder, Integer isActive);

    /**
     * 删除分类
     */
    boolean deleteCategory(Long id);
}
