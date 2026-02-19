package com.wyy.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceTypeEnum {

    PRIVATE("私有空间",0),
    TEAM("团队空间",1);

    private final String text;

    private final int value;

    /**
     *
     * @param text  文本
     * @param value  值
     */
    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据传入的空间等级，找相应的枚举对象
     * @param value 空间等级
     * @return 空间等级枚举对象
     */
    public static SpaceTypeEnum getSpaceTypeEnumByValue(Integer value){
        if (ObjUtil.isEmpty(value)){
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()){
            if (spaceTypeEnum.value == value){
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
