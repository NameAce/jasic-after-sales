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
 * @author Codex
 * @date 2026/05/06
 */
public class CompanyDataAccessServiceTest {

    private CompanyDataAccessService service;
    private Map<Long, SysCompany> companies;

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

    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

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

    @Test
    public void platformUserShouldUseTargetCompanyWithoutChangingLoginCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        Long targetCompanyId = service.resolveCurrentCompanyTarget(3003L);

        Assert.assertEquals(Long.valueOf(3003L), targetCompanyId);
        Assert.assertEquals(Long.valueOf(9999L), SecurityContext.getCurrentCompanyId());
    }

    @Test
    public void currentCompanyPageShouldAllowPlatformUserFallbackToCurrentCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        Long targetCompanyId = service.resolveCurrentCompanyOwnedTarget(null);

        Assert.assertEquals(Long.valueOf(9999L), targetCompanyId);
    }

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

    @Test
    public void companyUserShouldUseCurrentCompanyWhenTargetMissing() {
        switchContext(2002L, SubjectTypeEnum.HQ.getCode(), "HQ_A");

        Long targetCompanyId = service.resolveCurrentCompanyTarget(null);

        Assert.assertEquals(Long.valueOf(2002L), targetCompanyId);
    }

    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    private SysCompany buildCompany(Long id, String typeCode, Integer status) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setCompanyName("company-" + id);
        company.setTypeCode(typeCode);
        company.setStatus(status);
        return company;
    }

    private SysCompanyMapper createSysCompanyMapperProxy(Map<Long, SysCompany> store) {
        InvocationHandler handler = new InvocationHandler() {
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

    private ISysCompanyTypeService createCompanyTypeService() {
        return new ISysCompanyTypeService() {
            @Override
            public List<SysCompanyType> listAll() {
                return Collections.emptyList();
            }

            @Override
            public SysCompanyType getById(Long id) {
                return null;
            }

            @Override
            public Long save(SysCompanyType entity) {
                return null;
            }

            @Override
            public void update(SysCompanyType entity) {
            }

            @Override
            public void remove(Long id) {
            }
        };
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
