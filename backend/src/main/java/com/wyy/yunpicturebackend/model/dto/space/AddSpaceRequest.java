package com.wyy.yunpicturebackend.model.dto.space;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AddSpaceRequest implements Serializable {

    private static final long serialVersionUID = 8620082025700401961L;

    @Length(max = 30, message = "空间名称不能超过 30 个字符")
    private String spaceName; // 允许为空，后端会给默认值

    @Min(value = 0, message = "空间级别错误")
    @Max(value = 2, message = "空间级别错误")
    private Integer spaceLevel; // 允许为空，后端会给默认值

    // 重点：必须传类型！0-个人 1-团队
    @NotNull(message = "空间类型不能为空")
    @Min(value = 0, message = "空间类型错误")
    @Max(value = 1, message = "空间类型错误")
    private Integer spaceType;
}
