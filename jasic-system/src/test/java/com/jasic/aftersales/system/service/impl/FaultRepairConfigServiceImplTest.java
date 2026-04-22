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
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.FaultRepairConfigDTO;
import com.jasic.aftersales.system.domain.dto.FaultRepairConfigFaultDTO;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfig;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfigFault;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfigOption;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.mapper.FaultRepairConfigFaultMapper;
import com.jasic.aftersales.system.mapper.FaultRepairConfigMapper;
import com.jasic.aftersales.system.mapper.FaultRepairConfigOptionMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
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

/**
 * 故障与维修配置服务测试。
 *
 * @author Codex
 * @date 2026/04/01
 */
public class FaultRepairConfigServiceImplTest {

    @Before
    public void setUp() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        SecurityContext.setCurrentCompanyId(1L);
        SecurityContext.setCurrentSubjectType(SubjectTypeEnum.PLATFORM.getCode());
        SecurityContext.setCurrentTypeCode("PLATFORM");
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
    public void shouldReturnRepairFaultOptionsForExactProductMatch() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        FaultRepairConfig exactConfig = buildConfig(1L, 9L, "P-100", "M-200");
        FaultRepairConfig fallbackConfig = buildConfig(2L, 9L, "P-100", null);

        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Arrays.asList(fallbackConfig, exactConfig)));
        setField(service, "faultRepairConfigFaultMapper",
                createFaultMapperProxy(Arrays.asList(
                        buildFault(11L, 1L, "电源故障"),
                        buildFault(21L, 2L, "通用故障")
                )));
        setField(service, "faultRepairConfigOptionMapper",
                createOptionMapperProxy(Arrays.asList(
                        buildOption(101L, 11L, "更换电源板"),
                        buildOption(102L, 11L, "清洁接线"),
                        buildOption(201L, 21L, "检查线路")
                )));
        setField(service, "sysCompanyMapper",
                createCompanyMapperProxy(Collections.singletonList(buildCompany(9L, "总部A"))));

        List<WorkOrderRepairFaultOptionVO> result = service.listRepairFaultOptions(9L, "P-100", "M-200");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("电源故障", result.get(0).getFaultDesc());
        Assert.assertEquals(Arrays.asList("更换电源板", "清洁接线"), result.get(0).getRepairOptions());
    }

    @Test
    public void shouldReturnEmptyRepairFaultOptionsWhenNoConfigMatched() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(buildConfig(1L, 9L, "P-100", "M-200"))));

        List<WorkOrderRepairFaultOptionVO> result = service.listRepairFaultOptions(9L, "P-999", "M-999");

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void shouldReturnRepairFaultOptionsByBoundConfigId() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        FaultRepairConfig config = buildConfig(3L, 9L, "P-300", "M-500");
        config.setStatus(0);
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(config)));
        setField(service, "faultRepairConfigFaultMapper",
                createFaultMapperProxy(Collections.singletonList(buildFault(31L, 3L, "面板故障"))));
        setField(service, "faultRepairConfigOptionMapper",
                createOptionMapperProxy(Collections.singletonList(buildOption(301L, 31L, "更换面板"))));
        setField(service, "sysCompanyMapper",
                createCompanyMapperProxy(Collections.singletonList(buildCompany(9L, "总部A"))));

        List<WorkOrderRepairFaultOptionVO> result = service.listRepairFaultOptionsByConfigId(3L);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("面板故障", result.get(0).getFaultDesc());
        Assert.assertEquals(Collections.singletonList("更换面板"), result.get(0).getRepairOptions());
    }

    @Test
    public void shouldListDistinctEnabledProductModelsByCompanyAndKeyword() throws Exception {
        FaultRepairConfigServiceImpl allOptionsService = new FaultRepairConfigServiceImpl();
        setField(allOptionsService, "faultRepairConfigMapper",
                createConfigMapperProxy(Arrays.asList(
                        buildConfig(1L, 9L, "P-100", "M-200"),
                        buildConfig(2L, 9L, "P-101", "M-200"),
                        buildConfig(3L, 9L, "P-102", "M-300"),
                        buildConfig(4L, 9L, "P-103", null)
                )));
        FaultRepairConfigServiceImpl filteredOptionsService = new FaultRepairConfigServiceImpl();
        setField(filteredOptionsService, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(
                        buildConfig(5L, 9L, "P-104", "M-300")
                )));

        List<String> allOptions = allOptionsService.listEnabledProductModels(9L, null);
        List<String> filteredOptions = filteredOptionsService.listEnabledProductModels(9L, "300");

        Assert.assertEquals(Arrays.asList("M-200", "M-300"), allOptions);
        Assert.assertEquals(Collections.singletonList("M-300"), filteredOptions);
    }

    @Test
    public void shouldFindLatestEnabledConfigIdByModelWhenProductCodeMissing() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Arrays.asList(
                        buildConfig(5L, 9L, "P-500", "M-900"),
                        buildConfig(4L, 9L, "P-400", "M-900")
                )));

        Long configId = service.findEnabledConfigId(9L, null, "M-900");

        Assert.assertEquals(Long.valueOf(5L), configId);
    }

    @Test
    public void shouldDisableCurrentConfigAndInsertNewVersionWhenEditingEnabledConfig() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        List<FaultRepairConfig> storedConfigs = new ArrayList<>();
        FaultRepairConfig current = buildConfig(1L, 9L, "P-100", "M-200");
        current.setRemark("旧备注");
        storedConfigs.add(current);
        List<FaultRepairConfig> insertedConfigs = new ArrayList<>();
        List<FaultRepairConfigFault> insertedFaults = new ArrayList<>();
        List<FaultRepairConfigOption> insertedOptions = new ArrayList<>();
        setField(service, "faultRepairConfigMapper",
                createMutableConfigMapperProxy(storedConfigs, insertedConfigs));
        setField(service, "faultRepairConfigFaultMapper",
                createMutableFaultMapperProxy(insertedFaults));
        setField(service, "faultRepairConfigOptionMapper",
                createMutableOptionMapperProxy(insertedOptions));
        setField(service, "sysCompanyMapper",
                createCompanyMapperProxy(Collections.singletonList(buildCompany(9L, "总部A", "HQ"))));
        setField(service, "companyTypeService", createCompanyTypeServiceStub());

        FaultRepairConfigDTO dto = new FaultRepairConfigDTO();
        dto.setId(1L);
        dto.setCompanyId(9L);
        dto.setProductCode("P-100");
        dto.setProductModel("M-200");
        dto.setStatus(1);
        dto.setRemark("新备注");
        FaultRepairConfigFaultDTO fault = new FaultRepairConfigFaultDTO();
        fault.setFaultDesc("面板故障");
        fault.setRepairOptions(Arrays.asList("更换面板", "检查排线"));
        dto.setFaults(Collections.singletonList(fault));

        service.update(dto);

        Assert.assertEquals(Integer.valueOf(0), current.getStatus());
        Assert.assertEquals(2, storedConfigs.size());
        Assert.assertEquals(1, insertedConfigs.size());
        FaultRepairConfig latest = insertedConfigs.get(0);
        Assert.assertNotNull(latest.getId());
        Assert.assertNotEquals(current.getId(), latest.getId());
        Assert.assertEquals(Integer.valueOf(1), latest.getStatus());
        Assert.assertEquals("新备注", latest.getRemark());
        Assert.assertEquals(1, insertedFaults.size());
        Assert.assertEquals(latest.getId(), insertedFaults.get(0).getConfigId());
        Assert.assertEquals("面板故障", insertedFaults.get(0).getFaultDesc());
        Assert.assertEquals(2, insertedOptions.size());
    }

    @Test
    public void shouldRejectEditingDisabledHistoryConfig() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        FaultRepairConfig history = buildConfig(1L, 9L, "P-100", "M-200");
        history.setStatus(0);
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(history)));

        FaultRepairConfigDTO dto = new FaultRepairConfigDTO();
        dto.setId(1L);
        dto.setCompanyId(9L);
        dto.setStatus(1);

        try {
            service.update(dto);
            Assert.fail("预期应拒绝编辑停用历史配置");
        } catch (ServiceException ex) {
            Assert.assertEquals("停用历史配置不允许编辑", ex.getMessage());
        }
    }

    @Test
    public void shouldForceHqSaveToCurrentCompanyAndFillLogCompanyName() throws Exception {
        switchCompanyContext(9L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        List<FaultRepairConfig> storedConfigs = new ArrayList<>();
        List<FaultRepairConfig> insertedConfigs = new ArrayList<>();
        setField(service, "faultRepairConfigMapper", createMutableConfigMapperProxy(storedConfigs, insertedConfigs));
        setField(service, "faultRepairConfigFaultMapper", createMutableFaultMapperProxy(new ArrayList<>()));
        setField(service, "faultRepairConfigOptionMapper", createMutableOptionMapperProxy(new ArrayList<>()));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Arrays.asList(
                buildCompany(9L, "总部A", "HQ"),
                buildCompany(10L, "总部B", "HQ")
        )));
        setField(service, "companyTypeService", createCompanyTypeServiceStub());

        FaultRepairConfigDTO dto = new FaultRepairConfigDTO();
        dto.setCompanyId(10L);
        dto.setProductCode("P-100");
        dto.setProductModel("M-200");
        dto.setStatus(1);
        FaultRepairConfigFaultDTO fault = new FaultRepairConfigFaultDTO();
        fault.setFaultDesc("电源故障");
        fault.setRepairOptions(Collections.singletonList("更换电容"));
        dto.setFaults(Collections.singletonList(fault));

        service.save(dto);

        Assert.assertEquals(Long.valueOf(9L), dto.getCompanyId());
        Assert.assertEquals("总部A", dto.getTargetCompanyName());
        Assert.assertEquals(1, insertedConfigs.size());
        Assert.assertEquals(Long.valueOf(9L), insertedConfigs.get(0).getCompanyId());
    }

    @Test
    public void shouldOnlyReturnCurrentHqCompanyOption() throws Exception {
        switchCompanyContext(9L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Arrays.asList(
                buildCompany(9L, "总部A", "HQ"),
                buildCompany(10L, "总部B", "HQ")
        )));
        setField(service, "companyTypeService", createCompanyTypeServiceStub());

        List<SysCompanySimpleVO> result = service.listCompanyOptions();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(9L), result.get(0).getId());
        Assert.assertEquals("总部A", result.get(0).getCompanyName());
    }

    @Test
    public void shouldRejectReadingConfigOutsideCurrentHq() throws Exception {
        switchCompanyContext(9L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(buildConfig(2L, 10L, "P-100", "M-200"))));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Arrays.asList(
                buildCompany(9L, "总部A", "HQ"),
                buildCompany(10L, "总部B", "HQ")
        )));
        setField(service, "companyTypeService", createCompanyTypeServiceStub());

        try {
            service.getById(2L);
            Assert.fail("预期应拒绝查看其他总部配置");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权查看当前总部之外的配置", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectUpdatingConfigOutsideCurrentHq() throws Exception {
        switchCompanyContext(9L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(buildConfig(3L, 10L, "P-100", "M-200"))));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Arrays.asList(
                buildCompany(9L, "总部A", "HQ"),
                buildCompany(10L, "总部B", "HQ")
        )));
        setField(service, "companyTypeService", createCompanyTypeServiceStub());

        FaultRepairConfigDTO dto = new FaultRepairConfigDTO();
        dto.setId(3L);
        dto.setCompanyId(10L);
        dto.setStatus(0);

        try {
            service.update(dto);
            Assert.fail("预期应拒绝修改其他总部配置");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权查看当前总部之外的配置", ex.getMessage());
        }
    }

    private FaultRepairConfigMapper createConfigMapperProxy(List<FaultRepairConfig> configs) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return configs;
                }
                if ("selectById".equals(method.getName()) && args != null && args.length > 0) {
                    for (FaultRepairConfig config : configs) {
                        if (config != null && config.getId() != null && config.getId().equals(args[0])) {
                            return config;
                        }
                    }
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigMapper) Proxy.newProxyInstance(
                FaultRepairConfigMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigMapper.class},
                handler
        );
    }

    private FaultRepairConfigMapper createMutableConfigMapperProxy(List<FaultRepairConfig> configs,
                                                                   List<FaultRepairConfig> insertedConfigs) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = configs.stream()
                    .map(FaultRepairConfig::getId)
                    .filter(id -> id != null)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName()) && args != null && args.length > 0) {
                    for (FaultRepairConfig config : configs) {
                        if (config != null && config.getId() != null && config.getId().equals(args[0])) {
                            return config;
                        }
                    }
                    return null;
                }
                if ("selectOne".equals(method.getName())) {
                    return null;
                }
                if ("updateById".equals(method.getName()) && args != null && args.length > 0) {
                    FaultRepairConfig entity = (FaultRepairConfig) args[0];
                    for (int i = 0; i < configs.size(); i++) {
                        if (configs.get(i).getId().equals(entity.getId())) {
                            configs.set(i, entity);
                            return 1;
                        }
                    }
                    return 0;
                }
                if ("insert".equals(method.getName()) && args != null && args.length > 0) {
                    FaultRepairConfig entity = (FaultRepairConfig) args[0];
                    if (entity.getId() == null) {
                        entity.setId(++nextId);
                    }
                    configs.add(entity);
                    insertedConfigs.add(entity);
                    return 1;
                }
                if ("selectList".equals(method.getName())) {
                    return configs;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigMapper) Proxy.newProxyInstance(
                FaultRepairConfigMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigMapper.class},
                handler
        );
    }

    private FaultRepairConfigFaultMapper createFaultMapperProxy(List<FaultRepairConfigFault> faults) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return faults;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigFaultMapper) Proxy.newProxyInstance(
                FaultRepairConfigFaultMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigFaultMapper.class},
                handler
        );
    }

    private FaultRepairConfigFaultMapper createMutableFaultMapperProxy(List<FaultRepairConfigFault> insertedFaults) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = 0L;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName()) && args != null && args.length > 0) {
                    FaultRepairConfigFault entity = (FaultRepairConfigFault) args[0];
                    entity.setId(++nextId);
                    insertedFaults.add(entity);
                    return 1;
                }
                if ("selectList".equals(method.getName())) {
                    return Collections.emptyList();
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigFaultMapper) Proxy.newProxyInstance(
                FaultRepairConfigFaultMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigFaultMapper.class},
                handler
        );
    }

    private FaultRepairConfigOptionMapper createOptionMapperProxy(List<FaultRepairConfigOption> options) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return options;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigOptionMapper) Proxy.newProxyInstance(
                FaultRepairConfigOptionMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigOptionMapper.class},
                handler
        );
    }

    private FaultRepairConfigOptionMapper createMutableOptionMapperProxy(List<FaultRepairConfigOption> insertedOptions) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = 0L;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName()) && args != null && args.length > 0) {
                    FaultRepairConfigOption entity = (FaultRepairConfigOption) args[0];
                    entity.setId(++nextId);
                    insertedOptions.add(entity);
                    return 1;
                }
                if ("selectList".equals(method.getName())) {
                    return Collections.emptyList();
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigOptionMapper) Proxy.newProxyInstance(
                FaultRepairConfigOptionMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigOptionMapper.class},
                handler
        );
    }

    private SysCompanyMapper createCompanyMapperProxy(List<SysCompany> companies) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return companies;
                }
                if ("selectById".equals(method.getName()) && args != null && args.length > 0) {
                    for (SysCompany company : companies) {
                        if (company != null && company.getId() != null && company.getId().equals(args[0])) {
                            return company;
                        }
                    }
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysCompanyMapper) Proxy.newProxyInstance(
                SysCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysCompanyMapper.class},
                handler
        );
    }

    private FaultRepairConfig buildConfig(Long id, Long companyId, String productCode, String productModel) {
        FaultRepairConfig config = new FaultRepairConfig();
        config.setId(id);
        config.setCompanyId(companyId);
        config.setProductCode(productCode);
        config.setProductModel(productModel);
        config.setStatus(1);
        return config;
    }

    private FaultRepairConfigFault buildFault(Long id, Long configId, String faultDesc) {
        FaultRepairConfigFault fault = new FaultRepairConfigFault();
        fault.setId(id);
        fault.setConfigId(configId);
        fault.setFaultDesc(faultDesc);
        fault.setSortNum(1);
        return fault;
    }

    private FaultRepairConfigOption buildOption(Long id, Long faultId, String repairDesc) {
        FaultRepairConfigOption option = new FaultRepairConfigOption();
        option.setId(id);
        option.setFaultId(faultId);
        option.setRepairDesc(repairDesc);
        option.setSortNum(1);
        return option;
    }

    private SysCompany buildCompany(Long id, String companyName) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setCompanyName(companyName);
        return company;
    }

    private SysCompany buildCompany(Long id, String companyName, String typeCode) {
        SysCompany company = buildCompany(id, companyName);
        company.setTypeCode(typeCode);
        return company;
    }

    private ISysCompanyTypeService createCompanyTypeServiceStub() {
        return new ISysCompanyTypeService() {
            @Override
            public List<SysCompanyType> listAll() {
                SysCompanyType type = new SysCompanyType();
                type.setTypeCode("HQ");
                type.setTypeName("总部");
                type.setSubjectType(SubjectTypeEnum.HQ.getCode());
                return Collections.singletonList(type);
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

    private void switchCompanyContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = FaultRepairConfigServiceImpl.class.getDeclaredField(fieldName);
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

        private final Map<String, Object> data = new LinkedHashMap<>();

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public Object get(String key) {
            return data.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            data.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            data.remove(key);
            return this;
        }
    }
}
