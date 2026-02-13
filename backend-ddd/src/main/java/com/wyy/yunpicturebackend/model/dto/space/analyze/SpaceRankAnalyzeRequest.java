package com.wyy.yunpicturebackend.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间使用排行分析
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 前N个空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = -2772560404879930528L;
}
