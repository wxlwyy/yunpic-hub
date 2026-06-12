package com.wyy.yunpicturebackend.aop;

import cn.hutool.core.util.StrUtil;
import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.UserRoleEnum;
import com.wyy.yunpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取允许访问的角色列表
        String[] allowedRoles = authCheck.anyRole();

        // 如果没有配置角色要求，直接放行
        if (allowedRoles == null || allowedRoles.length == 0) {
            return joinPoint.proceed();
        }

        // 获取当前登录用户
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User user = userService.getLoginUser(request);
        String currentUserRole = user.getUserRole();

        // 判断用户角色是否在允许的角色列表中
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(currentUserRole)) {
                // 角色匹配，放行
                return joinPoint.proceed();
            }
        }

        // 用户角色不在允许列表中，拒绝访问
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "权限不足，需要以下任一角色：" + String.join("、", allowedRoles));
    }
}
