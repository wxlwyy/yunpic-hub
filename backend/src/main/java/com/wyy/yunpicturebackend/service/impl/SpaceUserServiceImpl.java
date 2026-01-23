package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.SpaceUser;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.wyy.yunpicturebackend.model.vo.SpaceUserVO;
import com.wyy.yunpicturebackend.model.vo.SpaceVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.SpaceUserService;
import com.wyy.yunpicturebackend.mapper.SpaceUserMapper;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author wxl
* @description 针对表【space_user(空间成员表)】的数据库操作Service实现
* @createDate 2026-01-21 21:50:09
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加成员进入团队空间或编辑成员的角色时检验参数
     * @param spaceUser
     * @param add
     */
    @Override
    public void validateSpaceUserParams(SpaceUser spaceUser, boolean add) {
        // 校验基本参数
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 添加空间成员时
        if (add) {
            Long userId = spaceUser.getUserId();
            Long spaceId = spaceUser.getSpaceId();
            // 两个id必须同时存在
            ThrowUtils.throwIf(ObjUtil.hasEmpty(userId, spaceId), ErrorCode.PARAMS_ERROR);
            // 用户和空间没被逻辑删除
            User user = userService.getById(userId);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 添加空间成员或修改空间成员角色时
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            // 空间角色参数必须存在
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间成员角色不存在");
        }
    }

    /**
     * 添加用户进入团队空间
     * @param spaceUserAddRequest
     * @return
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 校验基本参数
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验合法性
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        validateSpaceUserParams(spaceUser, true);
        // 数据库操作
        boolean success = save(spaceUser);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 获取查询包装类
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        // 校验基本参数
        QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return spaceUserQueryWrapper;
        }
        // 封装查询条件
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        spaceUserQueryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        spaceUserQueryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        spaceUserQueryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        spaceUserQueryWrapper.eq(StrUtil.isNotBlank(spaceRole), "spaceRole", spaceRole);
        return spaceUserQueryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUserVO(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpaceVO(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 将空间成员列表脱敏
     * @param spaceUserList
     * @return
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 校验基本参数
        ThrowUtils.throwIf(spaceUserList == null, ErrorCode.PARAMS_ERROR);
        // 将spaceUserList里的数据转到spaceUserListVO里（除了UserVO和SpaceVO）
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 将spaceUserListVO里的userId和spaceId取出
        Set<Long> spaceIdSet = spaceUserVOList.stream().map(SpaceUserVO::getSpaceId).collect(Collectors.toSet());
        Set<Long> userIdSet = spaceUserVOList.stream().map(SpaceUserVO::getUserId).collect(Collectors.toSet());
        // 查询相关的User和Space对象
        List<Space> spaceList = spaceService.listByIds(spaceIdSet);
        List<User> userList = userService.listByIds(userIdSet);
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceList.stream().collect(Collectors.groupingBy(Space::getId));
        Map<Long, List<User>> userIdUserListMap = userList.stream().collect(Collectors.groupingBy(User::getId));
        // 遍历spaceUserListVO，将User和Space对象脱敏，设置UserVO和SpaceVO的值
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            if (spaceIdSpaceListMap.containsKey(userId)) {
                Space space = spaceIdSpaceListMap.get(userId).get(0);
                SpaceVO spaceVO = SpaceVO.objToVo(space);
                spaceUserVO.setSpaceVO(spaceVO);
            }
            if (userIdUserListMap.containsKey(spaceId)) {
                User user = userIdUserListMap.get(spaceId).get(0);
                UserVO userVO = userService.getUserVO(user);
                spaceUserVO.setUserVO(userVO);
            }
        });
        return spaceUserVOList;
    }
}




