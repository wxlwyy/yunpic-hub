package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
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
import com.wyy.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.wyy.yunpicturebackend.model.dto.space.*;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.wyy.yunpicturebackend.model.vo.SpaceVO;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

@RestController
@RequestMapping("/space")
@Validated
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody AddSpaceRequest addSpaceRequest, HttpServletRequest request){
        //校验参数
        ThrowUtils.throwIf(ObjUtil.isNull(addSpaceRequest), ErrorCode.PARAMS_ERROR);
        User currentUser = userService.getCurrentUser(request);
        long newSpaceId = spaceService.addSpace(addSpaceRequest, currentUser);
        return ResultUtils.success(newSpaceId);
    }

    /**
     * 删除空间
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@Validated @RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        spaceService.deleteSpace(deleteRequest.getId(), currentUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（管理员），注意只会更改非null的字段
     * @param updateSpaceRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSpace(@Validated @RequestBody UpdateSpaceRequest updateSpaceRequest){
        // 校验
//        if (updateSpaceRequest == null || updateSpaceRequest.getId() < 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
        boolean success = spaceService.updateSpaceInfo(updateSpaceRequest);
        return ResultUtils.success(success);
    }

    /**
     * 编辑空间，注意只会更改非null的字段
     * @param editSpaceRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@Validated @RequestBody EditSpaceRequest editSpaceRequest,
                                             HttpServletRequest request){
        User currentUser = userService.getCurrentUser(request);
        boolean success = spaceService.editSpace(editSpaceRequest, currentUser);
        return ResultUtils.success(success);
    }

    /**
     * 空间管理页面，分页获取空间列表（管理员）
     * @param querySpaceRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody QuerySpaceRequest querySpaceRequest){
        //获取当前页，每页条数，查询条件
        int current = querySpaceRequest.getCurrent();
        int pageSize = querySpaceRequest.getPageSize();
        QueryWrapper<Space> queryWrapper = spaceService.buildQueryWrapper(querySpaceRequest);
        //查询
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize), queryWrapper);
        return ResultUtils.success(spacePage);
    }

    /**
     * 空间列表页面，分页获取空间列表
     * @param querySpaceRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody QuerySpaceRequest querySpaceRequest){
        //当前页，每页条数，查询条件
        int current = querySpaceRequest.getCurrent();
        int pageSize = querySpaceRequest.getPageSize();

        QueryWrapper<Space> queryWrapper = spaceService.buildQueryWrapper(querySpaceRequest);
        Page<Space> spacePage = spaceService.page(new Page<>(current, pageSize), queryWrapper);
        //防爬
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //脱敏
        Page<SpaceVO> spaceVOPage = spaceService.convertToSpaceVOPage(spacePage);
        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 空间列表页面，分页获取空间列表
     * @param querySpaceRequest
     * @return
     */

    /**
     * 根据id查询空间信息（管理员）
     * @param id
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get")
    public BaseResponse<Space> getSpaceById(Long id){
        //校验参数
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //查询
        Space space = spaceService.getById(id);
        //是否存在
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 根据id查询空间信息
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request){
        //校验参数
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        //查询
        Space space = spaceService.getById(id);
        //是否存在
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //封装
        SpaceVO spaceVO = spaceService.convertToSpaceVO(space, request);
        // 获取空间详情时返回给前端权限列表，方便展示是否显示相关按钮
        User currentUser = userService.getCurrentUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, currentUser);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 给前端展示所有空间的级别信息
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}
