package com.wyy.yunpicturebackend.model.dto.user;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户个人更新信息请求
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    private static final long serialVersionUID = -1709674550556668954L;

    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 20, message = "昵称长度不能超过 20 个字符")
    private String userName;

    private String userAvatar;

    @Size(max = 100, message = "简介不能超过 100 个字符")
    private String userProfile;
}