package com.wyy.yunpicturebackend.model.vo.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUsageAnalyzeResponse implements Serializable {

    /**
     * 空间已使用的大小
     */
    private Long usedSize;

    /**
     * 空间总大小
     */
    private Long maxSize;

    /**
     * 空间大小使用率
     */
    private Double sizeUsage;

    /**
     * 空间已使用的图片数量
     */
    private Long usedCount;

    /**
     * 空间容纳的图片总数量
     */
    private Long maxCount;

    /**
     * 空间图片数量使用率
     */
    private Double countUsage;


    private static final long serialVersionUID = -3842929018868089250L;
}
