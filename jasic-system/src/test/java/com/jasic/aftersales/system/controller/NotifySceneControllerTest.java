package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * 通知场景配置控制器权限测试。
 *
 * @author Zoro
 * @date 2026/05/16
 */
public class NotifySceneControllerTest {

    /**shouldDeclareQueryPermissions 接口入口，负责接收请求参数并委托服务层完成业务处理。*/
    @Test
    public void shouldDeclareQueryPermissions() throws Exception {
        assertPermission("options", "system:notifyScene:view");
        assertPermission("list", "system:notifyScene:list",
                com.jasic.aftersales.system.notify.domain.query.NotifySceneConfigQuery.class);
        assertPermission("getDetail", "system:notifyScene:view", String.class);
    }

    /**shouldDeclareOperatePermissions 接口入口，负责接收请求参数并委托服务层完成业务处理。*/
    @Test
    public void shouldDeclareOperatePermissions() throws Exception {
        assertPermission("save", "system:notifyScene:update", String.class,
                com.jasic.aftersales.system.notify.domain.dto.NotifySceneConfigSaveDTO.class);
        assertPermission("preview", "system:notifyScene:preview",
                com.jasic.aftersales.system.notify.domain.dto.NotifyScenePreviewDTO.class);
    }

    /**assertPermission 接口入口，负责接收请求参数并委托服务层完成业务处理。
@param methodName 名称文本，用于展示、匹配或保存业务对象名称。
@param expectedPermission expectedPermission 字段参数。
@param parameterTypes parameterTypes 字段参数。*/
    private void assertPermission(String methodName, String expectedPermission, Class<?>... parameterTypes) throws Exception {
        Method method = NotifySceneController.class.getDeclaredMethod(methodName, parameterTypes);
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        Assert.assertNotNull(permission);
        Assert.assertArrayEquals(new String[]{expectedPermission}, permission.value());
    }
}
