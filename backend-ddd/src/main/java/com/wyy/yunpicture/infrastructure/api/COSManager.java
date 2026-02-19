package com.wyy.yunpicture.infrastructure.api;


import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.wyy.yunpicture.infrastructure.config.COSClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

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
        //转化格式规则（转成webp格式）
        PicOperations.Rule compressRule = new PicOperations.Rule();
        String webFileId = FileUtil.mainName(key) + ".webp";
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webFileId);
        compressRule.setRule("imageMogr2/format/webp");
        ArrayList<PicOperations.Rule> ruleArrayList = new ArrayList<>();
        ruleArrayList.add(compressRule);
        //图片压缩规则
        if (localFile.length() > 20 * 1024L) {  //大于20kb的图片才进行压缩
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailFileId = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnailFileId);
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            ruleArrayList.add(thumbnailRule);
        }

        PicOperations picOperations = new PicOperations();
        //返回原图信息
        picOperations.setIsPicInfo(1);
        //设置规则
        picOperations.setRules(ruleArrayList);

        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除单个对象
     * @param key cos中对象的路径（上传到指定路径，不用用户传）
     * @return
     */
    public void deleteObject(String key){
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
