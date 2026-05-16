package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * 通知场景配置控制器权限测试。
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifySceneControllerTest {

    @Test
    public void shouldDeclareQueryPermissions() throws Exception {
        assertPermission("options", "system:notifyScene:view");
        assertPermission("list", "system:notifyScene:list",
                com.jasic.aftersales.system.notify.domain.query.NotifySceneConfigQuery.class);
        assertPermission("getDetail", "system:notifyScene:view", String.class);
    }

    @Test
    public void shouldDeclareOperatePermissions() throws Exception {
        assertPermission("save", "system:notifyScene:update", String.class,
                com.jasic.aftersales.system.notify.domain.dto.NotifySceneConfigSaveDTO.class);
        assertPermission("preview", "system:notifyScene:preview",
                com.jasic.aftersales.system.notify.domain.dto.NotifyScenePreviewDTO.class);
    }

    private void assertPermission(String methodName, String expectedPermission, Class<?>... parameterTypes) throws Exception {
        Method method = NotifySceneController.class.getDeclaredMethod(methodName, parameterTypes);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        Assert.assertNotNull(permission);
        Assert.assertArrayEquals(new String[]{expectedPermission}, permission.value());
    }
}
