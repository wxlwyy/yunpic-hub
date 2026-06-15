package com.wyy.yunpicturebackend.model.dto.category;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 新增分类请求
 */
@Data
public class AddCategoryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类名称（必填）
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 排序权重（数字越小越靠前，不传默认 99）
     */
    private Integer sortOrder;
}
