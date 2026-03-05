package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.sharding.DynamicShardingManager;
import com.wyy.yunpicturebackend.model.dto.space.AddSpaceRequest;
import com.wyy.yunpicturebackend.model.dto.space.QuerySpaceRequest;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.SpaceUser;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.wyy.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.wyy.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.wyy.yunpicturebackend.model.enums.UserRoleEnum;
import com.wyy.yunpicturebackend.model.vo.PictureVO;
import com.wyy.yunpicturebackend.model.vo.SpaceVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.mapper.SpaceMapper;
import com.wyy.yunpicturebackend.service.SpaceUserService;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author wxl
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-11-19 17:15:39
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{


    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /*@Resource
    @Lazy
    private DynamicShardingManager dynamicShardingManager;*/

    /**
     * 创建空间、修改空间时校验参数的合法性
     * @param space
     * @param add 是否是创建空间
     */
    @Override
    public void validateSpaceParams(Space space , boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnumByValue = SpaceTypeEnum.getSpaceTypeEnumByValue(spaceType);
        //创建空间
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceLevel), ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            ThrowUtils.throwIf(spaceTypeEnumByValue == null, ErrorCode.PARAMS_ERROR, "空间类型不能为空");
        }
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30,
                ErrorCode.PARAMS_ERROR, "空间名称过长");
        ThrowUtils.throwIf(ObjUtil.isNotEmpty(spaceLevel) && spaceLevelEnum == null,
                ErrorCode.PARAMS_ERROR, "空间等级不存在");
        ThrowUtils.throwIf(spaceType != null && spaceTypeEnumByValue == null,
                ErrorCode.PARAMS_ERROR, "空间类型不存在");
    }

    /**
     * 获取空间包装类（主要是给userVO赋值）
     * @param space
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        //将数据转移
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //查出图片相关的User信息
        Long userId = spaceVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUserVO(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间包装类（分页）
     * @param spacePage
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        //取出分页数据
        List<Space> spaceList = spacePage.getRecords();
        //new一个包装page
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        //判断数据是否为空，因为 spaceList 是一个空列表 []，它不是 null。
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //把spaceList转为包装list
        List<SpaceVO> spaceVOList = spaceList.stream().map(p -> SpaceVO.objToVo(p)).collect(Collectors.toList());
        //关联用户信息，先查所有用户Id（放到set防止重复），用Id查出集合，用户信息绑定
        Set<Long> userIdSet = spaceList.stream().map(p -> p.getUserId()).collect(Collectors.toSet());
        List<User> users = userService.listByIds(userIdSet);
        Map<Long, List<User>> userIdUserListMap = users.stream().collect(Collectors.groupingBy(user -> user.getId()));
        //遍历包装类，将对应的Id的User信息赋值
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)){
                user = userIdUserListMap.get(userId).get(0);
            }
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUserVO(userVO);
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 将查询条件封装为QueryWrapper对象
     * @param querySpaceRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(QuerySpaceRequest querySpaceRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (querySpaceRequest == null) {
            return queryWrapper;
        }
        String spaceName = querySpaceRequest.getSpaceName();
        Integer spaceLevel = querySpaceRequest.getSpaceLevel();
        Integer spaceType = querySpaceRequest.getSpaceType();
        Long id = querySpaceRequest.getId();
        Long userId = querySpaceRequest.getUserId();
        String sortField = querySpaceRequest.getSortField();
        String sortOrder = querySpaceRequest.getSortOrder();
        //拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);

        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    /**
     * 根据空间级别自动填充空间限额数据
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        //根据空间级别自动填充空间限额数据
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (ObjUtil.isNotNull(spaceLevelEnum)) {
            if (ObjUtil.isNull(space.getMaxSize())) {
                space.setMaxSize(spaceLevelEnum.getMaxSize());
            }
            if (ObjUtil.isNull(space.getMaxCount())) {
                space.setMaxCount(spaceLevelEnum.getMaxCount());
            }
        }
    }

    /**
     * 创建私有空间或团队空间
     * @return
     */
    @Override
    public long addSpace(AddSpaceRequest addSpaceRequest, User currentUser) {
        //创建空间时用户没写数据，填默认值
        if (StrUtil.isBlank(addSpaceRequest.getSpaceName())) {
            addSpaceRequest.setSpaceName("默认空间");
        }
        if (ObjUtil.isEmpty(addSpaceRequest.getSpaceLevel())) {
            addSpaceRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (addSpaceRequest.getSpaceType() == null) {
            addSpaceRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        boolean isAdmin = userService.isAdmin(currentUser);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(currentUser.getUserRole());
        Integer spaceLevel = addSpaceRequest.getSpaceLevel();
        // 管理员可创建任何等级的空间，VIP能创建普通和专业空间，普通用户只能创建普通空间
        if (!(isAdmin || (UserRoleEnum.VIP.equals(userRoleEnum) && spaceLevel <= SpaceLevelEnum.PROFESSIONAL.getValue()) || (UserRoleEnum.USER.equals(userRoleEnum) && spaceLevel == SpaceLevelEnum.COMMON.getValue()))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        //数据转化
        Space space = new Space();
        BeanUtil.copyProperties(addSpaceRequest, space);
        //填充数据
        fillSpaceBySpaceLevel(space);
        //添加用户id
        Long userId = currentUser.getId();
        space.setUserId(userId);
        //校验参数合法性
        validateSpaceParams(space, true);
        //校验权限（普通用户只能创建普通版，管理员可以创建任何版）
        /*if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(currentUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }*/

        //加锁，写入数据库
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每种空间只能创建一个");
                boolean success = save(space);
                ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
                // 如果用户创建的时团队空间，将这个用户等信息填入空间用户关联表中
                if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    success = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 为旗舰版团队空间创建分表（目前不用分表功能）
                // dynamicShardingManager.createSpacePictureTable(space);
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    /**
     * 校验访问空间的权限（仅本人或管理员可访问）
     * @param currentUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User currentUser, Space space) {
        if (!(space.getUserId().equals(currentUser.getId()) || userService.isAdmin(currentUser))){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


}




