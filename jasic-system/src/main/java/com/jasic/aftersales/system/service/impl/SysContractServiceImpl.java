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

    /**
     * CRM业务公司快照Mapper数据访问接口。
     *
     * @param query 参数
     * @return 处理结果
     */
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
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (query == null) {
            // 调用HqFirstContractQuery方法，复用统一能力并保证业务规则一致。
            query = new HqFirstContractQuery();
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(query.getTargetCompanyId());
        // 调用bindHqFirstQueryToTarget方法，复用统一能力并保证业务规则一致。
        bindHqFirstQueryToTarget(query, targetHqId);
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<HqFirstContractVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 说明：执行该步骤以保证业务流程正确。
        IPage<HqFirstContractVO> result = hqFirstContractMapper.selectHqFirstPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 分页查询CRM总部一级Import分页列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<CrmHqFirstContractImportVO> listCrmHqFirstImportPage(CrmHqFirstContractImportQuery query) {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (query == null) {
            // 调用CrmHqFirstContractImportQuery方法，复用统一能力并保证业务规则一致。
            query = new CrmHqFirstContractImportQuery();
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(query.getTargetCompanyId());
        // 调用bindCrmHqFirstQueryToTarget方法，复用统一能力并保证业务规则一致。
        bindCrmHqFirstQueryToTarget(query, targetHqId);
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        validateImportHqCompany(query.getHqCompanyId());
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        String salesOrg = resolveSalesOrgByHqCompanyId(query.getHqCompanyId());
        // 调用listSnapshotsBySalesOrg方法，复用统一能力并保证业务规则一致。
        List<CrmHqFirstContractSnapshot> snapshots = listSnapshotsBySalesOrg(salesOrg);
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        List<CrmHqFirstContractImportVO> records = buildCrmImportVOList(query.getHqCompanyId(), snapshots);
        // 调用filterCrmImportRecords方法，复用统一能力并保证业务规则一致。
        records = filterCrmImportRecords(records, query);
        if (!Boolean.TRUE.equals(query.getShowAbnormal())) {
            records = records.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCanImport()))
                    // 调用toList方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (dto == null) {
            throw new ServiceException("签约参数不能为空");
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(dto.getTargetCompanyId());
        // 调用bindHqFirstDtoToTarget方法，复用统一能力并保证业务规则一致。
        bindHqFirstDtoToTarget(dto, targetHqId);
        // 调用normalizeHqFirst方法，复用统一能力并保证业务规则一致。
        normalizeHqFirst(dto);
        // 调用buildSubjectTypeMap方法，复用统一能力并保证业务规则一致。
        Map<String, String> subjectTypeMap = buildSubjectTypeMap();

        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany hqCompany = requireCompany(dto.getHqCompanyId(), "总部公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(hqCompany, "总部公司");
        // 调用validateHqCompany方法，复用统一能力并保证业务规则一致。
        validateHqCompany(hqCompany, subjectTypeMap);

        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany firstCompany = requireCompany(dto.getFirstCompanyId(), "一级网点公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(firstCompany, "一级网点公司");
        // 调用validateFirstCompany方法，复用统一能力并保证业务规则一致。
        validateFirstCompany(firstCompany);

        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        validateRegionBelongToHq(dto.getRegionId(), dto.getHqCompanyId());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        validateContractStatus(dto.getStatus(), "签约");
        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        checkHqFirstDuplicate(null, dto.getHqCompanyId(), dto.getFirstCompanyId());
        if (StrUtil.isNotBlank(dto.getContractNo())) {
            // 调用getContractNo方法，复用统一能力并保证业务规则一致。
            checkContractNoDuplicate(null, dto.getContractNo());
        }

        // 调用HqFirstContract方法，复用统一能力并保证业务规则一致。
        HqFirstContract entity = new HqFirstContract();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
        applyDefaultStatus(entity);
        try {
            // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (dto == null) {
            throw new ServiceException("签约参数不能为空");
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(dto.getTargetCompanyId());
        // 调用bindHqFirstDtoToTarget方法，复用统一能力并保证业务规则一致。
        bindHqFirstDtoToTarget(dto, targetHqId);
        // 调用normalizeHqFirst方法，复用统一能力并保证业务规则一致。
        normalizeHqFirst(dto);
        if (dto.getId() == null) {
            throw new ServiceException("签约ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        HqFirstContract entity = hqFirstContractMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("签约记录不存在");
        }

        // 调用buildSubjectTypeMap方法，复用统一能力并保证业务规则一致。
        Map<String, String> subjectTypeMap = buildSubjectTypeMap();
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany hqCompany = requireCompany(dto.getHqCompanyId(), "总部公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(hqCompany, "总部公司");
        // 调用validateHqCompany方法，复用统一能力并保证业务规则一致。
        validateHqCompany(hqCompany, subjectTypeMap);

        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany firstCompany = requireCompany(dto.getFirstCompanyId(), "一级网点公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(firstCompany, "一级网点公司");
        // 调用validateFirstCompany方法，复用统一能力并保证业务规则一致。
        validateFirstCompany(firstCompany);

        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        validateRegionBelongToHq(dto.getRegionId(), dto.getHqCompanyId());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        validateContractStatus(dto.getStatus(), "签约");
        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        checkHqFirstDuplicate(dto.getId(), dto.getHqCompanyId(), dto.getFirstCompanyId());
        if (StrUtil.isNotBlank(dto.getContractNo())) {
            // 调用getContractNo方法，复用统一能力并保证业务规则一致。
            checkContractNoDuplicate(dto.getId(), dto.getContractNo());
        }

        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
        applyDefaultStatus(entity);
        try {
            // 说明：执行该步骤以保证业务流程正确。
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
    public void removeHqFirst(Long id, Long targetCompanyId) {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        // 调用resolveContractTargetHq方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        HqFirstContract entity = hqFirstContractMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("签约记录不存在");
        }
        // 调用validateHqFirstInTarget方法，复用统一能力并保证业务规则一致。
        validateHqFirstInTarget(entity, targetHqId);
        // 调用saveHqFirstDeleteRecord方法，复用统一能力并保证业务规则一致。
        saveHqFirstDeleteRecord(entity);
        // 说明：执行该步骤以保证业务流程正确。
        hqFirstContractMapper.deleteById(id);
    }

    /**
     * import总部一级从CRM。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CrmHqFirstContractImportResultVO importHqFirstFromCrm(CrmHqFirstContractImportDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (dto == null) {
            throw new ServiceException("导入参数不能为空");
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(dto.getTargetCompanyId());
        // 调用bindCrmHqFirstDtoToTarget方法，复用统一能力并保证业务规则一致。
        bindCrmHqFirstDtoToTarget(dto, targetHqId);
        if (CollUtil.isEmpty(dto.getSnapshotIds())) {
            throw new ServiceException("请选择要导入的 CRM 签约关系");
        }
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        validateImportHqCompany(dto.getHqCompanyId());
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        String salesOrg = resolveSalesOrgByHqCompanyId(dto.getHqCompanyId());
        Set<Long> snapshotIds = dto.getSnapshotIds().stream()
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(snapshotIds)) {
            throw new ServiceException("请选择要导入的 CRM 签约关系");
        }

        LambdaQueryWrapper<CrmHqFirstContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CrmHqFirstContractSnapshot::getId, snapshotIds)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(CrmHqFirstContractSnapshot::getSalesOrg, salesOrg);
        // 说明：执行该步骤以保证业务流程正确。
        List<CrmHqFirstContractSnapshot> snapshots = crmHqFirstContractSnapshotMapper.selectList(wrapper);
        Map<Long, CrmHqFirstContractImportVO> importVOMap = buildCrmImportVOList(dto.getHqCompanyId(), snapshots).stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(CrmHqFirstContractImportVO::getId, item -> item, (a, b) -> a));

        // 调用CrmHqFirstContractImportResultVO方法，复用统一能力并保证业务规则一致。
        CrmHqFirstContractImportResultVO result = new CrmHqFirstContractImportResultVO();
        // 调用size方法，复用统一能力并保证业务规则一致。
        result.setSelectedCount(snapshotIds.size());
        for (Long snapshotId : snapshotIds) {
            // 调用get方法，复用统一能力并保证业务规则一致。
            CrmHqFirstContractImportVO importVO = importVOMap.get(snapshotId);
            if (importVO == null) {
                // 调用getFailedCount方法，复用统一能力并保证业务规则一致。
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }
            if (Boolean.TRUE.equals(importVO.getExistingContract())) {
                // 调用getExistedCount方法，复用统一能力并保证业务规则一致。
                result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                continue;
            }
            if (!Boolean.TRUE.equals(importVO.getCanImport())) {
                // 调用getFailedCount方法，复用统一能力并保证业务规则一致。
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }

            // 调用HqFirstContractDTO方法，复用统一能力并保证业务规则一致。
            HqFirstContractDTO saveDTO = new HqFirstContractDTO();
            // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
            saveDTO.setTargetCompanyId(targetHqId);
            // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
            saveDTO.setHqCompanyId(dto.getHqCompanyId());
            // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
            saveDTO.setFirstCompanyId(importVO.getFirstCompanyId());
            // 调用getRegionId方法，复用统一能力并保证业务规则一致。
            saveDTO.setRegionId(importVO.getRegionId());
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            saveDTO.setStatus(STATUS_ENABLED);
            // 调用setRemark方法，复用统一能力并保证业务规则一致。
            saveDTO.setRemark(CRM_IMPORT_REMARK);
            try {
                // 调用saveHqFirst方法，复用统一能力并保证业务规则一致。
                saveHqFirst(saveDTO);
                // 调用getSuccessCount方法，复用统一能力并保证业务规则一致。
                result.setSuccessCount(defaultInt(result.getSuccessCount()) + 1);
            } catch (ServiceException ex) {
                if (isDuplicateHqFirstMessage(ex.getMessage())) {
                    // 调用getExistedCount方法，复用统一能力并保证业务规则一致。
                    result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                } else {
                    // 调用getFailedCount方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (query == null) {
            // 调用FirstSecondRelationQuery方法，复用统一能力并保证业务规则一致。
            query = new FirstSecondRelationQuery();
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(query.getTargetCompanyId());
        // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
        query.setTargetCompanyId(targetHqId);
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<FirstSecondRelationVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        // 说明：执行该步骤以保证业务流程正确。
        IPage<FirstSecondRelationVO> result = firstSecondRelationMapper.selectFirstSecondPage(page, query);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 分页查询CRM一级二级Import分页列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<CrmFirstSecondRelationImportVO> listCrmFirstSecondImportPage(CrmFirstSecondRelationImportQuery query) {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (query == null) {
            // 调用CrmFirstSecondRelationImportQuery方法，复用统一能力并保证业务规则一致。
            query = new CrmFirstSecondRelationImportQuery();
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(query.getTargetCompanyId());
        // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
        query.setTargetCompanyId(targetHqId);
        // 调用listFirstSecondSnapshots方法，复用统一能力并保证业务规则一致。
        List<CrmFirstSecondRelationSnapshot> snapshots = listFirstSecondSnapshots();
        // 调用buildCrmFirstSecondImportVOList方法，复用统一能力并保证业务规则一致。
        List<CrmFirstSecondRelationImportVO> records = buildCrmFirstSecondImportVOList(targetHqId, snapshots);
        // 调用filterCrmFirstSecondImportRecords方法，复用统一能力并保证业务规则一致。
        records = filterCrmFirstSecondImportRecords(records, query);
        if (!Boolean.TRUE.equals(query.getShowAbnormal())) {
            records = records.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getCanImport()))
                    // 调用toList方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toList());
        }
        return buildPageResult(records, query.getPageNum(), query.getPageSize());
    }

    /**
     * import一级二级从CRM。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public CrmFirstSecondRelationImportResultVO importFirstSecondFromCrm(CrmFirstSecondRelationImportDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (dto == null) {
            throw new ServiceException("导入参数不能为空");
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(dto.getTargetCompanyId());
        if (CollUtil.isEmpty(dto.getSnapshotIds())) {
            throw new ServiceException("请选择要导入的一二级关系");
        }
        Set<Long> snapshotIds = dto.getSnapshotIds().stream()
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(snapshotIds)) {
            throw new ServiceException("请选择要导入的一二级关系");
        }

        LambdaQueryWrapper<CrmFirstSecondRelationSnapshot> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(CrmFirstSecondRelationSnapshot::getId, snapshotIds);
        // 说明：执行该步骤以保证业务流程正确。
        List<CrmFirstSecondRelationSnapshot> snapshots = crmFirstSecondRelationSnapshotMapper.selectList(wrapper);
        Map<Long, CrmFirstSecondRelationImportVO> importVOMap = buildCrmFirstSecondImportVOList(targetHqId, snapshots).stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(CrmFirstSecondRelationImportVO::getId, item -> item, (a, b) -> a));

        // 调用CrmFirstSecondRelationImportResultVO方法，复用统一能力并保证业务规则一致。
        CrmFirstSecondRelationImportResultVO result = new CrmFirstSecondRelationImportResultVO();
        // 调用size方法，复用统一能力并保证业务规则一致。
        result.setSelectedCount(snapshotIds.size());
        for (Long snapshotId : snapshotIds) {
            // 调用get方法，复用统一能力并保证业务规则一致。
            CrmFirstSecondRelationImportVO importVO = importVOMap.get(snapshotId);
            if (importVO == null) {
                // 调用getFailedCount方法，复用统一能力并保证业务规则一致。
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }
            if (Boolean.TRUE.equals(importVO.getExistingRelation())) {
                // 调用getExistedCount方法，复用统一能力并保证业务规则一致。
                result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                continue;
            }
            if (Boolean.TRUE.equals(importVO.getConflictingRelation())) {
                // 调用getConflictCount方法，复用统一能力并保证业务规则一致。
                result.setConflictCount(defaultInt(result.getConflictCount()) + 1);
                continue;
            }
            if (!Boolean.TRUE.equals(importVO.getCanImport())) {
                // 调用getFailedCount方法，复用统一能力并保证业务规则一致。
                result.setFailedCount(defaultInt(result.getFailedCount()) + 1);
                continue;
            }

            // 调用FirstSecondRelationDTO方法，复用统一能力并保证业务规则一致。
            FirstSecondRelationDTO saveDTO = new FirstSecondRelationDTO();
            // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
            saveDTO.setTargetCompanyId(targetHqId);
            // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
            saveDTO.setFirstCompanyId(importVO.getFirstCompanyId());
            // 调用getSecondCompanyId方法，复用统一能力并保证业务规则一致。
            saveDTO.setSecondCompanyId(importVO.getSecondCompanyId());
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            saveDTO.setStatus(STATUS_ENABLED);
            // 调用setRemark方法，复用统一能力并保证业务规则一致。
            saveDTO.setRemark(CRM_FIRST_SECOND_IMPORT_REMARK);
            try {
                // 调用saveFirstSecond方法，复用统一能力并保证业务规则一致。
                saveFirstSecond(saveDTO);
                // 调用getSuccessCount方法，复用统一能力并保证业务规则一致。
                result.setSuccessCount(defaultInt(result.getSuccessCount()) + 1);
            } catch (ServiceException ex) {
                if (isFirstSecondConflictMessage(ex.getMessage())) {
                    // 调用getConflictCount方法，复用统一能力并保证业务规则一致。
                    result.setConflictCount(defaultInt(result.getConflictCount()) + 1);
                } else if (isDuplicateFirstSecondMessage(ex.getMessage())) {
                    // 调用getExistedCount方法，复用统一能力并保证业务规则一致。
                    result.setExistedCount(defaultInt(result.getExistedCount()) + 1);
                } else {
                    // 调用getFailedCount方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        if (dto == null) {
            throw new ServiceException("从属关系参数不能为空");
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(dto.getTargetCompanyId());
        // 调用normalizeFirstSecond方法，复用统一能力并保证业务规则一致。
        normalizeFirstSecond(dto);

        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany firstCompany = requireCompany(dto.getFirstCompanyId(), "一级网点公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(firstCompany, "一级网点公司");
        // 调用validateFirstCompany方法，复用统一能力并保证业务规则一致。
        validateFirstCompany(firstCompany);
        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        validateFirstCompanyBelongsToTargetHq(dto.getFirstCompanyId(), targetHqId);

        // 调用getSecondCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany secondCompany = requireCompany(dto.getSecondCompanyId(), "二级网点公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(secondCompany, "二级网点公司");
        // 调用validateSecondCompany方法，复用统一能力并保证业务规则一致。
        validateSecondCompany(secondCompany);

        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        validateContractStatus(dto.getStatus(), "从属关系");
        // 调用getSecondCompanyId方法，复用统一能力并保证业务规则一致。
        checkFirstSecondDuplicate(null, dto.getFirstCompanyId(), dto.getSecondCompanyId());

        // 调用FirstSecondRelation方法，复用统一能力并保证业务规则一致。
        FirstSecondRelation entity = new FirstSecondRelation();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
        applyDefaultStatus(entity);
        try {
            // 说明：执行该步骤以保证业务流程正确。
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
    public void removeFirstSecond(Long id, Long targetCompanyId) {
        // 说明：执行该步骤以保证业务流程正确。
        requirePlatformOperator();
        // 调用resolveContractTargetHq方法，复用统一能力并保证业务规则一致。
        Long targetHqId = resolveContractTargetHq(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        FirstSecondRelation entity = firstSecondRelationMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("从属关系记录不存在");
        }
        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        validateFirstCompanyBelongsToTargetHq(entity.getFirstCompanyId(), targetHqId);
        // 调用saveFirstSecondDeleteRecord方法，复用统一能力并保证业务规则一致。
        saveFirstSecondDeleteRecord(entity);
        // 说明：执行该步骤以保证业务流程正确。
        firstSecondRelationMapper.deleteById(id);
    }

    /**
     * 构建主体类型Map。
     *
     * @return 处理结果
     */
    private Map<String, String> buildSubjectTypeMap() {
        // 调用listAll方法，复用统一能力并保证业务规则一致。
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
    }

    /**
     * requirePlatformOperator。
     */
    private void requirePlatformOperator() {
        try {
            // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
            SecurityContext.getCurrentUserId();
            if (!SecurityContext.isPlatformUser()) {
                throw new ServiceException("无权操作组织关系配置数据");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("缺少登录态");
        }
    }

    /**
     * 解析合同Target总部。
     *
     * @return 处理结果
     */
    private Long resolveContractTargetHq(Long targetCompanyId) {
        if (targetCompanyId == null) {
            throw new ServiceException("缺少目标公司上下文");
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateImportHqCompany(targetCompanyId);
        return targetCompanyId;
    }

    /**
     * bind总部一级查询ToTarget。
     *
     * @param query 参数
     * @param targetHqId target Hq ID
     */
    private void bindHqFirstQueryToTarget(HqFirstContractQuery query, Long targetHqId) {
        if (query.getHqCompanyId() != null && !Objects.equals(query.getHqCompanyId(), targetHqId)) {
            throw new ServiceException("无权操作目标总部组织关系");
        }
        // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
        query.setTargetCompanyId(targetHqId);
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        query.setHqCompanyId(targetHqId);
    }

    /**
     * bindCRM总部一级查询ToTarget。
     *
     * @param query 参数
     * @param targetHqId target Hq ID
     */
    private void bindCrmHqFirstQueryToTarget(CrmHqFirstContractImportQuery query, Long targetHqId) {
        if (query.getHqCompanyId() != null && !Objects.equals(query.getHqCompanyId(), targetHqId)) {
            throw new ServiceException("无权操作目标总部组织关系");
        }
        // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
        query.setTargetCompanyId(targetHqId);
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        query.setHqCompanyId(targetHqId);
    }

    /**
     * bind总部一级DtoToTarget。
     *
     * @param dto 参数
     * @param targetHqId target Hq ID
     */
    private void bindHqFirstDtoToTarget(HqFirstContractDTO dto, Long targetHqId) {
        if (dto.getHqCompanyId() != null && !Objects.equals(dto.getHqCompanyId(), targetHqId)) {
            throw new ServiceException("无权操作目标总部组织关系");
        }
        // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
        dto.setTargetCompanyId(targetHqId);
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        dto.setHqCompanyId(targetHqId);
    }

    /**
     * bindCRM总部一级DtoToTarget。
     *
     * @param dto 参数
     * @param targetHqId target Hq ID
     */
    private void bindCrmHqFirstDtoToTarget(CrmHqFirstContractImportDTO dto, Long targetHqId) {
        if (dto.getHqCompanyId() != null && !Objects.equals(dto.getHqCompanyId(), targetHqId)) {
            throw new ServiceException("无权操作目标总部组织关系");
        }
        // 调用setTargetCompanyId方法，复用统一能力并保证业务规则一致。
        dto.setTargetCompanyId(targetHqId);
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        dto.setHqCompanyId(targetHqId);
    }

    /**
     * 校验总部一级InTarget。
     *
     * @param entity 参数
     * @param targetHqId target Hq ID
     */
    private void validateHqFirstInTarget(HqFirstContract entity, Long targetHqId) {
        if (!Objects.equals(entity.getHqCompanyId(), targetHqId)) {
            throw new ServiceException("无权操作目标总部组织关系");
        }
    }

    /**
     * 校验一级公司BelongsToTarget总部。
     *
     * @param firstCompanyId first Company ID
     * @param targetHqId target Hq ID
     */
    private void validateFirstCompanyBelongsToTargetHq(Long firstCompanyId, Long targetHqId) {
        if (firstCompanyId == null) {
            throw new ServiceException("一级网点公司ID不能为空");
        }
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, targetHqId)
                .eq(HqFirstContract::getFirstCompanyId, firstCompanyId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(HqFirstContract::getStatus, STATUS_ENABLED);
        // 说明：执行该步骤以保证业务流程正确。
        if (hqFirstContractMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("一级网点不属于目标总部");
        }
    }

    /**
     * 校验Import总部公司。
     *
     * @param hqCompanyId hq Company ID
     */
    private void validateImportHqCompany(Long hqCompanyId) {
        // 调用buildSubjectTypeMap方法，复用统一能力并保证业务规则一致。
        Map<String, String> subjectTypeMap = buildSubjectTypeMap();
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany hqCompany = requireCompany(hqCompanyId, "总部公司");
        // 调用validateEnabledCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledCompany(hqCompany, "总部公司");
        // 调用validateHqCompany方法，复用统一能力并保证业务规则一致。
        validateHqCompany(hqCompany, subjectTypeMap);
    }

    /**
     * 解析SalesOrgBy总部公司ID。
     *
     * @param hqCompanyId hq Company ID
     * @return 处理结果
     */
    private String resolveSalesOrgByHqCompanyId(Long hqCompanyId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany hqCompany = requireCompany(hqCompanyId, "总部公司");
        // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
        String salesOrg = StrUtil.trim(hqCompany.getSalesOrg());
        if (StrUtil.isBlank(salesOrg)) {
            throw new ServiceException("当前总部未维护销售组织");
        }
        return salesOrg;
    }

    /**
     * 分页查询SnapshotsBySalesOrg列表。
     *
     * @param salesOrg 参数
     * @return 处理结果
     */
    private List<CrmHqFirstContractSnapshot> listSnapshotsBySalesOrg(String salesOrg) {
        LambdaQueryWrapper<CrmHqFirstContractSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmHqFirstContractSnapshot::getSalesOrg, salesOrg)
                .orderByDesc(CrmHqFirstContractSnapshot::getCrmOperTime)
                .orderByDesc(CrmHqFirstContractSnapshot::getCrmAddTime)
                .orderByAsc(CrmHqFirstContractSnapshot::getKunnr)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(CrmHqFirstContractSnapshot::getId);
        // 说明：执行该步骤以保证业务流程正确。
        return crmHqFirstContractSnapshotMapper.selectList(wrapper);
    }

    /**
     * 构建CRMImport视图列表。
     *
     * @param hqCompanyId hq Company ID
     * @param snapshots 参数
     * @return 处理结果
     */
    private List<CrmHqFirstContractImportVO> buildCrmImportVOList(Long hqCompanyId,
                                                                  List<CrmHqFirstContractSnapshot> snapshots) {
        if (CollUtil.isEmpty(snapshots)) {
            return Collections.emptyList();
        }

        Set<String> companyCodes = snapshots.stream()
                .map(CrmHqFirstContractSnapshot::getKunnr)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> regionCodes = snapshots.stream()
                .map(CrmHqFirstContractSnapshot::getRegionCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 调用loadCompanyByCode方法，复用统一能力并保证业务规则一致。
        Map<String, SysCompany> companyByCode = loadCompanyByCode(companyCodes);
        // 调用loadRegionsByCode方法，复用统一能力并保证业务规则一致。
        Map<String, List<SysRegion>> regionByCode = loadRegionsByCode(regionCodes);
        Set<Long> firstCompanyIds = companyByCode.values().stream()
                .map(SysCompany::getId)
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用loadExistingContractFirstCompanyIds方法，复用统一能力并保证业务规则一致。
        Set<Long> existingFirstCompanyIds = loadExistingContractFirstCompanyIds(hqCompanyId, firstCompanyIds);

        // 调用size方法，复用统一能力并保证业务规则一致。
        List<CrmHqFirstContractImportVO> result = new ArrayList<>(snapshots.size());
        for (CrmHqFirstContractSnapshot snapshot : snapshots) {
            // 调用CrmHqFirstContractImportVO方法，复用统一能力并保证业务规则一致。
            CrmHqFirstContractImportVO vo = new CrmHqFirstContractImportVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(snapshot.getId());
            // 调用getKunnr方法，复用统一能力并保证业务规则一致。
            vo.setKunnr(snapshot.getKunnr());
            // 调用getCustId方法，复用统一能力并保证业务规则一致。
            vo.setCustId(snapshot.getCustId());
            // 调用getCrmCompanyName方法，复用统一能力并保证业务规则一致。
            vo.setCrmCompanyName(snapshot.getCrmCompanyName());
            // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
            vo.setSalesOrg(snapshot.getSalesOrg());
            // 调用getRegionCode方法，复用统一能力并保证业务规则一致。
            vo.setRegionCode(snapshot.getRegionCode());
            // 调用getRegionName方法，复用统一能力并保证业务规则一致。
            vo.setRegionName(snapshot.getRegionName());
            // 调用getAliveFlag方法，复用统一能力并保证业务规则一致。
            vo.setAliveFlag(snapshot.getAliveFlag());
            // 调用getCrmAddTime方法，复用统一能力并保证业务规则一致。
            vo.setCrmAddTime(snapshot.getCrmAddTime());
            // 调用getCrmOperTime方法，复用统一能力并保证业务规则一致。
            vo.setCrmOperTime(snapshot.getCrmOperTime());

            // 调用getKunnr方法，复用统一能力并保证业务规则一致。
            SysCompany localCompany = companyByCode.get(StrUtil.trim(snapshot.getKunnr()));
            if (localCompany == null) {
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark("CRM 客户未匹配本地一级公司");
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setFirstCompanyId(localCompany.getId());
            // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
            vo.setFirstCompanyName(localCompany.getCompanyName());
            if (existingFirstCompanyIds.contains(localCompany.getId())) {
                // 调用setExistingContract方法，复用统一能力并保证业务规则一致。
                vo.setExistingContract(Boolean.TRUE);
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark("已存在正式签约");
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            if (!CompanyCategoryEnum.getFirstLevelTypeCodes().contains(localCompany.getTypeCode())) {
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark("CRM 客户未匹配本地一级公司");
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            if (!Objects.equals(localCompany.getStatus(), STATUS_ENABLED)) {
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark("一级公司已停用");
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }

            // 调用getRegionCode方法，复用统一能力并保证业务规则一致。
            RegionMatchResult regionMatch = matchRegion(hqCompanyId, StrUtil.trim(snapshot.getRegionCode()), regionByCode);
            if (regionMatch.getRegion() == null) {
                // 调用getRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark(regionMatch.getRemark());
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setRegionId(regionMatch.getRegion().getId());
            // 调用getRegionName方法，复用统一能力并保证业务规则一致。
            vo.setLocalRegionName(regionMatch.getRegion().getRegionName());
            // 调用setExistingContract方法，复用统一能力并保证业务规则一致。
            vo.setExistingContract(Boolean.FALSE);
            // 调用setCanImport方法，复用统一能力并保证业务规则一致。
            vo.setCanImport(Boolean.TRUE);
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * load公司By编码。
     *
     * @param companyCodes 参数
     * @return 处理结果
     */
    private Map<String, SysCompany> loadCompanyByCode(Set<String> companyCodes) {
        if (CollUtil.isEmpty(companyCodes)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(SysCompany::getCompanyCode, companyCodes);
        // 说明：执行该步骤以保证业务流程正确。
        return sysCompanyMapper.selectList(wrapper).stream()
                .filter(item -> StrUtil.isNotBlank(item.getCompanyCode()))
                // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(item -> StrUtil.trim(item.getCompanyCode()), item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * loadRegionsBy编码。
     *
     * @param regionCodes 参数
     * @return 处理结果
     */
    private Map<String, List<SysRegion>> loadRegionsByCode(Set<String> regionCodes) {
        if (CollUtil.isEmpty(regionCodes)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(SysRegion::getRegionCode, regionCodes);
        // 说明：执行该步骤以保证业务流程正确。
        return sysRegionMapper.selectList(wrapper).stream()
                .filter(item -> StrUtil.isNotBlank(item.getRegionCode()))
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.groupingBy(item -> StrUtil.trim(item.getRegionCode()), LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * loadExisting合同一级公司Ids。
     *
     * @param hqCompanyId hq Company ID
     * @return 处理结果
     */
    private Set<Long> loadExistingContractFirstCompanyIds(Long hqCompanyId, Set<Long> firstCompanyIds) {
        if (CollUtil.isEmpty(firstCompanyIds)) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(HqFirstContract::getFirstCompanyId, firstCompanyIds);
        // 说明：执行该步骤以保证业务流程正确。
        return hqFirstContractMapper.selectList(wrapper).stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 分页查询一级二级Snapshots列表。
     *
     * @return 处理结果
     */
    private List<CrmFirstSecondRelationSnapshot> listFirstSecondSnapshots() {
        LambdaQueryWrapper<CrmFirstSecondRelationSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CrmFirstSecondRelationSnapshot::getCrmOperTime)
                .orderByAsc(CrmFirstSecondRelationSnapshot::getSecondCustId)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(CrmFirstSecondRelationSnapshot::getId);
        // 说明：执行该步骤以保证业务流程正确。
        return crmFirstSecondRelationSnapshotMapper.selectList(wrapper);
    }

    /**
     * 构建CRM一级二级Import视图列表。
     *
     * @param targetHqId target Hq ID
     * @param snapshots 参数
     * @return 处理结果
     */
    private List<CrmFirstSecondRelationImportVO> buildCrmFirstSecondImportVOList(Long targetHqId,
                                                                                 List<CrmFirstSecondRelationSnapshot> snapshots) {
        if (CollUtil.isEmpty(snapshots)) {
            return Collections.emptyList();
        }

        Set<Long> custIds = snapshots.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getFirstCustId(), item.getSecondCustId()))
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用loadCrmCompanySnapshotByCustIds方法，复用统一能力并保证业务规则一致。
        Map<Long, CrmBizCompanySnapshot> crmCompanyByCustId = loadCrmCompanySnapshotByCustIds(custIds);
        List<CrmFirstSecondRelationSnapshot> validSourceSnapshots = snapshots.stream()
                .filter(item -> isValidFirstSecondSourceSnapshot(item, crmCompanyByCustId))
                // 调用toList方法，复用统一能力并保证业务规则一致。
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
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用loadCompanyByCode方法，复用统一能力并保证业务规则一致。
        Map<String, SysCompany> companyByCode = loadCompanyByCode(companyCodes);
        Set<Long> firstCompanyIds = validSourceSnapshots.stream()
                .map(CrmFirstSecondRelationSnapshot::getFirstCustId)
                .map(crmCompanyByCustId::get)
                .filter(Objects::nonNull)
                .map(CrmBizCompanySnapshot::getSapCompanyCode)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .map(companyByCode::get)
                .filter(Objects::nonNull)
                .map(SysCompany::getId)
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用loadTargetHqFirstCompanyIds方法，复用统一能力并保证业务规则一致。
        Set<Long> allowedFirstCompanyIds = loadTargetHqFirstCompanyIds(targetHqId, firstCompanyIds);

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
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用loadFirstSecondRelationBySecondCompanyIds方法，复用统一能力并保证业务规则一致。
        Map<Long, FirstSecondRelation> relationBySecondCompanyId = loadFirstSecondRelationBySecondCompanyIds(secondCompanyIds);

        Set<Long> conflictFirstCompanyIds = relationBySecondCompanyId.values().stream()
                .map(FirstSecondRelation::getFirstCompanyId)
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysCompany> companyById = new LinkedHashMap<>();
        for (SysCompany company : companyByCode.values()) {
            // 调用getId方法，复用统一能力并保证业务规则一致。
            companyById.put(company.getId(), company);
        }
        // 调用loadCompanyByIds方法，复用统一能力并保证业务规则一致。
        companyById.putAll(loadCompanyByIds(conflictFirstCompanyIds));

        // 调用size方法，复用统一能力并保证业务规则一致。
        List<CrmFirstSecondRelationImportVO> result = new ArrayList<>(validSourceSnapshots.size());
        for (CrmFirstSecondRelationSnapshot snapshot : validSourceSnapshots) {
            // 调用CrmFirstSecondRelationImportVO方法，复用统一能力并保证业务规则一致。
            CrmFirstSecondRelationImportVO vo = new CrmFirstSecondRelationImportVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(snapshot.getId());
            // 调用getFirstCustId方法，复用统一能力并保证业务规则一致。
            vo.setFirstCustId(snapshot.getFirstCustId());
            // 调用getSecondCustId方法，复用统一能力并保证业务规则一致。
            vo.setSecondCustId(snapshot.getSecondCustId());
            // 调用getCrmOperTime方法，复用统一能力并保证业务规则一致。
            vo.setCrmOperTime(snapshot.getCrmOperTime());

            // 调用getFirstCustId方法，复用统一能力并保证业务规则一致。
            CrmBizCompanySnapshot firstSnapshot = crmCompanyByCustId.get(snapshot.getFirstCustId());
            if (firstSnapshot == null) {
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark("一级来源客户未在CRM公司快照中找到");
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            // 调用fillSourceCompanyInfo方法，复用统一能力并保证业务规则一致。
            fillSourceCompanyInfo(vo, firstSnapshot, true);
            // 调用getSapCompanyCode方法，复用统一能力并保证业务规则一致。
            SysCompany firstCompany = companyByCode.get(StrUtil.trim(firstSnapshot.getSapCompanyCode()));
            if (firstCompany != null) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                vo.setFirstCompanyId(firstCompany.getId());
                // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
                vo.setLocalFirstCompanyName(firstCompany.getCompanyName());
            }
            // 调用resolveFirstCompanyImportDisabledReason方法，复用统一能力并保证业务规则一致。
            String firstReason = resolveFirstCompanyImportDisabledReason(firstSnapshot, firstCompany);
            if (firstReason != null) {
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark(firstReason);
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            if (!allowedFirstCompanyIds.contains(firstCompany.getId())) {
                continue;
            }

            // 调用getSecondCustId方法，复用统一能力并保证业务规则一致。
            CrmBizCompanySnapshot secondSnapshot = crmCompanyByCustId.get(snapshot.getSecondCustId());
            if (secondSnapshot == null) {
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark("二级来源客户未在CRM公司快照中找到");
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }
            // 调用fillSourceCompanyInfo方法，复用统一能力并保证业务规则一致。
            fillSourceCompanyInfo(vo, secondSnapshot, false);
            // 调用getSapCompanyCode方法，复用统一能力并保证业务规则一致。
            SysCompany secondCompany = companyByCode.get(StrUtil.trim(secondSnapshot.getSapCompanyCode()));
            if (secondCompany != null) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                vo.setSecondCompanyId(secondCompany.getId());
                // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
                vo.setLocalSecondCompanyName(secondCompany.getCompanyName());
            }
            // 调用resolveSecondCompanyImportDisabledReason方法，复用统一能力并保证业务规则一致。
            String secondReason = resolveSecondCompanyImportDisabledReason(secondSnapshot, secondCompany);
            if (secondReason != null) {
                // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                vo.setCanImport(Boolean.FALSE);
                // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                vo.setMatchRemark(secondReason);
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }

            // 调用getId方法，复用统一能力并保证业务规则一致。
            FirstSecondRelation relation = relationBySecondCompanyId.get(secondCompany.getId());
            if (relation != null) {
                if (Objects.equals(relation.getFirstCompanyId(), firstCompany.getId())) {
                    // 调用setExistingRelation方法，复用统一能力并保证业务规则一致。
                    vo.setExistingRelation(Boolean.TRUE);
                    // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                    vo.setCanImport(Boolean.FALSE);
                    // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                    vo.setMatchRemark("已存在相同的一级二级关系");
                } else {
                    // 调用setConflictingRelation方法，复用统一能力并保证业务规则一致。
                    vo.setConflictingRelation(Boolean.TRUE);
                    // 调用setCanImport方法，复用统一能力并保证业务规则一致。
                    vo.setCanImport(Boolean.FALSE);
                    // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
                    SysCompany conflictFirst = companyById.get(relation.getFirstCompanyId());
                    if (conflictFirst == null) {
                        // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
                        vo.setMatchRemark("该二级网点已归属其他一级网点");
                    } else {
                        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
                        vo.setMatchRemark("该二级网点已归属其他一级网点：" + conflictFirst.getCompanyName());
                    }
                }
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(vo);
                continue;
            }

            // 调用setExistingRelation方法，复用统一能力并保证业务规则一致。
            vo.setExistingRelation(Boolean.FALSE);
            // 调用setConflictingRelation方法，复用统一能力并保证业务规则一致。
            vo.setConflictingRelation(Boolean.FALSE);
            // 调用setCanImport方法，复用统一能力并保证业务规则一致。
            vo.setCanImport(Boolean.TRUE);
            // 调用setMatchRemark方法，复用统一能力并保证业务规则一致。
            vo.setMatchRemark("可导入");
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * loadTarget总部一级公司Ids。
     *
     * @param targetHqId target Hq ID
     * @return 处理结果
     */
    private Set<Long> loadTargetHqFirstCompanyIds(Long targetHqId, Set<Long> firstCompanyIds) {
        if (targetHqId == null || CollUtil.isEmpty(firstCompanyIds)) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, targetHqId)
                .eq(HqFirstContract::getStatus, STATUS_ENABLED)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(HqFirstContract::getFirstCompanyId, firstCompanyIds);
        // 说明：执行该步骤以保证业务流程正确。
        return hqFirstContractMapper.selectList(wrapper).stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断是否Valid一级二级来源快照。
     *
     * @param snapshot 参数
     * @param crmCompanyByCustId crm Company By Cust ID
     */
    private boolean isValidFirstSecondSourceSnapshot(CrmFirstSecondRelationSnapshot snapshot,
                                                     Map<Long, CrmBizCompanySnapshot> crmCompanyByCustId) {
        if (snapshot == null) {
            return false;
        }
        // 调用getFirstCustId方法，复用统一能力并保证业务规则一致。
        CrmBizCompanySnapshot firstSnapshot = crmCompanyByCustId.get(snapshot.getFirstCustId());
        if (firstSnapshot == null || !Objects.equals(firstSnapshot.getCustRage(), 0)) {
            return false;
        }
        // 调用getSecondCustId方法，复用统一能力并保证业务规则一致。
        CrmBizCompanySnapshot secondSnapshot = crmCompanyByCustId.get(snapshot.getSecondCustId());
        return secondSnapshot != null && Objects.equals(secondSnapshot.getCustRage(), 3);
    }

    /**
     * fill来源公司Info。
     *
     * @param vo 参数
     * @param snapshot 参数
     * @param first 参数
     */
    private void fillSourceCompanyInfo(CrmFirstSecondRelationImportVO vo, CrmBizCompanySnapshot snapshot, boolean first) {
        if (first) {
            // 调用getSapCompanyCode方法，复用统一能力并保证业务规则一致。
            vo.setFirstCompanyCode(StrUtil.trim(snapshot.getSapCompanyCode()));
            // 调用getCustName方法，复用统一能力并保证业务规则一致。
            vo.setFirstCompanyName(snapshot.getCustName());
            return;
        }
        // 调用getSapCompanyCode方法，复用统一能力并保证业务规则一致。
        vo.setSecondCompanyCode(StrUtil.trim(snapshot.getSapCompanyCode()));
        // 调用getCustName方法，复用统一能力并保证业务规则一致。
        vo.setSecondCompanyName(snapshot.getCustName());
    }

    /**
     * 解析一级公司ImportDisabled原因。
     *
     * @param sourceSnapshot 参数
     * @param localCompany 参数
     * @return 处理结果
     */
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

    /**
     * 解析二级公司ImportDisabled原因。
     *
     * @param sourceSnapshot 参数
     * @param localCompany 参数
     * @return 处理结果
     */
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

    /**
     * loadCRM公司快照ByCustIds。
     *
     * @return 处理结果
     */
    private Map<Long, CrmBizCompanySnapshot> loadCrmCompanySnapshotByCustIds(Set<Long> custIds) {
        if (CollUtil.isEmpty(custIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(CrmBizCompanySnapshot::getCustId, custIds);
        // 说明：执行该步骤以保证业务流程正确。
        return crmBizCompanySnapshotMapper.selectList(wrapper).stream()
                .filter(item -> item.getCustId() != null)
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(CrmBizCompanySnapshot::getCustId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * load一级二级关系By二级公司Ids。
     *
     * @return 处理结果
     */
    private Map<Long, FirstSecondRelation> loadFirstSecondRelationBySecondCompanyIds(Set<Long> secondCompanyIds) {
        if (CollUtil.isEmpty(secondCompanyIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(FirstSecondRelation::getSecondCompanyId, secondCompanyIds);
        // 说明：执行该步骤以保证业务流程正确。
        return firstSecondRelationMapper.selectList(wrapper).stream()
                .filter(item -> item.getSecondCompanyId() != null)
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(FirstSecondRelation::getSecondCompanyId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * load公司ByIds。
     *
     * @return 处理结果
     */
    private Map<Long, SysCompany> loadCompanyByIds(Set<Long> companyIds) {
        if (CollUtil.isEmpty(companyIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(SysCompany::getId, companyIds);
        // 说明：执行该步骤以保证业务流程正确。
        return sysCompanyMapper.selectList(wrapper).stream()
                .filter(item -> item.getId() != null)
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompany::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * match地区。
     *
     * @param hqCompanyId hq Company ID
     * @param regionCode 参数
     * @param regionByCode 参数
     * @return 处理结果
     */
    private RegionMatchResult matchRegion(Long hqCompanyId,
                                          String regionCode,
                                          Map<String, List<SysRegion>> regionByCode) {
        if (StrUtil.isBlank(regionCode)) {
            return RegionMatchResult.fail("CRM大区编码为空");
        }
        // 调用get方法，复用统一能力并保证业务规则一致。
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

    /**
     * 构建分页结果。
     *
     * @param records 参数
     * @param pageNum 参数
     * @param pageSize 参数
     * @return 处理结果
     */
    private <T> PageResult<T> buildPageResult(List<T> records,
                                              Integer pageNum,
                                              Integer pageSize) {
        int currentPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        // 调用size方法，复用统一能力并保证业务规则一致。
        int total = records == null ? 0 : records.size();
        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0L, currentPageNum, currentPageSize);
        }
        // 调用min方法，复用统一能力并保证业务规则一致。
        int fromIndex = Math.min((currentPageNum - 1) * currentPageSize, total);
        // 调用min方法，复用统一能力并保证业务规则一致。
        int toIndex = Math.min(fromIndex + currentPageSize, total);
        return PageResult.of(records.subList(fromIndex, toIndex), (long) total, currentPageNum, currentPageSize);
    }

    /**
     * filterCRMImportRecords。
     *
     * @param records 参数
     * @param query 参数
     * @return 处理结果
     */
    private List<CrmHqFirstContractImportVO> filterCrmImportRecords(List<CrmHqFirstContractImportVO> records,
                                                                    CrmHqFirstContractImportQuery query) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        // 调用getKunnr方法，复用统一能力并保证业务规则一致。
        String kunnr = StrUtil.trim(query.getKunnr());
        return records.stream()
                .filter(item -> query.getFirstCompanyId() == null
                        || Objects.equals(item.getFirstCompanyId(), query.getFirstCompanyId()))
                .filter(item -> query.getRegionId() == null
                        || Objects.equals(item.getRegionId(), query.getRegionId()))
                .filter(item -> StrUtil.isBlank(kunnr)
                        || StrUtil.containsIgnoreCase(StrUtil.blankToDefault(item.getKunnr(), ""), kunnr))
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * filterCRM一级二级ImportRecords。
     *
     * @param records 参数
     * @param query 参数
     * @return 处理结果
     */
    private List<CrmFirstSecondRelationImportVO> filterCrmFirstSecondImportRecords(List<CrmFirstSecondRelationImportVO> records,
                                                                                   CrmFirstSecondRelationImportQuery query) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        // 调用getFirstCompanyCode方法，复用统一能力并保证业务规则一致。
        String firstCompanyCode = StrUtil.trim(query.getFirstCompanyCode());
        // 调用getSecondCompanyCode方法，复用统一能力并保证业务规则一致。
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
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * require公司。
     *
     * @param label 参数
     * @return 处理结果
     */
    private SysCompany requireCompany(Long companyId, String label) {
        if (companyId == null) {
            throw new ServiceException(label + "ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(label + "不存在");
        }
        return company;
    }

    /**
     * 校验Enabled公司。
     *
     * @param company 参数
     * @param label 参数
     */
    private void validateEnabledCompany(SysCompany company, String label) {
        if (!Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException(label + "已停用");
        }
    }

    /**
     * 校验总部公司。
     *
     * @param company 参数
     * @param subjectTypeMap 参数
     */
    private void validateHqCompany(SysCompany company, Map<String, String> subjectTypeMap) {
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        String subjectType = subjectTypeMap.get(company.getTypeCode());
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            throw new ServiceException("总部公司必须是总部类型");
        }
    }

    /**
     * 校验一级公司。
     *
     * @param company 参数
     */
    private void validateFirstCompany(SysCompany company) {
        if (!CompanyCategoryEnum.getFirstLevelTypeCodes().contains(company.getTypeCode())) {
            throw new ServiceException("一级网点公司必须是一级网点类型");
        }
    }

    /**
     * 校验二级公司。
     *
     * @param company 参数
     */
    private void validateSecondCompany(SysCompany company) {
        if (!CompanyCategoryEnum.getSecondLevelTypeCodes().contains(company.getTypeCode())) {
            throw new ServiceException("二级网点公司必须是二级网点类型");
        }
    }

    /**
     * 校验地区BelongTo总部。
     *
     * @param regionId region ID
     * @param hqCompanyId hq Company ID
     */
    private void validateRegionBelongToHq(Long regionId, Long hqCompanyId) {
        if (regionId == null) {
            return;
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysRegion region = sysRegionMapper.selectById(regionId);
        if (region == null) {
            throw new ServiceException("所属大区不存在");
        }
        if (!Objects.equals(region.getCompanyId(), hqCompanyId)) {
            throw new ServiceException("所属大区不属于当前总部");
        }
    }

    /**
     * 校验合同状态。
     *
     * @param status 参数
     * @param label 参数
     */
    private void validateContractStatus(Integer status, String label) {
        if (status == null) {
            return;
        }
        if (!Objects.equals(STATUS_ENABLED, status) && !Objects.equals(STATUS_DISABLED, status)) {
            throw new ServiceException(label + "状态不合法");
        }
    }

    /**
     * check总部一级Duplicate。
     *
     * @param excludeId exclude ID
     * @param hqCompanyId hq Company ID
     * @param firstCompanyId first Company ID
     */
    private void checkHqFirstDuplicate(Long excludeId, Long hqCompanyId, Long firstCompanyId) {
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(HqFirstContract::getFirstCompanyId, firstCompanyId);
        if (excludeId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(HqFirstContract::getId, excludeId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (hqFirstContractMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该总部与一级网点的签约关系已存在");
        }
    }

    /**
     * check合同编号Duplicate。
     *
     * @param excludeId exclude ID
     * @param contractNo 参数
     */
    private void checkContractNoDuplicate(Long excludeId, String contractNo) {
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(HqFirstContract::getContractNo, contractNo);
        if (excludeId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(HqFirstContract::getId, excludeId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (hqFirstContractMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("合同编号已存在");
        }
    }

    /**
     * check一级二级Duplicate。
     *
     * @param excludeId exclude ID
     * @param firstCompanyId first Company ID
     * @param secondCompanyId second Company ID
     */
    private void checkFirstSecondDuplicate(Long excludeId, Long firstCompanyId, Long secondCompanyId) {
        LambdaQueryWrapper<FirstSecondRelation> pairWrapper = new LambdaQueryWrapper<>();
        pairWrapper.eq(FirstSecondRelation::getFirstCompanyId, firstCompanyId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(FirstSecondRelation::getSecondCompanyId, secondCompanyId);
        if (excludeId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            pairWrapper.ne(FirstSecondRelation::getId, excludeId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (firstSecondRelationMapper.selectCount(pairWrapper) > 0) {
            throw new ServiceException("该一级、二级网点的从属关系已存在");
        }

        LambdaQueryWrapper<FirstSecondRelation> secondWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        secondWrapper.eq(FirstSecondRelation::getSecondCompanyId, secondCompanyId);
        if (excludeId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            secondWrapper.ne(FirstSecondRelation::getId, excludeId);
        }
        // 调用selectOne方法，复用统一能力并保证业务规则一致。
        FirstSecondRelation relation = firstSecondRelationMapper.selectOne(secondWrapper);
        if (relation != null) {
            throw new ServiceException("该二级网点已归属其他一级网点");
        }
    }

    /**
     * 新增总部一级删除Record。
     *
     * @param entity 参数
     */
    private void saveHqFirstDeleteRecord(HqFirstContract entity) {
        // 调用HqFirstContractRecord方法，复用统一能力并保证业务规则一致。
        HqFirstContractRecord record = new HqFirstContractRecord();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        record.setSourceId(entity.getId());
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        record.setHqCompanyId(entity.getHqCompanyId());
        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        record.setFirstCompanyId(entity.getFirstCompanyId());
        // 调用getRegionId方法，复用统一能力并保证业务规则一致。
        record.setRegionId(entity.getRegionId());
        // 调用getContractNo方法，复用统一能力并保证业务规则一致。
        record.setContractNo(entity.getContractNo());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        record.setStatus(entity.getStatus());
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        record.setRemark(entity.getRemark());
        // 调用setOperationType方法，复用统一能力并保证业务规则一致。
        record.setOperationType(OPERATION_DELETE);
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        record.setOperatorUserId(getCurrentUserId());
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        record.setOperatorCompanyId(getCurrentCompanyId());
        // 说明：执行该步骤以保证业务流程正确。
        hqFirstContractRecordMapper.insert(record);
    }

    /**
     * 新增一级二级删除Record。
     *
     * @param entity 参数
     */
    private void saveFirstSecondDeleteRecord(FirstSecondRelation entity) {
        // 调用FirstSecondRelationRecord方法，复用统一能力并保证业务规则一致。
        FirstSecondRelationRecord record = new FirstSecondRelationRecord();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        record.setSourceId(entity.getId());
        // 调用getFirstCompanyId方法，复用统一能力并保证业务规则一致。
        record.setFirstCompanyId(entity.getFirstCompanyId());
        // 调用getSecondCompanyId方法，复用统一能力并保证业务规则一致。
        record.setSecondCompanyId(entity.getSecondCompanyId());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        record.setStatus(entity.getStatus());
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        record.setRemark(entity.getRemark());
        // 调用setOperationType方法，复用统一能力并保证业务规则一致。
        record.setOperationType(OPERATION_DELETE);
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        record.setOperatorUserId(getCurrentUserId());
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        record.setOperatorCompanyId(getCurrentCompanyId());
        // 说明：执行该步骤以保证业务流程正确。
        firstSecondRelationRecordMapper.insert(record);
    }

    /**
     * 获取Current用户ID。
     *
     * @return 处理结果
     */
    private Long getCurrentUserId() {
        try {
            return SecurityContext.getCurrentUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 获取Current公司ID。
     *
     * @return 处理结果
     */
    private Long getCurrentCompanyId() {
        try {
            return SecurityContext.getCurrentCompanyId();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 应用Default状态。
     *
     * @param entity 参数
     */
    private void applyDefaultStatus(HqFirstContract entity) {
        if (entity.getStatus() == null) {
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            entity.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * 应用Default状态。
     *
     * @param entity 参数
     */
    private void applyDefaultStatus(FirstSecondRelation entity) {
        if (entity.getStatus() == null) {
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            entity.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * 规范化总部一级。
     *
     * @param dto 参数
     */
    private void normalizeHqFirst(HqFirstContractDTO dto) {
        // 调用getContractNo方法，复用统一能力并保证业务规则一致。
        dto.setContractNo(normalizeNullableText(dto.getContractNo()));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        dto.setRemark(normalizeNullableText(dto.getRemark()));
    }

    /**
     * 规范化一级二级。
     *
     * @param dto 参数
     */
    private void normalizeFirstSecond(FirstSecondRelationDTO dto) {
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        dto.setRemark(normalizeNullableText(dto.getRemark()));
    }

    /**
     * 规范化NullableText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeNullableText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * defaultInt。
     *
     * @param value 参数
     * @return 处理结果
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 判断是否Duplicate一级二级消息。
     *
     * @param message 参数
     */
    private boolean isDuplicateFirstSecondMessage(String message) {
        return StrUtil.equals(message, "该一级、二级网点的从属关系已存在")
                // 调用equals方法，复用统一能力并保证业务规则一致。
                || StrUtil.equals(message, "从属关系已存在，请勿重复保存");
    }

    /**
     * 判断是否一级二级Conflict消息。
     *
     * @param message 参数
     */
    private boolean isFirstSecondConflictMessage(String message) {
        return StrUtil.equals(message, "该二级网点已归属其他一级网点");
    }

    /**
     * 判断是否Duplicate总部一级消息。
     *
     * @param message 参数
     */
    private boolean isDuplicateHqFirstMessage(String message) {
        return StrUtil.equals(message, "该总部与一级网点的签约关系已存在")
                // 调用equals方法，复用统一能力并保证业务规则一致。
                || StrUtil.equals(message, "签约关系已存在，请勿重复保存");
    }

    /**
     * translate总部一级DuplicateException。
     *
     * @param ex 参数
     * @return 处理结果
     */
    private ServiceException translateHqFirstDuplicateException(DuplicateKeyException ex) {
        // 调用getMessage方法，复用统一能力并保证业务规则一致。
        String message = ex.getMessage();
        if (StrUtil.containsIgnoreCase(message, "uk_hq_first")) {
            return new ServiceException("该总部与一级网点的签约关系已存在");
        }
        return new ServiceException("签约关系已存在，请勿重复保存");
    }

    /**
     * translate一级二级DuplicateException。
     *
     * @param ex 参数
     * @return 处理结果
     */
    private ServiceException translateFirstSecondDuplicateException(DuplicateKeyException ex) {
        // 调用getMessage方法，复用统一能力并保证业务规则一致。
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

        /**
     * 系统地区字段。
     *
     * @param region 参数
     * @param remark 参数
     * @return 处理结果
         */
        private final SysRegion region;
        private final String remark;

        /**
     * 构造系统合同实例。
     *
     * @param region 参数
     * @param remark 参数
     * @return 处理结果
         */
        private RegionMatchResult(SysRegion region, String remark) {
            this.region = region;
            this.remark = remark;
        }

        /**
     * success。
     *
     * @param region 参数
     * @return 处理结果
         */
        private static RegionMatchResult success(SysRegion region) {
            return new RegionMatchResult(region, null);
        }

        /**
     * fail。
     *
     * @param remark 参数
     * @return 处理结果
         */
        private static RegionMatchResult fail(String remark) {
            return new RegionMatchResult(null, remark);
        }

        /**
     * 获取地区。
     *
     * @return 处理结果
         */
        private SysRegion getRegion() {
            return region;
        }

        /**
     * 获取Remark。
     *
     * @return 处理结果
         */
        private String getRemark() {
            return remark;
        }
    }
}




