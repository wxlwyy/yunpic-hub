package com.wyy.yunpicturebackend.common;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 4643166198339644076L;

    /**
     * 当前页号，当使用 @RequestBody 接收参数时，前端传递的值会自动覆盖 Java 对象的默认值
     */
    @Min(value = 1, message = "页码最小为 1")
    private int current = 1;

    /**
     * 页面信息条数
     */
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
