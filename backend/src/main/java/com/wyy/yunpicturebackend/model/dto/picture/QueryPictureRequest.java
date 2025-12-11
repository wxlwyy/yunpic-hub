package com.wyy.yunpicturebackend.model.dto.picture;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wyy.yunpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 查询图片请求数据
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryPictureRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 9169681866426519625L;
    /**
     * 主键
     */
    private Long id;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否只查询spaceId为null的数据
     */
    private boolean nullSpaceId;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（集合）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建的用户id
     */
    private Long userId;

    /**
     * 搜索词（同时搜名称、简介）
     */
    private String searchText;

    /**
     * 审核人id
     */
    private Long reviewerId;

    /**
     * 审核状态：0-待审核；1-通过；2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;
}