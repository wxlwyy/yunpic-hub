package com.wyy.yunpicture.interfaces.dto.user;

import com.wyy.yunpicture.infrastructure.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询用户请求的信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryUserRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1165482241016565135L;

    /**
     * 用户id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}