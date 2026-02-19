package com.wyy.yunpicturebackend.model.dto.space;

import com.wyy.yunpicture.infrastructure.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class QuerySpaceRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = -3025469502774928370L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型：0-私人空间 1-团队空间
     */
    private Integer spaceType;
}
