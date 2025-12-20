package com.wyy.yunpicturebackend.service.impl;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.COSManager;
import com.wyy.yunpicturebackend.manager.upload.UploadFilePicture;
import com.wyy.yunpicturebackend.manager.upload.UploadPictureTemplate;
import com.wyy.yunpicturebackend.manager.upload.UploadUrlPicture;
import com.wyy.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.wyy.yunpicturebackend.model.dto.picture.*;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.PictureService;
import com.wyy.yunpicturebackend.mapper.PictureMapper;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.UserService;
import com.wyy.yunpicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author wxl
* @description 针对表【picture(图片表)】的数据库操作Service实现
* @createDate 2025-07-25 15:27:13
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    /*@Resource
    private FileManager fileManager;*/

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UploadFilePicture uploadFilePicture;

    @Resource
    private UploadUrlPicture uploadUrlPicture;

    @Resource
    private COSManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 上传或替换图片
     * @param inputSource 输入源（图片文件或url）
     * @param uploadPictureRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, UploadPictureRequest uploadPictureRequest, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(loginUser), ErrorCode.NO_AUTH_ERROR);
        //如果是个人空间，判断空间是否存在，校验权限
        Long spaceId = uploadPictureRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR);
            //判断是否是空间管理员
            Long userId = space.getUserId();
            if (!loginUser.getId().equals(userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            //校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容纳的图片总数量不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容纳的图片总大小不足");
            }
        }
        //判断是否有图片id，id不为空查库是否存在是更新，id为空是新增
        Long pictureId = null;
        if (uploadPictureRequest != null) {
            pictureId = uploadPictureRequest.getId();
        }
        //如果是替换图片查看是否存在这个图片
        if (pictureId != null) {
            //boolean exists = lambdaQuery().eq(Picture::getId, pictureId).exists();
            //ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "图片不存在");
            //修改图片需要本人或管理员
            Picture oldPicture = getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
            if (!(oldPicture.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser))){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 防止替换图片时修改图片的空间id，因为之前的图片的空间属性已经固定，不能修改
            // 防止用户把【私人/团队空间】的图片 -> 转移到 -> 【公共空间】
            // 逻辑：如果你没传 spaceId，我就强制给你塞回原来的 spaceId，不让你变空。
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                // 1. 防止把【公共图库】的图片 -> 转移到 -> 【私人/团队空间】
                // 2. 防止把【私人空间】 -> 转移到 -> 【团队空间】（或反之）
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        //设置相应的图片存放目录
        String uploadPicturePrefix;
        if (spaceId == null) {
            uploadPicturePrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPicturePrefix = String.format("space/%s", spaceId);
        }
        //上传图片，并赋值
        //先默认为文件上传
        UploadPictureTemplate uploadPictureTemplate = uploadFilePicture;
        if (inputSource instanceof String) {
            uploadPictureTemplate = uploadUrlPicture;
        }
        UploadPictureResult uploadPictureResult = uploadPictureTemplate.uploadPicture(inputSource, uploadPicturePrefix);
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (uploadPictureRequest != null && StrUtil.isNotBlank(uploadPictureRequest.getPicName())){
            picName = uploadPictureRequest.getPicName();
        }
        picture.setName(picName);  //这里没有用工具类直接copy，是因为这两个字段不一样
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        //设置审核参数
        fillPictureReviewParams(picture, loginUser);
        //如果是更新就额外赋值
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //判断是否是更新
        boolean isUpdate = (pictureId != null);
        long oldPicSize = 0;
        if (isUpdate) {
            Picture oldPic = getById(pictureId);
            //计算出老图片大小
            oldPicSize = oldPic.getPicSize();
        }
        //开启事务
        Long finalSpaceId = spaceId;
        Long finalOldPicSize = oldPicSize;
        transactionTemplate.execute(status -> {
            //插入数据库，转为vo返回给前端
            boolean success = saveOrUpdate(picture);
            ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "图片上传失败");
            //私人图库计算大小
            if (finalSpaceId != null) {
                if (isUpdate) {
                    //替换图片
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize - {0} + {1}", finalOldPicSize, picture.getPicSize())
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                } else {
                    //新增图片
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize + {0}", picture.getPicSize())
                            .setSql("totalCount = totalCount + 1")
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    /**
     * 批量抓取并创建图片
     * @param uploadPictureByBatchRequest
     * @param loginUser
     * @return 成功创建图片的数量
     */
    @Override
    public int uploadPictureByBatch(UploadPictureByBatchRequest uploadPictureByBatchRequest, User loginUser) {
        String searchText = uploadPictureByBatchRequest.getSearchText();
        Integer count = uploadPictureByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        //设置抓取的图片前缀
        String picNamePrefix = uploadPictureByBatchRequest.getPicNamePrefix();
        if (StrUtil.isBlank(picNamePrefix)){
            picNamePrefix = searchText;
        }
        //要抓取的页面地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        //记录上传成功的数量
        int uploadCount = 0;
        for (Element imgElement : imgElementList){
            String imgSrc = imgElement.attr("src");
            if (StrUtil.isBlank(imgSrc)){
                log.info("当前链接为空，已跳过：{}", imgSrc);
                continue;
            }
            //处理图片地址，去掉修饰图片的元素
            int questionMarkIndex = imgSrc.indexOf("?");
            if (questionMarkIndex > -1){
                imgSrc = imgSrc.substring(0, questionMarkIndex);
            }
            try {
                UploadPictureRequest uploadPictureRequest = new UploadPictureRequest();
                if (StrUtil.isNotBlank(picNamePrefix)){
                    uploadPictureRequest.setPicName(picNamePrefix + (uploadCount + 1));
                }
                PictureVO pictureVO = uploadPicture(imgSrc, uploadPictureRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e){
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count){
                break;
            }
        }
        return uploadCount;
    }

    /**
     * 删除图片
     * @param pictureId
     * @param loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        //查询图片是否存在
        Picture oldPicture = getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //空间权限校验
        checkPictureAuth(loginUser, oldPicture);
        //开启事务
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            //删除
            boolean success = removeById(pictureId);
            ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败");
            //私人图库计算大小
            if (finalSpaceId != null) {
                //删除图片，更新空间大小
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize - {0}", oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        clearCosPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     * @param editPictureRequest
     * @param loginUser
     */
    @Override
    public void editPicture(EditPictureRequest editPictureRequest, User loginUser) {
        //查看是否存在
        Picture oldPicture = getById(editPictureRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //转为实体类，加上一个编辑字段
        Picture picture = new Picture();
        BeanUtil.copyProperties(editPictureRequest, picture);
        picture.setEditTime(new Date());
        picture.setTags(JSONUtil.toJsonStr(editPictureRequest.getTags()));
        //详细校验
        validPicture(picture);
        //空间权限校验
        checkPictureAuth(loginUser, oldPicture);
        //补充审核参数
        fillPictureReviewParams(picture, loginUser);
        //操作数据库更新信息
        boolean success = updateById(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 将个人空间中的图片批量修改为相同的分类/标签/名称
     * @param editPictureByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(EditPictureByBatchRequest editPictureByBatchRequest, User loginUser) {
        Long spaceId = editPictureByBatchRequest.getSpaceId();
        List<Long> pictureIdList = editPictureByBatchRequest.getPictureIdList();
        String category = editPictureByBatchRequest.getCategory();
        List<String> tags = editPictureByBatchRequest.getTags();
        String nameRule = editPictureByBatchRequest.getNameRule();

        //校验参数（必要）
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验空间是否存在，校验用户是否是该空间的创建者
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //查询该空间存在的图片
        List<Picture> pictureList = lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .select(Picture::getId)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        //给图片类集合赋值统一的标签和分类
        for (Picture picture : pictureList) {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        }
        //给图片类集合赋值图片名
        if (StrUtil.isNotBlank(nameRule)) {
            fillPictureWithNameRule(pictureList, nameRule);
        }
        //更新
        boolean success = updateBatchById(pictureList);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 给图片类集合赋值统一的图片名称（nameRule 格式：图片{序号}）
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        for (Picture picture : pictureList) {
            String pictureName = nameRule.replace("{序号}", String.valueOf(count++));
            picture.setName(pictureName);
        }
    }

    /**
     * 对某个图片操作的空间权限校验
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            //公共图库，仅本人或管理员操作图片
            if (!(loginUser.getId().equals(picture.getUserId()) || userService.isAdmin(loginUser))) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            //私人图库，仅本人可操作图片
            if (!(loginUser.getId().equals(picture.getUserId()))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        }
    }

    /**
     * 校验修改图片的参数
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String introduction = picture.getIntroduction();
        //id不能为空
        ThrowUtils.throwIf(null == id, ErrorCode.PARAMS_ERROR, "id不能为空");
        //简介长度不能大于500
        if (StrUtil.isNotBlank(introduction)){
            ThrowUtils.throwIf(introduction.length() > 500, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 获取图片包装类（主要是给userVO赋值）
     * @param picture
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        //将数据转移
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //查出图片相关的User信息
        Long userId = pictureVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片包装类（分页）
     * @param picturePage
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        //取出分页数据
        List<Picture> pictureList = picturePage.getRecords();
        //判断数据是否为空
        if (pictureList == null) {
            return null;
        }
        //new一个包装page
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        //把pictureList转为包装list
        List<PictureVO> pictureVOList = pictureList.stream().map(p -> PictureVO.objToVo(p)).collect(Collectors.toList());
        //关联用户信息，先查所有用户Id（放到set防止重复），用Id查出集合，用户信息绑定
        Set<Long> userIdSet = pictureList.stream().map(p -> p.getUserId()).collect(Collectors.toSet());
        List<User> users = userService.listByIds(userIdSet);
        Map<Long, List<User>> userIdUserListMap = users.stream().collect(Collectors.groupingBy(user -> user.getId()));
        //遍历包装类，将对应的Id的User信息赋值
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 将查询条件封装为QueryWrapper对象
     * @param queryPictureRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(QueryPictureRequest queryPictureRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (queryPictureRequest == null) {
            return queryWrapper;
        }
        Long id = queryPictureRequest.getId();
        Long spaceId = queryPictureRequest.getSpaceId();
        boolean nullSpaceId = queryPictureRequest.isNullSpaceId();
        String name = queryPictureRequest.getName();
        String introduction = queryPictureRequest.getIntroduction();
        String category = queryPictureRequest.getCategory();
        List<String> tags = queryPictureRequest.getTags();
        Long picSize = queryPictureRequest.getPicSize();
        Integer picWidth = queryPictureRequest.getPicWidth();
        Integer picHeight = queryPictureRequest.getPicHeight();
        Double picScale = queryPictureRequest.getPicScale();
        String picFormat = queryPictureRequest.getPicFormat();
        Long userId = queryPictureRequest.getUserId();
        String searchText = queryPictureRequest.getSearchText();
        String sortField = queryPictureRequest.getSortField();
        String sortOrder = queryPictureRequest.getSortOrder();
        Integer reviewStatus = queryPictureRequest.getReviewStatus();
        Long reviewerId = queryPictureRequest.getReviewerId();
        String reviewMessage = queryPictureRequest.getReviewMessage();
        Date startEditTime = queryPictureRequest.getStartEditTime();
        Date endEditTime = queryPictureRequest.getEndEditTime();
        //搜索框查询
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or().like("introduction", searchText));
        }
        //其他查询
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // >=
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // <
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        if (CollUtil.isNotEmpty(tags)){
            tags.forEach(tag -> queryWrapper.like("tags", "\"" + tag + "\""));
        }
        // 如果前端没传 sortField，那就默认按 createTime 倒序
        if (StrUtil.isBlank(sortField)) {
            sortField = "createTime";
            sortOrder = "descend";
        }
        // 统一构建排序条件（默认true是升序，false是降序）
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 审核图片
     * @param reviewPictureRequest
     * @return
     */
    @Override
    public Boolean reviewPicture(ReviewPictureRequest reviewPictureRequest,  User loginUser) {
        //图片id不能为null，审核状态参数要合法，审核状态不能为0（不能逆向操作）
        Long pictureId = reviewPictureRequest.getId();
        Integer reviewStatus = reviewPictureRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getPictureReviewStatusEnum(reviewStatus);
        if (pictureId == null || pictureReviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查看图片是否存在
        Picture oldPicture = getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //不能重复审核
        ThrowUtils.throwIf(oldPicture.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR,"请勿重复审核");
        //更新审核信息
        Picture picture = new Picture();
        BeanUtil.copyProperties(reviewPictureRequest, picture);
        picture.setReviewTime(new Date());
        picture.setReviewerId(loginUser.getId());
        boolean success = updateById(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 设置图片审核参数
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillPictureReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)){
            //管理员自动过审
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        } else {
            //非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 删除云存储上的图片
     * @param oldPicture
     */
    @Async
    @Override
    public void clearCosPictureFile(Picture oldPicture) {
        String pictureUrl = oldPicture.getUrl();
        Long count = userService.lambdaQuery().eq(User::getUserAvatar, pictureUrl).count();
        //如果图片存在于其他表中（这里比如说用户表），就不能删除
        if (count > 0) {
            return;
        }
        //清理以webp为后缀的图片
        cosManager.deleteObject(pictureUrl);
        //清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * 通过颜色搜索图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceId == null || picColor == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验用户的空间权限
        Space space = spaceService.getById(spaceId);
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有改空间的访问权限");
        }
        //查询空间所有图片（必须要有主色调）
        List<Picture> pictureList = lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        //按照相似度大小降序排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble((Picture picture) -> {
                    String picColor2 = picture.getPicColor();
                    if (StrUtil.isBlank(picColor2)) {
                        return -1.0;
                    }
                    return ColorSimilarUtils.calculateSimilarity(picColor, picColor2);
                }).reversed())
                .limit(12)  //取前12个
                .collect(Collectors.toList());
        //转换为PictureVO
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }
}




