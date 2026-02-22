package com.wyy.yunpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private COSClientConfig cosClientConfig;

    @Resource
    private COSManager cosManager;


    /**
     * 上传图片（通过本地文件方式），并将上传后的图片部分信息进行封装
     * @param multipartFile
     * @param bizPath 路径前缀（一般是用户id，或者私有共有）
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String bizPath){
        // todo 校验图片参数
        validPictureMetadata(multipartFile);
        //准备上传图片的路径   "yun-picture/public/001（用户Id）/2023-07-20_abcdefg.jpg"
        //准备文件名
        String projectName = "yun-picture";
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename(); // 比如：abc.jpg
        String suffix = FileUtil.getSuffix(originalFilename);  // 获取后缀，如: jpg
        // todo
        // 云端文件名: 20261024_uuid.jpg
        String cloudFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        // 云端完整路径: yun-picture/public/123/20261024_uuid.jpg
        String cloudFilePath = String.format(projectName + "/%s/%s", bizPath, cloudFileName);
        //通过本地图片上传
        File tempFile = null;
        try {
            tempFile = File.createTempFile(uuid, "." + suffix);
            // todo
            multipartFile.transferTo(tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(cloudFilePath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int width = imageInfo.getWidth();
            int height = imageInfo.getHeight();
            String format = imageInfo.getFormat();
            double picScale = NumberUtil.round((width * 1.0 / height), 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicFormat(format);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            uploadPictureResult.setUrl("https://" + cosClientConfig.getHost() + "/" + cloudFilePath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }



    /**
     * 校验图片参数
     * @param multipartFile
     */
    private void validPictureMetadata(MultipartFile multipartFile) {
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

    /**
     * 通过url上传文件
     * @param fileUrl
     * @param bizPath
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String bizPath){
        //校验图片
        //validPicture(multipartFile);
        validPictureByUrl(fileUrl);
        //准备上传图片的路径   "yun-picture/public/001（用户Id）/2023-07-20_abcdefg.jpg"
        //准备文件名
        String projectName = "yun-picture";
        String dateString = DateUtil.formatDate(new Date());
        String uuid = RandomUtil.randomString(16);
        //String originalFilename = multipartFile.getOriginalFilename();
        String originalFilename = FileUtil.getName(fileUrl);
        String suffix = FileUtil.getSuffix(originalFilename);
        String uploadFilename = String.format("%s_%s.%s", dateString, uuid, suffix);

        String cloudFilePath = String.format(projectName + "/%s/%s", bizPath, uploadFilename);
        //通过本地图片上传
        File tempFile = null;
        try {
            tempFile = File.createTempFile(cloudFilePath, null);
            //multipartFile.transferTo(tempFile);
            HttpUtil.downloadFile(fileUrl, tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(cloudFilePath, tempFile);
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
            uploadPictureResult.setUrl("https://" + cosClientConfig.getHost() + "/" + cloudFilePath);
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            deleteTempFile(tempFile);
        }
        //获取cos返回的部分图片信息
    }

    /**
     * 校验图片的url
     * @param fileUrl
     */
    private void validPictureByUrl(String fileUrl) {
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
                    ThrowUtils.throwIf(contentLength > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
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

    private void deleteTempFile(File tempFile) {
        if (tempFile != null) {
            boolean delete = tempFile.delete();
            if (!delete) {
                log.error("file upload error, filePath = {}", tempFile.getAbsoluteFile());
            }
        }
    }
}
