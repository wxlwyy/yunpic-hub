package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.DeleteRequest;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.model.dto.picture.*;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wyy.yunpicturebackend.model.vo.PictureTagCategory;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import com.wyy.yunpicturebackend.service.PictureService;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;


    /**
     * 上传或新增图片
     * @param multipartFile
     * @param uploadPictureRequest
     * @param request
     * @return
     */
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file")MultipartFile multipartFile,
                                                 UploadPictureRequest uploadPictureRequest,
                                                 HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        //校验删除图片参数
        Long pictureId = deleteRequest.getId();
        if (deleteRequest != null && pictureId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询图片是否存在
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //是本人或管理员
        User user = userService.getLoginUser(request);
        if (!(oldPicture.getUserId().equals(user.getId()) || userService.isAdmin(user))){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //删除
        boolean success = pictureService.removeById(pictureId);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（管理员），注意只会更改非null的字段
     * @param updatePictureRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody UpdatePictureRequest updatePictureRequest,
                                               HttpServletRequest request){
        //粗略校验
        if (updatePictureRequest == null || updatePictureRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //转为实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(updatePictureRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(updatePictureRequest.getTags()));
        //查看是否存在
        Long pictureId = picture.getId();
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //详细校验
        pictureService.validPicture(picture);
        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillPictureReviewParams(picture, loginUser);
        //更新
        boolean success = pictureService.updateById(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片，注意只会更改非null的字段
     * @param editPictureRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody EditPictureRequest editPictureRequest,
                                             HttpServletRequest request){
        //粗略校验
        if (editPictureRequest == null || editPictureRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查看是否存在
        Picture oldPicture = pictureService.getById(editPictureRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //转为实体类，加上一个编辑字段
        Picture picture = new Picture();
        BeanUtil.copyProperties(editPictureRequest, picture);
        picture.setEditTime(new Date());
        picture.setTags(JSONUtil.toJsonStr(editPictureRequest.getTags()));
        //详细校验
        pictureService.validPicture(picture);
        //是否为本人或管理员
        User loginUser = userService.getLoginUser(request);
        if (!(oldPicture.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) ){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //补充审核参数
        pictureService.fillPictureReviewParams(picture, loginUser);
        //更新信息
        boolean success = pictureService.updateById(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取图片列表（管理员）
     * @param queryPictureRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody QueryPictureRequest queryPictureRequest){
        //获取当前页，每页条数，查询条件
        int current = queryPictureRequest.getCurrent();
        int pageSize = queryPictureRequest.getPageSize();
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(queryPictureRequest);
        //查询
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表
     * @param queryPictureRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody QueryPictureRequest queryPictureRequest){
        //当前页，每页条数，查询条件
        int current = queryPictureRequest.getCurrent();
        int pageSize = queryPictureRequest.getPageSize();
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(queryPictureRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
        //防爬
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //普通用户只可以查看过审的图片
        queryPictureRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //脱敏
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 根据id查询图片信息（管理员）
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public BaseResponse<Picture> getPictureById(Long id){
        //校验参数
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //查询
        Picture picture = pictureService.getById(id);
        //是否存在
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * 根据id查询图片信息
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id){
        //校验参数
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //查询
        Picture picture = pictureService.getById(id);
        //是否存在
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //封装
        PictureVO pictureVO = pictureService.getPictureVO(picture);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 获取预置标签和分类
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory(){
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     * @param reviewPictureRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewPicture(@RequestBody ReviewPictureRequest reviewPictureRequest, HttpServletRequest request){
        ThrowUtils.throwIf(reviewPictureRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.reviewPicture(reviewPictureRequest, loginUser));
    }
}
