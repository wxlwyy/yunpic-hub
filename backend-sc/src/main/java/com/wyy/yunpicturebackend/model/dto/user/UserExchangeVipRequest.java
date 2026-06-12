package com.wyy.yunpicturebackend.model.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 用户兑换 VIP 请求
 */
@Data
public class UserExchangeVipRequest implements Serializable {

    /**
     * 兑换码
     */
    @NotBlank(message = "兑换码不能为空")
    private String vipCode;

    private static final long serialVersionUID = -6830727058868702081L;
}