package com.wyy.yunpicturebackend.common;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    @NotNull(message = "删除请求的 ID 不能为空")
    @Min(value = 1, message = "非法的请求参数") // 防负数、防 0（value=1表示最小值为1），防止空耗数据库性能
    private Long id;

    private static final long serialVersionUID = 1L;
}
