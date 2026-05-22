package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.CompanyAddressCreateDTO;
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
 * @author Zoro
 * @date 2026/04/11
 */
public class CompanyAddressServiceImplTest {

    /**service 字段，用于当前类内部业务处理。*/
    private CompanyAddressServiceImpl service;
    /**companyDataAccessContext 字段，用于当前类内部业务处理。*/
    private CompanyDataAccessContext companyDataAccessContext;

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @Before
    public void setUp() throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        SecurityContext.setCurrentCompanyId(2002L);
        service = new CompanyAddressServiceImpl();
        companyDataAccessContext = new CompanyDataAccessContext();
        setField(service, "companyDataAccessContext", companyDataAccessContext);
    }

    /**tearDown 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    /**验证SetEditedAddressAsDefault，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证PromoteLatestRemainingAddressWhenDefaultUnset，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证CreateAddressInScopedTargetCompanyWithoutChangingLoginCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldCreateAddressInScopedTargetCompanyWithoutChangingLoginCompany() throws Exception {
        Map<Long, CompanyAddress> store = new LinkedHashMap<>();
        setField(service, "companyAddressMapper", createCompanyAddressMapperProxy(store));

        CompanyAddressCreateDTO dto = new CompanyAddressCreateDTO();
        dto.setContactName("平台维护地址");
        dto.setContactPhone("13700137000");
        dto.setAddress("广东省深圳市南山区1号");
        dto.setIsDefault(1);

        Long id = companyDataAccessContext.runWithTargetCompany(3003L, () -> service.create(dto));

        Assert.assertNotNull(id);
        Assert.assertEquals(Long.valueOf(2002L), SecurityContext.getCurrentCompanyId());
        Assert.assertEquals(Long.valueOf(3003L), store.get(id).getCompanyId());
        Assert.assertNull(companyDataAccessContext.getTargetCompanyId());
    }

    /**buildAddress 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param companyId 公司ID。
@param contactName 名称文本，用于展示、匹配或保存业务对象名称。
@param isDefault isDefault 字段参数。
@param updateTime 更新时间参数。
@return 处理后的业务结果。*/
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

    /**createCompanyAddressMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
    private CompanyAddressMapper createCompanyAddressMapperProxy(Map<Long, CompanyAddress> store) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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
                    if (entity.getId() == null) {
                        entity.setId((long) (store.size() + 1));
                    }
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

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**MockSaRequest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaRequest implements SaRequest {

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**getParam 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getParam(String name) {
            return null;
        }

        /**getParamNames 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public List<String> getParamNames() {
            return new ArrayList<>();
        }

        /**getParamMap 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public Map<String, String> getParamMap() {
            return new LinkedHashMap<>();
        }

        /**getHeader 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getHeader(String name) {
            return null;
        }

        /**getCookieValue 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getCookieValue(String name) {
            return null;
        }

        /**getRequestPath 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getRequestPath() {
            return "/";
        }

        /**getUrl 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getUrl() {
            return "http://localhost/";
        }

        /**getMethod 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getMethod() {
            return "GET";
        }

        /**forward 处理逻辑，服务于当前类的业务编排和数据转换。
@param path path 字段参数。
@return 处理后的业务结果。*/
        @Override
        public String forward(String path) {
            return path;
        }
    }

    /**MockSaResponse 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaResponse implements SaResponse {

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**setStatus 处理逻辑，服务于当前类的业务编排和数据转换。
@param sc sc 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        /**setHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        /**addHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        /**redirect 处理逻辑，服务于当前类的业务编排和数据转换。
@param url url 字段参数。
@return 处理后的业务结果。*/
        @Override
        public Object redirect(String url) {
            return url;
        }
    }

    /**MockSaStorage 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaStorage implements SaStorage {

        /**storage 字段，用于当前类内部业务处理。*/
        private final Map<String, Object> storage = new LinkedHashMap<>();

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**get 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param key key 字段参数。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object get(String key) {
            return storage.get(key);
        }

        /**set 处理逻辑，服务于当前类的业务编排和数据转换。
@param key key 字段参数。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaStorage set(String key, Object value) {
            storage.put(key, value);
            return this;
        }

        /**delete 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param key key 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaStorage delete(String key) {
            storage.remove(key);
            return this;
        }
    }
}
