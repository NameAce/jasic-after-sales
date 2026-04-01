package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeDTO;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeImportItemDTO;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.junit.Assert;
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
 * 条码档案服务测试
 *
 * @author Codex
 * @date 2026/04/01
 */
public class MachineBarcodeServiceImplTest {

    @Test
    public void shouldRejectNonHqCompanyWhenSavingBarcode() throws Exception {
        MachineBarcodeServiceImpl service = new MachineBarcodeServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Collections.singletonMap(11L, buildServiceCompany())));
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(new LinkedHashMap<String, MachineBarcode>()));
        setField(service, "companyTypeService", createCompanyTypeService(buildCompanyTypes()));

        MachineBarcodeDTO dto = new MachineBarcodeDTO();
        dto.setBarcode("JASIC-001");
        dto.setHqCompanyId(11L);
        dto.setStatus(1);

        try {
            service.save(dto);
            Assert.fail("预期应拒绝非总部公司");
        } catch (ServiceException ex) {
            Assert.assertEquals("归属公司不是总部类型", ex.getMessage());
        }
    }

    @Test
    public void shouldReturnOnlyActiveHqCompanyOptions() throws Exception {
        MachineBarcodeServiceImpl service = new MachineBarcodeServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(21L, buildHqCompany(21L, "总部A", 1));
        companies.put(22L, buildHqCompany(22L, "总部B", 0));
        companies.put(31L, buildServiceCompany());

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(new LinkedHashMap<String, MachineBarcode>()));
        setField(service, "companyTypeService", createCompanyTypeService(buildCompanyTypes()));

        List<SysCompanySimpleVO> options = service.listHqCompanyOptions();

        Assert.assertEquals(1, options.size());
        Assert.assertEquals(Long.valueOf(21L), options.get(0).getId());
        Assert.assertEquals("总部A", options.get(0).getCompanyName());
    }

    @Test
    public void shouldRejectDuplicateBarcodeInImportPayload() throws Exception {
        MachineBarcodeServiceImpl service = new MachineBarcodeServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(21L, buildHqCompany(21L, "总部A", 1));

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(new LinkedHashMap<String, MachineBarcode>()));
        setField(service, "companyTypeService", createCompanyTypeService(buildCompanyTypes()));

        MachineBarcodeImportItemDTO first = new MachineBarcodeImportItemDTO();
        first.setBarcode("JASIC-001");
        first.setHqCompanyId(21L);
        first.setStatus(1);

        MachineBarcodeImportItemDTO second = new MachineBarcodeImportItemDTO();
        second.setBarcode("JASIC-001");
        second.setHqCompanyId(21L);
        second.setStatus(1);

        try {
            service.importItems(java.util.Arrays.asList(first, second));
            Assert.fail("预期应拒绝重复条码");
        } catch (ServiceException ex) {
            Assert.assertEquals("导入数据存在重复条码：JASIC-001", ex.getMessage());
        }
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
        hq.setTypeName("总部A");
        hq.setSubjectType("HQ");
        result.add(hq);

        SysCompanyType first = new SysCompanyType();
        first.setTypeCode("FIRST");
        first.setTypeName("一级网点");
        first.setSubjectType("SERVICE");
        result.add(first);

        return result;
    }

    private ISysCompanyTypeService createCompanyTypeService(List<SysCompanyType> companyTypes) {
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

    private SysCompanyMapper createCompanyMapperProxy(Map<Long, SysCompany> companies) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return companies.get(args[0]);
                }
                if ("selectList".equals(method.getName())) {
                    List<SysCompany> result = new ArrayList<>();
                    for (SysCompany company : companies.values()) {
                        if (company.getStatus() != null && company.getStatus() == 1 && "HQ_A".equals(company.getTypeCode())) {
                            result.add(company);
                        }
                    }
                    return result;
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

    private MachineBarcodeMapper createMachineBarcodeMapperProxy(Map<String, MachineBarcode> store) {
        InvocationHandler handler = new InvocationHandler() {
            private long nextId = 1L;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    if (store.isEmpty()) {
                        return null;
                    }
                    return store.values().iterator().next();
                }
                if ("insert".equals(method.getName())) {
                    MachineBarcode entity = (MachineBarcode) args[0];
                    if (entity.getId() == null) {
                        entity.setId(nextId++);
                    }
                    store.put(entity.getBarcode(), entity);
                    return 1;
                }
                if ("updateById".equals(method.getName())) {
                    MachineBarcode entity = (MachineBarcode) args[0];
                    store.put(entity.getBarcode(), entity);
                    return 1;
                }
                if ("selectById".equals(method.getName())) {
                    Long id = (Long) args[0];
                    for (MachineBarcode value : store.values()) {
                        if (id.equals(value.getId())) {
                            return value;
                        }
                    }
                    return null;
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = MachineBarcodeServiceImpl.class.getDeclaredField(fieldName);
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
}
