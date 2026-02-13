package com.wyy.yunpicturebackend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {

    /**
     * 时间范围
     */
    private String period;

    /**
     * 上传图片的数量
     */
    private Long count;

    private static final long serialVersionUID = 1393207553554722471L;
}
