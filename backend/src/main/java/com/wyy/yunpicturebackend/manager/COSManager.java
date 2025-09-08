package com.wyy.yunpicturebackend.manager;


import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.wyy.yunpicturebackend.config.COSClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;

@Component
public class COSManager {

    @Resource
    private COSClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 对象上传（图片，音频，视频，文档），通过本地文件的方式
     * @param key cos中对象的路径（上传到指定路径，不用用户传）
     * @param localFile 本地文件
     * @return
     */
    public PutObjectResult putObject(String key, File localFile){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, localFile);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 对象下载（图片，音频，视频，文档），通过流的形式
     * @param key cos中对象的路径（从指定路径获取，需要用户传）
     * @return
     */
    public InputStream getObjectContent(String key){
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        return cosObject.getObjectContent();
    }

    /**
     * 上传图片，并返回基本信息
     * @param key cos中对象的路径
     * @param localFile 本地文件
     * @return
     */
    public PutObjectResult putPictureObject(String key, File localFile){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, localFile);
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }
}
