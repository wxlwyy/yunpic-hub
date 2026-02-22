package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.server.HttpServerResponse;
import com.qcloud.cos.utils.IOUtils;
import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.manager.COSManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RestController
@Slf4j
public class FileController {

    @Resource
    private COSManager cosManager;

    /**
     * 测试文件上传
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart MultipartFile multipartFile){
        // 获取原文件名 (比如: 1.jpg)
        String originalFilename = multipartFile.getOriginalFilename();
        // 截取文件后缀名 (结果: .jpg)
        // 假设 originalFilename 不为空，这里简单处理，实际可用 Hutool 工具类
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        // String cloudFilePath = String.format("/test/%s",originalFilename);
        // 生成全局唯一文件名，防止互相覆盖！
        // 结果类似: 9b2d3c4e5f6a.jpg
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String uniqueFileName = uuid + extension;
        // 拼装云端路径
        // 结果类似: /test/9b2d3c4e5f6a.jpg
        String cloudFilePath = String.format("/test/%s", uniqueFileName);
        File tempFile = null;
        try {
            // 标准创建临时文件：前缀用UUID，后缀用真实后缀。
            // 操作系统会在默认临时目录下生成类似：C:\Temp\9b2d3c4e5f6a8837482.jpg 的规范文件
            tempFile = File.createTempFile(uuid, extension);
            multipartFile.transferTo(tempFile);
            cosManager.putObject(cloudFilePath, tempFile);
            return ResultUtils.success(cloudFilePath);
        } catch (IOException e) {
            log.error("文件上传失败, 路径: {}" ,cloudFilePath,  e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null) {
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.error("本地临时文件删除失败: {}", tempFile.getAbsoluteFile());
                }
            }
        }

    }


    /**
     * 测试文件下载
     * @param cloudFilePath cos中文件的路径
     * @param response
     * @throws IOException
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/download")
    public void testDownloadFile(String cloudFilePath, HttpServletResponse response) throws IOException {
        InputStream cosObjInputStream = cosManager.getObjectContent(cloudFilePath);
        try {
            // byte[] byteArray = IOUtils.toByteArray(cosObjInputStream);
            response.setContentType("application/octet-stream;charset=UTF-8");
            // cloudFilePath不是完整的 URL！它仅仅是那个相对路径，比如 /test/aaa.jpg
            // 从 "/test/aaa.jpg" 中只截取出 "aaa.jpg"
            String cloudFilePath1 = cloudFilePath.substring(cloudFilePath.lastIndexOf("/") + 1);
            response.setHeader("Content-Disposition", "attachment; filename=" + cloudFilePath1);
            // response.getOutputStream().write(byteArray);
            StreamUtils.copy(cosObjInputStream, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error, cloudFilePath =" + cloudFilePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjInputStream != null) {
                cosObjInputStream.close();
            }
        }
    }

}
