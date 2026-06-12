package com.wyy.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadUrlPicture extends UploadPictureTemplate{
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        //验证参数合法性
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        //验证url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片地址格式不正确");
        }
        //验证URL协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的图片地址");
        HttpResponse response = null;
        try {
            //发送HEAD请求验证文件是否存在
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK){
                return;
            }
            //验证文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)){
                final List<String> ALLOW_CONTENT_TYPE = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR,
                        "文件类型错误");
            }
            //验证文件的大小
            String contentLengthStr = response.header("Content-Length");
            try {
                if (StrUtil.isNotBlank(contentLengthStr)){
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_MB = 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > 5 * ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过5MB");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件过大");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * 通过图片url获取原名
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        int questionMarkIndex = fileUrl.indexOf("?");
        if (questionMarkIndex > -1) {
            fileUrl = fileUrl.substring(0, questionMarkIndex);
        }
        return FileUtil.getName(fileUrl);
    }

    @Override
    protected void processTempFile(Object inputSource, File tempFile) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, tempFile);
    }
}
