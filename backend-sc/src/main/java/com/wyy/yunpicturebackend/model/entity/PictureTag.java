package com.wyy.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片标签表
 * @TableName picture_tag
 */
@TableName(value ="picture_tag")
@Data
public class PictureTag implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 标签名称（统一小写、去前后空格存储）
     */
    private String name;

    /**
     * 首次创建时间
     */
    private Date createTime;

    /**
     * 最近更新时间
     */
    private Date updateTime;
}
