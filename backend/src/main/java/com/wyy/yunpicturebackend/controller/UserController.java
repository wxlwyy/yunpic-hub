package com.wyy.yunpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicturebackend.annotation.AuthCheck;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.DeleteRequest;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.model.dto.user.*;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.LoginUserVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController  // @Controller + @ResponseBody
@RequestMapping("/user")
@Validated
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest 注册的信息
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@Validated @RequestBody UserRegisterRequest userRegisterRequest){
//        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        Long userId = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(userId);
    }

    /**
     *
     * @param userLoginRequest 用户登录时传的参数
     * @param request 请求对象
     * @return 脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@Validated @RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
//        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        LoginUserVO loginUserVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录的用户
     * @param request 请求对象
     * @return 用户对象
     */
    @GetMapping("/get/login")  // 方法名叫getLoginUserVO更好
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request){
        User user = userService.getCurrentUser(request);
        LoginUserVO loginUserVO = userService.convertToLoginUserVO(user);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户退出登录
     * @param request 请求对象
     * @return 是否成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        Boolean res = userService.userLogout(request);
        return ResultUtils.success(res);
    }

    /**
     * 创建用户（仅管理员）
     * @param addUserRequest 封装的创建用户的信息
     * @return 用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@Validated @RequestBody AddUserRequest addUserRequest){
        Long userId = userService.addUser(addUserRequest);
        return ResultUtils.success(userId)
    }

    /**
     * 删除用户（仅管理员）
     * @param deleteRequest 封装的删除用户的信息
     * @return 是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@Validated @RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest request){
        // 获取当前操作的管理员（为了防止他删掉自己信息）
        User currentUser = userService.getCurrentUser(request);
        userService.deleteUser(deleteRequest, currentUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户（仅管理员）
     * @param updateUserRequest 封装好的修改用户的信息
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@Validated @RequestBody UpdateUserRequest updateUserRequest){
        boolean result = userService.updateUser(updateUserRequest);
        return ResultUtils.success(result);
    }

    /**
     * 根据id查询用户数据（仅管理员）
     * @param id 用户id
     * @return 用户数据
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@NotNull(message = "用户ID不能为空")
                                              @Min(value = 1, message = "用户ID不合法")
                                              Long id){
        User user = userService.getUserById(id);
        return ResultUtils.success(user);
    }

    /**
     * 根据id查询单个用户脱敏数据
     * @param id 用户id
     * @return 用户脱敏数据
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@NotNull(message = "用户ID不能为空")
                                                  @Min(value = 1, message = "用户ID不合法")
                                                  Long id){
        User user = userService.getUserById(id);
        UserVO userVO = userService.convertToUserVO(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 分页查询用户列表脱敏数据 （仅管理员）
     * @param queryUserRequest 封装的分页查询用户的数据
     * @return 用户列表脱敏数据
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  // pageUserVO更好
    public BaseResponse<Page<UserVO>> getListUserVOByPage(@Validated @RequestBody QueryUserRequest queryUserRequest){
        Page<UserVO> userVOPage = userService.pageUserVO(queryUserRequest);
        return ResultUtils.success(userVOPage);
    }
}
