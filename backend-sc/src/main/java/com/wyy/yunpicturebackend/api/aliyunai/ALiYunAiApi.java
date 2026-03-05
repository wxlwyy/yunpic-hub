package com.wyy.yunpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.wyy.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.wyy.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wyy.yunpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ALiYunAiApi {

    //读取配置文件
    @Value("${aliyunAi.apiKey}")
    private String apiKey;

    /**
     * 模型，例如 "image-out-painting"
     */
    @Value("${aliyunAi.model}")
    private String model;

    //创建AI扩图任务请求的url
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    //查询AI扩图任务请求的url
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建AI扩图任务
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest){
        //校验参数
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);
        // 【关键修复】：如果请求里的 model 是空的，就用配置文件里的默认值
        if (StrUtil.isBlank(createOutPaintingTaskRequest.getModel())) {
            createOutPaintingTaskRequest.setModel(model);
        }
        //发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try(HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("创建AI扩图任务接口请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建AI扩图任务接口请求异常");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String code = response.getCode();
            if (StrUtil.isNotBlank(code)) {
                String message = response.getMessage();
                log.error("创建AI扩图任务接口响应异常，code:{}, message:{}", code, message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建AI扩图任务接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询AI扩图任务
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        //校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        //发送请求
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey);
        try(HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("查询AI扩图任务接口请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询AI扩图任务接口请求异常");
            }
            GetOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
            String code = response.getOutput().getCode();
            if (StrUtil.isNotBlank(code)) {
                String message = response.getOutput().getMessage();
                log.error("查询AI扩图任务接口请求异常，code:{}, message:{}", code, message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "查询AI扩图任务接口请求异常");
            }
            return response;
        }
    }
}
