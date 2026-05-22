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

/**MachineBarcodeServiceImplTest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
public class MachineBarcodeServiceImplTest {

    /**companies 字段，用于当前类内部业务处理。*/
    private Map<Long, SysCompany> companies;

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
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

    /**tearDown 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    /**验证platformUserShouldFailClosedWhenOwnerHqMissing，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证platformUserShouldRejectNonHqOwner，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证hqUserShouldRejectCrossHqOwner，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证hqUserShouldSaveWithCurrentHqOwnerWhenOwnerMissing，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证hqUserOptionsShouldOnlyReturnCurrentHq，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void hqUserOptionsShouldOnlyReturnCurrentHq() throws Exception {
        switchContext(21L, SubjectTypeEnum.HQ.getCode(), "HQ_A");
        MachineBarcodeServiceImpl service = buildService(new LinkedHashMap<String, MachineBarcode>());

        List<SysCompanySimpleVO> options = service.listHqCompanyOptions();

        Assert.assertEquals(1, options.size());
        Assert.assertEquals(Long.valueOf(21L), options.get(0).getId());
    }

    /**buildService 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param store 业务映射数据，用于提升后续组装或匹配效率。
@return 处理后的业务结果。*/
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

    /**switchContext 处理逻辑，服务于当前类的业务编排和数据转换。
@param companyId 公司ID。
@param subjectType subjectType 字段参数。
@param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。*/
    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    /**buildServiceCompany 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private SysCompany buildServiceCompany() {
        SysCompany company = new SysCompany();
        company.setId(11L);
        company.setCompanyName("一级网点A");
        company.setTypeCode("FIRST");
        company.setStatus(1);
        return company;
    }

    /**buildHqCompany 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param companyName 名称文本，用于展示、匹配或保存业务对象名称。
@param status 业务状态编码，用于判断或更新当前流程节点。
@return 处理后的业务结果。*/
    private SysCompany buildHqCompany(Long id, String companyName, Integer status) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setCompanyName(companyName);
        company.setCompanyCode("HQ-" + id);
        company.setTypeCode("HQ_A");
        company.setStatus(status);
        return company;
    }

    /**buildCompanyTypes 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 查询或组装后的业务数据集合。*/
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

    /**createCompanyTypeService 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private ISysCompanyTypeService createCompanyTypeService() {
        return new ISysCompanyTypeService() {
            /**listAll 业务数据，按查询条件和数据权限返回可见范围内的结果。
@return 查询或组装后的业务数据集合。*/
            @Override
            public List<SysCompanyType> listAll() {
                return buildCompanyTypes();
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

    /**createCompanyMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
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

    /**createMachineBarcodeMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
    @SuppressWarnings("unchecked")
    private MachineBarcodeMapper createMachineBarcodeMapperProxy(Map<String, MachineBarcode> store) {
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

    /**setAnyField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setAnyField(Object target, String fieldName, Object value) throws Exception {
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
            return Collections.emptyList();
        }

        /**getParamMap 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
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
            return "http://localhost/test";
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
        public Object forward(String path) {
            return null;
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
            return null;
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
