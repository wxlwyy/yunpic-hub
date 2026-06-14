package com.wyy.yunpicturebackend.model.enums;

import lombok.Getter;

/**
 * VIP 兑换码状态枚举
 */
@Getter
public enum VipCodeStatusEnum {

    UNUSED(0, "未使用"),
    USED(1, "已使用"),
    DISABLED(2, "已禁用");

    private final int value;
    private final String text;

    VipCodeStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     */
    public static VipCodeStatusEnum getByValue(int value) {
        for (VipCodeStatusEnum statusEnum : values()) {
            if (statusEnum.value == value) {
                return statusEnum;
            }
        }
        return null;
    }
}
