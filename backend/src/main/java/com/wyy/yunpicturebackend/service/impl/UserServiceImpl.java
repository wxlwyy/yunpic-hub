package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.common.BaseResponse;
import com.wyy.yunpicturebackend.common.DeleteRequest;
import com.wyy.yunpicturebackend.common.ResultUtils;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.auth.StpKit;
import com.wyy.yunpicturebackend.model.dto.user.*;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.UserRoleEnum;
import com.wyy.yunpicturebackend.model.vo.LoginUserVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.UserService;
import com.wyy.yunpicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author wxl
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2025-07-13 15:23:17
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * 用户注册
     * @param userRegisterRequest 注册的信息
     * @return 用户id
     */
    @Override
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
//        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "请求参数为空");
//        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号不能小于4位");
//        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        // 参数校验
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");
        // 业务校验，查看账号是否重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        // Long count = baseMapper.selectCount(userQueryWrapper);
        long count = count(userQueryWrapper);  // 一般统一用封装好的sql语句方法
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");

        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName("user");
        String encryptPassword = encryptPassword(userPassword);  //加密
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean success = save(user);
        // 业务校验
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        return user.getId();
    }

    /**
     * 对密码进行加密
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String encryptPassword(String userPassword) {
        final String SALT = "wyy";
        //使用spring框架的工具类加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        return encryptPassword;
    }

    /**
     *
     * @param userLoginRequest 用户登录时传的参数
     * @param request 请求对象
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
//        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空");
//        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号不能小于4位");
//        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        // 业务校验
        String encryptPassword = encryptPassword(userPassword);
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        userQueryWrapper.eq("userPassword", encryptPassword);
        // User user = baseMapper.selectOne(userQueryWrapper);
        User user = getOne(userQueryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        //将查出的信息存到session中
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 将用户id和用户信息分别存到sa-token中，
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        //返回脱敏信息
        return convertToLoginUserVO(user);
    }

    /**
     * 通过cookie中的sessionId从session中获取当前登录的用户信息
     * @param request 请求对象
     * @return 用户对象
     */
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        //业务校验，判断session中是否有用户信息
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.NOT_LOGIN_ERROR);
        //权限校验，在数据库中查用户信息，并且保持用户信息的最新状态
        User currentUser = (User) userObj;
        Long userId = currentUser.getId();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("id", userId);
        // currentUser = baseMapper.selectOne(userQueryWrapper);
        currentUser = getOne(userQueryWrapper);
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return currentUser;
    }

    /**
     * 用户退出登录
     * @param request 请求对象
     * @return 是否成功
     */
    @Override
    public Boolean userLogout(HttpServletRequest request) {
        //业务校验，判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.NOT_LOGIN_ERROR);
        //如果登录，移除登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 对登录的用户信息进行脱敏（仅仅是一个 “数据转换器”（从 Entity 转换到 VO 的单一职责边界）。它的职责就是“翻译数据”，
     * 而不是“做业务校验”。“输入是空，输出自然是空”，这在逻辑上是完美的。VO 转换）
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    public LoginUserVO convertToLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 对用户信息（脱敏）
     * @param user 用户信息
     * @return 脱敏的用户信息
     */
    @Override
    public UserVO convertToUserVO(User user) {
        if (user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 对用户列表进行脱敏
     * @param users 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> convertToUserVOList(List<User> users) {
        if (CollUtil.isEmpty(users)){
            return new ArrayList<>();
        }
        List<UserVO> userVOList = users.stream().map(user -> convertToUserVO(user)).collect(Collectors.toList());
        return userVOList;
    }

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public Long addUser(AddUserRequest addUserRequest) {
        // 业务校验，查看账号是否重复
        String userAccount = addUserRequest.getUserAccount();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        long count = count(userQueryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");

        User user = new User();
        BeanUtil.copyProperties(addUserRequest, user);
        //设置默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = encryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean success = save(user);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public void deleteUser(DeleteRequest deleteRequest, User currentUser) {
        // 1. 业务校验
        Long userId = deleteRequest.getId();
        ThrowUtils.throwIf(userId.equals(currentUser.getId()), ErrorCode.PARAMS_ERROR, "管理员不能删除自己");

        // 2. 业务校验：防空删
        User oldUser = getById(userId);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR, "要删除的用户不存在");

        // 3. 执行核心动作
        boolean success = removeById(userId);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "删除失败，数据库异常");
    }

    @Override
    public Boolean updateUser(UpdateUserRequest updateUserRequest) {
        Long id = updateUserRequest.getId();

        // 参数校验：防非法角色枚举
        // 如果前端传了角色，必须确保这个角色是系统里规定的（user 或 admin）
        String userRole = updateUserRequest.getUserRole();
        if (StrUtil.isNotBlank(userRole)) {
            // 假设你们有一个 UserRoleEnum 枚举类
            UserRoleEnum roleEnum = UserRoleEnum.getUserRoleEnumByValue(userRole);
            ThrowUtils.throwIf(roleEnum == null, ErrorCode.PARAMS_ERROR, "非法的用户角色");
        }

        // 业务校验：防空更新（必须先确认库里有这个人）
        User oldUser = getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 3. 实体转换与更新
        User user = new User();
        BeanUtil.copyProperties(updateUserRequest, user);

        // 4. 操作数据库并断言结果
        boolean success = updateById(user);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "更新失败，数据库异常");

        return true;
    }

    @Override
    public User getUserById(Long id) {
//        ThrowUtils.throwIf(null == id || id < 0, ErrorCode.PARAMS_ERROR);
        User user = getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    /**
     * 构造查询条件
     * @param queryUserRequest 查询的数据
     * @return 查询条件对象
     */
    @Override
    public QueryWrapper<User> buildQueryWrapper(QueryUserRequest queryUserRequest) {
        ThrowUtils.throwIf(queryUserRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

        String userName = queryUserRequest.getUserName();
        String userProfile = queryUserRequest.getUserProfile();
        String userRole = queryUserRequest.getUserRole();
        String userAccount = queryUserRequest.getUserAccount();
        Long id = queryUserRequest.getId();
        String sortField = queryUserRequest.getSortField();
        String sortOrder = queryUserRequest.getSortOrder();

        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        userQueryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        userQueryWrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        userQueryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        userQueryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        userQueryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        return userQueryWrapper;
    }

    @Override
    public Page<UserVO> pageUserVO(QueryUserRequest queryUserRequest) {
        long current = queryUserRequest.getCurrent();
        long pageSize = queryUserRequest.getPageSize();

        // 查询数据库
        QueryWrapper<User> userQueryWrapper = buildQueryWrapper(queryUserRequest);
        Page<User> userPage = page(new Page<>(current, pageSize), userQueryWrapper);
        // 拿到原始数据列表
        List<User> userList = userPage.getRecords();
        // 判断一下，如果查出来数据为空，直接返回空的分页对象，没必要去走脱敏逻辑了
        if (CollUtil.isEmpty(userList)) {
            return new Page<>(current, pageSize);
        }
        // 构建新的 VO 分页对象，脱敏并塞回分页对象
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = convertToUserVOList(userList);
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }
}




