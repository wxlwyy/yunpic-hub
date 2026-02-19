package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicture.infrastructure.annotation.AuthCheck;
import com.wyy.yunpicture.infrastructure.common.BaseResponse;
import com.wyy.yunpicture.infrastructure.common.DeleteRequest;
import com.wyy.yunpicture.infrastructure.common.ResultUtils;
import com.wyy.yunpicture.interfaces.dto.user.*;
import com.wyy.yunpicture.domain.user.constant.UserConstant;
import com.wyy.yunpicture.infrastructure.exception.ErrorCode;
import com.wyy.yunpicture.infrastructure.exception.ThrowUtils;
import com.wyy.yunpicturebackend.model.dto.user.*;
import com.wyy.yunpicture.domain.user.entity.User;
import com.wyy.yunpicture.interfaces.vo.user.LoginUserVO;
import com.wyy.yunpicture.interfaces.vo.user.UserVO;
import com.wyy.yunpicture.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 用户注册
     * @param userRegisterRequest 注册的信息
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        Long userId = userApplicationService.userRegister(userRegisterRequest);
        return ResultUtils.success(userId);
    }

    /**
     *
     * @param userLoginRequest 用户登录时传的参数
     * @param request 请求对象
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        LoginUserVO loginUserVO = userApplicationService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录的用户
     * @param request 请求对象
     * @return 用户对象
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        User user = userApplicationService.getLoginUser(request);
        LoginUserVO loginUserVO = userApplicationService.getLoginUserVO(user);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户退出登录
     * @param request 请求对象
     * @return 是否成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        Boolean res = userApplicationService.userLogout(request);
        return ResultUtils.success(res);
    }

    /**
     * 创建用户（仅管理员）
     * @param addUserRequest 封装的创建用户的信息
     * @return 用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody AddUserRequest addUserRequest){
        ThrowUtils.throwIf(addUserRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(addUserRequest, user);
        //设置默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userApplicationService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean success = userApplicationService.save(user);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户（仅管理员）
     * @param deleteRequest 封装的删除用户的信息
     * @return 是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean success = userApplicationService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!success, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户（仅管理员）
     * @param updateUserRequest 封装好的修改用户的信息
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UpdateUserRequest updateUserRequest){
        ThrowUtils.throwIf(updateUserRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(updateUserRequest, user);
        boolean success = userApplicationService.updateById(user);
        ThrowUtils.throwIf(!success, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id查询用户数据（仅管理员）
     * @param id 用户id
     * @return 用户数据
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id){
        ThrowUtils.throwIf(null == id || id < 0, ErrorCode.PARAMS_ERROR);
        User user = userApplicationService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id查询单个用户脱敏数据
     * @param id 用户id
     * @return 用户脱敏数据
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id){
        BaseResponse<User> userBaseResponse = getUserById(id);
        User user = userBaseResponse.getData();
        UserVO userVO = userApplicationService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 分页查询用户列表脱敏数据 （仅管理员）
     * @param queryUserRequest 封装的分页查询用户的数据
     * @return 用户列表脱敏数据
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> getListUserVOByPage(@RequestBody QueryUserRequest queryUserRequest){
        int current = queryUserRequest.getCurrent();
        int pageSize = queryUserRequest.getPageSize();
        Page<User> userPage = userApplicationService.page(new Page<>(current, pageSize),
                userApplicationService.getQueryWrapper(queryUserRequest));
        List<User> userList = userPage.getRecords();
        List<UserVO> userVOList = userApplicationService.getUserVOList(userList);
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
