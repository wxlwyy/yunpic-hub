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

import javax.servlet.http.HttpServletRequest;
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

    //  1. 未登录异常
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e, HttpServletRequest request){
        log.warn("\n[Not Login] {}\n" +
                        "Request URI: {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI(), e.getMessage());
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    //  2. 无权限异常
    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e, HttpServletRequest request){
        log.warn("\n[No Permission] {}\n" +
                        "Request URI: {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI(), e.getMessage());
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }

    //  3. 业务异常 (彻底抛弃繁琐的堆栈解析！)
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e, HttpServletRequest request) {
        log.warn("\n[Business Exception] {}\n" +
                        "Request URI: {}\n" +
                        "Code: {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI(), e.getCode(), e.getMessage());
        return new BaseResponse<>(e.getCode(), null, e.getMessage());
    }

    //  4. 系统异常 (这个保留堆栈，因为是真 Bug，并加上 URI 方便复现)
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e, HttpServletRequest request){
        log.error("\n[System Error] {}\n" +
                        "Request URI: {}\n" +
                        "Message: 系统发生未知异常！\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI(), e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 1. 拦截 [空请求体] 异常 (HttpMessageNotReadableException),和validated注解无关，
     * 这个异常是spring MVC在解析请求体这一步直接报错了，他是spring底层负责把Json转化成Java对象的组件，
     * 抛异常的时机非常早，还没进入到controller方法。更不用说参数校验
     * 触发场景：POST/PUT 请求连 {} 都没有，或者 JSON 格式写错了（比如少个逗号）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("\n[Parameter Empty] {}\n" +
                        "Request URI: {}\n" +
                        "Message: 请求体为空或格式错误\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求体不可为空");
    }

    /**
     * 2. 拦截 [DTO 校验] 异常 (MethodArgumentNotValidException)，和validated注解有关
     * 触发场景：@Validated 校验失败（例如 @NotBlank, @Size 等不通过）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = "参数错误";
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldErrors().get(0);
            errorMessage = fieldError.getDefaultMessage();
        }

        log.warn("\n[Validation Error] {}\n" +
                        "Request URI: {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI(), errorMessage);

        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    /**
     * 3. 拦截 [GET/单参数] 校验异常 (ConstraintViolationException)，和validated注解有关
     * 触发场景：@RequestParam 标注的参数不符合 @NotNull, @Min 等
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getMessage();
        if (message != null && message.contains(":")) {
            message = message.substring(message.lastIndexOf(":") + 1).trim();
        }

        log.warn("\n[Constraint Violation] {}\n" +
                        "Request URI: {}\n" +
                        "Message: {}\n" +
                        "--------------------------------------------------",
                getLogPrefix(), request.getRequestURI(), message);

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
