package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.CompanyAddressUpdateDTO;
import com.jasic.aftersales.system.domain.entity.CompanyAddress;
import com.jasic.aftersales.system.mapper.CompanyAddressMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公司地址簿服务测试。
 *
 * @author Codex
 * @date 2026/04/11
 */
public class CompanyAddressServiceImplTest {

    private CompanyAddressServiceImpl service;

    @Before
    public void setUp() throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        SecurityContext.setCurrentCompanyId(2002L);
        service = new CompanyAddressServiceImpl();
    }

    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    @Test
    public void shouldSetEditedAddressAsDefault() throws Exception {
        Map<Long, CompanyAddress> store = new LinkedHashMap<>();
        store.put(1L, buildAddress(1L, 2002L, "默认地址", 1, LocalDateTime.of(2026, 4, 10, 9, 0)));
        store.put(2L, buildAddress(2L, 2002L, "备用地址", 0, LocalDateTime.of(2026, 4, 11, 9, 0)));
        setField(service, "companyAddressMapper", createCompanyAddressMapperProxy(store));

        CompanyAddressUpdateDTO dto = new CompanyAddressUpdateDTO();
        dto.setId(2L);
        dto.setContactName("新默认地址");
        dto.setContactPhone("13800138000");
        dto.setAddress("江苏省苏州市工业园区1号");
        dto.setIsDefault(1);

        service.update(dto);

        Assert.assertEquals(Integer.valueOf(0), store.get(1L).getIsDefault());
        Assert.assertEquals(Integer.valueOf(1), store.get(2L).getIsDefault());
        Assert.assertEquals("新默认地址", store.get(2L).getContactName());
    }

    @Test
    public void shouldPromoteLatestRemainingAddressWhenDefaultUnset() throws Exception {
        Map<Long, CompanyAddress> store = new LinkedHashMap<>();
        store.put(1L, buildAddress(1L, 2002L, "当前默认", 1, LocalDateTime.of(2026, 4, 10, 9, 0)));
        store.put(2L, buildAddress(2L, 2002L, "旧地址", 0, LocalDateTime.of(2026, 4, 9, 9, 0)));
        store.put(3L, buildAddress(3L, 2002L, "最新地址", 0, LocalDateTime.of(2026, 4, 11, 9, 0)));
        setField(service, "companyAddressMapper", createCompanyAddressMapperProxy(store));

        CompanyAddressUpdateDTO dto = new CompanyAddressUpdateDTO();
        dto.setId(1L);
        dto.setContactName("当前默认");
        dto.setContactPhone("13900139000");
        dto.setAddress("上海市闵行区2号");
        dto.setIsDefault(0);

        service.update(dto);

        Assert.assertEquals(Integer.valueOf(0), store.get(1L).getIsDefault());
        Assert.assertEquals(Integer.valueOf(0), store.get(2L).getIsDefault());
        Assert.assertEquals(Integer.valueOf(1), store.get(3L).getIsDefault());
    }

    private CompanyAddress buildAddress(Long id, Long companyId, String contactName, Integer isDefault,
                                        LocalDateTime updateTime) {
        CompanyAddress address = new CompanyAddress();
        address.setId(id);
        address.setCompanyId(companyId);
        address.setContactName(contactName);
        address.setContactPhone("13800000000");
        address.setAddress("测试地址");
        address.setIsDefault(isDefault);
        address.setUpdateTime(updateTime);
        return address;
    }

    private CompanyAddressMapper createCompanyAddressMapperProxy(Map<Long, CompanyAddress> store) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("selectById".equals(name)) {
                    return store.get((Long) args[0]);
                }
                if ("selectList".equals(name)) {
                    return new ArrayList<>(store.values());
                }
                if ("updateById".equals(name)) {
                    CompanyAddress entity = (CompanyAddress) args[0];
                    store.put(entity.getId(), entity);
                    return 1;
                }
                if ("insert".equals(name)) {
                    CompanyAddress entity = (CompanyAddress) args[0];
                    store.put(entity.getId(), entity);
                    return 1;
                }
                if ("deleteById".equals(name)) {
                    store.remove((Long) args[0]);
                    return 1;
                }
                if ("equals".equals(name)) {
                    return proxy == args[0];
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(name)) {
                    return "CompanyAddressMapperProxy";
                }
                throw new UnsupportedOperationException(name);
            }
        };
        return (CompanyAddressMapper) Proxy.newProxyInstance(
                CompanyAddressMapper.class.getClassLoader(),
                new Class[]{CompanyAddressMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class MockSaRequest implements SaRequest {

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public String getParam(String name) {
            return null;
        }

        @Override
        public List<String> getParamNames() {
            return new ArrayList<>();
        }

        @Override
        public Map<String, String> getParamMap() {
            return new LinkedHashMap<>();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public String getRequestPath() {
            return "/";
        }

        @Override
        public String getUrl() {
            return "http://localhost/";
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public String forward(String path) {
            return path;
        }
    }

    private static class MockSaResponse implements SaResponse {

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        @Override
        public Object redirect(String url) {
            return url;
        }
    }

    private static class MockSaStorage implements SaStorage {

        private final Map<String, Object> storage = new LinkedHashMap<>();

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public Object get(String key) {
            return storage.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            storage.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            storage.remove(key);
            return this;
        }
    }
}
