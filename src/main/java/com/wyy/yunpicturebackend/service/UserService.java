package com.wyy.yunpicturebackend.service;

import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.dto.user.QueryUserRequest;
import com.wyy.yunpicturebackend.model.dto.user.UserLoginRequest;
import com.wyy.yunpicturebackend.model.dto.user.UserRegisterRequest;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.LoginUserVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author wxl
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-07-13 15:23:17
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest 注册的信息
     * @return 用户id
     */
    Long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 对密码进行加密
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * @param userLoginRequest 用户登录时传的参数
     * @param request 请求对象
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 对登录后的用户信息脱敏
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录的用户信息
     * @param request 请求对象
     * @return 用户对象
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户退出登录
     * @param request 请求对象
     * @return 是否成功
     */
    Boolean userLogout(HttpServletRequest request);

    /**
     * 对用户信息进行脱敏
     * @param user 用户信息
     * @return 脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 对用户列表进行脱敏
     * @param users 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> users);

    /**
     * 将查询的数据封装为查询条件对象
     * @param queryUserRequest 查询的数据
     * @return 查询条件对象
     */
    QueryWrapper<User> getQueryWrapper(QueryUserRequest queryUserRequest);
}
