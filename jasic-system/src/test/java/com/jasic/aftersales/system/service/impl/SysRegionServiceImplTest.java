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

/**SysRegionServiceImplTest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
public class SysRegionServiceImplTest {

    /**service 字段，用于当前类内部业务处理。*/
    private SysRegionServiceImpl service;
    /**companyDataAccessContext 字段，用于当前类内部业务处理。*/
    private CompanyDataAccessContext companyDataAccessContext;
    /**companies 字段，用于当前类内部业务处理。*/
    private Map<Long, SysCompany> companies;
    /**regionState 字段，用于当前类内部业务处理。*/
    private RegionMapperState regionState;

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
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

    /**tearDown 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            companyDataAccessContext.clear();
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    /**验证platformUserShouldFailClosedWhenSavingRegionWithoutTargetCompany，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证platformUserShouldFailClosedWhenListingUserRegionsWithoutTargetCompany，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证platformUserShouldFailClosedWhenAssigningUserRegionsWithoutTargetCompany，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RejectNonHqTargetCompanyWhenSavingRegion，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证WriteRegionCompanyIdFromResolvedTargetCompany，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RejectRemovingReferencedRegion，保证相关业务规则在回归场景下保持稳定。*/
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
        company.setTypeCode(typeCode);
        company.setStatus(status);
        return company;
    }

    /**createCompanyMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
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

    /**createRegionMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private SysRegionMapper createRegionMapperProxy(RegionMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**nextId 字段，用于当前类内部业务处理。*/
            private long nextId = 1L;

            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**createUserRegionMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
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

    /**createUserCompanyMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
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

    /**createHqFirstContractMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
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

    /**createCompanyTypeService 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private ISysCompanyTypeService createCompanyTypeService() {
        List<SysCompanyType> companyTypes = Arrays.asList(
                buildCompanyType("HQ_A", "HQ"),
                buildCompanyType("FIRST", "SERVICE")
        );
        return new ISysCompanyTypeService() {
            /**listAll 业务数据，按查询条件和数据权限返回可见范围内的结果。
@return 查询或组装后的业务数据集合。*/
            @Override
            public List<SysCompanyType> listAll() {
                return companyTypes;
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

    /**buildCompanyType 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。
@param subjectType subjectType 字段参数。
@return 处理后的业务结果。*/
    private SysCompanyType buildCompanyType(String typeCode, String subjectType) {
        SysCompanyType type = new SysCompanyType();
        type.setTypeCode(typeCode);
        type.setSubjectType(subjectType);
        return type;
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

    /**RegionMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class RegionMapperState {
        /**regions 字段，用于当前类内部业务处理。*/
        private final Map<Long, SysRegion> regions = new LinkedHashMap<>();
        /**userRegions 字段，用于当前类内部业务处理。*/
        private final List<SysUserRegion> userRegions = new ArrayList<>();
        /**contractCount 字段，用于当前类内部业务处理。*/
        private Long contractCount = 0L;
        /**userCompanyCount 字段，用于当前类内部业务处理。*/
        private Long userCompanyCount = 1L;
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
