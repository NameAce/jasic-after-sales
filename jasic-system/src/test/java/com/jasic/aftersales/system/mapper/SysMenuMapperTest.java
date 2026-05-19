package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * 菜单 Mapper 注解测试。
 *
 * <p>这几条“按用户+公司加载权限/菜单”的 SQL 自身已经显式带有 companyId 条件，
 * 因此必须关闭租户拦截，避免异步线程或非登录上下文下再次回退读取 Sa-Token 请求态。</p>
 *
 * @author Codex
 * @date 2026/05/19
 */
public class SysMenuMapperTest {

    /**
     * 权限与菜单查询方法都应关闭租户拦截，防止通知异步消费链路再次触发非 Web 上下文异常。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldIgnoreTenantLineForCompanyScopedPermissionQueries() throws Exception {
        assertTenantLineIgnored("selectPermsByUserIdAndCompanyId");
        assertTenantLineIgnored("selectPermissionMenusByUserIdAndCompanyId");
        assertTenantLineIgnored("selectMenuTreeByUserIdAndCompanyId");
    }

    /**
     * 断言指定方法已声明关闭租户拦截。
     *
     * @param methodName 方法名
     * @throws Exception 反射异常
     */
    private void assertTenantLineIgnored(String methodName) throws Exception {
        Method method = SysMenuMapper.class.getMethod(methodName, Long.class, Long.class);
        InterceptorIgnore annotation = method.getAnnotation(InterceptorIgnore.class);
        Assert.assertNotNull("方法缺少 InterceptorIgnore 注解: " + methodName, annotation);
        Assert.assertEquals("true", annotation.tenantLine());
    }
}
