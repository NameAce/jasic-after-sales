package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeDTO;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.query.MachineBarcodeQuery;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MachineBarcodeServiceImplTest {

    private Map<Long, SysCompany> companies;

    @Before
    public void setUp() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        companies = new LinkedHashMap<>();
        companies.put(11L, buildServiceCompany());
        companies.put(21L, buildHqCompany(21L, "总部A", 1));
        companies.put(22L, buildHqCompany(22L, "总部B", 1));
        companies.put(23L, buildHqCompany(23L, "总部C", 0));
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
    public void platformUserShouldFailClosedWhenOwnerHqMissing() throws Exception {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        MachineBarcodeServiceImpl service = buildService(new LinkedHashMap<String, MachineBarcode>());

        try {
            service.listPage(new MachineBarcodeQuery());
            Assert.fail("平台用户缺少目标总部应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("缺少目标公司上下文", ex.getMessage());
        }
    }

    @Test
    public void platformUserShouldRejectNonHqOwner() throws Exception {
        switchContext(9999L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        MachineBarcodeServiceImpl service = buildService(new LinkedHashMap<String, MachineBarcode>());

        MachineBarcodeDTO dto = new MachineBarcodeDTO();
        dto.setBarcode("JASIC-001");
        dto.setOwnerHqId(11L);
        dto.setStatus(1);

        try {
            service.save(dto);
            Assert.fail("平台用户指定非总部目标应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("目标公司不是总部类型", ex.getMessage());
        }
    }

    @Test
    public void hqUserShouldRejectCrossHqOwner() throws Exception {
        switchContext(21L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        MachineBarcodeServiceImpl service = buildService(new LinkedHashMap<String, MachineBarcode>());
        MachineBarcodeQuery query = new MachineBarcodeQuery();
        query.setOwnerHqId(22L);

        try {
            service.listPage(query);
            Assert.fail("总部用户跨总部查询应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权操作目标公司数据", ex.getMessage());
        }
    }

    @Test
    public void hqUserShouldSaveWithCurrentHqOwnerWhenOwnerMissing() throws Exception {
        switchContext(21L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        Map<String, MachineBarcode> store = new LinkedHashMap<>();
        MachineBarcodeServiceImpl service = buildService(store);

        MachineBarcodeDTO dto = new MachineBarcodeDTO();
        dto.setBarcode(" JASIC-001 ");
        dto.setProductName("  ZX7逆变焊机  ");
        dto.setMachineNo("  M-001  ");
        dto.setStatus(1);

        service.save(dto);

        MachineBarcode saved = store.get("JASIC-001");
        Assert.assertNotNull(saved);
        Assert.assertEquals(Long.valueOf(21L), saved.getHqCompanyId());
        Assert.assertEquals("ZX7逆变焊机", saved.getProductName());
        Assert.assertEquals("M-001", saved.getMachineNo());
    }

    @Test
    public void hqUserOptionsShouldOnlyReturnCurrentHq() throws Exception {
        switchContext(21L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        MachineBarcodeServiceImpl service = buildService(new LinkedHashMap<String, MachineBarcode>());

        List<SysCompanySimpleVO> options = service.listHqCompanyOptions();

        Assert.assertEquals(1, options.size());
        Assert.assertEquals(Long.valueOf(21L), options.get(0).getId());
    }

    private MachineBarcodeServiceImpl buildService(Map<String, MachineBarcode> store) throws Exception {
        SysCompanyMapper companyMapper = createCompanyMapperProxy(companies);
        ISysCompanyTypeService companyTypeService = createCompanyTypeService();

        CompanyDataAccessService accessService = new CompanyDataAccessService();
        setAnyField(accessService, "companyDataAccessContext", new CompanyDataAccessContext());
        setAnyField(accessService, "sysCompanyMapper", companyMapper);
        setAnyField(accessService, "companyTypeService", companyTypeService);

        MachineBarcodeServiceImpl service = new MachineBarcodeServiceImpl();
        setAnyField(service, "sysCompanyMapper", companyMapper);
        setAnyField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(store));
        setAnyField(service, "companyTypeService", companyTypeService);
        setAnyField(service, "companyDataAccessService", accessService);
        return service;
    }

    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    private SysCompany buildServiceCompany() {
        SysCompany company = new SysCompany();
        company.setId(11L);
        company.setCompanyName("一级网点A");
        company.setTypeCode("FIRST");
        company.setStatus(1);
        return company;
    }

    private SysCompany buildHqCompany(Long id, String companyName, Integer status) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setCompanyName(companyName);
        company.setCompanyCode("HQ-" + id);
        company.setTypeCode("HQ_A");
        company.setStatus(status);
        return company;
    }

    private List<SysCompanyType> buildCompanyTypes() {
        List<SysCompanyType> result = new ArrayList<>();
        SysCompanyType hq = new SysCompanyType();
        hq.setTypeCode("HQ_A");
        hq.setTypeName("总部");
        hq.setSubjectType(SubjectTypeEnum.HQ.getCode());
        result.add(hq);
        SysCompanyType first = new SysCompanyType();
        first.setTypeCode("FIRST");
        first.setTypeName("一级网点");
        first.setSubjectType(SubjectTypeEnum.SERVICE.getCode());
        result.add(first);
        return result;
    }

    private ISysCompanyTypeService createCompanyTypeService() {
        return new ISysCompanyTypeService() {
            @Override
            public List<SysCompanyType> listAll() {
                return buildCompanyTypes();
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

    private SysCompanyMapper createCompanyMapperProxy(Map<Long, SysCompany> store) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectById".equals(method.getName())) {
                return store.get(args[0]);
            }
            if ("selectList".equals(method.getName())) {
                List<SysCompany> result = new ArrayList<>();
                for (SysCompany company : store.values()) {
                    if (Integer.valueOf(1).equals(company.getStatus()) && "HQ_A".equals(company.getTypeCode())) {
                        result.add(company);
                    }
                }
                return result;
            }
            return defaultValue(method.getReturnType());
        };
        return (SysCompanyMapper) Proxy.newProxyInstance(
                SysCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysCompanyMapper.class},
                handler
        );
    }

    @SuppressWarnings("unchecked")
    private MachineBarcodeMapper createMachineBarcodeMapperProxy(Map<String, MachineBarcode> store) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = 1L;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    return store.isEmpty() ? null : store.values().iterator().next();
                }
                if ("selectPage".equals(method.getName())) {
                    Page<MachineBarcode> page = (Page<MachineBarcode>) args[0];
                    page.setRecords(new ArrayList<>(store.values()));
                    page.setTotal(store.size());
                    return page;
                }
                if ("insert".equals(method.getName())) {
                    MachineBarcode entity = (MachineBarcode) args[0];
                    if (entity.getId() == null) {
                        entity.setId(nextId++);
                    }
                    store.put(entity.getBarcode(), entity);
                    return 1;
                }
                if ("update".equals(method.getName())) {
                    MachineBarcode entity = (MachineBarcode) args[0];
                    store.put(entity.getBarcode(), entity);
                    return 1;
                }
                if ("delete".equals(method.getName())) {
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (MachineBarcodeMapper) Proxy.newProxyInstance(
                MachineBarcodeMapper.class.getClassLoader(),
                new Class<?>[]{MachineBarcodeMapper.class},
                handler
        );
    }

    private void setAnyField(Object target, String fieldName, Object value) throws Exception {
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
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
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
            return "http://localhost/test";
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public Object forward(String path) {
            return null;
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
            return null;
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
