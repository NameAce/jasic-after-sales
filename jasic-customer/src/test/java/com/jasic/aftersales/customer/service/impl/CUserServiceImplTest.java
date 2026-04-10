package com.jasic.aftersales.customer.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * C端客户登录服务测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class CUserServiceImplTest {

    @Test
    public void shouldMergeRealWechatIdentityByPhone() throws Exception {
        CUserServiceImpl service = new CUserServiceImpl();
        List<CUser> store = new ArrayList<>();
        store.add(buildUser(1L, "SYS_WO_FAKE", "13800138000", 1));
        setField(service, "cUserMapper", createUserMapperProxy(store));

        CUser user = service.loginOrRegister("real-openid", "13800138000");

        Assert.assertEquals(Long.valueOf(1L), user.getId());
        Assert.assertEquals("real-openid", user.getOpenid());
        Assert.assertEquals("13800138000", user.getPhone());
        Assert.assertNotNull(user.getLastLoginTime());
    }

    @Test
    public void shouldRejectFirstLoginWithoutPhone() throws Exception {
        CUserServiceImpl service = new CUserServiceImpl();
        setField(service, "cUserMapper", createUserMapperProxy(new ArrayList<>()));

        try {
            service.loginOrRegister("real-openid", null);
            Assert.fail("预期应拒绝首次未授权手机号登录");
        } catch (ServiceException ex) {
            Assert.assertEquals("首次登录请先授权手机号", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectDuplicatePhoneBinding() throws Exception {
        CUserServiceImpl service = new CUserServiceImpl();
        List<CUser> store = new ArrayList<>();
        store.add(buildUser(1L, "old-openid-1", "13800138000", 1));
        store.add(buildUser(2L, "old-openid-2", "13800138000", 1));
        setField(service, "cUserMapper", createUserMapperProxy(store));

        try {
            service.loginOrRegister("real-openid", "13800138000");
            Assert.fail("预期应拒绝重复手机号自动绑定");
        } catch (ServiceException ex) {
            Assert.assertEquals("客户手机号存在重复数据，请联系管理员处理", ex.getMessage());
        }
    }

    private CUser buildUser(Long id, String openid, String phone, Integer status) {
        CUser user = new CUser();
        user.setId(id);
        user.setOpenid(openid);
        user.setPhone(phone);
        user.setStatus(status);
        user.setLastLoginTime(LocalDateTime.now().minusDays(1));
        return user;
    }

    private CUserMapper createUserMapperProxy(List<CUser> store) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = store.size() + 1L;
            private boolean openidQueried = false;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    if (!openidQueried) {
                        openidQueried = true;
                        return null;
                    }
                    return null;
                }
                if ("selectList".equals(method.getName())) {
                    return new ArrayList<>(store);
                }
                if ("selectById".equals(method.getName())) {
                    Long id = (Long) args[0];
                    for (CUser user : store) {
                        if (user.getId().equals(id)) {
                            return user;
                        }
                    }
                    return null;
                }
                if ("updateById".equals(method.getName())) {
                    CUser updating = (CUser) args[0];
                    for (int i = 0; i < store.size(); i++) {
                        if (store.get(i).getId().equals(updating.getId())) {
                            store.set(i, updating);
                            return 1;
                        }
                    }
                    return 0;
                }
                if ("insert".equals(method.getName())) {
                    CUser inserting = (CUser) args[0];
                    inserting.setId(nextId++);
                    store.add(inserting);
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CUserMapper) Proxy.newProxyInstance(
                CUserMapper.class.getClassLoader(),
                new Class<?>[]{CUserMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CUserServiceImpl.class.getDeclaredField(fieldName);
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
