package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wyy.yunpicturebackend.model.dto.picture.*;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import io.swagger.models.auth.In;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
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
     * @param currentUser
     * @return
     */
    PictureVO uploadOrUpdatePicture(Object inputSource, UploadPictureRequest uploadPictureRequest, User currentUser);

    /**
     * 批量抓取并创建图片
     * @param uploadPictureByBatchRequest
     * @param currentUser
     * @return 成功创建图片的数量
     */
    int uploadPictureByBatch(UploadPictureByBatchRequest uploadPictureByBatchRequest, User currentUser);

    /**
     * 删除图片
     * @param pictureId
     * @param request
     */
    void deletePicture(long pictureId, HttpServletRequest request);

    /**
     * 编辑图片
     * @param editPictureRequest
     * @param currentUser
     */
    void editPictureInfo(EditPictureRequest editPictureRequest, User currentUser);

    /**
     * 将个人空间中的图片批量修改为相同的分类/标签/名称
     * @param editPictureByBatchRequest
     * @param currentUser
     * @return
     */
    void editPictureInfoByBatch(EditPictureByBatchRequest editPictureByBatchRequest, User currentUser);

    /**
     * 空间权限校验
     * @param currentUser
     * @param picture
     */
    void checkPictureManagerAuth(User currentUser, Picture picture);

    /**
     * 校验新增、修改图片时的参数
     * @param picture
     */
    void validPictureParam(Picture picture);

    /**
     * 获取图片包装类（主要是给userVO赋值）
     * @param picture
     * @return
     */
    PictureVO convertToPictureVO(Picture picture);

    /**
     * 获取图片包装类（分页）
     * @param picturePage
     * @return
     */
//    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 将查询条件封装为QueryWrapper对象
     * @param queryPictureRequest
     * @return
     */
    QueryWrapper<Picture> buildQueryWrapper(QueryPictureRequest queryPictureRequest);

    /**
     * 审核图片
     * @param reviewPictureRequest
     * @return
     */
    Boolean reviewPicture(ReviewPictureRequest reviewPictureRequest, User currentUser);

    /**
     * 设置图片审核参数
     * @param picture
     * @param currentUser
     */
    void fillPictureReviewParams(Picture picture, User currentUser);

    /**
     * 删除云存储上的图片
     * @param oldPicture
     */
    void clearCosPictureFile(Picture oldPicture);

    /**
     * 通过颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param currentUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User currentUser);

    /**
     * 创建AI扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param currentUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User currentUser);

    void updatePictureInfo(UpdatePictureRequest updatePictureRequest, User currentUser);

    Picture getPictureById(Long id);

    Page<Picture> pagePicture(QueryPictureRequest queryPictureRequest);

    List<PictureVO> convertToPictureVOList(List<Picture> pictureList)

    Page<PictureVO> convertToPictureVOPage(Page<Picture> picturePage)
}
