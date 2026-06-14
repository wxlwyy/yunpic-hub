package com.wyy.yunpicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP 兑换记录表
 * @TableName vip_order
 */
@TableName(value = "vip_order")
@Data
public class VipOrder implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 兑换码 ID
     */
    private Long codeId;

    /**
     * 兑换码（冗余，方便查询）
     */
    private String code;

    /**
     * VIP 时长（天）
     */
    private Integer vipDuration;

    /**
     * 用户使用兑换码前，他的VIP到期时间
     */
    private LocalDateTime oldExpireTime;

    /**
     * 用户使用兑换码后，他的VIP到期时间
     */
    private LocalDateTime newExpireTime;

    /**
     * 兑换时间
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
