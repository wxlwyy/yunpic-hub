package com.wyy.yunpicturebackend.manager;


import cn.hutool.core.io.FileUtil;
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
import java.util.ArrayList;

@Component
public class COSManager {

    @Resource
    private COSClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象（图片，音频，视频，文档），通过后端服务器的方式，是最基础的上传（只传文件，啥也不管）。没调用数据万象服务
     * @param key cos中对象的路径（上传到指定路径，不用用户传，没有桶名等前缀）
     * @param localFile 本地文件
     * @return
     */
    public PutObjectResult putObject(String key, File localFile){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, localFile);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 通用下载对象（图片，音频，视频，文档），通过流的形式
     * @param key cos中对象的路径（从指定路径获取，需要用户传）
     * @return
     */
    public InputStream getObjectContent(String key){
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        return cosObject.getObjectContent();
    }

    /**
     * 上传图片并自动处理规则（比如转格式、压缩），并返回基本信息，调用数据万象的 API 时，需要给腾讯云额外传一些特殊的请求头或参数
     * （比如告诉腾讯云：不仅帮我存起来，还要顺便帮我解析一下图片信息）。
     * @param key cos中对象的路径
     * @param localFile 本地文件
     * @return
     */
    public PutObjectResult putPictureObject(String key, File localFile){
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, localFile);
        //转化格式规则（转成webp格式）
        PicOperations.Rule compressRule = new PicOperations.Rule();
        String webpFilename = FileUtil.mainName(key) + ".webp";
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpFilename);
        compressRule.setRule("imageMogr2/format/webp");
        ArrayList<PicOperations.Rule> ruleArrayList = new ArrayList<>();
        ruleArrayList.add(compressRule);
        //图片压缩规则
        if (localFile.length() > 20 * 1024L) {  //大于20kb的图片才进行压缩
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailFilename = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnailFilename);
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
