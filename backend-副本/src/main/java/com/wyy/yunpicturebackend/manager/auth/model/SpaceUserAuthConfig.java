package com.wyy.yunpicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 团队空间成员权限配置类
 */
@Data
public class SpaceUserAuthConfig implements Serializable {

    /**
     * 团队空间成员权限
     */
    private List<SpaceUserPermission> permissionList;

    /**
     * 团队空间成员角色
     */
    private List<SpaceUserRole> roleList;

    private static final long serialVersionUID = 9134184230543059983L;
}
