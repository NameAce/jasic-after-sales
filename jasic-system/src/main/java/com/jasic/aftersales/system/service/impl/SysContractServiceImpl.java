package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.CompanyCategoryEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.CrmFirstSecondRelationImportDTO;
import com.jasic.aftersales.system.domain.dto.CrmHqFirstContractImportDTO;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.entity.CrmBizCompanySnapshot;
import com.jasic.aftersales.system.domain.entity.CrmFirstSecondRelationSnapshot;
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
import com.jasic.aftersales.system.domain.query.FirstSecondRelationQuery;
import com.jasic.aftersales.system.domain.query.HqFirstContractQuery;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationImportResultVO;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationImportVO;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractImportResultVO;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractImportVO;
import com.jasic.aftersales.system.domain.vo.FirstSecondRelationVO;
import com.jasic.aftersales.system.domain.vo.HqFirstContractVO;
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
import com.jasic.aftersales.system.service.ISysContractService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 签约管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysContractServiceImpl implements ISysContractService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;
    private static final String OPERATION_DELETE = "DELETE";
    private static final String CRM_FIRST_SECOND_IMPORT_REMARK = "CRM导入初始化（一级二级关系）";
    private static final String CRM_IMPORT_REMARK = "CRM导入初始化";

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysRegionMapper sysRegionMapper;

    @Resource
    private HqFirstContractRecordMapper hqFirstContractRecordMapper;

    @Resource
    private FirstSecondRelationRecordMapper firstSecondRelationRecordMapper;

    @Resource
    private CrmHqFirstContractSnapshotMapper crmHqFirstContractSnapshotMapper;

    @Resource
    private CrmFirstSecondRelationSnapshotMapper crmFirstSecondRelationSnapshotMapper;

    @Resource
    private CrmBizCompanySnapshotMapper crmBizCompanySnapshotMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**
     * 总部-一级签约分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<HqFirstContractVO> listHqFirstPage(HqFirstContractQuery query) {
        Page<HqFirstContractVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<HqFirstContractVO> result = hqFirstContractMapper.selectHqFirstPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<CrmHqFirstContractImportVO> listCrmHqFirstImportPage(CrmHqFirstContractImportQuery query) {
        if (query == null || query.getHqCompanyId() == null) {
            throw new ServiceException("请选择总部公司");
        }
        validateImportHqCompany(query.getHqCompanyId());
        String salesOrg = resolveSalesOrgByHqCompanyId(query.getHqCompanyId());
        List<CrmHqFirstContractSnapshot> snapshots = listSnapshotsBySalesOrg(salesOrg);
        List<CrmHqFirstContractImportVO> records = buildCrmImportVOList(query.getHqCompanyId(), snapshots);
        records = filterCrmImportRecords(records, query);
        if (!Boolean.TRUE.equals(query.getShowAbnormal())) {
            records = records.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCanImport()))
                    .collect(Collectors.toList());
        }
        return buildPageResult(records, query.getPageNum(), query.getPageSize());
    }

    /**
     * 新增总部-一级签约
     *
     * @param dto 签约参数
     * @return 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long saveHqFirst(HqFirstContractDTO dto) {
        normalizeHqFirst(dto);
        Map<String, String> subjectTypeMap = buildSubjectTypeMap();

        SysCompany hqCompany = requireCompany(dto.getHqCompanyId(), "总部公司");
        validateEnabledCompany(hqCompany, "总部公司");
        validateHqCompany(hqCompany, subjectTypeMap);

        SysCompany firstCompany = requireCompany(dto.getFirstCompanyId(), "一级网点公司");
        validateEnabledCompany(firstCompany, "一级网点公司");
        validateFirstCompany(firstCompany);

        validateRegionBelongToHq(dto.getRegionId(), dto.getHqCompanyId());
        validateContractStatus(dto.getStatus(), "签约");
        checkHqFirstDuplicate(null, dto.getHqCompanyId(), dto.getFirstCompanyId());
        if (StrUtil.isNotBlank(dto.getContractNo())) {
            checkContractNoDuplicate(null, dto.getContractNo());
        }

        HqFirstContract entity = new HqFirstContract();
        BeanUtil.copyProperties(dto, entity);
        applyDefaultStatus(entity);
        try {
            hqFirstContractMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw translateHqFirstDuplicateException(ex);
        }
        return entity.getId();
    }

    /**
     * 修改总部-一级签约
     *
     * @param dto 签约参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateHqFirst(HqFirstContractDTO dto) {
        normalizeHqFirst(dto);
        if (dto.getId() == null) {
            throw new ServiceException("签约ID不能为空");
        }
        HqFirstContract entity = hqFirstContractMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("签约记录不存在");
        }

        Map<String, String> subjectTypeMap = buildSubjectTypeMap();
        SysCompany hqCompany = requireCompany(dto.getHqCompanyId(), "总部公司");
        validateEnabledCompany(hqCompany, "总部公司");
        validateHqCompany(hqCompany, subjectTypeMap);

        SysCompany firstCompany = requireCompany(dto.getFirstCompanyId(), "一级网点公司");
        validateEnabledCompany(firstCompany, "一级网点公司");
        validateFirstCompany(firstCompany);

        validateRegionBelongToHq(dto.getRegionId(), dto.getHqCompanyId());
        validateContractStatus(dto.getStatus(), "签约");
        checkHqFirstDuplicate(dto.getId(), dto.getHqCompanyId(), dto.getFirstCompanyId());
        if (StrUtil.isNotBlank(dto.getContractNo())) {
            checkContractNoDuplicate(dto.getId(), dto.getContractNo());
        }

        BeanUtil.copyProperties(dto, entity);
        applyDefaultStatus(entity);
        try {
            hqFirstContractMapper.updateById(entity);
        } catch (DuplicateKeyException ex) {
            throw translateHqFirstDuplicateException(ex);
        }
    }

    /**
     * 删除总部-一级签约
     *
     * @param id 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeHqFirst(Long id) {
        HqFirstContract entity = hqFirstContractMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("签约记录不存在");
        }
        saveHqFirstDeleteRecord(entity);
        hqFirstContractMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CrmHqFirstContractImportResultVO importHqFirstFromCrm(CrmHqFirstContractImportDTO dto) {
        if (dto == null || dto.getHqCompanyId() == null) {
            throw new ServiceException("请选择总部公司");
        }
        if (CollUtil.isEmpty(dto.getSnapshotIds())) {
            throw new ServiceException("请选择要导入的 CRM 签约关系");
        }
        validateImportHqCompany(dto.getHqCompanyId());
        String salesOrg = resolveSalesOrgByHqCompanyId(dto.getHqCompanyId());
        Set<Long> snapshotIds = dto.getSnapshotIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(snapshotIds)) {
            throw new ServiceException("请选择要导入的 CRM 签约关系");
        }

        LambdaQueryWrapper<CrmHqFirstContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CrmHqFirstContractSnapshot::getId, snapshotIds)
                .eq(CrmHqFirstContractSnapshot::getSalesOrg, salesOrg);
        List<CrmHqFirstContractSnapshot> snapshots = crmHqFirstContractSnapshotMapper.selectList(wrapper);
        Map<Long, CrmHqFirstContractImportVO> importVOMap = buildCrmImportVOList(dto.getHqCompanyId(), snapshots).stream()
                .collect(Collectors.toMap(CrmHqFirstContractImportVO::getId, item -> item, (a, b) -> a));

        CrmHqFirstContractImportResultVO result = new CrmHqFirstContractImportResultVO();
        result.setSelectedCount(snapshotIds.size());
        for (Long snapshotId : snapshotIds) {
            CrmHqFirstContractImportVO importVO = importVOMap.get(snapshotId);
            if (importVO == null) {
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }
            if (Boolean.TRUE.equals(importVO.getExistingContract())) {
                result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                continue;
            }
            if (!Boolean.TRUE.equals(importVO.getCanImport())) {
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }

            HqFirstContractDTO saveDTO = new HqFirstContractDTO();
            saveDTO.setHqCompanyId(dto.getHqCompanyId());
            saveDTO.setFirstCompanyId(importVO.getFirstCompanyId());
            saveDTO.setRegionId(importVO.getRegionId());
            saveDTO.setStatus(STATUS_ENABLED);
            saveDTO.setRemark(CRM_IMPORT_REMARK);
            try {
                saveHqFirst(saveDTO);
                result.setSuccessCount(defaultInt(result.getSuccessCount()) + 1);
            } catch (ServiceException ex) {
                if (isDuplicateHqFirstMessage(ex.getMessage())) {
                    result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                } else {
                    result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                }
            }
        }
        return result;
    }

    /**
     * 一级-二级从属分页列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<FirstSecondRelationVO> listFirstSecondPage(FirstSecondRelationQuery query) {
        Page<FirstSecondRelationVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<FirstSecondRelationVO> result = firstSecondRelationMapper.selectFirstSecondPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<CrmFirstSecondRelationImportVO> listCrmFirstSecondImportPage(CrmFirstSecondRelationImportQuery query) {
        if (query == null) {
            query = new CrmFirstSecondRelationImportQuery();
        }
        List<CrmFirstSecondRelationSnapshot> snapshots = listFirstSecondSnapshots();
        List<CrmFirstSecondRelationImportVO> records = buildCrmFirstSecondImportVOList(snapshots);
        records = filterCrmFirstSecondImportRecords(records, query);
        if (!Boolean.TRUE.equals(query.getShowAbnormal())) {
            records = records.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCanImport()))
                    .collect(Collectors.toList());
        }
        return buildPageResult(records, query.getPageNum(), query.getPageSize());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CrmFirstSecondRelationImportResultVO importFirstSecondFromCrm(CrmFirstSecondRelationImportDTO dto) {
        if (dto == null || CollUtil.isEmpty(dto.getSnapshotIds())) {
            throw new ServiceException("璇烽€夋嫨瑕佸鍏ョ殑涓€浜岀骇鍏崇郴");
        }
        Set<Long> snapshotIds = dto.getSnapshotIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(snapshotIds)) {
            throw new ServiceException("璇烽€夋嫨瑕佸鍏ョ殑涓€浜岀骇鍏崇郴");
        }

        LambdaQueryWrapper<CrmFirstSecondRelationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CrmFirstSecondRelationSnapshot::getId, snapshotIds);
        List<CrmFirstSecondRelationSnapshot> snapshots = crmFirstSecondRelationSnapshotMapper.selectList(wrapper);
        Map<Long, CrmFirstSecondRelationImportVO> importVOMap = buildCrmFirstSecondImportVOList(snapshots).stream()
                .collect(Collectors.toMap(CrmFirstSecondRelationImportVO::getId, item -> item, (a, b) -> a));

        CrmFirstSecondRelationImportResultVO result = new CrmFirstSecondRelationImportResultVO();
        result.setSelectedCount(snapshotIds.size());
        for (Long snapshotId : snapshotIds) {
            CrmFirstSecondRelationImportVO importVO = importVOMap.get(snapshotId);
            if (importVO == null) {
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }
            if (Boolean.TRUE.equals(importVO.getExistingRelation())) {
                result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                continue;
            }
            if (Boolean.TRUE.equals(importVO.getConflictingRelation())) {
                result.setConflictCount(defaultInt(result.getConflictCount()) + 1);
                continue;
            }
            if (!Boolean.TRUE.equals(importVO.getCanImport())) {
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }

            FirstSecondRelationDTO saveDTO = new FirstSecondRelationDTO();
            saveDTO.setFirstCompanyId(importVO.getFirstCompanyId());
            saveDTO.setSecondCompanyId(importVO.getSecondCompanyId());
            saveDTO.setStatus(STATUS_ENABLED);
            saveDTO.setRemark(CRM_FIRST_SECOND_IMPORT_REMARK);
            try {
                saveFirstSecond(saveDTO);
                result.setSuccessCount(defaultInt(result.getSuccessCount()) + 1);
            } catch (ServiceException ex) {
                if (isFirstSecondConflictMessage(ex.getMessage())) {
                    result.setConflictCount(defaultInt(result.getConflictCount()) + 1);
                } else if (isDuplicateFirstSecondMessage(ex.getMessage())) {
                    result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                } else {
                    result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                }
            }
        }
        return result;
    }

    /**
     * 新增一级-二级从属
     *
     * @param dto 从属关系参数
     * @return 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long saveFirstSecond(FirstSecondRelationDTO dto) {
        normalizeFirstSecond(dto);

        SysCompany firstCompany = requireCompany(dto.getFirstCompanyId(), "一级网点公司");
        validateEnabledCompany(firstCompany, "一级网点公司");
        validateFirstCompany(firstCompany);

        SysCompany secondCompany = requireCompany(dto.getSecondCompanyId(), "二级网点公司");
        validateEnabledCompany(secondCompany, "二级网点公司");
        validateSecondCompany(secondCompany);

        validateContractStatus(dto.getStatus(), "从属关系");
        checkFirstSecondDuplicate(null, dto.getFirstCompanyId(), dto.getSecondCompanyId());

        FirstSecondRelation entity = new FirstSecondRelation();
        BeanUtil.copyProperties(dto, entity);
        applyDefaultStatus(entity);
        try {
            firstSecondRelationMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw translateFirstSecondDuplicateException(ex);
        }
        return entity.getId();
    }

    /**
     * 删除一级-二级从属
     *
     * @param id 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeFirstSecond(Long id) {
        FirstSecondRelation entity = firstSecondRelationMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("从属关系记录不存在");
        }
        saveFirstSecondDeleteRecord(entity);
        firstSecondRelationMapper.deleteById(id);
    }

    private Map<String, String> buildSubjectTypeMap() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
    }

    private void validateImportHqCompany(Long hqCompanyId) {
        Map<String, String> subjectTypeMap = buildSubjectTypeMap();
        SysCompany hqCompany = requireCompany(hqCompanyId, "总部公司");
        validateEnabledCompany(hqCompany, "总部公司");
        validateHqCompany(hqCompany, subjectTypeMap);
    }

    private String resolveSalesOrgByHqCompanyId(Long hqCompanyId) {
        SysCompany hqCompany = requireCompany(hqCompanyId, "总部公司");
        String salesOrg = StrUtil.trim(hqCompany.getSalesOrg());
        if (StrUtil.isBlank(salesOrg)) {
            throw new ServiceException("当前总部未维护销售组织");
        }
        return salesOrg;
    }

    private List<CrmHqFirstContractSnapshot> listSnapshotsBySalesOrg(String salesOrg) {
        LambdaQueryWrapper<CrmHqFirstContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmHqFirstContractSnapshot::getSalesOrg, salesOrg)
                .orderByDesc(CrmHqFirstContractSnapshot::getCrmOperTime)
                .orderByDesc(CrmHqFirstContractSnapshot::getCrmAddTime)
                .orderByAsc(CrmHqFirstContractSnapshot::getKunnr)
                .orderByDesc(CrmHqFirstContractSnapshot::getId);
        return crmHqFirstContractSnapshotMapper.selectList(wrapper);
    }

    private List<CrmHqFirstContractImportVO> buildCrmImportVOList(Long hqCompanyId,
                                                                  List<CrmHqFirstContractSnapshot> snapshots) {
        if (CollUtil.isEmpty(snapshots)) {
            return Collections.emptyList();
        }

        Set<String> companyCodes = snapshots.stream()
                .map(CrmHqFirstContractSnapshot::getKunnr)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> regionCodes = snapshots.stream()
                .map(CrmHqFirstContractSnapshot::getRegionCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, SysCompany> companyByCode = loadCompanyByCode(companyCodes);
        Map<String, List<SysRegion>> regionByCode = loadRegionsByCode(regionCodes);
        Set<Long> firstCompanyIds = companyByCode.values().stream()
                .map(SysCompany::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> existingFirstCompanyIds = loadExistingContractFirstCompanyIds(hqCompanyId, firstCompanyIds);

        List<CrmHqFirstContractImportVO> result = new ArrayList<>(snapshots.size());
        for (CrmHqFirstContractSnapshot snapshot : snapshots) {
            CrmHqFirstContractImportVO vo = new CrmHqFirstContractImportVO();
            vo.setId(snapshot.getId());
            vo.setKunnr(snapshot.getKunnr());
            vo.setCustId(snapshot.getCustId());
            vo.setCrmCompanyName(snapshot.getCrmCompanyName());
            vo.setSalesOrg(snapshot.getSalesOrg());
            vo.setRegionCode(snapshot.getRegionCode());
            vo.setRegionName(snapshot.getRegionName());
            vo.setAliveFlag(snapshot.getAliveFlag());
            vo.setCrmAddTime(snapshot.getCrmAddTime());
            vo.setCrmOperTime(snapshot.getCrmOperTime());

            SysCompany localCompany = companyByCode.get(StrUtil.trim(snapshot.getKunnr()));
            if (localCompany == null) {
                vo.setMatchRemark("CRM 客户未匹配本地一级公司");
                vo.setCanImport(Boolean.FALSE);
                result.add(vo);
                continue;
            }
            vo.setFirstCompanyId(localCompany.getId());
            vo.setFirstCompanyName(localCompany.getCompanyName());
            if (existingFirstCompanyIds.contains(localCompany.getId())) {
                vo.setExistingContract(Boolean.TRUE);
                vo.setCanImport(Boolean.FALSE);
                vo.setMatchRemark("已存在正式签约");
                result.add(vo);
                continue;
            }
            if (!CompanyCategoryEnum.getFirstLevelTypeCodes().contains(localCompany.getTypeCode())) {
                vo.setMatchRemark("CRM 客户未匹配本地一级公司");
                vo.setCanImport(Boolean.FALSE);
                result.add(vo);
                continue;
            }
            if (!Objects.equals(localCompany.getStatus(), STATUS_ENABLED)) {
                vo.setMatchRemark("一级公司已停用");
                vo.setCanImport(Boolean.FALSE);
                result.add(vo);
                continue;
            }

            RegionMatchResult regionMatch = matchRegion(hqCompanyId, StrUtil.trim(snapshot.getRegionCode()), regionByCode);
            if (regionMatch.getRegion() == null) {
                vo.setMatchRemark(regionMatch.getRemark());
                vo.setCanImport(Boolean.FALSE);
                result.add(vo);
                continue;
            }
            vo.setRegionId(regionMatch.getRegion().getId());
            vo.setLocalRegionName(regionMatch.getRegion().getRegionName());
            vo.setExistingContract(Boolean.FALSE);
            vo.setCanImport(Boolean.TRUE);
            result.add(vo);
        }
        return result;
    }

    private Map<String, SysCompany> loadCompanyByCode(Set<String> companyCodes) {
        if (CollUtil.isEmpty(companyCodes)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getCompanyCode, companyCodes);
        return sysCompanyMapper.selectList(wrapper).stream()
                .filter(item -> StrUtil.isNotBlank(item.getCompanyCode()))
                .collect(Collectors.toMap(item -> StrUtil.trim(item.getCompanyCode()), item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, List<SysRegion>> loadRegionsByCode(Set<String> regionCodes) {
        if (CollUtil.isEmpty(regionCodes)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRegion::getRegionCode, regionCodes);
        return sysRegionMapper.selectList(wrapper).stream()
                .filter(item -> StrUtil.isNotBlank(item.getRegionCode()))
                .collect(Collectors.groupingBy(item -> StrUtil.trim(item.getRegionCode()), LinkedHashMap::new, Collectors.toList()));
    }

    private Set<Long> loadExistingContractFirstCompanyIds(Long hqCompanyId, Set<Long> firstCompanyIds) {
        if (CollUtil.isEmpty(firstCompanyIds)) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                .in(HqFirstContract::getFirstCompanyId, firstCompanyIds);
        return hqFirstContractMapper.selectList(wrapper).stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<CrmFirstSecondRelationSnapshot> listFirstSecondSnapshots() {
        LambdaQueryWrapper<CrmFirstSecondRelationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CrmFirstSecondRelationSnapshot::getCrmOperTime)
                .orderByAsc(CrmFirstSecondRelationSnapshot::getSecondCustId)
                .orderByDesc(CrmFirstSecondRelationSnapshot::getId);
        return crmFirstSecondRelationSnapshotMapper.selectList(wrapper);
    }

    private List<CrmFirstSecondRelationImportVO> buildCrmFirstSecondImportVOList(List<CrmFirstSecondRelationSnapshot> snapshots) {
        if (CollUtil.isEmpty(snapshots)) {
            return Collections.emptyList();
        }

        Set<Long> custIds = snapshots.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getFirstCustId(), item.getSecondCustId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, CrmBizCompanySnapshot> crmCompanyByCustId = loadCrmCompanySnapshotByCustIds(custIds);
        List<CrmFirstSecondRelationSnapshot> validSourceSnapshots = snapshots.stream()
                .filter(item -> isValidFirstSecondSourceSnapshot(item, crmCompanyByCustId))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(validSourceSnapshots)) {
            return Collections.emptyList();
        }

        Set<String> companyCodes = validSourceSnapshots.stream()
                .flatMap(item -> java.util.stream.Stream.of(
                        crmCompanyByCustId.get(item.getFirstCustId()),
                        crmCompanyByCustId.get(item.getSecondCustId())))
                .filter(Objects::nonNull)
                .map(CrmBizCompanySnapshot::getSapCompanyCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, SysCompany> companyByCode = loadCompanyByCode(companyCodes);

        Set<Long> secondCompanyIds = validSourceSnapshots.stream()
                .map(CrmFirstSecondRelationSnapshot::getSecondCustId)
                .map(crmCompanyByCustId::get)
                .filter(Objects::nonNull)
                .map(CrmBizCompanySnapshot::getSapCompanyCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(companyByCode::get)
                .filter(Objects::nonNull)
                .map(SysCompany::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, FirstSecondRelation> relationBySecondCompanyId = loadFirstSecondRelationBySecondCompanyIds(secondCompanyIds);

        Set<Long> conflictFirstCompanyIds = relationBySecondCompanyId.values().stream()
                .map(FirstSecondRelation::getFirstCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysCompany> companyById = new LinkedHashMap<>();
        for (SysCompany company : companyByCode.values()) {
            companyById.put(company.getId(), company);
        }
        companyById.putAll(loadCompanyByIds(conflictFirstCompanyIds));

        List<CrmFirstSecondRelationImportVO> result = new ArrayList<>(validSourceSnapshots.size());
        for (CrmFirstSecondRelationSnapshot snapshot : validSourceSnapshots) {
            CrmFirstSecondRelationImportVO vo = new CrmFirstSecondRelationImportVO();
            vo.setId(snapshot.getId());
            vo.setFirstCustId(snapshot.getFirstCustId());
            vo.setSecondCustId(snapshot.getSecondCustId());
            vo.setCrmOperTime(snapshot.getCrmOperTime());

            CrmBizCompanySnapshot firstSnapshot = crmCompanyByCustId.get(snapshot.getFirstCustId());
            if (firstSnapshot == null) {
                vo.setCanImport(Boolean.FALSE);
                vo.setMatchRemark("一级来源客户未在CRM公司快照中找到");
                result.add(vo);
                continue;
            }
            fillSourceCompanyInfo(vo, firstSnapshot, true);
            String firstReason = resolveFirstCompanyImportDisabledReason(firstSnapshot,
                    companyByCode.get(StrUtil.trim(firstSnapshot.getSapCompanyCode())));
            if (firstReason != null) {
                vo.setCanImport(Boolean.FALSE);
                vo.setMatchRemark(firstReason);
                result.add(vo);
                continue;
            }

            CrmBizCompanySnapshot secondSnapshot = crmCompanyByCustId.get(snapshot.getSecondCustId());
            if (secondSnapshot == null) {
                vo.setCanImport(Boolean.FALSE);
                vo.setMatchRemark("二级来源客户未在CRM公司快照中找到");
                result.add(vo);
                continue;
            }
            fillSourceCompanyInfo(vo, secondSnapshot, false);
            String secondReason = resolveSecondCompanyImportDisabledReason(secondSnapshot,
                    companyByCode.get(StrUtil.trim(secondSnapshot.getSapCompanyCode())));
            if (secondReason != null) {
                vo.setCanImport(Boolean.FALSE);
                vo.setMatchRemark(secondReason);
                result.add(vo);
                continue;
            }

            SysCompany firstCompany = companyByCode.get(StrUtil.trim(firstSnapshot.getSapCompanyCode()));
            SysCompany secondCompany = companyByCode.get(StrUtil.trim(secondSnapshot.getSapCompanyCode()));
            vo.setFirstCompanyId(firstCompany.getId());
            vo.setLocalFirstCompanyName(firstCompany.getCompanyName());
            vo.setSecondCompanyId(secondCompany.getId());
            vo.setLocalSecondCompanyName(secondCompany.getCompanyName());

            FirstSecondRelation relation = relationBySecondCompanyId.get(secondCompany.getId());
            if (relation != null) {
                if (Objects.equals(relation.getFirstCompanyId(), firstCompany.getId())) {
                    vo.setExistingRelation(Boolean.TRUE);
                    vo.setCanImport(Boolean.FALSE);
                    vo.setMatchRemark("已存在相同的一级二级关系");
                } else {
                    vo.setConflictingRelation(Boolean.TRUE);
                    vo.setCanImport(Boolean.FALSE);
                    SysCompany conflictFirst = companyById.get(relation.getFirstCompanyId());
                    if (conflictFirst == null) {
                        vo.setMatchRemark("该二级网点已归属其他一级网点");
                    } else {
                        vo.setMatchRemark("该二级网点已归属其他一级网点：" + conflictFirst.getCompanyName());
                    }
                }
                result.add(vo);
                continue;
            }

            vo.setExistingRelation(Boolean.FALSE);
            vo.setConflictingRelation(Boolean.FALSE);
            vo.setCanImport(Boolean.TRUE);
            vo.setMatchRemark("可导入");
            result.add(vo);
        }
        return result;
    }

    private boolean isValidFirstSecondSourceSnapshot(CrmFirstSecondRelationSnapshot snapshot,
                                                     Map<Long, CrmBizCompanySnapshot> crmCompanyByCustId) {
        if (snapshot == null) {
            return false;
        }
        CrmBizCompanySnapshot firstSnapshot = crmCompanyByCustId.get(snapshot.getFirstCustId());
        if (firstSnapshot == null || !Objects.equals(firstSnapshot.getCustRage(), 0)) {
            return false;
        }
        CrmBizCompanySnapshot secondSnapshot = crmCompanyByCustId.get(snapshot.getSecondCustId());
        return secondSnapshot != null && Objects.equals(secondSnapshot.getCustRage(), 3);
    }

    private void fillSourceCompanyInfo(CrmFirstSecondRelationImportVO vo, CrmBizCompanySnapshot snapshot, boolean first) {
        if (first) {
            vo.setFirstCompanyCode(StrUtil.trim(snapshot.getSapCompanyCode()));
            vo.setFirstCompanyName(snapshot.getCustName());
            return;
        }
        vo.setSecondCompanyCode(StrUtil.trim(snapshot.getSapCompanyCode()));
        vo.setSecondCompanyName(snapshot.getCustName());
    }

    private String resolveFirstCompanyImportDisabledReason(CrmBizCompanySnapshot sourceSnapshot, SysCompany localCompany) {
        if (!Objects.equals(sourceSnapshot.getCustRage(), 0)) {
            return "一级来源客户不是一级网点";
        }
        if (StrUtil.isBlank(sourceSnapshot.getSapCompanyCode())) {
            return "一级来源客户缺少公司编码";
        }
        if (localCompany == null) {
            return "一级未匹配本地公司";
        }
        if (!CompanyCategoryEnum.getFirstLevelTypeCodes().contains(localCompany.getTypeCode())) {
            return "一级不是启用的SITE_FIRST";
        }
        if (!Objects.equals(localCompany.getStatus(), STATUS_ENABLED)) {
            return "一级本地公司已停用";
        }
        return null;
    }

    private String resolveSecondCompanyImportDisabledReason(CrmBizCompanySnapshot sourceSnapshot, SysCompany localCompany) {
        if (!Objects.equals(sourceSnapshot.getCustRage(), 3)) {
            return "二级来源客户不是二级网点";
        }
        if (StrUtil.isBlank(sourceSnapshot.getSapCompanyCode())) {
            return "二级来源客户缺少公司编码";
        }
        if (localCompany == null) {
            return "二级未匹配本地公司";
        }
        if (!CompanyCategoryEnum.getSecondLevelTypeCodes().contains(localCompany.getTypeCode())) {
            return "二级不是启用的SITE_SECOND";
        }
        if (!Objects.equals(localCompany.getStatus(), STATUS_ENABLED)) {
            return "二级本地公司已停用";
        }
        return null;
    }

    private Map<Long, CrmBizCompanySnapshot> loadCrmCompanySnapshotByCustIds(Set<Long> custIds) {
        if (CollUtil.isEmpty(custIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CrmBizCompanySnapshot::getCustId, custIds);
        return crmBizCompanySnapshotMapper.selectList(wrapper).stream()
                .filter(item -> item.getCustId() != null)
                .collect(Collectors.toMap(CrmBizCompanySnapshot::getCustId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, FirstSecondRelation> loadFirstSecondRelationBySecondCompanyIds(Set<Long> secondCompanyIds) {
        if (CollUtil.isEmpty(secondCompanyIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FirstSecondRelation::getSecondCompanyId, secondCompanyIds);
        return firstSecondRelationMapper.selectList(wrapper).stream()
                .filter(item -> item.getSecondCompanyId() != null)
                .collect(Collectors.toMap(FirstSecondRelation::getSecondCompanyId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, SysCompany> loadCompanyByIds(Set<Long> companyIds) {
        if (CollUtil.isEmpty(companyIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getId, companyIds);
        return sysCompanyMapper.selectList(wrapper).stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(SysCompany::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    private RegionMatchResult matchRegion(Long hqCompanyId,
                                          String regionCode,
                                          Map<String, List<SysRegion>> regionByCode) {
        if (StrUtil.isBlank(regionCode)) {
            return RegionMatchResult.fail("CRM大区编码为空");
        }
        List<SysRegion> regions = regionByCode.get(regionCode);
        if (CollUtil.isEmpty(regions)) {
            return RegionMatchResult.fail("CRM大区未匹配本地大区");
        }
        for (SysRegion region : regions) {
            if (Objects.equals(region.getCompanyId(), hqCompanyId)) {
                return RegionMatchResult.success(region);
            }
        }
        return RegionMatchResult.fail("CRM大区不属于当前总部");
    }

    private <T> PageResult<T> buildPageResult(List<T> records,
                                              Integer pageNum,
                                              Integer pageSize) {
        int currentPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int total = records == null ? 0 : records.size();
        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0L, currentPageNum, currentPageSize);
        }
        int fromIndex = Math.min((currentPageNum - 1) * currentPageSize, total);
        int toIndex = Math.min(fromIndex + currentPageSize, total);
        return PageResult.of(records.subList(fromIndex, toIndex), (long) total, currentPageNum, currentPageSize);
    }

    private List<CrmHqFirstContractImportVO> filterCrmImportRecords(List<CrmHqFirstContractImportVO> records,
                                                                    CrmHqFirstContractImportQuery query) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        String kunnr = StrUtil.trim(query.getKunnr());
        return records.stream()
                .filter(item -> query.getFirstCompanyId() == null
                        || Objects.equals(item.getFirstCompanyId(), query.getFirstCompanyId()))
                .filter(item -> query.getRegionId() == null
                        || Objects.equals(item.getRegionId(), query.getRegionId()))
                .filter(item -> StrUtil.isBlank(kunnr)
                        || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(item.getKunnr(), ""), kunnr))
                .collect(Collectors.toList());
    }

    private List<CrmFirstSecondRelationImportVO> filterCrmFirstSecondImportRecords(List<CrmFirstSecondRelationImportVO> records,
                                                                                   CrmFirstSecondRelationImportQuery query) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        String firstCompanyCode = StrUtil.trim(query.getFirstCompanyCode());
        String secondCompanyCode = StrUtil.trim(query.getSecondCompanyCode());
        return records.stream()
                .filter(item -> query.getFirstCompanyId() == null
                        || Objects.equals(item.getFirstCompanyId(), query.getFirstCompanyId()))
                .filter(item -> query.getSecondCompanyId() == null
                        || Objects.equals(item.getSecondCompanyId(), query.getSecondCompanyId()))
                .filter(item -> StrUtil.isBlank(firstCompanyCode)
                        || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(item.getFirstCompanyCode(), ""), firstCompanyCode))
                .filter(item -> StrUtil.isBlank(secondCompanyCode)
                        || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(item.getSecondCompanyCode(), ""), secondCompanyCode))
                .collect(Collectors.toList());
    }

    private SysCompany requireCompany(Long companyId, String label) {
        if (companyId == null) {
            throw new ServiceException(label + "ID不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(label + "不存在");
        }
        return company;
    }

    private void validateEnabledCompany(SysCompany company, String label) {
        if (!Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException(label + "已停用");
        }
    }

    private void validateHqCompany(SysCompany company, Map<String, String> subjectTypeMap) {
        String subjectType = subjectTypeMap.get(company.getTypeCode());
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            throw new ServiceException("总部公司必须是总部类型");
        }
    }

    private void validateFirstCompany(SysCompany company) {
        if (!CompanyCategoryEnum.getFirstLevelTypeCodes().contains(company.getTypeCode())) {
            throw new ServiceException("一级网点公司必须是一级网点类型");
        }
    }

    private void validateSecondCompany(SysCompany company) {
        if (!CompanyCategoryEnum.getSecondLevelTypeCodes().contains(company.getTypeCode())) {
            throw new ServiceException("二级网点公司必须是二级网点类型");
        }
    }

    private void validateRegionBelongToHq(Long regionId, Long hqCompanyId) {
        if (regionId == null) {
            return;
        }
        SysRegion region = sysRegionMapper.selectById(regionId);
        if (region == null) {
            throw new ServiceException("所属大区不存在");
        }
        if (!Objects.equals(region.getCompanyId(), hqCompanyId)) {
            throw new ServiceException("所属大区不属于当前总部");
        }
    }

    private void validateContractStatus(Integer status, String label) {
        if (status == null) {
            return;
        }
        if (!Objects.equals(STATUS_ENABLED, status) && !Objects.equals(STATUS_DISABLED, status)) {
            throw new ServiceException(label + "状态不合法");
        }
    }

    private void checkHqFirstDuplicate(Long excludeId, Long hqCompanyId, Long firstCompanyId) {
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                .eq(HqFirstContract::getFirstCompanyId, firstCompanyId);
        if (excludeId != null) {
            wrapper.ne(HqFirstContract::getId, excludeId);
        }
        if (hqFirstContractMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该总部与一级网点的签约关系已存在");
        }
    }

    private void checkContractNoDuplicate(Long excludeId, String contractNo) {
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getContractNo, contractNo);
        if (excludeId != null) {
            wrapper.ne(HqFirstContract::getId, excludeId);
        }
        if (hqFirstContractMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("合同编号已存在");
        }
    }

    private void checkFirstSecondDuplicate(Long excludeId, Long firstCompanyId, Long secondCompanyId) {
        LambdaQueryWrapper<FirstSecondRelation> pairWrapper = new LambdaQueryWrapper<>();
        pairWrapper.eq(FirstSecondRelation::getFirstCompanyId, firstCompanyId)
                .eq(FirstSecondRelation::getSecondCompanyId, secondCompanyId);
        if (excludeId != null) {
            pairWrapper.ne(FirstSecondRelation::getId, excludeId);
        }
        if (firstSecondRelationMapper.selectCount(pairWrapper) > 0) {
            throw new ServiceException("该一级、二级网点的从属关系已存在");
        }

        LambdaQueryWrapper<FirstSecondRelation> secondWrapper = new LambdaQueryWrapper<>();
        secondWrapper.eq(FirstSecondRelation::getSecondCompanyId, secondCompanyId);
        if (excludeId != null) {
            secondWrapper.ne(FirstSecondRelation::getId, excludeId);
        }
        FirstSecondRelation relation = firstSecondRelationMapper.selectOne(secondWrapper);
        if (relation != null) {
            throw new ServiceException("该二级网点已归属其他一级网点");
        }
    }

    private void saveHqFirstDeleteRecord(HqFirstContract entity) {
        HqFirstContractRecord record = new HqFirstContractRecord();
        record.setSourceId(entity.getId());
        record.setHqCompanyId(entity.getHqCompanyId());
        record.setFirstCompanyId(entity.getFirstCompanyId());
        record.setRegionId(entity.getRegionId());
        record.setContractNo(entity.getContractNo());
        record.setStatus(entity.getStatus());
        record.setRemark(entity.getRemark());
        record.setOperationType(OPERATION_DELETE);
        record.setOperatorUserId(getCurrentUserId());
        record.setOperatorCompanyId(getCurrentCompanyId());
        hqFirstContractRecordMapper.insert(record);
    }

    private void saveFirstSecondDeleteRecord(FirstSecondRelation entity) {
        FirstSecondRelationRecord record = new FirstSecondRelationRecord();
        record.setSourceId(entity.getId());
        record.setFirstCompanyId(entity.getFirstCompanyId());
        record.setSecondCompanyId(entity.getSecondCompanyId());
        record.setStatus(entity.getStatus());
        record.setRemark(entity.getRemark());
        record.setOperationType(OPERATION_DELETE);
        record.setOperatorUserId(getCurrentUserId());
        record.setOperatorCompanyId(getCurrentCompanyId());
        firstSecondRelationRecordMapper.insert(record);
    }

    private Long getCurrentUserId() {
        try {
            return SecurityContext.getCurrentUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    private Long getCurrentCompanyId() {
        try {
            return SecurityContext.getCurrentCompanyId();
        } catch (Exception ex) {
            return null;
        }
    }

    private void applyDefaultStatus(HqFirstContract entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
    }

    private void applyDefaultStatus(FirstSecondRelation entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
    }

    private void normalizeHqFirst(HqFirstContractDTO dto) {
        dto.setContractNo(normalizeNullableText(dto.getContractNo()));
        dto.setRemark(normalizeNullableText(dto.getRemark()));
    }

    private void normalizeFirstSecond(FirstSecondRelationDTO dto) {
        dto.setRemark(normalizeNullableText(dto.getRemark()));
    }

    private String normalizeNullableText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isDuplicateFirstSecondMessage(String message) {
        return StrUtil.equals(message, "该一级、二级网点的从属关系已存在")
                || StrUtil.equals(message, "从属关系已存在，请勿重复保存");
    }

    private boolean isFirstSecondConflictMessage(String message) {
        return StrUtil.equals(message, "该二级网点已归属其他一级网点");
    }

    private boolean isDuplicateHqFirstMessage(String message) {
        return StrUtil.equals(message, "该总部与一级网点的签约关系已存在")
                || StrUtil.equals(message, "签约关系已存在，请勿重复保存");
    }

    private ServiceException translateHqFirstDuplicateException(DuplicateKeyException ex) {
        String message = ex.getMessage();
        if (StrUtil.containsIgnoreCase(message, "uk_hq_first")) {
            return new ServiceException("该总部与一级网点的签约关系已存在");
        }
        return new ServiceException("签约关系已存在，请勿重复保存");
    }

    private ServiceException translateFirstSecondDuplicateException(DuplicateKeyException ex) {
        String message = ex.getMessage();
        if (StrUtil.containsIgnoreCase(message, "uk_second")) {
            return new ServiceException("该二级网点已归属其他一级网点");
        }
        if (StrUtil.containsIgnoreCase(message, "uk_first_second")) {
            return new ServiceException("该一级、二级网点的从属关系已存在");
        }
        return new ServiceException("从属关系已存在，请勿重复保存");
    }

    private static class RegionMatchResult {

        private final SysRegion region;
        private final String remark;

        private RegionMatchResult(SysRegion region, String remark) {
            this.region = region;
            this.remark = remark;
        }

        private static RegionMatchResult success(SysRegion region) {
            return new RegionMatchResult(region, null);
        }

        private static RegionMatchResult fail(String remark) {
            return new RegionMatchResult(null, remark);
        }

        private SysRegion getRegion() {
            return region;
        }

        private String getRemark() {
            return remark;
        }
    }
}
