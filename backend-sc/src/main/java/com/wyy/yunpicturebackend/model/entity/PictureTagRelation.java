package com.wyy.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片-标签关联表
 * @TableName picture_tag_relation
 */
@TableName(value ="picture_tag_relation")
@Data
public class PictureTagRelation implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 关联时间
     */
    private Date createTime;
}
