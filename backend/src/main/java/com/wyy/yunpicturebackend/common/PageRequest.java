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
    // 必须是正数，最小是第 1 页
    @Min(value = 1, message = "当前页码不合法")
    private int current = 1;

    /**
     * 页面信息条数
     */
    // 必须大于0，且为了防止内存溢出，单页最多不允许超过 100 条！
    @Min(value = 1, message = "每页条数不合法")
    @Max(value = 100, message = "单页请求条数过大")
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
