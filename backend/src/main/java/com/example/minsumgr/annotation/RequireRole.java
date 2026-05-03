package com.example.minsumgr.annotation;

import java.lang.annotation.*;

/**
 * 角色权限检查注解
 * 用于标记需要特定角色的API端点
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 允许的角色列表
     */
    String[] roles() default {"ADMIN", "CUSTOMER"};
}
