package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.dto.vip.GenerateVipCodeRequest;
import com.wyy.yunpicturebackend.model.dto.vip.VipCodeQueryRequest;
import com.wyy.yunpicturebackend.model.entity.VipCode;

import java.util.List;

/**
 * VIP 兑换码服务
 */
public interface VipCodeService extends IService<VipCode> {

    /**
     * 校验兑换码是否可用
     * @param code 兑换码
     * @return VipCode 对象
     */
    VipCode validateCode(String code);

    /**
     * 批量生成兑换码
     * @param request 生成请求
     * @return 生成的兑换码列表
     */
    List<String> generateVipCodes(GenerateVipCodeRequest request);

    /**
     * 切换兑换码状态（启用/禁用）
     * @param id 兑换码 ID
     * @return 是否成功
     */
    boolean toggleVipCodeStatus(Long id);

    /**
     * 分页查询兑换码列表（支持筛选）
     * @param queryRequest 查询条件
     * @return 分页结果
     */
    Page<VipCode> pageVipCode(VipCodeQueryRequest queryRequest);
}
