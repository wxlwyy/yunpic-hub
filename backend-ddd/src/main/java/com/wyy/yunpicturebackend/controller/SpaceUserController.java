package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wyy.yunpicture.application.service.UserApplicationService;
import com.wyy.yunpicture.infrastructure.common.BaseResponse;
import com.wyy.yunpicture.infrastructure.common.DeleteRequest;
import com.wyy.yunpicture.infrastructure.common.ResultUtils;
import com.wyy.yunpicture.infrastructure.exception.BusinessException;
import com.wyy.yunpicture.infrastructure.exception.ErrorCode;
import com.wyy.yunpicture.infrastructure.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.wyy.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wyy.yunpicturebackend.model.entity.SpaceUser;
import com.wyy.yunpicture.domain.user.entity.User;
import com.wyy.yunpicturebackend.model.vo.SpaceUserVO;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.SpaceUserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceUserService spaceUserService;

    /**
     * 添加成员到团队空间
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest){
        //校验基本参数
        ThrowUtils.throwIf(ObjUtil.isNull(spaceUserAddRequest), ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 在团队空间删除成员
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest){
        //校验基本参数及合法性
        Long id = deleteRequest.getId();
        if (deleteRequest == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询空间成员是否存在
        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //删除
        boolean success = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间成员角色
     * @param spaceUserEditRequest
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest){
        //校验基本参数及合法性
        Long id = spaceUserEditRequest.getId();
        if (spaceUserEditRequest == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查看是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        //转为实体类
        SpaceUser newSpaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserEditRequest, newSpaceUser);
        //校验参数合法性
        spaceUserService.validateSpaceUserParams(newSpaceUser, false);
        //更新信息
        boolean success = spaceUserService.updateById(newSpaceUser);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据查询条件查询所有空间成员信息
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUserVO(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest){
        // 校验基本参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取查询包装类
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        // 查询
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);
        // 脱敏
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 查询某个空间成员信息
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 校验基本参数
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验参数合法性
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 获取查询包装类
        QueryWrapper<SpaceUser> spaceUserQueryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserQueryWrapper);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询我加入的某个或多个团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }
}
