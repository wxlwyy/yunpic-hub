package com.wyy.yunpicture.infrastructure.common;

import com.wyy.yunpicture.infrastructure.exception.ErrorCode;

public class ResultUtils {
    /**
     * 方法执行成功，返回正确结果
     * @param data 返回的数据
     * @return 返回结果
     * @param <T> 数据的类型
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 条件不符，返回错误结果（使用枚举类的变量）
     * @param errorCode 枚举类的结果
     * @return 返回结果
     */
    public static BaseResponse<?> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }

    /**
     * 条件不符，返回错误结果（使用枚举类的变量code，灵活输入message）
     * @param errorCode 枚举类的结果
     * @param message 错误的描述
     * @return 返回结果
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message){
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

    /**
     * 条件不符，返回错误结果（灵活输入code，灵活输入message）
     * @param code 错误码
     * @param message 错误的描述
     * @return 返回结果
     */
    public static BaseResponse<?> error(int code, String message){
        return new BaseResponse<>(code, null, message);
    }
}
