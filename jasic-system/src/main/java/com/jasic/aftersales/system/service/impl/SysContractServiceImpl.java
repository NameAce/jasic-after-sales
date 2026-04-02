package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.CompanyCategoryEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.FirstSecondRelationDTO;
import com.jasic.aftersales.system.domain.dto.HqFirstContractDTO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelationRecord;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.HqFirstContractRecord;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRegion;
import com.jasic.aftersales.system.domain.query.FirstSecondRelationQuery;
import com.jasic.aftersales.system.domain.query.HqFirstContractQuery;
import com.jasic.aftersales.system.domain.vo.FirstSecondRelationVO;
import com.jasic.aftersales.system.domain.vo.HqFirstContractVO;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
}
