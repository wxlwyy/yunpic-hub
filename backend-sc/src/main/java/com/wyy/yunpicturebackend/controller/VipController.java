package com.wyy.yunpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.model.dto.vip.GenerateVipCodeRequest;
import com.wyy.yunpicturebackend.model.dto.vip.VipCodeQueryRequest;
import com.wyy.yunpicturebackend.model.dto.vip.VipOrderQueryRequest;
import com.wyy.yunpicturebackend.model.entity.VipCode;
import com.wyy.yunpicturebackend.model.entity.VipOrder;
import com.wyy.yunpicturebackend.service.VipCodeService;
import com.wyy.yunpicturebackend.service.VipOrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * VIP 管理控制器（仅管理员）
 */
@RestController
@RequestMapping("/vip")
@Validated
public class VipController {

    @Resource
    private VipCodeService vipCodeService;

    @Resource
    private VipOrderService vipOrderService;

    /**
     * 批量生成兑换码（管理员，一码一用）
     */
    @PostMapping("/code/generate")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<List<String>> generateVipCode(@Valid @RequestBody GenerateVipCodeRequest request) {
        List<String> codes = vipCodeService.generateVipCodes(request);
        return ResultUtils.success(codes);
    }

    /**
     * 查询兑换码列表（管理员，支持筛选）
     */
    @GetMapping("/code/list")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Page<VipCode>> listVipCode(@Valid VipCodeQueryRequest queryRequest) {
        Page<VipCode> result = vipCodeService.pageVipCode(queryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 查询兑换记录（管理员，支持筛选）
     */
    @GetMapping("/order/list")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Page<VipOrder>> listVipOrder(@Valid VipOrderQueryRequest queryRequest) {
        Page<VipOrder> result = vipOrderService.pageVipOrder(queryRequest);
        return ResultUtils.success(result);
    }

    /**
     * 禁用/启用兑换码（管理员）
     */
    @PostMapping("/code/toggle/{id}")
    @AuthCheck(anyRole = {UserConstant.ADMIN_ROLE})
    public BaseResponse<Boolean> toggleVipCode(@PathVariable @Min(value = 1, message = "ID 必须大于 0") Long id) {
        boolean result = vipCodeService.toggleVipCodeStatus(id);
        return ResultUtils.success(result);
    }
}
