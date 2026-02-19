package com.wyy.yunpicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 团队空间成员角色
 */
@Data
public class SpaceUserRole implements Serializable {

    /**
     * 角色唯一标识，如：admin
     */
    private String key;

    /**
     * 角色名称，如：管理员
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 该角色拥有的权限Key列表
     * 注意：这里存的是权限的字符串Key，不是Permission对象
     * 运行时需要通过Key去Permission配置里查找具体权限
     */
    private List<String> permissionKeyList;

    private static final long serialVersionUID = -4470748309693140634L;
}
