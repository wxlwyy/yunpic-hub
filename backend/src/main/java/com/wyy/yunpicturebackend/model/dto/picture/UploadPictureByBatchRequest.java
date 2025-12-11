package com.wyy.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量抓取图片的请求
 */
@Data
public class UploadPictureByBatchRequest implements Serializable {


    private static final long serialVersionUID = -8154490045681446744L;
    /**
     * 一次性抓取图片的数量
     */
    private Integer count;

    /**
     * 图片的关键词
     */
    private String searchText;

    /**
     * 指定图片名称前缀
     */
    private String picNamePrefix;
}
