package com.wyy.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class EditPictureByBatchRequest implements Serializable {

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 图片id集合
     */
    private List<Long> pictureIdList;

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;

    private static final long serialVersionUID = 7376561290552588015L;
}
