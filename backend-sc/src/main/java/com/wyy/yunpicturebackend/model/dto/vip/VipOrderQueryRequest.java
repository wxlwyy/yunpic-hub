package com.wyy.yunpicturebackend.model.dto.vip;

import com.wyy.yunpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VIP 兑换记录查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VipOrderQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 兑换码（精确匹配）
     */
    private String code;

    /**
     * 兑换时间-开始
     */
    private LocalDateTime startCreateTime;

    /**
     * 兑换时间-结束
     */
    private LocalDateTime endCreateTime;

    private static final long serialVersionUID = 1L;
}
