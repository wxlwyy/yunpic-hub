package com.wyy.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum PictureReviewStatusEnum {

    REVIEWING("待审核",0),
    PASS("通过",1),
    REJECT("拒绝",2);

    /**
     * 角色标签
     */
    private final String text;

    /**
     * 用户角色
     */
    private final int value;

    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据审核状态字段取出相应的枚举对象
     * @param value 审核状态
     * @return
     */
    public static PictureReviewStatusEnum getPictureReviewStatusEnum(int value){
        if (ObjectUtil.isNull(value)){
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()){
            if (pictureReviewStatusEnum.value == value){
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
