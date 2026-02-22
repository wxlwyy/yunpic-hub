package com.wyy.yunpicturebackend.model.dto.user;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -6450567479154080259L;

    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    @Length(min = 4, message = "账号不能小于4位")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    @Length(min = 8, message = "密码不能小于8位")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;
}
