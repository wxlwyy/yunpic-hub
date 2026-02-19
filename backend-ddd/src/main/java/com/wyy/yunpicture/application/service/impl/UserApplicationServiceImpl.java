package com.wyy.yunpicture.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicture.application.service.UserApplicationService;
import com.wyy.yunpicture.domain.service.UserDomainService;
import com.wyy.yunpicture.infrastructure.common.DeleteRequest;
import com.wyy.yunpicture.infrastructure.exception.BusinessException;
import com.wyy.yunpicture.infrastructure.exception.ErrorCode;
import com.wyy.yunpicture.infrastructure.exception.ThrowUtils;
import com.wyy.yunpicture.interfaces.dto.user.QueryUserRequest;
import com.wyy.yunpicture.interfaces.dto.user.UserLoginRequest;
import com.wyy.yunpicture.interfaces.dto.user.UserRegisterRequest;
import com.wyy.yunpicture.domain.user.entity.User;
import com.wyy.yunpicture.interfaces.vo.user.LoginUserVO;
import com.wyy.yunpicture.interfaces.vo.user.UserVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author wxl
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-07-13 15:23:17
*/
@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    /**
     * 用户注册
     * @param userRegisterRequest 注册的信息
     * @return 用户id
     */
    @Override
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        //校验：账号，密码，确认密码
        User.validUserRegister(userAccount, userPassword, checkPassword);
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    /**
     * 对密码进行加密
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }

    /**
     *
     * @param userLoginRequest 用户登录时传的参数
     * @param request 请求对象
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        //校验参数
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        User.validUserLogin(userAccount, userPassword);
        return userDomainService.userLogin(userAccount, userPassword, request);
    }


    /**
     * 对用户信息进行脱敏
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    public LoginUserVO getLoginUserVO(User user) {
        return userDomainService.getLoginUserVO(user);
    }

    /**
     * 通过cookie中的sessionId从session中获取当前登录的用户信息
     * @param request 请求对象
     * @return 用户对象
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    /**
     * 用户退出登录
     * @param request 请求对象
     * @return 是否成功
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    /**
     * 查询用户信息（脱敏）
     * @param user 用户信息
     * @return 脱敏的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    /**
     * 对用户列表进行脱敏
     * @param users 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> users) {
        return userDomainService.getUserVOList(users);
    }

    /**
     * 将查询的数据封装为查询条件对象
     * @param queryUserRequest 查询的数据
     * @return 查询条件对象
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(QueryUserRequest queryUserRequest) {
       ThrowUtils.throwIf(queryUserRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

       return userDomainService.getQueryWrapper(queryUserRequest);
    }

    @Override
    public long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(QueryUserRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }
}




