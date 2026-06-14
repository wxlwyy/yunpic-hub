package com.wyy.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.mapper.VipOrderMapper;
import com.wyy.yunpicturebackend.model.dto.user.UserExchangeVipRequest;
import com.wyy.yunpicturebackend.model.dto.vip.VipOrderQueryRequest;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.entity.VipCode;
import com.wyy.yunpicturebackend.model.entity.VipOrder;
import com.wyy.yunpicturebackend.model.enums.UserRoleEnum;
import com.wyy.yunpicturebackend.model.enums.VipCodeStatusEnum;
import com.wyy.yunpicturebackend.service.UserService;
import com.wyy.yunpicturebackend.service.VipCodeService;
import com.wyy.yunpicturebackend.service.VipOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * VIP 兑换记录服务实现
 */
@Service
public class VipOrderServiceImpl extends ServiceImpl<VipOrderMapper, VipOrder> implements VipOrderService {

    @Resource
    private VipCodeService vipCodeService;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exchangeVip(UserExchangeVipRequest userExchangeVipRequest, HttpServletRequest request) {
        // 1. 基本校验
        String code = userExchangeVipRequest.getVipCode();
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        // 2. 校验兑换码是否可用
        VipCode vipCode = vipCodeService.validateCode(code);

        // 3. 检查用户是否已经兑换过此码（防止重复兑换）
        QueryWrapper<VipOrder> orderQueryWrapper = new QueryWrapper<>();
        orderQueryWrapper.eq("userId", userId).eq("code", code);
        long count = this.count(orderQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已经使用过该兑换码");
        }

        // 4. 计算新的过期时间（统一使用 LocalDateTime）
        LocalDateTime oldExpireTime = loginUser.getVipExpireTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baseTime = (oldExpireTime != null && oldExpireTime.isAfter(now)) ? oldExpireTime : now;
        LocalDateTime newExpireTime = baseTime.plusDays(vipCode.getVipDuration());

        // 5. 更新用户信息（设置 VIP 角色和过期时间）
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUserRole(UserRoleEnum.VIP.getValue());
        updateUser.setVipExpireTime(newExpireTime);
        boolean updateResult = userService.updateById(updateUser);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新用户信息失败");

        // 6. 标记兑换码为已使用（原子更新，防止一码多用）
        // 只有状态是"未使用"时才能更新成功，数据库层面保证并发安全
        boolean updateCodeResult = vipCodeService.lambdaUpdate()
                .eq(VipCode::getId, vipCode.getId())
                .eq(VipCode::getStatus, VipCodeStatusEnum.UNUSED.getValue())
                .set(VipCode::getStatus, VipCodeStatusEnum.USED.getValue())
                .update();
        ThrowUtils.throwIf(!updateCodeResult, ErrorCode.OPERATION_ERROR, "兑换码已被使用，请刷新后重试");

        // 7. 记录兑换订单
        VipOrder vipOrder = new VipOrder();
        vipOrder.setUserId(userId);
        vipOrder.setCodeId(vipCode.getId());
        vipOrder.setCode(code);
        vipOrder.setVipDuration(vipCode.getVipDuration());
        vipOrder.setOldExpireTime(oldExpireTime);
        vipOrder.setNewExpireTime(newExpireTime);
        boolean saveOrderResult = this.save(vipOrder);
        ThrowUtils.throwIf(!saveOrderResult, ErrorCode.OPERATION_ERROR, "保存兑换记录失败");

        return true;
    }

    @Override
    public Page<VipOrder> pageVipOrder(VipOrderQueryRequest queryRequest) {
        // 构建查询条件
        QueryWrapper<VipOrder> queryWrapper = new QueryWrapper<>();

        // 按用户 ID 筛选
        if (queryRequest.getUserId() != null) {
            queryWrapper.eq("userId", queryRequest.getUserId());
        }

        // 按兑换码筛选
        if (queryRequest.getCode() != null && !queryRequest.getCode().trim().isEmpty()) {
            queryWrapper.eq("code", queryRequest.getCode());
        }

        // 按兑换时间范围筛选
        if (queryRequest.getStartCreateTime() != null) {
            queryWrapper.ge("createTime", queryRequest.getStartCreateTime());
        }
        if (queryRequest.getEndCreateTime() != null) {
            queryWrapper.le("createTime", queryRequest.getEndCreateTime());
        }

        // 按兑换时间降序
        queryWrapper.orderByDesc("createTime");

        // 分页查询
        Page<VipOrder> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        return this.page(page, queryWrapper);
    }
}
