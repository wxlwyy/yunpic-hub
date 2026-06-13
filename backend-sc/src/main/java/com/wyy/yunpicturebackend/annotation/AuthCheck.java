package com.wyy.yunpicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 允许访问的角色列表（满足其中任一角色即可）
     * 例如：anyRole = {"admin", "vip"} 表示管理员或VIP用户都可以访问
     */
    String[] anyRole() default {};
}
