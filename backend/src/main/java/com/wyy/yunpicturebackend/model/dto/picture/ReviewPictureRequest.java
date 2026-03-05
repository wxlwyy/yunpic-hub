package com.wyy.yunpicturebackend.model.dto.picture;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ReviewPictureRequest implements Serializable {

    private static final long serialVersionUID = -2664780119723564459L;

    /**
     * 图片id
     */
    @NotNull(message = "图片ID不能为空")
    @Min(value = 1, message = "图片ID不合法")
    private Long id;

    /**
     * 审核状态：0-待审核；1-通过；2-拒绝
     */
    @NotNull(message = "审核状态不能为空")
    @Min(value = 1, message = "审核状态不合法，只能是通过或拒绝")
    @Max(value = 2, message = "审核状态不合法，只能是通过或拒绝")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;
}
