package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysRegionDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRegionMapper;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大区管理服务测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class SysRegionServiceImplTest {

    @Test
    public void shouldRejectNonHqCompanyWhenSavingRegion() throws Exception {
        SysRegionServiceImpl service = new SysRegionServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "FIRST", 1));

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());

        SysRegionDTO dto = new SysRegionDTO();
        dto.setCompanyId(1L);
        dto.setRegionName("华南");

        try {
            service.save(dto);
            Assert.fail("预期应拒绝非总部公司");
        } catch (ServiceException ex) {
            Assert.assertEquals("所属公司必须是总部类型", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectRemovingReferencedRegion() throws Exception {
        SysRegionServiceImpl service = new SysRegionServiceImpl();
        RegionMapperState regionState = new RegionMapperState();
        SysRegion region = new SysRegion();
        region.setId(2L);
        region.setCompanyId(1L);
        regionState.region = region;

        setField(service, "sysRegionMapper", createRegionMapperProxy(regionState));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(1L));

        try {
            service.remove(2L);
            Assert.fail("预期应拒绝删除被引用的大区");
        } catch (ServiceException ex) {
            Assert.assertEquals("该大区已被签约关系引用，不允许删除", ex.getMessage());
        }
    }

    private SysCompany buildCompany(Long id, String typeCode, Integer status) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setTypeCode(typeCode);
        company.setStatus(status);
        return company;
    }

    private SysCompanyMapper createCompanyMapperProxy(Map<Long, SysCompany> companies) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return companies.get(args[0]);
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

    private SysRegionMapper createRegionMapperProxy(RegionMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return state.region;
                }
                if ("deleteById".equals(method.getName())) {
                    state.deleted = true;
                    return 1;
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

    private HqFirstContractMapper createHqFirstContractMapperProxy(Long count) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return count;
                }
                return defaultValue(method.getReturnType());
            }
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
        Field field = SysRegionServiceImpl.class.getDeclaredField(fieldName);
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

    private static class RegionMapperState {
        private SysRegion region;
        private boolean deleted;
    }
}
