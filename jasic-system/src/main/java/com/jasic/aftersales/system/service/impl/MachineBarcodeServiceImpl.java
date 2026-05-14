package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeDTO;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeImportItemDTO;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.query.MachineBarcodeQuery;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.CompanyDataAccessService;
import com.jasic.aftersales.system.service.IMachineBarcodeService;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import com.jasic.aftersales.system.service.support.MachineBarcodeWarrantyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 条码档案管理 Service 实现
 *
 * @author Codex
 * @date 2026/04/01
 */
@Service
public class MachineBarcodeServiceImpl implements IMachineBarcodeService {

    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    @Resource
    private CompanyDataAccessService companyDataAccessService;

    @Override
    public PageResult<MachineBarcodeVO> listPage(MachineBarcodeQuery query) {
        Long ownerHqId = resolveOwnerHqId(query.getOwnerHqId(), query.getTargetCompanyId());
        Page<MachineBarcode> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getHqCompanyId, ownerHqId);
        if (StrUtil.isNotBlank(query.getBarcode())) {
            wrapper.like(MachineBarcode::getBarcode, query.getBarcode().trim());
        }
        if (StrUtil.isNotBlank(query.getDeliverNumber())) {
            wrapper.like(MachineBarcode::getDeliverNumber, query.getDeliverNumber().trim());
        }
        if (StrUtil.isNotBlank(query.getCustId())) {
            wrapper.like(MachineBarcode::getCustId, query.getCustId().trim());
        }
        if (StrUtil.isNotBlank(query.getSalesOrg())) {
            wrapper.like(MachineBarcode::getSalesOrg, query.getSalesOrg().trim());
        }
        if (StrUtil.isNotBlank(query.getProductCode())) {
            wrapper.like(MachineBarcode::getProductCode, query.getProductCode().trim());
        }
        if (StrUtil.isNotBlank(query.getMachineNo())) {
            wrapper.like(MachineBarcode::getMachineNo, query.getMachineNo().trim());
        }
        if (StrUtil.isNotBlank(query.getProductModel())) {
            wrapper.like(MachineBarcode::getProductModel, query.getProductModel().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(MachineBarcode::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(MachineBarcode::getId);
        // ??????????????????????????
        Page<MachineBarcode> result = machineBarcodeMapper.selectPage(page, wrapper);
        List<MachineBarcodeVO> records = buildMachineBarcodeVOList(result.getRecords());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @param ownerHqIdParam ??
     * @param targetCompanyId ????ID
     * @return ????
     */
    @Override
    public MachineBarcodeVO getById(Long id, Long ownerHqIdParam, Long targetCompanyId) {
        Long ownerHqId = resolveOwnerHqId(ownerHqIdParam, targetCompanyId);
        MachineBarcode entity = selectByIdAndOwnerHq(id, ownerHqId);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        return buildMachineBarcodeVOList(Collections.singletonList(entity)).get(0);
    }

    /**
     * ???????
     *
     * @return ????
     */
    @Override
    public List<SysCompanySimpleVO> listHqCompanyOptions() {
        if (!SecurityContext.isPlatformUser()) {
            if (SubjectTypeEnum.HQ.getCode().equals(SecurityContext.getCurrentSubjectType())) {
                Long currentCompanyId = SecurityContext.getCurrentCompanyId();
                if (currentCompanyId == null) {
                    throw new ServiceException("缺少公司数据访问上下文");
                }
                // ?????????????????????????????
                validateHqCompany(currentCompanyId, buildSubjectTypeMap());
                // ??????????????????????????
                SysCompany company = sysCompanyMapper.selectById(currentCompanyId);
                if (company == null || !Objects.equals(company.getStatus(), STATUS_ENABLED)) {
                    throw new ServiceException("当前总部不存在或已停用");
                }
                return Collections.singletonList(buildCompanySimpleVo(company, buildTypeNameMap()));
            }
            throw new ServiceException("当前公司不支持维护条码档案");
        }
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        List<String> hqTypeCodes = listTypeCodesBySubjectType(companyTypes, SubjectTypeEnum.HQ.getCode());
        if (CollUtil.isEmpty(hqTypeCodes)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, hqTypeCodes)
                .eq(SysCompany::getStatus, STATUS_ENABLED)
                .orderByAsc(SysCompany::getCompanyName)
                .orderByAsc(SysCompany::getId);
        List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);

        Map<String, String> typeNameMap = companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
        List<SysCompanySimpleVO> result = new ArrayList<>();
        for (SysCompany company : companies) {
            SysCompanySimpleVO vo = new SysCompanySimpleVO();
            vo.setId(company.getId());
            vo.setCompanyName(company.getCompanyName());
            vo.setCompanyCode(company.getCompanyCode());
            vo.setTypeCode(company.getTypeCode());
            vo.setTypeName(typeNameMap.get(company.getTypeCode()));
            result.add(vo);
        }
        return result;
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ????
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(MachineBarcodeDTO dto) {
        Long ownerHqId = resolveOwnerHqId(dto.getOwnerHqId(), dto.getTargetCompanyId());
        MachineBarcode entity = new MachineBarcode();
        BeanUtil.copyProperties(dto, entity);
        entity.setHqCompanyId(ownerHqId);
        normalizeEntity(entity);
        applyDefaultStatus(entity);
        // ?????????????????????????????
        validateMachineBarcode(entity, null, buildSubjectTypeMap());
        // ???????????????????????
        machineBarcodeMapper.insert(entity);
        return entity.getId();
    }

    /**
     * ?????
     *
     * @param dto ????
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MachineBarcodeDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("条码档案ID不能为空");
        }
        Long ownerHqId = resolveOwnerHqId(dto.getOwnerHqId(), dto.getTargetCompanyId());
        MachineBarcode entity = selectByIdAndOwnerHq(dto.getId(), ownerHqId);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        BeanUtil.copyProperties(dto, entity);
        entity.setHqCompanyId(ownerHqId);
        normalizeEntity(entity);
        applyDefaultStatus(entity);
        // ?????????????????????????????
        validateMachineBarcode(entity, entity.getId(), buildSubjectTypeMap());
        LambdaQueryWrapper<MachineBarcode> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(MachineBarcode::getId, entity.getId())
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // ???????????????????????
        if (machineBarcodeMapper.update(entity, updateWrapper) == 0) {
            throw new ServiceException("无权操作该条码档案");
        }
    }

    /**
     * ?????
     *
     * @param id ??ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Long id) {
        Long ownerHqId = resolveOwnerHqId(null, null);
        MachineBarcode entity = selectByIdAndOwnerHq(id, ownerHqId);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        LambdaQueryWrapper<MachineBarcode> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(MachineBarcode::getId, id)
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // ???????????????????????
        if (machineBarcodeMapper.delete(deleteWrapper) == 0) {
            throw new ServiceException("无权操作该条码档案");
        }
    }

    /**
     * ?? importItems ?????
     *
     * @param items ??
     * @return ????
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer importItems(List<MachineBarcodeImportItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            throw new ServiceException("导入数据不能为空");
        }

        Map<String, String> subjectTypeMap = buildSubjectTypeMap();
        Set<String> payloadBarcodes = new LinkedHashSet<>();
        List<MachineBarcode> normalizedItems = new ArrayList<>();
        for (MachineBarcodeImportItemDTO item : items) {
            MachineBarcode incoming = new MachineBarcode();
            BeanUtil.copyProperties(item, incoming);
            normalizeEntity(incoming);
            applyDefaultStatus(incoming);
            // ?????????????????????????????
            validateImportPayloadItem(incoming, subjectTypeMap);
            if (!payloadBarcodes.add(incoming.getBarcode())) {
                throw new ServiceException("导入数据存在重复条码：" + incoming.getBarcode());
            }
            normalizedItems.add(incoming);
        }

        int count = 0;
        for (MachineBarcode incoming : normalizedItems) {
            MachineBarcode existing = findByBarcodeAndOwnerHq(incoming.getBarcode(), incoming.getHqCompanyId());
            if (existing == null) {
                validateMachineBarcode(incoming, null, subjectTypeMap);
                // ???????????????????????
                machineBarcodeMapper.insert(incoming);
            } else {
                BeanUtil.copyProperties(incoming, existing);
                validateMachineBarcode(existing, existing.getId(), subjectTypeMap);
                LambdaQueryWrapper<MachineBarcode> updateWrapper = new LambdaQueryWrapper<>();
                updateWrapper.eq(MachineBarcode::getId, existing.getId())
                        .eq(MachineBarcode::getHqCompanyId, incoming.getHqCompanyId());
                if (machineBarcodeMapper.update(existing, updateWrapper) == 0) {
                    throw new ServiceException("无权操作该条码档案");
                }
            }
            count++;
        }
        return count;
    }

    /**
     * ???????
     *
     * @param records ??
     * @return ????
     */
    private List<MachineBarcodeVO> buildMachineBarcodeVOList(List<MachineBarcode> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }

        List<Long> hqCompanyIds = records.stream()
                .map(MachineBarcode::getHqCompanyId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, SysCompany> companyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(hqCompanyIds)) {
            LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SysCompany::getId, hqCompanyIds);
            // ??????????????????????????
            List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
            companyMap = companies.stream()
                    .collect(Collectors.toMap(SysCompany::getId, company -> company, (a, b) -> a));
        }

        List<MachineBarcodeVO> result = new ArrayList<>();
        for (MachineBarcode record : records) {
            MachineBarcodeVO vo = BeanUtil.copyProperties(record, MachineBarcodeVO.class);
            vo.setLastOutDate(MachineBarcodeWarrantyResolver.resolveLastOutDate(record));
            vo.setWarrantyStatus(MachineBarcodeWarrantyResolver.resolveWarrantyStatus(record));
            SysCompany company = companyMap.get(record.getHqCompanyId());
            if (company != null) {
                vo.setHqCompanyName(company.getCompanyName());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * ???????
     *
     * @param entity ????
     * @param currentId current ID
     * @param subjectTypeMap ??
     */
    private void validateMachineBarcode(MachineBarcode entity, Long currentId, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        // ?????????????????????????????
        validateStatus(entity.getStatus());
        validateUniqueBarcode(entity, currentId);
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    /**
     * ???????
     *
     * @param entity ????
     * @param subjectTypeMap ??
     */
    private void validateImportPayloadItem(MachineBarcode entity, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        // ?????????????????????????????
        validateStatus(entity.getStatus());
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    /**
     * ???????
     *
     * @param status ??
     */
    private void validateStatus(Integer status) {
        if (status == null) {
            throw new ServiceException("条码档案状态不能为空");
        }
        if (!Objects.equals(STATUS_ENABLED, status) && !Objects.equals(STATUS_DISABLED, status)) {
            throw new ServiceException("条码档案状态不合法");
        }
    }

    /**
     * ???????
     *
     * @param entity ????
     * @param currentId current ID
     */
    private void validateUniqueBarcode(MachineBarcode entity, Long currentId) {
        MachineBarcode duplicated = findByBarcodeAndOwnerHq(entity.getBarcode(), entity.getHqCompanyId());
        if (duplicated != null && !Objects.equals(duplicated.getId(), currentId)) {
            throw new ServiceException("机器条码已存在");
        }
    }

    /**
     * ???????
     *
     * @param hqCompanyId hq Company ID
     * @param subjectTypeMap ??
     */
    private void validateHqCompany(Long hqCompanyId, Map<String, String> subjectTypeMap) {
        if (hqCompanyId == null) {
            throw new ServiceException("归属总部不能为空");
        }
        // ??????????????????????????
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        String subjectType = subjectTypeMap.get(company.getTypeCode());
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            throw new ServiceException("归属公司不是总部类型");
        }
    }

    /**
     * ?? findByBarcodeAndOwnerHq ?????
     *
     * @param barcode ??
     * @param ownerHqId ????ID
     * @return ????
     */
    private MachineBarcode findByBarcodeAndOwnerHq(String barcode, Long ownerHqId) {
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, barcode)
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // ??????????????????????????
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * ?????
     *
     * @param id ??ID
     * @param ownerHqId ????ID
     * @return ????
     */
    private MachineBarcode selectByIdAndOwnerHq(Long id, Long ownerHqId) {
        if (id == null || ownerHqId == null) {
            return null;
        }
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getId, id)
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // ??????????????????????????
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * ???????
     *
     * @param ownerHqId ????ID
     * @param targetCompanyId ????ID
     * @return ????
     */
    private Long resolveOwnerHqId(Long ownerHqId, Long targetCompanyId) {
        Long requestedOwnerHqId = ownerHqId != null ? ownerHqId : targetCompanyId;
        if (!SecurityContext.isPlatformUser() && requestedOwnerHqId == null) {
            requestedOwnerHqId = SecurityContext.getCurrentCompanyId();
        }
        return companyDataAccessService.resolveOwnerHqTarget(requestedOwnerHqId);
    }

    /**
     * ???????
     *
     * @return ????
     */
    private Map<String, String> buildSubjectTypeMap() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
    }

    /**
     * ???????
     *
     * @return ????
     */
    private Map<String, String> buildTypeNameMap() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
    }

    /**
     * ???????
     *
     * @param companyTypes ??
     * @param subjectType ????
     * @return ????
     */
    private List<String> listTypeCodesBySubjectType(List<SysCompanyType> companyTypes, String subjectType) {
        return companyTypes.stream()
                .filter(type -> subjectType.equals(type.getSubjectType()))
                .map(SysCompanyType::getTypeCode)
                .collect(Collectors.toList());
    }

    /**
     * ???????
     *
     * @param company ??
     * @param typeNameMap ??
     * @return ????
     */
    private SysCompanySimpleVO buildCompanySimpleVo(SysCompany company, Map<String, String> typeNameMap) {
        SysCompanySimpleVO vo = new SysCompanySimpleVO();
        vo.setId(company.getId());
        vo.setCompanyName(company.getCompanyName());
        vo.setCompanyCode(company.getCompanyCode());
        vo.setTypeCode(company.getTypeCode());
        vo.setTypeName(typeNameMap.get(company.getTypeCode()));
        return vo;
    }

    /**
     * ?? applyDefaultStatus ?????
     *
     * @param entity ????
     */
    private void applyDefaultStatus(MachineBarcode entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * ????????
     *
     * @param entity ????
     */
    private void normalizeEntity(MachineBarcode entity) {
        entity.setBarcode(normalizeRequiredText(entity.getBarcode()));
        entity.setDeliverNumber(normalizeOptionalText(entity.getDeliverNumber()));
        entity.setCustId(normalizeOptionalText(entity.getCustId()));
        entity.setSalesOrg(normalizeOptionalText(entity.getSalesOrg()));
        entity.setProductCode(normalizeOptionalText(entity.getProductCode()));
        entity.setProductName(normalizeOptionalText(entity.getProductName()));
        entity.setProductModel(normalizeOptionalText(entity.getProductModel()));
        entity.setMachineNo(normalizeOptionalText(entity.getMachineNo()));
        entity.setBrandCode(normalizeOptionalText(entity.getBrandCode()));
        entity.setRemark(normalizeOptionalText(entity.getRemark()));
    }

    /**
     * ????????
     *
     * @param value ???
     * @return ?????
     */
    private String normalizeRequiredText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * ????????
     *
     * @param value ???
     * @return ?????
     */
    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}
