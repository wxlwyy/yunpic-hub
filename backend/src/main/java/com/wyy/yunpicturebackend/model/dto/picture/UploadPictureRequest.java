package com.wyy.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class UploadPictureRequest implements Serializable {


    private static final long serialVersionUID = 5022088074548039971L;
    /**
     * 图片id（用于修改，没有id则是新增）
     */
    private Long id;
}
