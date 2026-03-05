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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Validated
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
    @PostMapping("/upload")  // uploadOrUpdatePicture
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file")MultipartFile multipartFile,
                                                 UploadPictureRequest uploadPictureRequest,
                                                 HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        PictureVO pictureVO = pictureService.uploadOrUpdatePicture(multipartFile, uploadPictureRequest, currentUser);
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
        User currentUser = userService.getCurrentUser(request);
        String fileUrl = uploadPictureRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadOrUpdatePicture(fileUrl, uploadPictureRequest, currentUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量抓取并创建图片（仅管理员）
     * @param uploadPictureByBatchRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/upload/batch")   // batchUploadPicture
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody UploadPictureByBatchRequest uploadPictureByBatchRequest,
                                                        HttpServletRequest request){
//        ThrowUtils.throwIf(uploadPictureByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User currentUser = userService.getCurrentUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(uploadPictureByBatchRequest, currentUser);
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
    public BaseResponse<Boolean> deletePicture(@Validated @RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request){
        //校验删除图片参数
        Long pictureId = deleteRequest.getId();
//        if (deleteRequest != null && pictureId < 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
        pictureService.deletePicture(pictureId, request);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（管理员），注意只会更改非null的字段
     * @param updatePictureRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@Validated @RequestBody UpdatePictureRequest updatePictureRequest,
                                               HttpServletRequest request){
        // 获取登录用户
        User currentUser = userService.getCurrentUser(request);
        pictureService.updatePictureInfo(updatePictureRequest, currentUser);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片，注意只会更改非null的字段
     * @param editPictureRequest
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@Validated @RequestBody EditPictureRequest editPictureRequest,
                                             HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        pictureService.editPictureInfo(editPictureRequest, currentUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量修改个人空间中的图片的分类/标签/名称
     * @param editPictureByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")  // batchEditPicture
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody EditPictureByBatchRequest editPictureByBatchRequest,
                                             HttpServletRequest request){
        ThrowUtils.throwIf(editPictureByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User currentUser = userService.getCurrentUser(request);
        pictureService.editPictureInfoByBatch(editPictureByBatchRequest, currentUser);
        return ResultUtils.success(true);
    }

    /**
     * 公共空间图片管理页面，分页获取图片列表（管理员）
     * @param queryPictureRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")  // pagePicture
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody QueryPictureRequest queryPictureRequest){
        Page<Picture> picturePage = pictureService.pagePicture(queryPictureRequest);
        return ResultUtils.success(picturePage);
    }

    /**
     * 公共空间或个人空间图片列表页面，分页获取图片列表
     * @param queryPictureRequest
     * @return
     */
    @PostMapping("/list/page/vo")  // pagePictureVO
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
            User currentUser = userService.getCurrentUser(request);
            //只有本人可以查看所有图片
            if (!currentUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }*/
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        QueryWrapper<Picture> queryWrapper = pictureService.buildQueryWrapper(queryPictureRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
        //防爬
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //脱敏
        Page<PictureVO> pictureVOPage = pictureService.convertToPictureVOPage(picturePage);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 图片列表页面，分页获取图片列表
     * @param queryPictureRequest
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@Validated @RequestBody QueryPictureRequest queryPictureRequest){
        //当前页，每页条数，查询条件
        int current = queryPictureRequest.getCurrent();
        int pageSize = queryPictureRequest.getPageSize();
        //普通用户只可以查看过审的图片
        queryPictureRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        QueryWrapper<Picture> queryWrapper = pictureService.buildQueryWrapper(queryPictureRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
        // 参数校验，防爬
//        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
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
        Page<PictureVO> pictureVOPage = pictureService.convertToPictureVOPage(picturePage);
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
    public BaseResponse<Picture> getPictureById(@NotNull(message = "用户ID不能为空")
                                                    @Min(value = 1, message = "用户ID不合法")
                                                    Long id){
//        //校验参数
//        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //查询
        Picture picture = pictureService.getPictureById(id);

        return ResultUtils.success(picture);
    }

    /**
     * 根据id查询图片信息
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@NotNull(message = "用户ID不能为空")
                                                    @Min(value = 1, message = "用户ID不合法")
                                                    Long id,
                                                    HttpServletRequest request){
        //查询
        Picture picture = pictureService.getById(id);
        //是否存在
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验图片管理权限（公共图库都可以查看详细信息，个人图库要校验是本人或管理员），已经改为使用sa-token鉴权
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User currentUser = userService.getCurrentUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, currentUser);
        PictureVO pictureVO = pictureService.convertToPictureVO(picture);
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
    public BaseResponse<Boolean> reviewPicture(@Validated @RequestBody ReviewPictureRequest reviewPictureRequest, HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        return ResultUtils.success(pictureService.reviewPicture(reviewPictureRequest, currentUser));
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
        User currentUser = userService.getCurrentUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, currentUser);
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
        User currentUser = userService.getCurrentUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, currentUser);
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
