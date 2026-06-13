package com.wyy.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {


    USER("普通用户", "user"),
    VIP("会员用户", "vip"),
    ADMIN("管理员", "admin");

    /**
     * 角色标签
     */
    private final String text;

    /**
     * 用户角色
     */
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
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
}
