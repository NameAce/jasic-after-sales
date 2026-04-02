package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelationRecord;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.HqFirstContractRecord;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.FirstSecondRelationRecordMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractRecordMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRegionMapper;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 签约管理服务测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class SysContractServiceImplTest {

    @Test
    public void shouldRejectNonHqCompanyWhenSavingHqFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "FIRST", 1));
        companies.put(2L, buildCompany(2L, "FIRST", 1));

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());

        HqFirstContractDTO dto = new HqFirstContractDTO();
        dto.setHqCompanyId(1L);
        dto.setFirstCompanyId(2L);
        dto.setStatus(1);

        try {
            service.saveHqFirst(dto);
            Assert.fail("预期应拒绝非总部公司");
        } catch (ServiceException ex) {
            Assert.assertEquals("总部公司必须是总部类型", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectRegionOutOfHqWhenSavingHqFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "HQ_A", 1));
        companies.put(2L, buildCompany(2L, "FIRST", 1));
        Map<Long, SysRegion> regions = new LinkedHashMap<>();
        SysRegion region = new SysRegion();
        region.setId(10L);
        region.setCompanyId(99L);
        regions.put(10L, region);

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "sysRegionMapper", createRegionMapperProxy(regions));
        setField(service, "companyTypeService", createCompanyTypeService());

        HqFirstContractDTO dto = new HqFirstContractDTO();
        dto.setHqCompanyId(1L);
        dto.setFirstCompanyId(2L);
        dto.setRegionId(10L);
        dto.setStatus(1);

        try {
            service.saveHqFirst(dto);
            Assert.fail("预期应拒绝错误大区归属");
        } catch (ServiceException ex) {
            Assert.assertEquals("所属大区不属于当前总部", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectSecondCompanyAlreadyBoundToOtherFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "FIRST", 1));
        companies.put(2L, buildCompany(2L, "SECOND", 1));
        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        relationState.selectCountResults.add(0L);
        FirstSecondRelation existing = new FirstSecondRelation();
        existing.setId(99L);
        existing.setFirstCompanyId(8L);
        existing.setSecondCompanyId(2L);
        relationState.selectOneResult = existing;

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));

        FirstSecondRelationDTO dto = new FirstSecondRelationDTO();
        dto.setFirstCompanyId(1L);
        dto.setSecondCompanyId(2L);
        dto.setStatus(1);

        try {
            service.saveFirstSecond(dto);
            Assert.fail("预期应拒绝重复挂接二级网点");
        } catch (ServiceException ex) {
            Assert.assertEquals("该二级网点已归属其他一级网点", ex.getMessage());
        }
    }

    @Test
    public void shouldTranslateDuplicateKeyWhenSavingFirstSecond() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "FIRST", 1));
        companies.put(2L, buildCompany(2L, "SECOND", 1));
        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        relationState.selectCountResults.add(0L);
        relationState.insertException = new DuplicateKeyException("Duplicate entry for key 'uk_second'");

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));

        FirstSecondRelationDTO dto = new FirstSecondRelationDTO();
        dto.setFirstCompanyId(1L);
        dto.setSecondCompanyId(2L);
        dto.setStatus(1);

        try {
            service.saveFirstSecond(dto);
            Assert.fail("预期应转换唯一索引异常");
        } catch (ServiceException ex) {
            Assert.assertEquals("该二级网点已归属其他一级网点", ex.getMessage());
        }
    }

    @Test
    public void shouldRecordDeleteSnapshotWhenRemovingHqFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        HqFirstContract entity = new HqFirstContract();
        entity.setId(7L);
        entity.setHqCompanyId(1L);
        entity.setFirstCompanyId(2L);
        entity.setRegionId(3L);
        entity.setContractNo("HT-001");
        entity.setStatus(1);
        entity.setRemark("备注");
        contractState.selectByIdResult = entity;
        HqFirstContractRecordHolder recordHolder = new HqFirstContractRecordHolder();

        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "hqFirstContractRecordMapper", createHqFirstRecordMapperProxy(recordHolder));

        service.removeHqFirst(7L);

        Assert.assertEquals(Long.valueOf(7L), contractState.deletedId);
        Assert.assertNotNull(recordHolder.record);
        Assert.assertEquals(Long.valueOf(7L), recordHolder.record.getSourceId());
        Assert.assertEquals("DELETE", recordHolder.record.getOperationType());
    }

    @Test
    public void shouldRecordDeleteSnapshotWhenRemovingFirstSecond() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        FirstSecondRelation entity = new FirstSecondRelation();
        entity.setId(8L);
        entity.setFirstCompanyId(1L);
        entity.setSecondCompanyId(2L);
        entity.setStatus(1);
        entity.setRemark("从属备注");
        relationState.selectByIdResult = entity;
        FirstSecondRelationRecordHolder recordHolder = new FirstSecondRelationRecordHolder();

        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));
        setField(service, "firstSecondRelationRecordMapper", createFirstSecondRecordMapperProxy(recordHolder));

        service.removeFirstSecond(8L);

        Assert.assertEquals(Long.valueOf(8L), relationState.deletedId);
        Assert.assertNotNull(recordHolder.record);
        Assert.assertEquals(Long.valueOf(8L), recordHolder.record.getSourceId());
        Assert.assertEquals("DELETE", recordHolder.record.getOperationType());
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

    private SysRegionMapper createRegionMapperProxy(Map<Long, SysRegion> regions) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return regions.get(args[0]);
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

    private HqFirstContractMapper createHqFirstContractMapperProxy(HqFirstContractMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return state.selectByIdResult;
                }
                if ("selectCount".equals(method.getName())) {
                    return state.selectCountResults.isEmpty() ? 0L : state.selectCountResults.poll();
                }
                if ("insert".equals(method.getName())) {
                    if (state.insertException != null) {
                        throw state.insertException;
                    }
                    HqFirstContract entity = (HqFirstContract) args[0];
                    entity.setId(1L);
                    state.insertedEntity = entity;
                    return 1;
                }
                if ("updateById".equals(method.getName())) {
                    state.updatedEntity = (HqFirstContract) args[0];
                    return 1;
                }
                if ("deleteById".equals(method.getName())) {
                    state.deletedId = (Long) args[0];
                    return 1;
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

    private FirstSecondRelationMapper createFirstSecondRelationMapperProxy(FirstSecondRelationMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return state.selectByIdResult;
                }
                if ("selectCount".equals(method.getName())) {
                    return state.selectCountResults.isEmpty() ? 0L : state.selectCountResults.poll();
                }
                if ("selectOne".equals(method.getName())) {
                    return state.selectOneResult;
                }
                if ("insert".equals(method.getName())) {
                    if (state.insertException != null) {
                        throw state.insertException;
                    }
                    FirstSecondRelation entity = (FirstSecondRelation) args[0];
                    entity.setId(1L);
                    state.insertedEntity = entity;
                    return 1;
                }
                if ("deleteById".equals(method.getName())) {
                    state.deletedId = (Long) args[0];
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FirstSecondRelationMapper) Proxy.newProxyInstance(
                FirstSecondRelationMapper.class.getClassLoader(),
                new Class<?>[]{FirstSecondRelationMapper.class},
                handler
        );
    }

    private HqFirstContractRecordMapper createHqFirstRecordMapperProxy(HqFirstContractRecordHolder holder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName())) {
                    holder.record = (HqFirstContractRecord) args[0];
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (HqFirstContractRecordMapper) Proxy.newProxyInstance(
                HqFirstContractRecordMapper.class.getClassLoader(),
                new Class<?>[]{HqFirstContractRecordMapper.class},
                handler
        );
    }

    private FirstSecondRelationRecordMapper createFirstSecondRecordMapperProxy(FirstSecondRelationRecordHolder holder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName())) {
                    holder.record = (FirstSecondRelationRecord) args[0];
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FirstSecondRelationRecordMapper) Proxy.newProxyInstance(
                FirstSecondRelationRecordMapper.class.getClassLoader(),
                new Class<?>[]{FirstSecondRelationRecordMapper.class},
                handler
        );
    }

    private ISysCompanyTypeService createCompanyTypeService() {
        List<SysCompanyType> companyTypes = Arrays.asList(
                buildCompanyType("HQ_A", "HQ"),
                buildCompanyType("FIRST", "SERVICE"),
                buildCompanyType("SECOND", "SERVICE")
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
        Field field = SysContractServiceImpl.class.getDeclaredField(fieldName);
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

    private static class HqFirstContractMapperState {
        private final Queue<Long> selectCountResults = new ArrayDeque<>();
        private HqFirstContract selectByIdResult;
        private DuplicateKeyException insertException;
        private HqFirstContract insertedEntity;
        private HqFirstContract updatedEntity;
        private Long deletedId;
    }

    private static class FirstSecondRelationMapperState {
        private final Queue<Long> selectCountResults = new ArrayDeque<>();
        private FirstSecondRelation selectByIdResult;
        private FirstSecondRelation selectOneResult;
        private DuplicateKeyException insertException;
        private FirstSecondRelation insertedEntity;
        private Long deletedId;
    }

    private static class HqFirstContractRecordHolder {
        private HqFirstContractRecord record;
    }

    private static class FirstSecondRelationRecordHolder {
        private FirstSecondRelationRecord record;
    }
}
