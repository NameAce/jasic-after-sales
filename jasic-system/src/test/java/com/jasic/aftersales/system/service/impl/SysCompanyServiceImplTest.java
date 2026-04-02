package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysCompanyDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplate;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.service.ICompanyGeoResolver;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.ISysRoleTemplateService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公司管理服务测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class SysCompanyServiceImplTest {

    @Test
    public void shouldRejectUnknownCompanyTypeWhenSaving() throws Exception {
        SysCompanyServiceImpl service = new SysCompanyServiceImpl();
        setField(service, "companyTypeService", createCompanyTypeService(Collections.singletonList(buildCompanyType("FIRST", "SERVICE"))));

        SysCompanyDTO dto = buildCompanyDto();
        dto.setTypeCode("UNKNOWN");

        try {
            service.save(dto);
            Assert.fail("预期应拒绝未知公司类型");
        } catch (ServiceException ex) {
            Assert.assertEquals("公司类型不存在", ex.getMessage());
        }
    }

    @Test
    public void shouldResolveCoordinatesWhenSavingCompany() throws Exception {
        SysCompanyServiceImpl service = new SysCompanyServiceImpl();
        CompanyMapperState companyState = new CompanyMapperState();
        UserMapperState userState = new UserMapperState();

        setField(service, "companyTypeService", createCompanyTypeService(Collections.singletonList(buildCompanyType("FIRST", "SERVICE"))));
        setField(service, "companyGeoResolver", createGeoResolver(new BigDecimal("113.930000"), new BigDecimal("22.540000")));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companyState));
        setField(service, "sysRoleTemplateMapper", createRoleTemplateMapperProxy(1L));
        setField(service, "sysUserMapper", createUserMapperProxy(userState));
        setField(service, "sysUserCompanyMapper", createNoopMapperProxy(SysUserCompanyMapper.class));
        setField(service, "sysUserRoleMapper", createNoopMapperProxy(SysUserRoleMapper.class));
        setField(service, "roleTemplateService", createRoleTemplateService());
        setField(service, "configService", createConfigService());

        Long id = service.save(buildCompanyDto());

        Assert.assertEquals(Long.valueOf(1L), id);
        Assert.assertNotNull(companyState.insertedCompany);
        Assert.assertEquals(new BigDecimal("113.930000"), companyState.insertedCompany.getLongitude());
        Assert.assertEquals(new BigDecimal("22.540000"), companyState.insertedCompany.getLatitude());
        Assert.assertEquals("深圳市南山区科技园", companyState.insertedCompany.getAddress());
        Assert.assertEquals(1, userState.insertCount);
    }

    private SysCompanyDTO buildCompanyDto() {
        SysCompanyDTO dto = new SysCompanyDTO();
        dto.setCompanyName("一级网点A");
        dto.setCompanyCode("FIRST-001");
        dto.setTypeCode("FIRST");
        dto.setContactName("张三");
        dto.setContactPhone("13800138000");
        dto.setAddress(" 深圳市南山区科技园 ");
        dto.setAdminUsername("first_admin");
        dto.setStatus(1);
        return dto;
    }

    private SysCompanyType buildCompanyType(String typeCode, String subjectType) {
        SysCompanyType type = new SysCompanyType();
        type.setTypeCode(typeCode);
        type.setSubjectType(subjectType);
        return type;
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

    private ICompanyGeoResolver createGeoResolver(BigDecimal longitude, BigDecimal latitude) {
        return new ICompanyGeoResolver() {
            @Override
            public GeoLocation resolve(String address) {
                return new GeoLocation(longitude, latitude);
            }
        };
    }

    private SysCompanyMapper createCompanyMapperProxy(CompanyMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return 0L;
                }
                if ("insert".equals(method.getName())) {
                    SysCompany entity = (SysCompany) args[0];
                    entity.setId(state.nextId++);
                    state.insertedCompany = entity;
                    state.companyStore.put(entity.getId(), entity);
                    return 1;
                }
                if ("selectById".equals(method.getName())) {
                    return state.companyStore.get(args[0]);
                }
                if ("updateById".equals(method.getName())) {
                    SysCompany entity = (SysCompany) args[0];
                    state.companyStore.put(entity.getId(), entity);
                    return 1;
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

    private SysRoleTemplateMapper createRoleTemplateMapperProxy(Long count) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return count;
                }
                if ("selectList".equals(method.getName())) {
                    List<SysRoleTemplate> result = new ArrayList<>();
                    SysRoleTemplate template = new SysRoleTemplate();
                    template.setId(1L);
                    result.add(template);
                    return result;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysRoleTemplateMapper) Proxy.newProxyInstance(
                SysRoleTemplateMapper.class.getClassLoader(),
                new Class<?>[]{SysRoleTemplateMapper.class},
                handler
        );
    }

    private SysUserMapper createUserMapperProxy(UserMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return 0L;
                }
                if ("insert".equals(method.getName())) {
                    SysUser entity = (SysUser) args[0];
                    entity.setId(state.nextId++);
                    state.insertCount++;
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    private <T> T createNoopMapperProxy(Class<T> mapperClass) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return defaultValue(method.getReturnType());
            }
        };
        return mapperClass.cast(Proxy.newProxyInstance(
                mapperClass.getClassLoader(),
                new Class<?>[]{mapperClass},
                handler
        ));
    }

    private ISysRoleTemplateService createRoleTemplateService() {
        return new ISysRoleTemplateService() {
            @Override
            public List<com.jasic.aftersales.system.domain.vo.SysRoleTemplateVO> listByTypeCode(String typeCode) {
                return Collections.emptyList();
            }

            @Override
            public com.jasic.aftersales.system.domain.vo.SysRoleTemplateVO getById(Long templateId) {
                return null;
            }

            @Override
            public Long save(com.jasic.aftersales.system.domain.dto.SysRoleTemplateDTO dto) {
                return null;
            }

            @Override
            public void update(com.jasic.aftersales.system.domain.dto.SysRoleTemplateDTO dto) {
            }

            @Override
            public void remove(Long templateId) {
            }

            @Override
            public void syncToCompanies(Long templateId) {
            }

            @Override
            public Long initCompanyRoles(Long companyId, String typeCode) {
                return 11L;
            }
        };
    }

    private ISysConfigService createConfigService() {
        return new ISysConfigService() {
            @Override
            public com.jasic.aftersales.common.core.domain.PageResult<com.jasic.aftersales.system.domain.vo.SysConfigVO> listPage(com.jasic.aftersales.system.domain.query.SysConfigQuery query) {
                return null;
            }

            @Override
            public com.jasic.aftersales.system.domain.vo.SysConfigVO getById(Long id) {
                return null;
            }

            @Override
            public Long save(com.jasic.aftersales.system.domain.dto.SysConfigDTO dto) {
                return null;
            }

            @Override
            public void update(com.jasic.aftersales.system.domain.dto.SysConfigDTO dto) {
            }

            @Override
            public void remove(Long id) {
            }

            @Override
            public String getValueByKey(String configKey) {
                return null;
            }

            @Override
            public void refreshCache() {
            }
        };
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SysCompanyServiceImpl.class.getDeclaredField(fieldName);
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

    private static class CompanyMapperState {
        private long nextId = 1L;
        private SysCompany insertedCompany;
        private final Map<Long, SysCompany> companyStore = new LinkedHashMap<>();
    }

    private static class UserMapperState {
        private long nextId = 1L;
        private int insertCount;
    }
}
