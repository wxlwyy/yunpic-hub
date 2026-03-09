package com.wyy.yunpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
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
                // 核心改动：构造 IDE 能够识别的“魔法格式”
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

    /**
     * 1. 拦截 [空请求体] 异常 (HttpMessageNotReadableException),和validated注解无关，
     * 这个异常是spring MVC在解析请求体这一步直接报错了，他是spring底层负责把Json转化成Java对象的组件，
     * 抛异常的时机非常早，还没进入到controller方法。更不用说参数校验
     * 触发场景：POST/PUT 请求连 {} 都没有，或者 JSON 格式写错了（比如少个逗号）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("\n[Parameter Empty] {}\n" +
                        "Message: 请求体为空或格式错误\n" +
                        "--------------------------------------------------",
                getLogPrefix());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求体不可为空");
    }

    /**
     * 2. 拦截 [DTO 校验] 异常 (MethodArgumentNotValidException)，和validated注解有关
     * 触发场景：@Validated 校验失败（例如 @NotBlank, @Size 等不通过）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = "参数错误";
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldErrors().get(0);
            // 提取出我们在 DTO 上写的 message，比如 "昵称不能为空"
            errorMessage = fieldError.getDefaultMessage();
        }

        // 同样提取路径，方便快速定位是哪个 Controller 的哪个字段报错
        StackTraceElement[] stackTrace = e.getStackTrace();
        String path = "Unknown Path";
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().contains("com.wyy.yunpicturebackend")) {
                path = String.format("%s.%s(%s:%d)",
                        element.getClassName(), element.getMethodName(),
                        element.getFileName(), element.getLineNumber());
                break;
            }
        }

        log.warn("\n[Validation Error] {}\n" +
                        "Message: {}\n" +
                        "Location: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), errorMessage, path);

        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    /**
     * 3. 拦截 [GET/单参数] 校验异常 (ConstraintViolationException)，和validated注解有关
     * 触发场景：@RequestParam 标注的参数不符合 @NotNull, @Min 等
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> handleConstraintViolationException(ConstraintViolationException e) {
        // 提取具体的错误信息（通常是：方法名.参数名: 错误描述）
        String message = e.getMessage();

        log.warn("\n[Constraint Violation] {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), message);

        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
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
