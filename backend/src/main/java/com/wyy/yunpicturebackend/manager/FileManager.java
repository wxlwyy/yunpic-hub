package com.wyy.yunpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.wyy.yunpicturebackend.config.COSClientConfig;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManager {

    @Resource
    private COSClientConfig cosClientConfig;

    @Resource
    private COSManager cosManager;


    /**
     * 上传图片（通过本地文件方式），并将上传后的图片部分信息进行封装
     * @param multipartFile
     * @param uploadPicturePrefix 路径前缀（一般是用户id，或者私有共有）
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPicturePrefix){
        //校验图片
        validPicture(multipartFile);
        //准备上传图片的路径   "yun-picture/public/001（用户Id）/2023-07-20_abcdefg.jpg"
        //准备文件名
        String projectName = "yun-picture";
        String dateString = DateUtil.formatDate(new Date());
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String uploadFilename = String.format("%s_%s.%s", dateString, uuid, suffix);

        String uploadPath = String.format(projectName + "/%s/%s", uploadPicturePrefix, uploadFilename);
        //通过本地图片上传
        File tempFile = null;
        try {
            tempFile = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            double picScale = NumberUtil.round((width * 1.0 / height), 2).doubleValue();
            String format = imageInfo.getFormat();

            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicFormat(format);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            uploadPictureResult.setUrl("https://" + cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            deleteTempFile(tempFile);
        }
        //获取cos返回的部分图片信息
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile != null) {
            boolean delete = tempFile.delete();
            if (!delete) {
                log.error("file upload error, filePath = {}", tempFile.getAbsoluteFile());
            }
        }
    }

    /**
     * 校验图片参数
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
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
}
