package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.vo.SysPermissionVO;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
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

    @Test
    public void shouldBuildLightweightPermissionItems() throws Exception {
        SysAuthServiceImpl service = new SysAuthServiceImpl();
        SysMenu menu = new SysMenu();
        menu.setId(10L);
        menu.setMenuName("工单转派");
        menu.setParentId(5L);
        menu.setMenuType("F");
        menu.setPerms("workorder:transfer");
        setField(service, "sysMenuMapper", createMenuMapperProxy(Collections.singletonList(menu)));

        Method method = SysAuthServiceImpl.class.getDeclaredMethod("buildCurrentPermissionVos", Long.class, Long.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<SysPermissionVO> result = (List<SysPermissionVO>) method.invoke(service, 1L, 2L);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(10L), result.get(0).getId());
        Assert.assertEquals("工单转派", result.get(0).getMenuName());
        Assert.assertEquals(Long.valueOf(5L), result.get(0).getParentId());
        Assert.assertEquals("F", result.get(0).getMenuType());
        Assert.assertEquals("workorder:transfer", result.get(0).getPerms());
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

    private SysMenuMapper createMenuMapperProxy(List<SysMenu> menus) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectPermissionMenusByUserIdAndCompanyId".equals(method.getName())) {
                    return menus;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysMenuMapper) Proxy.newProxyInstance(
                SysMenuMapper.class.getClassLoader(),
                new Class<?>[]{SysMenuMapper.class},
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
