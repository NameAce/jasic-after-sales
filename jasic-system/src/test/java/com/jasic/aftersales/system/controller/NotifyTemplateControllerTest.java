package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * 通知模板配置控制器权限测试。
 *
 * <p>阶段二只验证控制器入口已经切换到新模板配置语义，
 * 且旧 `/custom`、删除和刷新缓存权限不会继续被新的控制器方法声明复用。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
public class NotifyTemplateControllerTest {

    /**
     * 查询类接口应声明查询和查看权限。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldDeclareQueryPermissions() throws Exception {
        assertPermission("options", "system:notifyTemplate:view");
        assertPermission("list", "system:notifyTemplate:list",
                com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery.class);
        assertPermission("getById", "system:notifyTemplate:view", Long.class);
        assertPermission("listChannels", "system:notifyTemplate:view", String.class);
    }

    /**
     * 维护类接口应声明新增、修改、停用和预览权限。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldDeclareOperatePermissions() throws Exception {
        assertPermission("create", "system:notifyTemplate:add",
                com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO.class);
        assertPermission("update", "system:notifyTemplate:update",
                com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO.class);
        assertPermission("updateStatus", "system:notifyTemplate:remove", Long.class,
                com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateStatusDTO.class);
        assertPermission("preview", "system:notifyTemplate:preview",
                com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO.class);
        assertPermission("saveChannels", "system:notifyTemplate:update", String.class, java.util.List.class);
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
        Method method = NotifyTemplateController.class.getDeclaredMethod(methodName, parameterTypes);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        Assert.assertNotNull(permission);
        Assert.assertArrayEquals(new String[]{expectedPermission}, permission.value());
    }
}
