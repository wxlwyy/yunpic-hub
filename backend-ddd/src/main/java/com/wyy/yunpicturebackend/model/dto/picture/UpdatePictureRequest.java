package com.wyy.yunpicturebackend.model.dto.picture;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 管理员修改图片
 */
@Data
public class UpdatePictureRequest implements Serializable {

    private static final long serialVersionUID = 63369654311691699L;

    /**
     * 图片id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;
}
