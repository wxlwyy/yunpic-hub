package com.wyy.yunpicturebackend.model.dto.picture;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 用户修改图片
 */
@Data
public class EditPictureRequest implements Serializable {

    private static final long serialVersionUID = -862120834537692941L;

    /**
     * 图片id
     */
    @NotNull(message = "图片id不能为空")
    @Min(value = 1, message = "图片id错误")
    private Long id;

    /**
     * 图片名称
     */
    @Length(max = 128, message = "图片名称过长")  // 防恶意长文本
    private String name;

    /**
     * 简介
     */
    @Length(max = 500, message = "简介过长")
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（集合）
     */
    private List<String> tags;
}
