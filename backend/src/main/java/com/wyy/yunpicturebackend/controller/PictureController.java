package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.api.aliyunai.ALiYunAiApi;
import com.wyy.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wyy.yunpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.wyy.yunpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.wyy.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.DeleteRequest;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.wyy.yunpicturebackend.manager.auth.StpKit;
import com.wyy.yunpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.wyy.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.wyy.yunpicturebackend.model.dto.picture.*;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wyy.yunpicturebackend.model.vo.PictureTagCategory;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import com.wyy.yunpicturebackend.service.PictureService;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ALiYunAiApi aLiYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)  //设置初始容量
                    .maximumSize(10000L)  //最大1万条数据
                    .expireAfterWrite(5L, TimeUnit.MINUTES) // 缓存 5 分钟移除
                    .build();

    /**
     * 上传或替换图片
     * @param multipartFile
     * @param uploadPictureRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file")MultipartFile multipartFile,
                                                 UploadPictureRequest uploadPictureRequest,
                                                 HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传或新增图片通过url
     * @param uploadPictureRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody UploadPictureRequest uploadPictureRequest,
                                                      HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        String fileUrl = uploadPictureRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, uploadPictureRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量抓取并创建图片（仅管理员）
     * @param uploadPictureByBatchRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/batch")
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody UploadPictureByBatchRequest uploadPictureByBatchRequest,
                                                        HttpServletRequest request){
        ThrowUtils.throwIf(uploadPictureByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(uploadPictureByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 删除图片
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        //校验删除图片参数
        Long pictureId = deleteRequest.getId();
        if (deleteRequest != null && pictureId < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.deletePicture(pictureId, loginUser);
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
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody EditPictureRequest editPictureRequest,
                                             HttpServletRequest request){
        //粗略校验
        if (editPictureRequest == null || editPictureRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(editPictureRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量修改个人空间中的图片的分类/标签/名称
     * @param editPictureByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody EditPictureByBatchRequest editPictureByBatchRequest,
                                             HttpServletRequest request){
        ThrowUtils.throwIf(editPictureByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(editPictureByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 图片管理页面，分页获取图片列表（管理员）
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
     * 图片列表页面，分页获取图片列表
     * @param queryPictureRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody QueryPictureRequest queryPictureRequest,
                                                             HttpServletRequest request){
        //当前页，每页条数，查询条件
        int current = queryPictureRequest.getCurrent();
        int pageSize = queryPictureRequest.getPageSize();
        //校验空间权限
        Long spaceId = queryPictureRequest.getSpaceId();
        if (spaceId == null) {
            //公共图库
            //普通用户只可以查看过审的图片
            queryPictureRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            queryPictureRequest.setNullSpaceId(true);
        } else {
            //私人图库或团队空间
            /*Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            User loginUser = userService.getLoginUser(request);
            //只有本人可以查看所有图片
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }*/
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(queryPictureRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
        //防爬
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //脱敏
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 图片列表页面，分页获取图片列表
     * @param queryPictureRequest
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody QueryPictureRequest queryPictureRequest){
        //当前页，每页条数，查询条件
        int current = queryPictureRequest.getCurrent();
        int pageSize = queryPictureRequest.getPageSize();
        //普通用户只可以查看过审的图片
        queryPictureRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(queryPictureRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
        //防爬
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //构建缓存
        String queryKey = JSONUtil.toJsonStr(queryPictureRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryKey.getBytes());
        //Redis的key
        String redisKey = String.format("yunpicture:listPictureVOByPage:%s", hashKey);
        //本地缓存的key
        String cacheKey = String.format("yunpicture:listPictureVOByPage:%s", hashKey);
        //1.先查询本地缓存，如果命中直接返回
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cacheValue != null) {
            Page<PictureVO> value = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(value);
        }
        //2.本地缓存未命中，再先查询Redis缓存，如果命中更新本地缓存
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String redisValue = valueOperations.get(redisKey);
        if (redisValue != null) {
            LOCAL_CACHE.put(cacheKey, redisValue);
            Page<PictureVO> value = JSONUtil.toBean(redisValue, Page.class);
            return ResultUtils.success(value);
        }
        //3.如果Redis也未命中，脱敏，查询数据库
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        //缓存中没有数据的情况下，设置缓存
        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(cacheKey, cacheValue);
        redisValue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        valueOperations.set(redisKey, redisValue, cacheExpireTime, TimeUnit.SECONDS);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 图片管理页面，根据id查询图片信息（管理员）
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
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request){
        //校验参数
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //查询
        Picture picture = pictureService.getById(id);
        //是否存在
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验空间权限（公共图库都可以查看详细信息，个人图库要校验是本人或管理员），已经改为使用sa-token鉴权
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
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

    /**
     * 以图搜图
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest){
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId < 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> imageList = ImageSearchApiFacade.searchImage(picture.getUrl());
        return ResultUtils.success(imageList);
    }

    /**
     * 通过目标图片的主色调搜索图片
     * @param searchPictureByColorRequest
     * @param request
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 创建AI扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request){
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询AI扩图任务
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse response = aLiYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }
}
