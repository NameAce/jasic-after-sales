package com.jasic.aftersales.system.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公司数据访问目标解析测试。
 *
 * @author Zoro
 * @date 2026/05/06
 */
public class CompanyDataAccessServiceTest {

    /**service 字段，用于当前类内部业务处理。*/
    private CompanyDataAccessService service;
    /**companies 字段，用于当前类内部业务处理。*/
    private Map<Long, SysCompany> companies;

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @Before
    public void setUp() throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);

        companies = new LinkedHashMap<>();
        companies.put(2002L, buildCompany(2002L, "HQ_A", 1));
        companies.put(3003L, buildCompany(3003L, "FIRST", 1));
        companies.put(4004L, buildCompany(4004L, "SECOND", 0));

        service = new CompanyDataAccessService();
        setField(service, "companyDataAccessContext", new CompanyDataAccessContext());
        setField(service, "sysCompanyMapper", createSysCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
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

    /**验证platformUserShouldFailClosedWhenTargetCompanyMissing，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void platformUserShouldFailClosedWhenTargetCompanyMissing() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        try {
            service.resolveCurrentCompanyTarget(null);
            Assert.fail("平台用户缺少目标公司应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("缺少目标公司上下文", ex.getMessage());
        }
    }

    /**验证platformUserShouldRejectIllegalTargetCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void platformUserShouldRejectIllegalTargetCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        try {
            service.resolveCurrentCompanyTarget(404L);
            Assert.fail("非法目标公司应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("目标公司不存在", ex.getMessage());
        }
    }

    /**验证platformUserShouldUseTargetCompanyWithoutChangingLoginCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void platformUserShouldUseTargetCompanyWithoutChangingLoginCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        Long targetCompanyId = service.resolveCurrentCompanyTarget(3003L);

        Assert.assertEquals(Long.valueOf(3003L), targetCompanyId);
        Assert.assertEquals(Long.valueOf(9999L), SecurityContext.getCurrentCompanyId());
    }

    /**验证currentCompanyPageShouldAllowPlatformUserFallbackToCurrentCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void currentCompanyPageShouldAllowPlatformUserFallbackToCurrentCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        Long targetCompanyId = service.resolveCurrentCompanyOwnedTarget(null);

        Assert.assertEquals(Long.valueOf(9999L), targetCompanyId);
    }

    /**验证currentCompanyPageShouldRejectPlatformUserCrossCompanyTarget，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void currentCompanyPageShouldRejectPlatformUserCrossCompanyTarget() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        try {
            service.resolveCurrentCompanyOwnedTarget(3003L);
            Assert.fail("当前公司页不应允许平台账号直接跨公司访问");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权操作目标公司数据", ex.getMessage());
        }
    }

    /**验证companyUserShouldRejectDifferentTargetCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void companyUserShouldRejectDifferentTargetCompany() {
        switchContext(2002L, SubjectTypeEnum.HQ.getCode(), "HQ_A");

        try {
            service.resolveCurrentCompanyTarget(3003L);
            Assert.fail("普通公司用户越权传目标公司应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权操作目标公司数据", ex.getMessage());
        }
    }

    /**验证companyUserShouldUseCurrentCompanyWhenTargetMissing，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void companyUserShouldUseCurrentCompanyWhenTargetMissing() {
        switchContext(2002L, SubjectTypeEnum.HQ.getCode(), "HQ_A");

        Long targetCompanyId = service.resolveCurrentCompanyTarget(null);

        Assert.assertEquals(Long.valueOf(2002L), targetCompanyId);
    }

    /**switchContext 处理逻辑，服务于当前类的业务编排和数据转换。
@param companyId 公司ID。
@param subjectType subjectType 字段参数。
@param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。*/
    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    /**buildCompany 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。
@param status 业务状态编码，用于判断或更新当前流程节点。
@return 处理后的业务结果。*/
    private SysCompany buildCompany(Long id, String typeCode, Integer status) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setCompanyName("company-" + id);
        company.setTypeCode(typeCode);
        company.setStatus(status);
        return company;
    }

    /**createSysCompanyMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
    private SysCompanyMapper createSysCompanyMapperProxy(Map<Long, SysCompany> store) {
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
                if ("equals".equals(name)) {
                    return proxy == args[0];
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(name)) {
                    return "SysCompanyMapperProxy";
                }
                throw new UnsupportedOperationException(name);
            }
        };
        return (SysCompanyMapper) Proxy.newProxyInstance(
                SysCompanyMapper.class.getClassLoader(),
                new Class[]{SysCompanyMapper.class},
                handler
        );
    }

    /**createCompanyTypeService 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private ISysCompanyTypeService createCompanyTypeService() {
        return new ISysCompanyTypeService() {
            /**listAll 业务数据，按查询条件和数据权限返回可见范围内的结果。
@return 查询或组装后的业务数据集合。*/
            @Override
            public List<SysCompanyType> listAll() {
                return Collections.emptyList();
            }

            /**getById 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param id 主键ID。
@return 查询或解析得到的业务对象。*/
            @Override
            public SysCompanyType getById(Long id) {
                return null;
            }

            /**save 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param entity entity 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
            @Override
            public Long save(SysCompanyType entity) {
                return null;
            }

            /**update 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param entity entity 字段参数。*/
            @Override
            public void update(SysCompanyType entity) {
            }

            /**remove 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param id 主键ID。*/
            @Override
            public void remove(Long id) {
            }
        };
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
