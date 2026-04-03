package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * B端登录标识查询测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class SysAuthServiceImplTest {

    @Test
    public void shouldResolveLoginIdentityByPhone() throws Exception {
        SysAuthServiceImpl service = new SysAuthServiceImpl();
        SysUser user = buildUser(1L, "service_user", "13800138000");
        setField(service, "sysUserMapper", createMapperProxy(Collections.singletonList(user)));

        Method method = SysAuthServiceImpl.class.getDeclaredMethod("findByLoginIdentity", String.class);
        method.setAccessible(true);
        SysUser result = (SysUser) method.invoke(service, "13800138000");

        Assert.assertNotNull(result);
        Assert.assertEquals(Long.valueOf(1L), result.getId());
        Assert.assertEquals("service_user", result.getUsername());
    }

    @Test
    public void shouldRejectConflictingLoginIdentity() throws Exception {
        SysAuthServiceImpl service = new SysAuthServiceImpl();
        List<SysUser> users = Arrays.asList(
                buildUser(1L, "13800138000", "18800000000"),
                buildUser(2L, "service_user", "13800138000")
        );
        setField(service, "sysUserMapper", createMapperProxy(users));

        Method method = SysAuthServiceImpl.class.getDeclaredMethod("findByLoginIdentity", String.class);
        method.setAccessible(true);
        try {
            method.invoke(service, "13800138000");
            Assert.fail("预期应拒绝冲突登录标识");
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            Assert.assertTrue(cause instanceof ServiceException);
            Assert.assertEquals("登录标识存在冲突，请联系管理员处理", cause.getMessage());
        }
    }

    private SysUser buildUser(Long id, String username, String phone) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPhone(phone);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    private SysUserMapper createMapperProxy(List<SysUser> users) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return users;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SysAuthServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType) || short.class.equals(returnType)
                || int.class.equals(returnType) || long.class.equals(returnType)) {
            return 0;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        return null;
    }
}
