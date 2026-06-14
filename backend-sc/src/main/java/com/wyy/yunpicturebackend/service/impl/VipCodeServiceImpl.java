package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.mapper.VipCodeMapper;
import com.wyy.yunpicturebackend.model.dto.vip.GenerateVipCodeRequest;
import com.wyy.yunpicturebackend.model.dto.vip.VipCodeQueryRequest;
import com.wyy.yunpicturebackend.model.entity.VipCode;
import com.wyy.yunpicturebackend.model.enums.VipCodeStatusEnum;
import com.wyy.yunpicturebackend.service.VipCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * VIP 兑换码服务实现（一码一用）
 */
@Service
public class VipCodeServiceImpl extends ServiceImpl<VipCodeMapper, VipCode> implements VipCodeService {

    @Override
    public VipCode validateCode(String code) {
        // 1. 查询兑换码
        QueryWrapper<VipCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        VipCode vipCode = this.getOne(queryWrapper);

        // 2. 校验兑换码是否存在
        if (vipCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码不存在");
        }

        // 3. 校验兑换码状态
        if (vipCode.getStatus() == VipCodeStatusEnum.USED.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码已被使用");
        }
        if (vipCode.getStatus() == VipCodeStatusEnum.DISABLED.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码已被禁用");
        }

        // 4. 校验兑换码是否过期
        LocalDateTime expirationTime = vipCode.getExpirationTime();
        if (expirationTime != null && expirationTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码已过期");
        }

        return vipCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> generateVipCodes(GenerateVipCodeRequest request) {
        // 1. 计算过期时间（统一使用 LocalDateTime）
        LocalDateTime expirationTime = LocalDateTime.now().plusDays(request.getValidDays());

        // 2. 批量生成兑换码（使用 Set 去重）
        Set<String> codeSet = new HashSet<>();
        while (codeSet.size() < request.getCount()) {
            String code = request.getPrefix() + RandomUtil.randomString(8).toUpperCase();
            codeSet.add(code);
        }

        // 3. 使用流构建实体列表
        List<VipCode> vipCodeList = codeSet.stream()
                .map(code -> {
                    VipCode vipCode = new VipCode();
                    vipCode.setCode(code);
                    vipCode.setVipDuration(request.getVipDuration());
                    vipCode.setStatus(VipCodeStatusEnum.UNUSED.getValue());
                    vipCode.setExpirationTime(expirationTime);
                    vipCode.setDescription(request.getDescription());
                    return vipCode;
                })
                .collect(Collectors.toList());

        // 4. 批量插入（冲突概率极低，失败直接提示管理员重试）
        // 由于mp批量插入会分几批，底层是多条sql插入数据库不能保证原子性，因此需要事务注解
        boolean success = this.saveBatch(vipCodeList);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "批量生成失败，请重试");

        // 5. 返回生成的兑换码列表
        return new ArrayList<>(codeSet);
    }

    @Override
    public boolean toggleVipCodeStatus(Long id) {
        VipCode vipCode = this.getById(id);
        ThrowUtils.throwIf(vipCode == null, ErrorCode.NOT_FOUND_ERROR, "兑换码不存在");

        if (vipCode.getStatus() == VipCodeStatusEnum.USED.getValue()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已使用的兑换码不能修改状态");
        }

        // SQL 原子操作切换状态：0↔2（未使用↔已禁用）
        // 数据库层面保证并发安全，无需额外加锁
        // UPDATE vip_code SET status = IF(status = 0, 2, 0) WHERE id = ?;
        boolean success = this.lambdaUpdate()
                .eq(VipCode::getId, id)
                .setSql("status = IF(status = " + VipCodeStatusEnum.UNUSED.getValue()
                        + ", " + VipCodeStatusEnum.DISABLED.getValue()
                        + ", " + VipCodeStatusEnum.UNUSED.getValue() + ")")
                .update();

        return success;
    }

    @Override
    public Page<VipCode> pageVipCode(VipCodeQueryRequest queryRequest) {
        // 构建查询条件
        QueryWrapper<VipCode> queryWrapper = new QueryWrapper<>();

        // 按兑换码精确查询
        if (StrUtil.isNotBlank(queryRequest.getCode())) {
            queryWrapper.eq("code", queryRequest.getCode());
        }

        // 按状态筛选
        if (queryRequest.getStatus() != null) {
            queryWrapper.eq("status", queryRequest.getStatus());
        }

        // 按 VIP 时长筛选
        if (queryRequest.getVipDuration() != null) {
            queryWrapper.eq("vipDuration", queryRequest.getVipDuration());
        }

        // 按描述模糊搜索
        if (StrUtil.isNotBlank(queryRequest.getDescription())) {
            queryWrapper.like("description", queryRequest.getDescription());
        }

        // 按创建时间降序
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<VipCode> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return this.page(page, queryWrapper);
    }
}
