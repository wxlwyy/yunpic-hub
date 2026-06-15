package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.entity.PictureCategory;

import java.util.List;

/**
 * 图片分类服务
 */
public interface PictureCategoryService extends IService<PictureCategory> {

    // ==================== 用户端 ====================

    /**
     * 获取所有分类名称（首页下拉框、筛选用）
     */
    List<String> listCategoryNames();

    /**
     * 根据名称查找分类ID，仅查询不创建。不存在返回 null
     */
    Long getByName(String name);

    // ==================== 管理端 ====================

    /**
     * 获取所有启用的分类（含完整字段：id/name/sortOrder/isActive）
     */
    List<PictureCategory> listActiveCategories();

    /**
     * 管理员新建分类
     */
    Long createCategory(String name, Integer sortOrder);

    /**
     * 管理员更新分类（名称、排序、是否启用）
     */
    boolean updateCategory(Long id, String name, Integer sortOrder, Integer isActive);

    /**
     * 管理员删除分类（物理删除）
     */
    boolean deleteCategory(Long id);
}
