package com.wyy.yunpicturebackend.model.dto.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 更新用户请求的数据（管理员）
 */
@Data
public class UpdateUserRequest implements Serializable {

    private static final long serialVersionUID = 5820544181916611315L;

    /**
     * 用户id（先通过用户列表返回用户信息，再根据id进行更新）
     */
    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "非法的请求参数") // 防负数、防 0，防止空耗数据库性能
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}