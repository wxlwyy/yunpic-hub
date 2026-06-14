package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.dto.user.UserExchangeVipRequest;
import com.wyy.yunpicturebackend.model.dto.vip.VipOrderQueryRequest;
import com.wyy.yunpicturebackend.model.entity.VipOrder;

import javax.servlet.http.HttpServletRequest;

/**
 * VIP 兑换记录服务
 */
public interface VipOrderService extends IService<VipOrder> {

    /**
     * 兑换 VIP（核心业务：创建兑换订单）
     * @param userExchangeVipRequest 兑换请求
     * @param request HTTP 请求
     * @return 是否成功
     */
    boolean exchangeVip(UserExchangeVipRequest userExchangeVipRequest, HttpServletRequest request);

    /**
     * 分页查询兑换记录（支持筛选）
     * @param queryRequest 查询条件
     * @return 分页结果
     */
    Page<VipOrder> pageVipOrder(VipOrderQueryRequest queryRequest);
}
