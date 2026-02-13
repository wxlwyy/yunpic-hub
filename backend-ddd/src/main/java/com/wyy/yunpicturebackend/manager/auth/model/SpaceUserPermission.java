package com.wyy.yunpicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 团队空间成员权限
 */
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限唯一标识，如：picture:view
     */
    private String key;

    /**
     * 权限名称，如：查看图片
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = -8257349434348030614L;
}
