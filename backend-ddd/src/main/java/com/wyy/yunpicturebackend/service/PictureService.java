package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wyy.yunpicturebackend.model.dto.picture.*;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicture.domain.user.entity.User;
import com.wyy.yunpicturebackend.model.vo.PictureVO;

import java.util.List;

/**
* @author wxl
* @description 针对表【picture(图片表)】的数据库操作Service
* @createDate 2025-07-25 15:27:13
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传或替换图片
     * @param inputSource
     * @param uploadPictureRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, UploadPictureRequest uploadPictureRequest, User loginUser);

    /**
     * 批量抓取并创建图片
     * @param uploadPictureByBatchRequest
     * @param loginUser
     * @return 成功创建图片的数量
     */
    int uploadPictureByBatch(UploadPictureByBatchRequest uploadPictureByBatchRequest, User loginUser);

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param editPictureRequest
     * @param loginUser
     */
    void editPicture(EditPictureRequest editPictureRequest, User loginUser);

    /**
     * 将个人空间中的图片批量修改为相同的分类/标签/名称
     * @param editPictureByBatchRequest
     * @param loginUser
     * @return
     */
    void editPictureByBatch(EditPictureByBatchRequest editPictureByBatchRequest, User loginUser);

    /**
     * 空间权限校验
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

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
    void fillPictureReviewParams(Picture picture, User loginUser);

    /**
     * 删除云存储上的图片
     * @param oldPicture
     */
    void clearCosPictureFile(Picture oldPicture);

    /**
     * 通过颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 创建AI扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
