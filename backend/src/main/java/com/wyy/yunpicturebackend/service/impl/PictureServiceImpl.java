package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.FileManager;
import com.wyy.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.wyy.yunpicturebackend.model.dto.picture.QueryPictureRequest;
import com.wyy.yunpicturebackend.model.dto.picture.ReviewPictureRequest;
import com.wyy.yunpicturebackend.model.dto.picture.UploadPictureRequest;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.PictureService;
import com.wyy.yunpicturebackend.mapper.PictureMapper;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author wxl
* @description 针对表【picture(图片表)】的数据库操作Service实现
* @createDate 2025-07-25 15:27:13
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     * @param multipartFile
     * @param uploadPictureRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, UploadPictureRequest uploadPictureRequest, User loginUser) {
        //判断是否有图片id，id不为空查库是否存在是更新，id为空是新增
        Long pictureId = null;
        if (uploadPictureRequest != null) {
            pictureId = uploadPictureRequest.getId();
        }
        //如果id不为空，查看是否存在这个图片
        if (pictureId != null) {
//            boolean exists = lambdaQuery().eq(Picture::getId, pictureId).exists();
//            ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "图片不存在");
            //修改图片需要本人或管理员
            Picture oldPicture = getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
            if (!(oldPicture.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser))){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        String uploadPicturePrefix = String.format("public/%s", loginUser.getId());
        //上传图片，并赋值
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPicturePrefix);
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());  //这里没有用工具类直接copy，是因为这两个字段不一样
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //如果是更新就额外赋值
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //插入数据库，转为vo返回给前端
        boolean success = saveOrUpdate(picture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return PictureVO.objToVo(picture);
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
        //搜索框查询
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or().like("introduction", searchText));
        }
        //其他查询
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale), "picScale", picScale);
        queryWrapper.eq(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        if (CollUtil.isNotEmpty(tags)){
            tags.forEach(tag -> queryWrapper.like("tags", "\"" + tag + "\""));
        }
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
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



}




