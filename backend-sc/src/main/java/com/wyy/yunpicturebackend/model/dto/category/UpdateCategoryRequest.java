package com.wyy.yunpicturebackend.model.dto.category;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 更新分类请求（传什么改什么，不传的不动）
 */
@Data
public class UpdateCategoryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID（必填）
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 是否启用（0:禁用 1:启用）
     */
    private Integer isActive;
}
