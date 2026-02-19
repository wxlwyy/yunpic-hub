package com.wyy.yunpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.wyy.yunpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.wyy.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.wyy.yunpicturebackend.manager.auth.model.SpaceUserRole;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.SpaceUser;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.wyy.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.SpaceUserService;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将配置文件加载到SpaceUserAuthConfig对象中，根据空间团队成员角色key获取空间团队成员权限key列表
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String spaceUserAuthConfigJsonStr = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(spaceUserAuthConfigJsonStr, SpaceUserAuthConfig.class);
    }

    /**
     * 根据空间团队成员角色key获取空间团队成员权限key列表
     */
    public List<String> getPermissionKeyListByRoleKey(String spaceUserRoleKey) {
        // 校验基础参数
        if (StrUtil.isBlank(spaceUserRoleKey)) {
            return new ArrayList<>();
        }
        SpaceUserRole spaceUserRole1 = SPACE_USER_AUTH_CONFIG.getRoleList().stream()
                .filter(spaceUserRole -> spaceUserRoleKey.equals(spaceUserRole.getKey()))
                .findFirst()
                .orElse(null);
        // 角色key输错查不到空间团队成员角色
        if (spaceUserRole1 == null) {
            return new ArrayList<>();
        }
        return spaceUserRole1.getPermissionKeyList();
    }

    /**
     * 获取空间详情或图片详情时返回给前端权限列表，方便展示是否显示相关按钮
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        // 未登录，没权限，不显示按钮
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionKeyListByRoleKey(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库（登录后）
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getSpaceTypeEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionKeyListByRoleKey(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }
}
