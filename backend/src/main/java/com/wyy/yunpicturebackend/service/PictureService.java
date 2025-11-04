package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicturebackend.model.dto.picture.QueryPictureRequest;
import com.wyy.yunpicturebackend.model.dto.picture.ReviewPictureRequest;
import com.wyy.yunpicturebackend.model.dto.picture.UploadPictureRequest;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author wxl
* @description 针对表【picture(图片表)】的数据库操作Service
* @createDate 2025-07-25 15:27:13
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param multipartFile
     * @param uploadPictureRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile, UploadPictureRequest uploadPictureRequest, User loginUser);



    /**
     * 校验新增、修改图片时的参数
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 获取图片包装类（主要是给userVO赋值）
     * @param picture
     * @return
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类（分页）
     * @param picturePage
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 将查询条件封装为QueryWrapper对象
     * @param queryPictureRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(QueryPictureRequest queryPictureRequest);

    /**
     * 审核图片
     * @param reviewPictureRequest
     * @return
     */
    Boolean reviewPicture(ReviewPictureRequest reviewPictureRequest, User loginUser);

    /**
     * 设置图片审核参数
     * @param picture
     * @param loginUser
     */
    public void fillPictureReviewParams(Picture picture, User loginUser);

}
