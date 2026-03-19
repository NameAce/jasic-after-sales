package com.jasic.aftersales.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解，标记在 Service 或 Mapper 方法上，
 * 由 DataScopeAspect 切面拦截并注入数据过滤条件
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 公司表的别名
     *
     * @return 别名
     */
    String companyAlias() default "";

    /**
     * 用户表的别名（用于 SELF 范围过滤）
     *
     * @return 别名
     */
    String userAlias() default "";
}
