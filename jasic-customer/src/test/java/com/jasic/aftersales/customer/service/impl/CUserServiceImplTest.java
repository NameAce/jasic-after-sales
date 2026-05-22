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
 * @author Zoro
 * @date 2026/04/02
 */
public class CUserServiceImplTest {

    /**验证MergeRealWechatIdentityByPhone，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RejectFirstLoginWithoutPhone，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RejectDuplicatePhoneBinding，保证相关业务规则在回归场景下保持稳定。*/
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

    /**buildUser 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param openid openid 字段。
@param phone phone 字段参数。
@param status 业务状态编码，用于判断或更新当前流程节点。
@return 处理后的业务结果。*/
    private CUser buildUser(Long id, String openid, String phone, Integer status) {
        CUser user = new CUser();
        user.setId(id);
        user.setOpenid(openid);
        user.setPhone(phone);
        user.setStatus(status);
        user.setLastLoginTime(LocalDateTime.now().minusDays(1));
        return user;
    }

    /**createUserMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private CUserMapper createUserMapperProxy(List<CUser> store) {
        InvocationHandler handler = new InvocationHandler() {
            /**nextId 字段，用于当前类内部业务处理。*/
            private long nextId = store.size() + 1L;
            /**openidQueried 字段，用于当前类内部业务处理。*/
            private boolean openidQueried = false;

            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CUserServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**defaultValue 处理逻辑，服务于当前类的业务编排和数据转换。
@param returnType returnType 字段参数。
@return 处理后的业务结果。*/
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


