package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

/**
 * B端登录标识校验测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class SysUserIdentityValidatorTest {

    @Test
    public void shouldAllowSameUserUsernameEqualsPhone() throws Exception {
        SysUserIdentityValidator validator = new SysUserIdentityValidator();
        setField(validator, "sysUserMapper", createMapperProxy(0L, 0L, 0L));

        validator.validateLoginIdentityUnique(1L, "13800138000", "13800138000");
    }

    @Test
    public void shouldRejectWhenUsernameOccupiedByOtherPhone() throws Exception {
        SysUserIdentityValidator validator = new SysUserIdentityValidator();
        setField(validator, "sysUserMapper", createMapperProxy(0L, 0L, 1L));

        try {
            validator.validateLoginIdentityUnique(null, "jasic-admin", "13800138000");
            Assert.fail("预期应拒绝用户名与他人手机号冲突");
        } catch (ServiceException ex) {
            Assert.assertEquals("用户名（jasic-admin）已被其他用户手机号占用，请调整", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectWhenPhoneAlreadyExists() throws Exception {
        SysUserIdentityValidator validator = new SysUserIdentityValidator();
        setField(validator, "sysUserMapper", createMapperProxy(0L, 1L));

        try {
            validator.validateLoginIdentityUnique(null, "service_user", "13800138000");
            Assert.fail("预期应拒绝重复手机号");
        } catch (ServiceException ex) {
            Assert.assertEquals("手机号已存在", ex.getMessage());
        }
    }

    private SysUserMapper createMapperProxy(Long... counts) {
        Queue<Long> queue = new ArrayDeque<>(Arrays.asList(counts));
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return queue.isEmpty() ? 0L : queue.poll();
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
        Field field = SysUserIdentityValidator.class.getDeclaredField(fieldName);
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
