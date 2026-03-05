package com.wyy.yunpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.wyy.yunpicturebackend.config.COSClientConfig;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.manager.COSManager;
import com.wyy.yunpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class UploadPictureTemplate<T> {

    @Resource
    private COSClientConfig cosClientConfig;

    @Resource
    private COSManager cosManager;

    /**
     * 上传图片模板方法（上传图片到云服务返回图片基本信息）
     * @param inputSource 图片或图片的网络地址
     * @param bizPath 路径前缀（一般是用户id，或者私有共有）
     * @return
     */
    public UploadPictureResult uploadPicture(T inputSource, String bizPath){
        // 校验图片
        validPictureParam(inputSource);
        //准备上传图片的路径   "yun-picture/public/001（用户Id）/2023-07-20_abcdefg.jpg"
        //准备文件名
        String projectName = "yun-picture";
        String dateString = DateUtil.formatDate(new Date());
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String suffix = FileUtil.getSuffix(originFilename);
        String uploadFilename = String.format("%s_%s.%s", dateString, uuid, suffix);

        String cloudPath = String.format(projectName + "/%s/%s", bizPath, uploadFilename);
        //通过本地图片上传
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload_", null);
            // todo
            processTempFile(inputSource, tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(cloudPath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {  // 多个处理结果
                CIObject webpCiObject = objectList.get(0);  // 处理后的webp格式的图片
                CIObject thumbnailPicObject = webpCiObject;  // 压缩的图片默认先是webp格式的图片
                if (objectList.size() > 1) {
                    thumbnailPicObject = objectList.get(1);  // 如果有压缩的图片则赋值
                }
                // 如果本次上传添加了规则，则返回构建处理后的图片结果
                return buildUploadPictureResult(originFilename, webpCiObject, thumbnailPicObject, imageInfo);
            }
            // 如果本次上传没添加规则，则返回本身的图片结果
            return buildUploadPictureResult(originFilename, tempFile, cloudPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        } finally {
            deleteTempFile(tempFile);
        }
        //获取cos返回的部分图片信息
    }


    /**
     * 校验输入源（本地图片或url）
     * @param inputSource
     */
    protected abstract void validPictureParam(T inputSource);

    /**
     * 获取输入源的原始文件名
     * @param inputSource
     * @return
     */
    protected abstract String getOriginFilename(T inputSource);

    /**
     * 处理输入源生成本地临时文件
     * @param inputSource
     * @param tempFile
     */
    protected abstract void processTempFile(T inputSource, File tempFile) throws Exception;

    /**
     * 封装返回结果
     * @param originFilename
     * @param tempFile
     * @param cloudPath
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildUploadPictureResult(String originFilename, File tempFile,
                                                       String cloudPath, ImageInfo imageInfo) {
        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double picScale = NumberUtil.round((width * 1.0 / height), 2).doubleValue();
        String format = imageInfo.getFormat();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicSize(FileUtil.size(tempFile));
        uploadPictureResult.setUrl("https://" + cosClientConfig.getHost() + "/" + cloudPath);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    private UploadPictureResult buildUploadPictureResult(String originFilename, CIObject webpCiObject,
                                                       CIObject thumbnailPicObject, ImageInfo imageInfo) {
        int width = webpCiObject.getWidth();
        int height = webpCiObject.getHeight();
        double picScale = NumberUtil.round((width * 1.0 / height), 2).doubleValue();
        String format = webpCiObject.getFormat();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicSize(webpCiObject.getSize().longValue());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setUrl("https://" + cosClientConfig.getHost() + "/" + webpCiObject.getKey());
        uploadPictureResult.setThumbnailUrl("https://" + cosClientConfig.getHost() + "/" + thumbnailPicObject.getKey());
        return uploadPictureResult;
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
