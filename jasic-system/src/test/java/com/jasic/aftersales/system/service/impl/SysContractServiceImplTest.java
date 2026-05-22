package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.CrmFirstSecondRelationImportDTO;
import com.jasic.aftersales.system.domain.dto.CrmHqFirstContractImportDTO;
import com.jasic.aftersales.system.domain.entity.CrmBizCompanySnapshot;
import com.jasic.aftersales.system.domain.entity.CrmFirstSecondRelationSnapshot;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.entity.CrmHqFirstContractSnapshot;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelationRecord;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.HqFirstContractRecord;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.domain.query.CrmFirstSecondRelationImportQuery;
import com.jasic.aftersales.system.domain.query.CrmHqFirstContractImportQuery;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationImportResultVO;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationImportVO;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractImportResultVO;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractImportVO;
import com.jasic.aftersales.system.mapper.CrmBizCompanySnapshotMapper;
import com.jasic.aftersales.system.mapper.CrmFirstSecondRelationSnapshotMapper;
import com.jasic.aftersales.system.mapper.CrmHqFirstContractSnapshotMapper;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.FirstSecondRelationRecordMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractRecordMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRegionMapper;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
 * @author Zoro
 * @date 2026/04/02
 */
public class SysContractServiceImplTest {

    /**setUpSecurityContext 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @Before
    public void setUpSecurityContext() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        SecurityContext.setCurrentSubjectType("PLATFORM");
        SecurityContext.setCurrentTypeCode("PLATFORM");
    }

    /**tearDownSecurityContext 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @After
    public void tearDownSecurityContext() {
        try {
            StpUtil.logout();
        } catch (Exception ignored) {
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    /**验证RejectNonHqCompanyWhenSavingHqFirst，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectNonHqCompanyWhenSavingHqFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "FIRST", 1));
        companies.put(2L, buildCompany(2L, "FIRST", 1));

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());

        HqFirstContractDTO dto = new HqFirstContractDTO();
        dto.setTargetCompanyId(1L);
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

    /**验证RejectRegionOutOfHqWhenSavingHqFirst，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectRegionOutOfHqWhenSavingHqFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "HQ_A", 1));
        companies.put(2L, buildCompany(2L, "SITE_FIRST", 1));
        Map<Long, SysRegion> regions = new LinkedHashMap<>();
        SysRegion region = new SysRegion();
        region.setId(10L);
        region.setCompanyId(99L);
        regions.put(10L, region);

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "sysRegionMapper", createRegionMapperProxy(regions));
        setField(service, "companyTypeService", createCompanyTypeService());

        HqFirstContractDTO dto = new HqFirstContractDTO();
        dto.setTargetCompanyId(1L);
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

    /**验证RejectSecondCompanyAlreadyBoundToOtherFirst，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectSecondCompanyAlreadyBoundToOtherFirst() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(9L, buildCompany(9L, "HQ_A", 1));
        companies.put(1L, buildCompany(1L, "SITE_FIRST", 1));
        companies.put(2L, buildCompany(2L, "SITE_SECOND", 1));
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        contractState.selectCountResults.add(1L);
        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        relationState.selectCountResults.add(0L);
        FirstSecondRelation existing = new FirstSecondRelation();
        existing.setId(99L);
        existing.setFirstCompanyId(8L);
        existing.setSecondCompanyId(2L);
        relationState.selectOneResult = existing;

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));

        FirstSecondRelationDTO dto = new FirstSecondRelationDTO();
        dto.setTargetCompanyId(9L);
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

    /**验证TranslateDuplicateKeyWhenSavingFirstSecond，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldTranslateDuplicateKeyWhenSavingFirstSecond() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(9L, buildCompany(9L, "HQ_A", 1));
        companies.put(1L, buildCompany(1L, "SITE_FIRST", 1));
        companies.put(2L, buildCompany(2L, "SITE_SECOND", 1));
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        contractState.selectCountResults.add(1L);
        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        relationState.selectCountResults.add(0L);
        relationState.insertException = new DuplicateKeyException("Duplicate entry for key 'uk_second'");

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));

        FirstSecondRelationDTO dto = new FirstSecondRelationDTO();
        dto.setTargetCompanyId(9L);
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

    /**验证RecordDeleteSnapshotWhenRemovingHqFirst，保证相关业务规则在回归场景下保持稳定。*/
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
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(1L, buildCompany(1L, "HQ_A", 1));

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "hqFirstContractRecordMapper", createHqFirstRecordMapperProxy(recordHolder));

        service.removeHqFirst(7L, 1L);

        Assert.assertEquals(Long.valueOf(7L), contractState.deletedId);
        Assert.assertNotNull(recordHolder.record);
        Assert.assertEquals(Long.valueOf(7L), recordHolder.record.getSourceId());
        Assert.assertEquals("DELETE", recordHolder.record.getOperationType());
    }

    /**验证RecordDeleteSnapshotWhenRemovingFirstSecond，保证相关业务规则在回归场景下保持稳定。*/
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
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(9L, buildCompany(9L, "HQ_A", 1));
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        contractState.selectCountResults.add(1L);

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));
        setField(service, "firstSecondRelationRecordMapper", createFirstSecondRecordMapperProxy(recordHolder));

        service.removeFirstSecond(8L, 9L);

        Assert.assertEquals(Long.valueOf(8L), relationState.deletedId);
        Assert.assertNotNull(recordHolder.record);
        Assert.assertEquals(Long.valueOf(8L), recordHolder.record.getSourceId());
        Assert.assertEquals("DELETE", recordHolder.record.getOperationType());
    }

    /**验证ListOnlyImportableRowsWhenListingCrmImportPage，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldListOnlyImportableRowsWhenListingCrmImportPage() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        SysCompany hqCompany = buildCompany(1L, "HQ_A", 1);
        hqCompany.setCompanyCode("HQ001");
        hqCompany.setSalesOrg("1000");
        companies.put(1L, hqCompany);
        SysCompany firstCompany = buildCompany(2L, "SITE_FIRST", 1);
        firstCompany.setCompanyCode("K001");
        firstCompany.setCompanyName("一级公司A");
        companies.put(2L, firstCompany);

        Map<Long, SysRegion> regions = new LinkedHashMap<>();
        SysRegion region = new SysRegion();
        region.setId(10L);
        region.setCompanyId(1L);
        region.setRegionCode("R001");
        region.setRegionName("华东一区");
        regions.put(10L, region);

        CrmHqFirstContractSnapshotMapperState snapshotState = new CrmHqFirstContractSnapshotMapperState();
        snapshotState.selectListResult = Arrays.asList(
                buildSnapshot(100L, "K001", "1000", "R001"),
                buildSnapshot(101L, "K999", "1000", "R002")
        );
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "sysRegionMapper", createRegionMapperProxy(regions));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "crmHqFirstContractSnapshotMapper", createSnapshotMapperProxy(snapshotState));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));

        CrmHqFirstContractImportQuery query = new CrmHqFirstContractImportQuery();
        query.setTargetCompanyId(1L);
        query.setHqCompanyId(1L);
        query.setShowAbnormal(false);
        query.setPageNum(1);
        query.setPageSize(10);

        PageResult<CrmHqFirstContractImportVO> page = service.listCrmHqFirstImportPage(query);

        Assert.assertEquals(Long.valueOf(1L), page.getTotal());
        Assert.assertEquals(1, page.getRecords().size());
        Assert.assertEquals(Long.valueOf(100L), page.getRecords().get(0).getId());
        Assert.assertEquals(Boolean.TRUE, page.getRecords().get(0).getCanImport());
    }

    /**验证CountSuccessExistingAndFailedWhenImportingFromCrm，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldCountSuccessExistingAndFailedWhenImportingFromCrm() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        SysCompany hqCompany = buildCompany(1L, "HQ_A", 1);
        hqCompany.setCompanyCode("HQ001");
        hqCompany.setSalesOrg("1000");
        companies.put(1L, hqCompany);

        SysCompany importableCompany = buildCompany(2L, "SITE_FIRST", 1);
        importableCompany.setCompanyCode("K001");
        importableCompany.setCompanyName("一级公司A");
        companies.put(2L, importableCompany);

        SysCompany existedCompany = buildCompany(3L, "SITE_FIRST", 1);
        existedCompany.setCompanyCode("K002");
        existedCompany.setCompanyName("一级公司B");
        companies.put(3L, existedCompany);

        SysCompany failedCompany = buildCompany(4L, "SITE_FIRST", 1);
        failedCompany.setCompanyCode("K003");
        failedCompany.setCompanyName("一级公司C");
        companies.put(4L, failedCompany);

        Map<Long, SysRegion> regions = new LinkedHashMap<>();
        SysRegion importableRegion = new SysRegion();
        importableRegion.setId(10L);
        importableRegion.setCompanyId(1L);
        importableRegion.setRegionCode("R001");
        importableRegion.setRegionName("华东一区");
        regions.put(10L, importableRegion);

        SysRegion foreignRegion = new SysRegion();
        foreignRegion.setId(11L);
        foreignRegion.setCompanyId(99L);
        foreignRegion.setRegionCode("R002");
        foreignRegion.setRegionName("外部大区");
        regions.put(11L, foreignRegion);

        CrmHqFirstContractSnapshotMapperState snapshotState = new CrmHqFirstContractSnapshotMapperState();
        snapshotState.selectListResult = Arrays.asList(
                buildSnapshot(100L, "K001", "1000", "R001"),
                buildSnapshot(101L, "K002", "1000", "R001"),
                buildSnapshot(102L, "K003", "1000", "R002")
        );
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        HqFirstContract existingContract = new HqFirstContract();
        existingContract.setFirstCompanyId(3L);
        contractState.selectListResult = Collections.singletonList(existingContract);
        contractState.selectCountResults.add(0L);

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "sysRegionMapper", createRegionMapperProxy(regions));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "crmHqFirstContractSnapshotMapper", createSnapshotMapperProxy(snapshotState));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));

        CrmHqFirstContractImportDTO dto = new CrmHqFirstContractImportDTO();
        dto.setTargetCompanyId(1L);
        dto.setHqCompanyId(1L);
        dto.setSnapshotIds(Arrays.asList(100L, 101L, 102L));

        CrmHqFirstContractImportResultVO result = service.importHqFirstFromCrm(dto);

        Assert.assertEquals(Integer.valueOf(3), result.getSelectedCount());
        Assert.assertEquals(Integer.valueOf(1), result.getSuccessCount());
        Assert.assertEquals(Integer.valueOf(1), result.getExistedCount());
        Assert.assertEquals(Integer.valueOf(1), result.getFailedCount());
        Assert.assertNotNull(contractState.insertedEntity);
        Assert.assertEquals(Long.valueOf(2L), contractState.insertedEntity.getFirstCompanyId());
        Assert.assertEquals(Long.valueOf(10L), contractState.insertedEntity.getRegionId());
        Assert.assertEquals("CRM导入初始化", contractState.insertedEntity.getRemark());
    }

    /**验证ListOnlyImportableFirstSecondRowsWhenListingCrmImportPage，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldListOnlyImportableFirstSecondRowsWhenListingCrmImportPage() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(99L, buildCompany(99L, "HQ_A", 1));
        SysCompany firstCompany = buildCompany(1L, "SITE_FIRST", 1);
        firstCompany.setCompanyCode("F001");
        firstCompany.setCompanyName("First-A");
        companies.put(1L, firstCompany);
        SysCompany secondCompany = buildCompany(10L, "SITE_SECOND", 1);
        secondCompany.setCompanyCode("S001");
        secondCompany.setCompanyName("Second-A");
        companies.put(10L, secondCompany);

        CrmFirstSecondRelationSnapshotMapperState relationSnapshotState = new CrmFirstSecondRelationSnapshotMapperState();
        relationSnapshotState.selectListResult = Arrays.asList(
                buildFirstSecondSnapshot(500L, 100L, 200L),
                buildFirstSecondSnapshot(501L, 101L, 201L)
        );

        CrmBizCompanySnapshotMapperState companySnapshotState = new CrmBizCompanySnapshotMapperState();
        companySnapshotState.selectListResult = Arrays.asList(
                buildCrmCompanySnapshot(100L, "F001", "CRM-First-A", 0),
                buildCrmCompanySnapshot(200L, "S001", "CRM-Second-A", 3),
                buildCrmCompanySnapshot(101L, "F999", "CRM-First-B", 0),
                buildCrmCompanySnapshot(201L, "S999", "CRM-Second-B", 3)
        );

        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        relationState.selectListResult = Collections.emptyList();
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        HqFirstContract allowedContract = new HqFirstContract();
        allowedContract.setHqCompanyId(99L);
        allowedContract.setFirstCompanyId(1L);
        allowedContract.setStatus(1);
        contractState.selectListResult = Collections.singletonList(allowedContract);

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "crmFirstSecondRelationSnapshotMapper", createFirstSecondSnapshotMapperProxy(relationSnapshotState));
        setField(service, "crmBizCompanySnapshotMapper", createCrmBizCompanySnapshotMapperProxy(companySnapshotState));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));

        CrmFirstSecondRelationImportQuery query = new CrmFirstSecondRelationImportQuery();
        query.setTargetCompanyId(99L);
        query.setShowAbnormal(false);
        query.setPageNum(1);
        query.setPageSize(10);

        PageResult<CrmFirstSecondRelationImportVO> page = service.listCrmFirstSecondImportPage(query);

        Assert.assertEquals(Long.valueOf(1L), page.getTotal());
        Assert.assertEquals(1, page.getRecords().size());
        Assert.assertEquals(Long.valueOf(500L), page.getRecords().get(0).getId());
        Assert.assertEquals(Boolean.TRUE, page.getRecords().get(0).getCanImport());
    }

    /**验证CountSuccessExistingConflictAndFailedWhenImportingFirstSecondFromCrm，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldCountSuccessExistingConflictAndFailedWhenImportingFirstSecondFromCrm() throws Exception {
        SysContractServiceImpl service = new SysContractServiceImpl();
        Map<Long, SysCompany> companies = new LinkedHashMap<>();
        companies.put(99L, buildCompany(99L, "HQ_A", 1));
        SysCompany firstCompanyA = buildCompany(1L, "SITE_FIRST", 1);
        firstCompanyA.setCompanyCode("F001");
        firstCompanyA.setCompanyName("First-A");
        companies.put(1L, firstCompanyA);
        SysCompany firstCompanyB = buildCompany(2L, "SITE_FIRST", 1);
        firstCompanyB.setCompanyCode("F002");
        firstCompanyB.setCompanyName("First-B");
        companies.put(2L, firstCompanyB);
        SysCompany secondCompanyA = buildCompany(10L, "SITE_SECOND", 1);
        secondCompanyA.setCompanyCode("S001");
        secondCompanyA.setCompanyName("Second-A");
        companies.put(10L, secondCompanyA);
        SysCompany secondCompanyB = buildCompany(11L, "SITE_SECOND", 1);
        secondCompanyB.setCompanyCode("S002");
        secondCompanyB.setCompanyName("Second-B");
        companies.put(11L, secondCompanyB);
        SysCompany secondCompanyC = buildCompany(12L, "SITE_SECOND", 1);
        secondCompanyC.setCompanyCode("S003");
        secondCompanyC.setCompanyName("Second-C");
        companies.put(12L, secondCompanyC);

        CrmFirstSecondRelationSnapshotMapperState relationSnapshotState = new CrmFirstSecondRelationSnapshotMapperState();
        relationSnapshotState.selectListResult = Arrays.asList(
                buildFirstSecondSnapshot(500L, 100L, 200L),
                buildFirstSecondSnapshot(501L, 100L, 201L),
                buildFirstSecondSnapshot(502L, 100L, 202L),
                buildFirstSecondSnapshot(503L, 100L, 203L)
        );

        CrmBizCompanySnapshotMapperState companySnapshotState = new CrmBizCompanySnapshotMapperState();
        companySnapshotState.selectListResult = Arrays.asList(
                buildCrmCompanySnapshot(100L, "F001", "CRM-First-A", 0),
                buildCrmCompanySnapshot(200L, "S001", "CRM-Second-A", 3),
                buildCrmCompanySnapshot(201L, "S002", "CRM-Second-B", 3),
                buildCrmCompanySnapshot(202L, "S003", "CRM-Second-C", 3),
                buildCrmCompanySnapshot(203L, "S004", "CRM-Second-D", 3)
        );

        FirstSecondRelation existingRelation = new FirstSecondRelation();
        existingRelation.setFirstCompanyId(1L);
        existingRelation.setSecondCompanyId(11L);
        FirstSecondRelation conflictRelation = new FirstSecondRelation();
        conflictRelation.setFirstCompanyId(2L);
        conflictRelation.setSecondCompanyId(12L);
        FirstSecondRelationMapperState relationState = new FirstSecondRelationMapperState();
        relationState.selectListResult = Arrays.asList(existingRelation, conflictRelation);
        relationState.selectCountResults.add(0L);
        HqFirstContractMapperState contractState = new HqFirstContractMapperState();
        HqFirstContract allowedContract = new HqFirstContract();
        allowedContract.setHqCompanyId(99L);
        allowedContract.setFirstCompanyId(1L);
        allowedContract.setStatus(1);
        contractState.selectListResult = Collections.singletonList(allowedContract);
        contractState.selectCountResults.add(1L);

        setField(service, "sysCompanyMapper", createCompanyMapperProxy(companies));
        setField(service, "companyTypeService", createCompanyTypeService());
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(contractState));
        setField(service, "crmFirstSecondRelationSnapshotMapper", createFirstSecondSnapshotMapperProxy(relationSnapshotState));
        setField(service, "crmBizCompanySnapshotMapper", createCrmBizCompanySnapshotMapperProxy(companySnapshotState));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy(relationState));

        CrmFirstSecondRelationImportDTO dto = new CrmFirstSecondRelationImportDTO();
        dto.setTargetCompanyId(99L);
        dto.setSnapshotIds(Arrays.asList(500L, 501L, 502L, 503L));

        CrmFirstSecondRelationImportResultVO result = service.importFirstSecondFromCrm(dto);

        Assert.assertEquals(Integer.valueOf(4), result.getSelectedCount());
        Assert.assertEquals(Integer.valueOf(1), result.getSuccessCount());
        Assert.assertEquals(Integer.valueOf(1), result.getExistedCount());
        Assert.assertEquals(Integer.valueOf(1), result.getConflictCount());
        Assert.assertEquals(Integer.valueOf(1), result.getFailedCount());
        Assert.assertNotNull(relationState.insertedEntity);
        Assert.assertEquals(Long.valueOf(1L), relationState.insertedEntity.getFirstCompanyId());
        Assert.assertEquals(Long.valueOf(10L), relationState.insertedEntity.getSecondCompanyId());
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
@param companies 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
    private SysCompanyMapper createCompanyMapperProxy(Map<Long, SysCompany> companies) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return companies.get(args[0]);
                }
                if ("selectList".equals(method.getName())) {
                    return Arrays.asList(companies.values().toArray(new SysCompany[0]));
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

    /**createRegionMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param regions 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
    private SysRegionMapper createRegionMapperProxy(Map<Long, SysRegion> regions) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return regions.get(args[0]);
                }
                if ("selectList".equals(method.getName())) {
                    return Arrays.asList(regions.values().toArray(new SysRegion[0]));
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

    /**createHqFirstContractMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private HqFirstContractMapper createHqFirstContractMapperProxy(HqFirstContractMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return state.selectByIdResult;
                }
                if ("selectCount".equals(method.getName())) {
                    return state.selectCountResults.isEmpty() ? 0L : state.selectCountResults.poll();
                }
                if ("selectList".equals(method.getName())) {
                    return state.selectListResult;
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

    /**createSnapshotMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private CrmHqFirstContractSnapshotMapper createSnapshotMapperProxy(CrmHqFirstContractSnapshotMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return state.selectListResult;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CrmHqFirstContractSnapshotMapper) Proxy.newProxyInstance(
                CrmHqFirstContractSnapshotMapper.class.getClassLoader(),
                new Class<?>[]{CrmHqFirstContractSnapshotMapper.class},
                handler
        );
    }

    /**createFirstSecondSnapshotMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private CrmFirstSecondRelationSnapshotMapper createFirstSecondSnapshotMapperProxy(CrmFirstSecondRelationSnapshotMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return state.selectListResult;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CrmFirstSecondRelationSnapshotMapper) Proxy.newProxyInstance(
                CrmFirstSecondRelationSnapshotMapper.class.getClassLoader(),
                new Class<?>[]{CrmFirstSecondRelationSnapshotMapper.class},
                handler
        );
    }

    /**createCrmBizCompanySnapshotMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private CrmBizCompanySnapshotMapper createCrmBizCompanySnapshotMapperProxy(CrmBizCompanySnapshotMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return state.selectListResult;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CrmBizCompanySnapshotMapper) Proxy.newProxyInstance(
                CrmBizCompanySnapshotMapper.class.getClassLoader(),
                new Class<?>[]{CrmBizCompanySnapshotMapper.class},
                handler
        );
    }

    /**createFirstSecondRelationMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private FirstSecondRelationMapper createFirstSecondRelationMapperProxy(FirstSecondRelationMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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
                if ("selectList".equals(method.getName())) {
                    return state.selectListResult;
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

    /**createHqFirstRecordMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param holder holder 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private HqFirstContractRecordMapper createHqFirstRecordMapperProxy(HqFirstContractRecordHolder holder) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**createFirstSecondRecordMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param holder holder 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private FirstSecondRelationRecordMapper createFirstSecondRecordMapperProxy(FirstSecondRelationRecordHolder holder) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**createCompanyTypeService 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private ISysCompanyTypeService createCompanyTypeService() {
        List<SysCompanyType> companyTypes = Arrays.asList(
                buildCompanyType("HQ_A", "HQ"),
                buildCompanyType("SITE_FIRST", "SERVICE"),
                buildCompanyType("SITE_SECOND", "SERVICE")
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

    /**buildSnapshot 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param kunnr kunnr 字段参数。
@param salesOrg salesOrg 字段参数。
@param regionCode 业务编码，用于匹配枚举、配置或外部系统数据。
@return 处理后的业务结果。*/
    private CrmHqFirstContractSnapshot buildSnapshot(Long id, String kunnr, String salesOrg, String regionCode) {
        CrmHqFirstContractSnapshot snapshot = new CrmHqFirstContractSnapshot();
        snapshot.setId(id);
        snapshot.setKunnr(kunnr);
        snapshot.setSalesOrg(salesOrg);
        snapshot.setRegionCode(regionCode);
        snapshot.setCrmCompanyName("CRM-" + kunnr);
        return snapshot;
    }

    /**buildFirstSecondSnapshot 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param firstCustId firstCustId 字段。
@param secondCustId secondCustId 字段。
@return 处理后的业务结果。*/
    private CrmFirstSecondRelationSnapshot buildFirstSecondSnapshot(Long id, Long firstCustId, Long secondCustId) {
        CrmFirstSecondRelationSnapshot snapshot = new CrmFirstSecondRelationSnapshot();
        snapshot.setId(id);
        snapshot.setFirstCustId(firstCustId);
        snapshot.setSecondCustId(secondCustId);
        return snapshot;
    }

    /**buildCrmCompanySnapshot 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param custId custId 字段。
@param companyCode 业务编码，用于匹配枚举、配置或外部系统数据。
@param companyName 名称文本，用于展示、匹配或保存业务对象名称。
@param custRage custRage 字段参数。
@return 处理后的业务结果。*/
    private CrmBizCompanySnapshot buildCrmCompanySnapshot(Long custId, String companyCode, String companyName, Integer custRage) {
        CrmBizCompanySnapshot snapshot = new CrmBizCompanySnapshot();
        snapshot.setCustId(custId);
        snapshot.setSapCompanyCode(companyCode);
        snapshot.setCustName(companyName);
        snapshot.setCustRage(custRage);
        return snapshot;
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = SysContractServiceImpl.class.getDeclaredField(fieldName);
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

    /**HqFirstContractMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class HqFirstContractMapperState {
        /**selectCountResults 字段，用于当前类内部业务处理。*/
        private final Queue<Long> selectCountResults = new ArrayDeque<>();
        /**selectByIdResult 字段，用于当前类内部业务处理。*/
        private HqFirstContract selectByIdResult;
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<HqFirstContract> selectListResult = Collections.emptyList();
        /**insertException 字段，用于当前类内部业务处理。*/
        private DuplicateKeyException insertException;
        /**insertedEntity 字段，用于当前类内部业务处理。*/
        private HqFirstContract insertedEntity;
        /**updatedEntity 字段，用于当前类内部业务处理。*/
        private HqFirstContract updatedEntity;
        /**deletedId 字段，用于当前类内部业务处理。*/
        private Long deletedId;
    }

    /**CrmHqFirstContractSnapshotMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class CrmHqFirstContractSnapshotMapperState {
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<CrmHqFirstContractSnapshot> selectListResult = Collections.emptyList();
    }

    /**CrmFirstSecondRelationSnapshotMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class CrmFirstSecondRelationSnapshotMapperState {
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<CrmFirstSecondRelationSnapshot> selectListResult = Collections.emptyList();
    }

    /**CrmBizCompanySnapshotMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class CrmBizCompanySnapshotMapperState {
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<CrmBizCompanySnapshot> selectListResult = Collections.emptyList();
    }

    /**FirstSecondRelationMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class FirstSecondRelationMapperState {
        /**selectCountResults 字段，用于当前类内部业务处理。*/
        private final Queue<Long> selectCountResults = new ArrayDeque<>();
        /**selectByIdResult 字段，用于当前类内部业务处理。*/
        private FirstSecondRelation selectByIdResult;
        /**selectOneResult 字段，用于当前类内部业务处理。*/
        private FirstSecondRelation selectOneResult;
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<FirstSecondRelation> selectListResult = Collections.emptyList();
        /**insertException 字段，用于当前类内部业务处理。*/
        private DuplicateKeyException insertException;
        /**insertedEntity 字段，用于当前类内部业务处理。*/
        private FirstSecondRelation insertedEntity;
        /**deletedId 字段，用于当前类内部业务处理。*/
        private Long deletedId;
    }

    /**HqFirstContractRecordHolder 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class HqFirstContractRecordHolder {
        /**record 字段，用于当前类内部业务处理。*/
        private HqFirstContractRecord record;
    }

    /**FirstSecondRelationRecordHolder 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class FirstSecondRelationRecordHolder {
        /**record 字段，用于当前类内部业务处理。*/
        private FirstSecondRelationRecord record;
    }
}
