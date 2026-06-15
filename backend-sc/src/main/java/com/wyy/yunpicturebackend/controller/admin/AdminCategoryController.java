package com.wyy.yunpicturebackend.controller.admin;

import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.DeleteRequest;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.model.dto.category.AddCategoryRequest;
import com.wyy.yunpicturebackend.model.dto.category.UpdateCategoryRequest;
import com.wyy.yunpicturebackend.model.entity.PictureCategory;
import com.wyy.yunpicturebackend.service.PictureCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 管理员：分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Validated
public class AdminCategoryController {

    @Resource
    private PictureCategoryService pictureCategoryService;

    /**
     * 获取所有分类（含完整字段）
     */
    @GetMapping("/list")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<List<PictureCategory>> listCategories() {
        List<PictureCategory> list = pictureCategoryService.listActiveCategories();
        return ResultUtils.success(list);
    }

    /**
     * 新增分类
     */
    @PostMapping("/add")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Long> addCategory(@RequestBody @Valid AddCategoryRequest request) {
        Long id = pictureCategoryService.createCategory(request.getName(), request.getSortOrder());
        return ResultUtils.success(id);
    }

    /**
     * 更新分类（名称/排序/启用禁用）
     */
    @PostMapping("/update")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> updateCategory(@RequestBody @Valid UpdateCategoryRequest request) {
        boolean success = pictureCategoryService.updateCategory(
                request.getId(),
                request.getName(),
                request.getSortOrder(),
                request.getIsActive()
        );
        return ResultUtils.success(success);
    }

    /**
     * 删除分类
     */
    @PostMapping("/delete")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> deleteCategory(@RequestBody @Valid DeleteRequest request) {
        boolean success = pictureCategoryService.deleteCategory(request.getId());
        return ResultUtils.success(success);
    }
}
