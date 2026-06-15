package com.wyy.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片分类表
 * @TableName picture_category
 */
@TableName(value ="picture_category")
@Data
public class PictureCategory implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类名称（如：风景、人物、动漫）
     */
    private String name;

    /**
     * 排序权重（数字越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 是否启用（0:禁用 1:启用）
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
