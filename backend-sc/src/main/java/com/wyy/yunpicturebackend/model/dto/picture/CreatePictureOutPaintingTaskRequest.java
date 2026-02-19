package com.wyy.yunpicturebackend.model.dto.picture;

import com.wyy.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    private static final long serialVersionUID = -7563621134831574323L;

    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 创建扩图任务的参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;
}
