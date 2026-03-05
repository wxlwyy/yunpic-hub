package com.wyy.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadFilePicture extends UploadPictureTemplate<MultipartFile> {
    @Override
    protected void validPictureParam(MultipartFile multipartFile) {
//        MultipartFile multipartFile = (MultipartFile) multipartFile;
        //是否为null
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //要<=2MB
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        //后缀为"jpeg", "jpg", "png", "webp"
        String fileNameSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileNameSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    protected String getOriginFilename(MultipartFile multipartFile) {
//        MultipartFile multipartFile = (MultipartFile) multipartFile;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processTempFile(MultipartFile multipartFile, File tempFile) throws Exception {
//        MultipartFile multipartFile = (MultipartFile) multipartFile;
        multipartFile.transferTo(tempFile);
    }
}
