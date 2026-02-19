package com.wyy.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 分析空间的范围（公共图库、个人空间、公共图库和所有个人空间）
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 全空间
     */
    private boolean queryAll;

    /**
     * 公共空间
     */
    private boolean queryPublic;

    /**
     * 空间id
     */
    private Long spaceId;

    private static final long serialVersionUID = -4153335842115954764L;
}
