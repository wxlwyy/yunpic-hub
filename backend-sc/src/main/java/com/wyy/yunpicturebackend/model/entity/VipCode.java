package com.wyy.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP 兑换码表（一码一用）
 * @TableName vip_code
 */
@TableName(value = "vip_code")
@Data
public class VipCode implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 兑换码（唯一）
     */
    private String code;

    /**
     * VIP 时长（天）
     */
    private Integer vipDuration;

    /**
     * 状态：0-未使用，1-已使用，2-已禁用
     */
    private Integer status;

    /**
     * 兑换码过期时间
     */
    private LocalDateTime expirationTime;

    /**
     * 兑换码描述（如活动名称）
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}
