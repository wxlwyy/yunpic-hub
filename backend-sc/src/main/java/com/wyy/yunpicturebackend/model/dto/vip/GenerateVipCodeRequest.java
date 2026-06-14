package com.wyy.yunpicturebackend.model.dto.vip;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 生成 VIP 兑换码请求（一码一用）
 */
@Data
public class GenerateVipCodeRequest implements Serializable {

    /**
     * 兑换码前缀
     */
    @NotBlank(message = "兑换码前缀不能为空")
    private String prefix;

    /**
     * VIP 时长（天）
     */
    @NotNull(message = "VIP 时长不能为空")
    @Min(value = 1, message = "VIP 时长至少为 1 天")
    private Integer vipDuration;

    /**
     * 生成数量
     */
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为 1")
    private Integer count;

    /**
     * 兑换码有效期（天，兑换码本身的过期时间）
     */
    @NotNull(message = "兑换码有效期不能为空")
    @Min(value = 1, message = "兑换码有效期至少为 1 天")
    private Integer validDays;

    /**
     * 兑换码描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
