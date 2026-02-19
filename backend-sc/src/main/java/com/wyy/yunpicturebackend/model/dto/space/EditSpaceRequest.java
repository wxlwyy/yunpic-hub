package com.wyy.yunpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class EditSpaceRequest implements Serializable {

    private static final long serialVersionUID = 8747389502137040537L;

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;
}
