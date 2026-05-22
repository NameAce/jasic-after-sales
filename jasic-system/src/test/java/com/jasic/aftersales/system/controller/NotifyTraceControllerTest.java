package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/*** 通知记录排障控制器权限测试。
 *
 * <p>Phase 7 只验证接口权限边界是否仍挂在控制器入口，不启动 Web 容器。</p>

@author Zoro*/
public class NotifyTraceControllerTest {

    /**
     * 列表和详情接口应分别校验查询和查看权限。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldDeclareListAndViewPermissions() throws Exception {
        assertPermission("page", "system:notifyTrace:list", com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery.class);
        assertPermission("eventDetail", "system:notifyTrace:view", Long.class);
        assertPermission("dispatchDetail", "system:notifyTrace:view", Long.class);
    }

    /**
     * 人工重试和死信接口应分别校验操作权限。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldDeclareRetryAndDeadPermissions() throws Exception {
        assertPermission("retryEvent", "system:notifyTrace:retry", Long.class);
        assertPermission("retryDispatch", "system:notifyTrace:retry", Long.class);
        assertPermission("deadEvent", "system:notifyTrace:dead", Long.class,
                com.jasic.aftersales.system.notify.domain.dto.NotifyManualDeadDTO.class);
        assertPermission("deadDispatch", "system:notifyTrace:dead", Long.class,
                com.jasic.aftersales.system.notify.domain.dto.NotifyManualDeadDTO.class);
    }

    /**
     * 断言方法声明了指定权限。
     *
     * @param methodName 方法名
     * @param expectedPermission 期望权限编码
     * @param parameterTypes 方法参数类型
     * @throws Exception 反射异常
     */
    private void assertPermission(String methodName, String expectedPermission, Class<?>... parameterTypes) throws Exception {
        Method method = NotifyTraceController.class.getDeclaredMethod(methodName, parameterTypes);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        Assert.assertNotNull(permission);
        Assert.assertArrayEquals(new String[]{expectedPermission}, permission.value());
    }
}
