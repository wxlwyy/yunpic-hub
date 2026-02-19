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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s",filename);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(filePath, null);
            multipartFile.transferTo(tempFile);
            cosManager.putObject(filePath, tempFile);
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("file upload error, filepath =" + filePath,  e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null) {
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", tempFile.getAbsoluteFile());
                }
            }
        }

    }


    /**
     * 测试文件下载
     * @param filepath cos中文件的路径
     * @param response
     * @throws IOException
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        InputStream cosObjInputStream = cosManager.getObjectContent(filepath);
        try {
            byte[] byteArray = IOUtils.toByteArray(cosObjInputStream);
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error, filepath =" + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjInputStream != null) {
                cosObjInputStream.close();
            }
        }
    }

}
