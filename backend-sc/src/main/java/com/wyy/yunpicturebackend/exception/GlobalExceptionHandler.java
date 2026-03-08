package com.wyy.yunpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 获取日志上下文信息的辅助方法
    private String getLogPrefix() {
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : "SYSTEM";
        String userId = MDC.get("userId") != null ? MDC.get("userId") : "GUEST";
        return String.format("[User: %s] [Trace: %s]", userId, traceId);
    }

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e){
        // 业务校验失败，用 warn，且不打印满屏堆栈
        log.warn("\n[Not Login] {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), e.getMessage());
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e){
        log.warn("\n[No Permission] {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), e.getMessage());
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        List<String> paths = new ArrayList<>();

        String projectBasePackage = "com.wyy.yunpicturebackend";

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // 依然过滤掉噪音
            if (className.contains(projectBasePackage) && !className.contains("$$") && !className.contains("exception")) {
                // 🚀 核心改动：构造 IDE 能够识别的“魔法格式”
                // 格式必须是：类名.方法名(文件名:行号)
                String linkFormat = String.format("%s.%s(%s:%d)",
                        className,
                        element.getMethodName(),
                        element.getFileName(),
                        element.getLineNumber());
                paths.add(linkFormat);

                if (paths.size() >= 3) break;
            }
        }

        log.warn("\n[Business Exception] {}\n" +
                        "Code: {}\n" +
                        "Message: {}\n" +
                        "Trace: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), e.getCode(), e.getMessage(), String.join("\n    <- ", paths));

        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e){
        // 系统崩了：保留完整的 e（堆栈信息），加上 TraceID 方便和 SQL 对应
        log.error("\n[System Error] {}\n" +
                        "Message: 系统发生未知异常！\n" +
                        "--------------------------------------------------",
                getLogPrefix(), e); // 这里的 e 必须保留，用来排查致命 Bug
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}

/*@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e){
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e){
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e){
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e){
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}*/
