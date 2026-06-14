package com.wyy.yunpicturebackend.common;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    @NotNull(message = "ID 不能为空")
    private Long id;

    private static final long serialVersionUID = 1L;
}
