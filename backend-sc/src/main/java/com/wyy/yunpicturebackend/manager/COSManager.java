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
     * 上传图片，并返回基本信息（优化版：防止 WebP 二次压缩）
     * @param key cos中对象的路径
     * @param localFile 本地文件
     * @return
     */
    /*public PutObjectResult putPictureObject(String key, File localFile) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, localFile);

        // 1. 获取后缀，判断是否已经是 webp
        String suffix = FileUtil.getSuffix(key);
        ArrayList<PicOperations.Rule> ruleArrayList = new ArrayList<>();

        // 2. 只有当它不是 webp 时，才执行“转 WebP”的规则
        // 这样就避免了对已经是 webp 的图进行二次有损压缩，画质不再“包浆”
        if (!"webp".equalsIgnoreCase(suffix)) {
            PicOperations.Rule compressRule = new PicOperations.Rule();
            String webFileId = FileUtil.mainName(key) + ".webp";
            compressRule.setBucket(cosClientConfig.getBucket());
            compressRule.setFileId(webFileId);
//            compressRule.setRule("imageMogr2/format/webp");
            // 现在的代码：强行把质量压制在 80%（q/80）
            // 这在腾讯云 CI 里是画质与体积的完美平衡点
            compressRule.setRule("imageMogr2/format/webp/q/80");
            ruleArrayList.add(compressRule);
        }

        // 3. 缩略图规则依然保留（缩略图对预览加载非常重要，而且生成缩略图不影响原图画质）
        if (localFile.length() > 20 * 1024L) {  // 大于20kb才生成缩略图
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailFileId = FileUtil.mainName(key) + "_thumbnail." + suffix;
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnailFileId);
            // 这里根据原图后缀生成对应的缩略图，比如原图是 webp，缩略图也是 webp 的缩放版
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            ruleArrayList.add(thumbnailRule);
        }

        // 4. 封装操作
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1); // 返回原图信息这个必须留着

        // 如果没有任何规则需要执行（比如图片又小又是webp），就不必传 rules 列表
        if (!ruleArrayList.isEmpty()) {
            picOperations.setRules(ruleArrayList);
        }

        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }*/

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
        // 现在的代码：强行把质量压制在 60%（q/60）
        // 这在腾讯云 CI 里是画质与体积的完美平衡点
        compressRule.setRule("imageMogr2/format/webp/q/60");
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
