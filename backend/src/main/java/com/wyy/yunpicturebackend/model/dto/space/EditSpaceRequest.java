package com.wyy.yunpicturebackend.model.dto.space;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class EditSpaceRequest implements Serializable {

    private static final long serialVersionUID = 8747389502137040537L;

    /**
     * 空间 id
     */
    @NotNull(message = "空间 id 不能为空")
    @Min(value = 1, message = "空间 id 不合法")
    private Long id;

    /**
     * 空间名称
     */
    @Length(max = 30, message = "空间名称不能超过 30 个字符")
    private String spaceName;
}
