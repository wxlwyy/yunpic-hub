package com.wyy.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {


    USER("普通用户", "user", 1),
    VIP("会员用户", "vip", 2),
    ADMIN("管理员", "admin", 99);

    /**
     * 角色标签
     */
    private final String text;

    /**
     * 用户角色
     */
    private final String value;

    /**
     * 新增：代表权限等级
     */
    private final int level;

    UserRoleEnum(String text, String value, int level) {
        this.text = text;
        this.value = value;
        this.level = level;
    }

    /**
     * 根据传入的用户角色，找相应的枚举对象
     * @param value 用户角色
     * @return 用户角色枚举对象
     */
    public static UserRoleEnum getEnumByValue(String value){
        if (StrUtil.isBlank(value)){
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()){
            if (userRoleEnum.value.equals(value)){
                return userRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取等级，如果找不到角色则返回最低等级 0
     */
    public static int getLevelByValue(String value) {
        UserRoleEnum userRoleEnum = getEnumByValue(value);
        return userRoleEnum == null ? 0 : userRoleEnum.getLevel();
    }
}
