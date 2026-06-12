package com.wyy.yunpicturebackend.interceptor;

import cn.hutool.core.util.IdUtil;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.model.entity.User;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全链路日志追踪拦截器
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 发身份证
        MDC.put(TRACE_ID, IdUtil.fastSimpleUUID().substring(0, 10));

        // 2. 提取用户 ID
        String userIdStr = "GUEST";
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);

        if (userObj != null) {
            // 核心改进：强转为 User 对象（根据你项目的实际类名改，比如 User 或 LoginUserVO）
            User user = (User) userObj;
            if (user.getId() != null) {
                userIdStr = String.valueOf(user.getId());
            }
        }

        MDC.put("userId", userIdStr);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 3. 请求结束，必须清除，防止内存泄漏
        MDC.remove(TRACE_ID);
    }
}