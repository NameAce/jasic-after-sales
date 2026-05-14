package com.jasic.aftersales.system.service.impl;

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
import com.jasic.aftersales.system.domain.dto.SysRegionDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRegion;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRegionMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserRegionMapper;
import com.jasic.aftersales.system.service.CompanyDataAccessService;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SysRegionServiceImplTest {

    private SysRegionServiceImpl service;
    private CompanyDataAccessContext companyDataAccessContext;
    private Map<Long, SysCompany> companies;
    private RegionMapperState regionState;

    @Before
    public void setUp() throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);

        companies = new LinkedHashMap<>();
        companies.put(21L, buildCompany(21L, "HQ_A", 1));
        companies.put(11L, buildCompany(11L, "FIRST", 1));

        companyDataAccessContext = new CompanyDataAccessContext();
        CompanyDataAccessService companyDataAccessService = new CompanyDataAccessService();
        setField(companyDataAccessService, "companyDataAccessContext", companyDataAccessContext);
        setField(companyDataAccessService, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(companyDataAccessService, "companyTypeService", createCompanyTypeService());

        regionState = new RegionMapperState();
        service = new SysRegionServiceImpl();
        setField(service, "sysRegionMapper", createRegionMapperProxy(regionState));
        setField(service, "sysUserRegionMapper", createUserRegionMapperProxy(regionState));
        setField(service, "sysUserCompanyMapper", createUserCompanyMapperProxy(regionState));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(regionState));
        setField(service, "companyDataAccessService", companyDataAccessService);
    }

    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            companyDataAccessContext.clear();
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    @Test
    public void platformUserShouldFailClosedWhenSavingRegionWithoutTargetCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        SysRegionDTO dto = new SysRegionDTO();
        dto.setRegionName("South");

        try {
            service.save(dto);
            Assert.fail("platform user without targetCompanyId should be rejected");
        } catch (ServiceException ex) {
            Assert.assertNotNull(ex.getMessage());
        }
    }

    @Test
    public void platformUserShouldFailClosedWhenListingUserRegionsWithoutTargetCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        try {
            service.listUserRegionIdsByTargetCompanyId(101L, null);
            Assert.fail("platform user without targetCompanyId should be rejected");
        } catch (ServiceException ex) {
            Assert.assertNotNull(ex.getMessage());
        }
    }

    @Test
    public void platformUserShouldFailClosedWhenAssigningUserRegionsWithoutTargetCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");

        try {
            service.assignUserRegions(101L, null, Arrays.asList(1L, 2L));
            Assert.fail("platform user without targetCompanyId should be rejected");
        } catch (ServiceException ex) {
            Assert.assertNotNull(ex.getMessage());
        }
    }

    @Test
    public void shouldRejectNonHqTargetCompanyWhenSavingRegion() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        SysRegionDTO dto = new SysRegionDTO();
        dto.setTargetCompanyId(11L);
        dto.setRegionName("South");

        try {
            service.save(dto);
            Assert.fail("non-HQ target company should be rejected");
        } catch (ServiceException ex) {
            Assert.assertNotNull(ex.getMessage());
        }
    }

    @Test
    public void shouldWriteRegionCompanyIdFromResolvedTargetCompany() {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        SysRegionDTO dto = new SysRegionDTO();
        dto.setTargetCompanyId(21L);
        dto.setRegionCode("HN");
        dto.setRegionName("South");

        Long id = service.save(dto);

        Assert.assertNotNull(id);
        Assert.assertEquals(Long.valueOf(21L), regionState.regions.get(id).getCompanyId());
        Assert.assertEquals(Long.valueOf(9999L), SecurityContext.getCurrentCompanyId());
        Assert.assertNull(companyDataAccessContext.getTargetCompanyId());
    }

    @Test
    public void shouldRejectRemovingReferencedRegion() {
        switchContext(21L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        SysRegion region = new SysRegion();
        region.setId(2L);
        region.setCompanyId(21L);
        regionState.regions.put(2L, region);
        regionState.contractCount = 1L;

        try {
            service.remove(2L, null);
            Assert.fail("referenced region should be rejected");
        } catch (ServiceException ex) {
            Assert.assertNotNull(ex.getMessage());
        }
    }

    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    private SysCompany buildCompany(Long id, String typeCode, Integer status) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setTypeCode(typeCode);
        company.setStatus(status);
        return company;
    }

    private SysCompanyMapper createCompanyMapperProxy(Map<Long, SysCompany> store) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectById".equals(method.getName())) {
                return store.get(args[0]);
            }
            return defaultValue(method.getReturnType());
        };
        return (SysCompanyMapper) Proxy.newProxyInstance(
                SysCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysCompanyMapper.class},
                handler
        );
    }

    private SysRegionMapper createRegionMapperProxy(RegionMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = 1L;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String methodName = method.getName();
                if ("insert".equals(methodName)) {
                    SysRegion entity = (SysRegion) args[0];
                    if (entity.getId() == null) {
                        entity.setId(nextId++);
                    }
                    state.regions.put(entity.getId(), entity);
                    return 1;
                }
                if ("selectById".equals(methodName)) {
                    return state.regions.get(args[0]);
                }
                if ("updateById".equals(methodName)) {
                    SysRegion entity = (SysRegion) args[0];
                    state.regions.put(entity.getId(), entity);
                    return 1;
                }
                if ("deleteById".equals(methodName)) {
                    state.regions.remove(args[0]);
                    return 1;
                }
                if ("selectList".equals(methodName)) {
                    return new ArrayList<>(state.regions.values());
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysRegionMapper) Proxy.newProxyInstance(
                SysRegionMapper.class.getClassLoader(),
                new Class<?>[]{SysRegionMapper.class},
                handler
        );
    }

    private SysUserRegionMapper createUserRegionMapperProxy(RegionMapperState state) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("selectList".equals(methodName)) {
                return state.userRegions;
            }
            if ("delete".equals(methodName)) {
                state.userRegions.clear();
                return 1;
            }
            if ("insert".equals(methodName)) {
                state.userRegions.add((SysUserRegion) args[0]);
                return 1;
            }
            return defaultValue(method.getReturnType());
        };
        return (SysUserRegionMapper) Proxy.newProxyInstance(
                SysUserRegionMapper.class.getClassLoader(),
                new Class<?>[]{SysUserRegionMapper.class},
                handler
        );
    }

    private SysUserCompanyMapper createUserCompanyMapperProxy(RegionMapperState state) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectCount".equals(method.getName())) {
                return state.userCompanyCount;
            }
            return defaultValue(method.getReturnType());
        };
        return (SysUserCompanyMapper) Proxy.newProxyInstance(
                SysUserCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysUserCompanyMapper.class},
                handler
        );
    }

    private HqFirstContractMapper createHqFirstContractMapperProxy(RegionMapperState state) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectCount".equals(method.getName())) {
                return state.contractCount;
            }
            return defaultValue(method.getReturnType());
        };
        return (HqFirstContractMapper) Proxy.newProxyInstance(
                HqFirstContractMapper.class.getClassLoader(),
                new Class<?>[]{HqFirstContractMapper.class},
                handler
        );
    }

    private ISysCompanyTypeService createCompanyTypeService() {
        List<SysCompanyType> companyTypes = Arrays.asList(
                buildCompanyType("HQ_A", "HQ"),
                buildCompanyType("FIRST", "SERVICE")
        );
        return new ISysCompanyTypeService() {
            @Override
            public List<SysCompanyType> listAll() {
                return companyTypes;
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

    private SysCompanyType buildCompanyType(String typeCode, String subjectType) {
        SysCompanyType type = new SysCompanyType();
        type.setTypeCode(typeCode);
        type.setSubjectType(subjectType);
        return type;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
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
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (byte.class.equals(returnType) || short.class.equals(returnType) || int.class.equals(returnType)) {
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

    private static class RegionMapperState {
        private final Map<Long, SysRegion> regions = new LinkedHashMap<>();
        private final List<SysUserRegion> userRegions = new ArrayList<>();
        private Long contractCount = 0L;
        private Long userCompanyCount = 1L;
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
