package com.jasic.aftersales.common.annotation;

import com.jasic.aftersales.common.enums.OperTypeEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，标记在 Controller 方法上，
 * 由 OperLogAspect 切面拦截并自动记录操作日志
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 操作模块
     *
     * @return 模块名称
     */
    String title() default "";

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    OperTypeEnum operType() default OperTypeEnum.OTHER;

    /**
     * 是否保存请求参数
     *
     * @return true=保存
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应数据
     *
     * @return true=保存
     */
    boolean isSaveResponseData() default true;
}
