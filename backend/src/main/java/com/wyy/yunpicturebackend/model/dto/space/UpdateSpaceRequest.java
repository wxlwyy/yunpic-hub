package com.wyy.yunpicturebackend.model.dto.space;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class UpdateSpaceRequest implements Serializable {

    private static final long serialVersionUID = 5247771030073020064L;

    /**
     * id (更新时必定要有，且必须大于0)
     */
    @NotNull(message = "空间 id 不能为空")
    @Min(value = 1, message = "空间 id 不合法")
    private Long id;

    /**
     * 空间名称 (更新时可以不传，但如果传了，长度绝不能超)
     */
    @Length(max = 30, message = "空间名称不能超过 30 个字符")
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    @Min(value = 0, message = "空间级别错误")
    @Max(value = 2, message = "空间级别错误")
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;
}
