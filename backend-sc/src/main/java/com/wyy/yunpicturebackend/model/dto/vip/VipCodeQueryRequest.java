package com.wyy.yunpicturebackend.model.dto.vip;

import com.wyy.yunpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * VIP 兑换码查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VipCodeQueryRequest extends PageRequest implements Serializable {

    /**
     * 兑换码（精确匹配）
     */
    private String code;

    /**
     * 状态：0-未使用，1-已使用，2-已禁用
     */
    private Integer status;

    /**
     * VIP 时长（天）
     */
    private Integer vipDuration;

    /**
     * 描述（模糊搜索）
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
