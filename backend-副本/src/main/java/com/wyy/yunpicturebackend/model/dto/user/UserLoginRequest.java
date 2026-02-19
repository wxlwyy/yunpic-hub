package com.wyy.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 *
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = -3675891985267367776L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;
}
