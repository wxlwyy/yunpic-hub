package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.constant.UserConstant;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.manager.auth.StpKit;
import com.wyy.yunpicturebackend.manager.upload.UploadFilePicture;
import com.wyy.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.wyy.yunpicturebackend.model.dto.user.*;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.UserRoleEnum;
import com.wyy.yunpicturebackend.model.vo.LoginUserVO;
import com.wyy.yunpicturebackend.model.vo.UserVO;
import com.wyy.yunpicturebackend.service.UserService;
import com.wyy.yunpicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
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

    @Resource
    private UploadFilePicture uploadFilePicture;

    /**
     * 用户注册
     * @param userRegisterRequest 注册的信息
     * @return 用户id
     */
    @Override
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        //校验：账号，密码，确认密码
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次密码不一致");
        //查看账号是否重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        Long count = baseMapper.selectCount(userQueryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");
        //加密
        String encryptPassword = getEncryptPassword(userPassword);
        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName("user");
        user.setUserPassword(encryptPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean success = save(user);
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        return user.getId();
    }

    /**
     * 对密码进行加密
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
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
        //校验参数
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        //加密，查询数据
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        userQueryWrapper.eq("userPassword", encryptPassword);
        User user = baseMapper.selectOne(userQueryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        //将查出的信息存到session中，
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 将用户id和用户信息分别存到sa-token中，
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        //返回脱敏信息
        return getLoginUserVO(user);

    }


    /**
     * 对用户信息进行脱敏
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    public LoginUserVO getLoginUserVO(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "参数为空");
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 通过cookie中的sessionId从session中获取当前登录的用户信息
     * @param request 请求对象
     * @return 用户对象
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断session中是否有用户信息
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.NOT_LOGIN_ERROR);
        //在数据库中查用户信息，防止用户登录后且session未过期但用户被删除的情况
        User currentUser = (User) userObj;
        Long userId = currentUser.getId();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("id", userId);
        currentUser = baseMapper.selectOne(userQueryWrapper);
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
        //判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj == null, ErrorCode.NOT_LOGIN_ERROR);
        //如果登录，移除登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 查询用户信息（脱敏）
     * @param user 用户信息
     * @return 脱敏的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
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
    public List<UserVO> getUserVOList(List<User> users) {
        if (CollUtil.isEmpty(users)){
            return new ArrayList<>();
        }
        List<UserVO> userVOS = users.stream().map(user -> getUserVO(user)).collect(Collectors.toList());
        return userVOS;
    }

    /**
     * 将查询的数据封装为查询条件对象
     * @param queryUserRequest 查询的数据
     * @return 查询条件对象
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(QueryUserRequest queryUserRequest) {
       ThrowUtils.throwIf(queryUserRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");

       String userName = queryUserRequest.getUserName();
       String userProfile = queryUserRequest.getUserProfile();
       String userRole = queryUserRequest.getUserRole();
       String userAccount = queryUserRequest.getUserAccount();
       Long id = queryUserRequest.getId();
       String sortField = queryUserRequest.getSortField();
       String sortOrder = queryUserRequest.getSortOrder();

       QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
       userQueryWrapper.eq(null != id && id > 0, "id", id);
       userQueryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
       userQueryWrapper.eq(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
       userQueryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
       userQueryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
       userQueryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
       return userQueryWrapper;
    }

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 用户修改自己的信息
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @Override
    public boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户（
        User loginUser = getLoginUser(request);

        // 2. 创建一个更新对象，只拷贝允许修改的字段
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);

        // 3. 关键：强制设置 ID，确保只改自己
        user.setId(loginUser.getId());

        // 4. 执行更新
        return updateById(user);
    }

    /**
     * 上传用户头像
     * @param multipartFile
     * @param request
     * @return
     */
    @Override
    public String uploadUserAvatar(MultipartFile multipartFile, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = this.getLoginUser(request);

        // 2. 准备上传前缀（路径）：avatar/用户ID
        // 这样同一个用户的头像永远会覆盖之前的，节省 COS 空间
        String uploadPathPrefix = String.format("avatar/%s", loginUser.getId());

        // 3. 直接调用现成的模版方法！
        // 它会自动帮我们：校验格式大小、转 WebP、压缩、上传到 COS
        UploadPictureResult uploadPictureResult = uploadFilePicture.uploadPicture(multipartFile, uploadPathPrefix);

        // 4. 只把上传成功后的 URL 返回给前端即可
        return uploadPictureResult.getUrl();
    }

    /**
     * 兑换VIP功能
     * @param userExchangeVipRequest
     * @param request
     * @return
     */
    @Override
    public boolean exchangeVip(UserExchangeVipRequest userExchangeVipRequest, HttpServletRequest request) {
        // 1. 基本校验
        String vipCode = userExchangeVipRequest.getVipCode();
        User loginUser = this.getLoginUser(request);

        // 2. 设置一个兑换码（实际开发中这里应该查数据库的兑换码表）
        final String SECRET_VIP_CODE = "tuku666";

        // 3. 匹配兑换码
        if (!SECRET_VIP_CODE.equals(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码错误或已失效");
        }

        // 4. 检查是否已经是 VIP，防止浪费
        if (UserRoleEnum.VIP.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已经是尊贵的 VIP 用户，无需重复兑换");
        }

        // 5. 修改用户角色
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserRole(UserRoleEnum.VIP.getValue());

        // 6. 更新数据库
        boolean result = this.updateById(user);

        return result;
    }
}




