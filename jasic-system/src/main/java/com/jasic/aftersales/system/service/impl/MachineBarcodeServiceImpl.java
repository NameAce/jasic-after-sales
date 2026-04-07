package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
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

    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    @Override
    public PageResult<MachineBarcodeVO> listPage(MachineBarcodeQuery query) {
        Page<MachineBarcode> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getBarcode())) {
            wrapper.like(MachineBarcode::getBarcode, query.getBarcode().trim());
        }
        if (query.getHqCompanyId() != null) {
            wrapper.eq(MachineBarcode::getHqCompanyId, query.getHqCompanyId());
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
        if (StrUtil.isNotBlank(query.getProductTrumpet())) {
            wrapper.like(MachineBarcode::getProductTrumpet, query.getProductTrumpet().trim());
        }
        if (StrUtil.isNotBlank(query.getProductModel())) {
            wrapper.like(MachineBarcode::getProductModel, query.getProductModel().trim());
        }
        if (StrUtil.isNotBlank(query.getBrandCode())) {
            wrapper.like(MachineBarcode::getBrandCode, query.getBrandCode().trim());
        }
        if (StrUtil.isNotBlank(query.getWarrantyStatus())) {
            wrapper.eq(MachineBarcode::getWarrantyStatus, query.getWarrantyStatus().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(MachineBarcode::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(MachineBarcode::getId);
        Page<MachineBarcode> result = machineBarcodeMapper.selectPage(page, wrapper);
        List<MachineBarcodeVO> records = buildMachineBarcodeVOList(result.getRecords());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public MachineBarcodeVO getById(Long id) {
        MachineBarcode entity = machineBarcodeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        return buildMachineBarcodeVOList(Collections.singletonList(entity)).get(0);
    }

    @Override
    public List<SysCompanySimpleVO> listHqCompanyOptions() {
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(MachineBarcodeDTO dto) {
        MachineBarcode entity = new MachineBarcode();
        BeanUtil.copyProperties(dto, entity);
        normalizeEntity(entity);
        applyDefaultStatus(entity);
        validateMachineBarcode(entity, null, buildSubjectTypeMap());
        machineBarcodeMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MachineBarcodeDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("条码档案ID不能为空");
        }
        MachineBarcode entity = machineBarcodeMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        BeanUtil.copyProperties(dto, entity);
        normalizeEntity(entity);
        applyDefaultStatus(entity);
        validateMachineBarcode(entity, entity.getId(), buildSubjectTypeMap());
        machineBarcodeMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Long id) {
        MachineBarcode entity = machineBarcodeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        machineBarcodeMapper.deleteById(id);
    }

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
            validateImportPayloadItem(incoming, subjectTypeMap);
            if (!payloadBarcodes.add(incoming.getBarcode())) {
                throw new ServiceException("导入数据存在重复条码：" + incoming.getBarcode());
            }
            normalizedItems.add(incoming);
        }

        int count = 0;
        for (MachineBarcode incoming : normalizedItems) {
            MachineBarcode existing = findByBarcode(incoming.getBarcode());
            if (existing == null) {
                validateMachineBarcode(incoming, null, subjectTypeMap);
                machineBarcodeMapper.insert(incoming);
            } else {
                BeanUtil.copyProperties(incoming, existing);
                validateMachineBarcode(existing, existing.getId(), subjectTypeMap);
                machineBarcodeMapper.updateById(existing);
            }
            count++;
        }
        return count;
    }

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
            List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
            companyMap = companies.stream()
                    .collect(Collectors.toMap(SysCompany::getId, company -> company, (a, b) -> a));
        }

        List<MachineBarcodeVO> result = new ArrayList<>();
        for (MachineBarcode record : records) {
            MachineBarcodeVO vo = BeanUtil.copyProperties(record, MachineBarcodeVO.class);
            String machineTrumpet = resolveMachineTrumpet(record);
            vo.setProductTrumpet(machineTrumpet);
            vo.setMachineNo(machineTrumpet);
            vo.setWarrantyStatus(MachineBarcodeWarrantyResolver.resolveWarrantyStatus(record));
            SysCompany company = companyMap.get(record.getHqCompanyId());
            if (company != null) {
                vo.setHqCompanyName(company.getCompanyName());
            }
            result.add(vo);
        }
        return result;
    }

    private void validateMachineBarcode(MachineBarcode entity, Long currentId, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        validateStatus(entity.getStatus());
        validateUniqueBarcode(entity.getBarcode(), currentId);
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    private void validateImportPayloadItem(MachineBarcode entity, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        validateStatus(entity.getStatus());
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            throw new ServiceException("条码档案状态不能为空");
        }
        if (!Objects.equals(STATUS_ENABLED, status) && !Objects.equals(STATUS_DISABLED, status)) {
            throw new ServiceException("条码档案状态不合法");
        }
    }

    private void validateUniqueBarcode(String barcode, Long currentId) {
        MachineBarcode duplicated = findByBarcode(barcode);
        if (duplicated != null && !Objects.equals(duplicated.getId(), currentId)) {
            throw new ServiceException("机器条码已存在");
        }
    }

    private void validateHqCompany(Long hqCompanyId, Map<String, String> subjectTypeMap) {
        if (hqCompanyId == null) {
            throw new ServiceException("归属总部不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        String subjectType = subjectTypeMap.get(company.getTypeCode());
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            throw new ServiceException("归属公司不是总部类型");
        }
    }

    private MachineBarcode findByBarcode(String barcode) {
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, barcode);
        return machineBarcodeMapper.selectOne(wrapper);
    }

    private Map<String, String> buildSubjectTypeMap() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
    }

    private List<String> listTypeCodesBySubjectType(List<SysCompanyType> companyTypes, String subjectType) {
        return companyTypes.stream()
                .filter(type -> subjectType.equals(type.getSubjectType()))
                .map(SysCompanyType::getTypeCode)
                .collect(Collectors.toList());
    }

    private void applyDefaultStatus(MachineBarcode entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
    }

    private void normalizeEntity(MachineBarcode entity) {
        entity.setBarcode(normalizeRequiredText(entity.getBarcode()));
        entity.setCustId(normalizeOptionalText(entity.getCustId()));
        entity.setSalesOrg(normalizeOptionalText(entity.getSalesOrg()));
        entity.setProductCode(normalizeOptionalText(entity.getProductCode()));
        entity.setProductName(normalizeOptionalText(entity.getProductName()));
        entity.setProductTrumpet(normalizeOptionalText(entity.getProductTrumpet()));
        entity.setProductModel(normalizeOptionalText(entity.getProductModel()));
        entity.setMachineNo(normalizeOptionalText(entity.getMachineNo()));
        String machineTrumpet = resolveMachineTrumpet(entity);
        entity.setProductTrumpet(machineTrumpet);
        entity.setMachineNo(machineTrumpet);
        entity.setBrandCode(normalizeOptionalText(entity.getBrandCode()));
        entity.setWarrantyStatus(normalizeOptionalText(entity.getWarrantyStatus()));
        entity.setRemark(normalizeOptionalText(entity.getRemark()));
    }

    private String resolveMachineTrumpet(MachineBarcode entity) {
        String productTrumpet = normalizeOptionalText(entity.getProductTrumpet());
        if (productTrumpet != null) {
            return productTrumpet;
        }
        return normalizeOptionalText(entity.getMachineNo());
    }

    private String normalizeRequiredText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}
