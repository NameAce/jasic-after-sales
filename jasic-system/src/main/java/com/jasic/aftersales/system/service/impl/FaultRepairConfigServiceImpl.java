package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.FaultRepairConfigDTO;
import com.jasic.aftersales.system.domain.dto.FaultRepairConfigFaultDTO;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfig;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfigFault;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfigOption;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.query.FaultRepairConfigQuery;
import com.jasic.aftersales.system.domain.vo.FaultRepairConfigFaultVO;
import com.jasic.aftersales.system.domain.vo.FaultRepairConfigVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.mapper.FaultRepairConfigFaultMapper;
import com.jasic.aftersales.system.mapper.FaultRepairConfigMapper;
import com.jasic.aftersales.system.mapper.FaultRepairConfigOptionMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 故障与维修配置 Service 实现
 *
 * @author Codex
 * @date 2026/04/01
 */
@Service
public class FaultRepairConfigServiceImpl implements IFaultRepairConfigService {

    private static final Integer STATUS_ENABLED = 1;

    @Resource
    private FaultRepairConfigMapper faultRepairConfigMapper;

    @Resource
    private FaultRepairConfigFaultMapper faultRepairConfigFaultMapper;

    @Resource
    private FaultRepairConfigOptionMapper faultRepairConfigOptionMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    @Override
    public PageResult<FaultRepairConfigVO> listPage(FaultRepairConfigQuery query) {
        Page<FaultRepairConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        if (query.getCompanyId() != null) {
            wrapper.eq(FaultRepairConfig::getCompanyId, query.getCompanyId());
        }
        if (StrUtil.isNotBlank(query.getProductCode())) {
            wrapper.like(FaultRepairConfig::getProductCode, query.getProductCode().trim());
        }
        if (StrUtil.isNotBlank(query.getProductModel())) {
            wrapper.like(FaultRepairConfig::getProductModel, query.getProductModel().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(FaultRepairConfig::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getFaultDesc())) {
            List<Long> configIds = listConfigIdsByFaultDesc(query.getFaultDesc().trim());
            if (configIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            wrapper.in(FaultRepairConfig::getId, configIds);
        }
        wrapper.orderByDesc(FaultRepairConfig::getUpdateTime)
                .orderByDesc(FaultRepairConfig::getId);
        Page<FaultRepairConfig> result = faultRepairConfigMapper.selectPage(page, wrapper);
        List<FaultRepairConfigVO> records = buildConfigVos(result.getRecords(), false);
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public FaultRepairConfigVO getById(Long id) {
        FaultRepairConfig entity = faultRepairConfigMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("故障与维修配置不存在");
        }
        List<FaultRepairConfigVO> records = buildConfigVos(Collections.singletonList(entity), true);
        return records.isEmpty() ? null : records.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(FaultRepairConfigDTO dto) {
        FaultRepairConfig entity = new FaultRepairConfig();
        BeanUtil.copyProperties(dto, entity);
        normalizeConfig(entity);
        List<FaultRepairConfigFaultDTO> faults = normalizeFaultDtos(dto.getFaults());
        validateConfig(entity, null);
        validateFaults(faults);
        faultRepairConfigMapper.insert(entity);
        saveFaultItems(entity.getId(), faults);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(FaultRepairConfigDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("配置ID不能为空");
        }
        FaultRepairConfig entity = faultRepairConfigMapper.selectById(dto.getId());
        if (entity == null) {
            throw new ServiceException("故障与维修配置不存在");
        }
        BeanUtil.copyProperties(dto, entity);
        normalizeConfig(entity);
        List<FaultRepairConfigFaultDTO> faults = normalizeFaultDtos(dto.getFaults());
        validateConfig(entity, entity.getId());
        validateFaults(faults);
        faultRepairConfigMapper.updateById(entity);
        removeFaultItems(entity.getId());
        saveFaultItems(entity.getId(), faults);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Long id) {
        FaultRepairConfig entity = faultRepairConfigMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("故障与维修配置不存在");
        }
        removeFaultItems(id);
        faultRepairConfigMapper.deleteById(id);
    }

    @Override
    public List<SysCompanySimpleVO> listCompanyOptions() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        List<String> hqTypeCodes = companyTypes.stream()
                .filter(item -> SubjectTypeEnum.HQ.getCode().equals(item.getSubjectType()))
                .map(SysCompanyType::getTypeCode)
                .collect(Collectors.toList());
        if (hqTypeCodes.isEmpty()) {
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

    @Override
    public List<WorkOrderRepairFaultOptionVO> listRepairFaultOptions(Long companyId, String productCode, String productModel) {
        FaultRepairConfig config = findMatchedConfig(companyId, normalizeNullableText(productCode), normalizeNullableText(productModel));
        if (config == null) {
            return Collections.emptyList();
        }
        List<FaultRepairConfigVO> records = buildConfigVos(Collections.singletonList(config), true);
        if (records.isEmpty() || CollUtil.isEmpty(records.get(0).getFaults())) {
            return Collections.emptyList();
        }
        List<WorkOrderRepairFaultOptionVO> result = new ArrayList<>();
        for (FaultRepairConfigFaultVO fault : records.get(0).getFaults()) {
            WorkOrderRepairFaultOptionVO vo = new WorkOrderRepairFaultOptionVO();
            vo.setFaultDesc(fault.getFaultDesc());
            vo.setRepairOptions(fault.getRepairOptions());
            result.add(vo);
        }
        return result;
    }

    private List<Long> listConfigIdsByFaultDesc(String faultDesc) {
        LambdaQueryWrapper<FaultRepairConfigFault> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(FaultRepairConfigFault::getFaultDesc, faultDesc);
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(wrapper);
        if (faults.isEmpty()) {
            return Collections.emptyList();
        }
        return faults.stream()
                .map(FaultRepairConfigFault::getConfigId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<FaultRepairConfigVO> buildConfigVos(List<FaultRepairConfig> records, boolean includeFaults) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        Set<Long> companyIds = records.stream()
                .map(FaultRepairConfig::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> companyNameMap = buildCompanyNameMap(companyIds);
        Set<Long> configIds = records.stream()
                .map(FaultRepairConfig::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<FaultRepairConfigFaultVO>> faultMap = includeFaults
                ? buildFaultVoMap(configIds)
                : Collections.emptyMap();
        Map<Long, String> faultSummaryMap = includeFaults
                ? Collections.emptyMap()
                : buildFaultSummaryMap(configIds);

        List<FaultRepairConfigVO> result = new ArrayList<>();
        for (FaultRepairConfig record : records) {
            FaultRepairConfigVO vo = BeanUtil.copyProperties(record, FaultRepairConfigVO.class);
            vo.setCompanyName(companyNameMap.get(record.getCompanyId()));
            if (includeFaults) {
                List<FaultRepairConfigFaultVO> faults = faultMap.getOrDefault(record.getId(), Collections.emptyList());
                vo.setFaults(faults);
                vo.setFaultDescSummary(buildFaultSummary(faults));
            } else {
                vo.setFaultDescSummary(faultSummaryMap.get(record.getId()));
            }
            result.add(vo);
        }
        return result;
    }

    private Map<Long, String> buildCompanyNameMap(Set<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getId, companyIds);
        List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
        return companies.stream()
                .collect(Collectors.toMap(SysCompany::getId, SysCompany::getCompanyName, (a, b) -> a));
    }

    private Map<Long, List<FaultRepairConfigFaultVO>> buildFaultVoMap(Set<Long> configIds) {
        if (configIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FaultRepairConfigFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.in(FaultRepairConfigFault::getConfigId, configIds)
                .orderByAsc(FaultRepairConfigFault::getConfigId)
                .orderByAsc(FaultRepairConfigFault::getSortNum)
                .orderByAsc(FaultRepairConfigFault::getId);
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(faultWrapper);
        if (faults.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> faultIds = faults.stream()
                .map(FaultRepairConfigFault::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<String>> optionMap = buildOptionMap(faultIds);
        Map<Long, List<FaultRepairConfigFaultVO>> result = new LinkedHashMap<>();
        for (FaultRepairConfigFault fault : faults) {
            FaultRepairConfigFaultVO vo = new FaultRepairConfigFaultVO();
            vo.setFaultDesc(fault.getFaultDesc());
            vo.setRepairOptions(optionMap.getOrDefault(fault.getId(), Collections.emptyList()));
            result.computeIfAbsent(fault.getConfigId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    private Map<Long, String> buildFaultSummaryMap(Set<Long> configIds) {
        if (configIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FaultRepairConfigFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.in(FaultRepairConfigFault::getConfigId, configIds)
                .orderByAsc(FaultRepairConfigFault::getConfigId)
                .orderByAsc(FaultRepairConfigFault::getSortNum)
                .orderByAsc(FaultRepairConfigFault::getId);
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(faultWrapper);
        Map<Long, List<String>> grouped = new LinkedHashMap<>();
        for (FaultRepairConfigFault fault : faults) {
            grouped.computeIfAbsent(fault.getConfigId(), key -> new ArrayList<>()).add(fault.getFaultDesc());
        }
        Map<Long, String> result = new HashMap<>();
        grouped.forEach((configId, descs) -> result.put(configId, StrUtil.join("；", descs)));
        return result;
    }

    private Map<Long, List<String>> buildOptionMap(Set<Long> faultIds) {
        if (faultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FaultRepairConfigOption> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.in(FaultRepairConfigOption::getFaultId, faultIds)
                .orderByAsc(FaultRepairConfigOption::getFaultId)
                .orderByAsc(FaultRepairConfigOption::getSortNum)
                .orderByAsc(FaultRepairConfigOption::getId);
        List<FaultRepairConfigOption> options = faultRepairConfigOptionMapper.selectList(optionWrapper);
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (FaultRepairConfigOption option : options) {
            result.computeIfAbsent(option.getFaultId(), key -> new ArrayList<>()).add(option.getRepairDesc());
        }
        return result;
    }

    private void saveFaultItems(Long configId, List<FaultRepairConfigFaultDTO> faults) {
        int faultSort = 1;
        for (FaultRepairConfigFaultDTO item : faults) {
            FaultRepairConfigFault fault = new FaultRepairConfigFault();
            fault.setConfigId(configId);
            fault.setFaultDesc(item.getFaultDesc());
            fault.setSortNum(faultSort++);
            faultRepairConfigFaultMapper.insert(fault);

            int optionSort = 1;
            for (String repairDesc : item.getRepairOptions()) {
                FaultRepairConfigOption option = new FaultRepairConfigOption();
                option.setFaultId(fault.getId());
                option.setRepairDesc(repairDesc);
                option.setSortNum(optionSort++);
                faultRepairConfigOptionMapper.insert(option);
            }
        }
    }

    private void removeFaultItems(Long configId) {
        LambdaQueryWrapper<FaultRepairConfigFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.eq(FaultRepairConfigFault::getConfigId, configId);
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(faultWrapper);
        if (!faults.isEmpty()) {
            Set<Long> faultIds = faults.stream().map(FaultRepairConfigFault::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<FaultRepairConfigOption> optionWrapper = new LambdaQueryWrapper<>();
            optionWrapper.in(FaultRepairConfigOption::getFaultId, faultIds);
            faultRepairConfigOptionMapper.delete(optionWrapper);
        }
        faultRepairConfigFaultMapper.delete(faultWrapper);
    }

    private void validateConfig(FaultRepairConfig entity, Long currentId) {
        if (entity.getCompanyId() == null) {
            throw new ServiceException("归属总部不能为空");
        }
        if (StrUtil.isBlank(entity.getProductCode()) && StrUtil.isBlank(entity.getProductModel())) {
            throw new ServiceException("物料编码和产品型号不能同时为空");
        }
        if (!Objects.equals(entity.getStatus(), 0) && !Objects.equals(entity.getStatus(), 1)) {
            throw new ServiceException("配置状态不合法");
        }
        validateCompany(entity.getCompanyId());
        validateUnique(entity.getCompanyId(), entity.getProductCode(), entity.getProductModel(), currentId);
    }

    private void validateCompany(Long companyId) {
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        Map<String, String> subjectTypeMap = companyTypeService.listAll().stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()))) {
            throw new ServiceException("归属总部必须是总部公司");
        }
    }

    private void validateUnique(Long companyId, String productCode, String productModel, Long currentId) {
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, companyId);
        if (productCode == null) {
            wrapper.isNull(FaultRepairConfig::getProductCode);
        } else {
            wrapper.eq(FaultRepairConfig::getProductCode, productCode);
        }
        if (productModel == null) {
            wrapper.isNull(FaultRepairConfig::getProductModel);
        } else {
            wrapper.eq(FaultRepairConfig::getProductModel, productModel);
        }
        FaultRepairConfig exists = faultRepairConfigMapper.selectOne(wrapper);
        if (exists != null && (currentId == null || !exists.getId().equals(currentId))) {
            throw new ServiceException("当前归属总部下已存在相同产品配置");
        }
    }

    private void validateFaults(List<FaultRepairConfigFaultDTO> faults) {
        if (CollUtil.isEmpty(faults)) {
            throw new ServiceException("请至少添加一条故障信息");
        }
        Set<String> faultSet = new LinkedHashSet<>();
        for (FaultRepairConfigFaultDTO item : faults) {
            if (item == null || StrUtil.isBlank(item.getFaultDesc())) {
                throw new ServiceException("故障描述不能为空");
            }
            if (!faultSet.add(item.getFaultDesc())) {
                throw new ServiceException("同一配置下故障描述不能重复");
            }
            if (CollUtil.isEmpty(item.getRepairOptions())) {
                throw new ServiceException("维修说明不能为空");
            }
            Set<String> optionSet = new LinkedHashSet<>();
            for (String repairDesc : item.getRepairOptions()) {
                if (StrUtil.isBlank(repairDesc)) {
                    throw new ServiceException("维修说明不能为空");
                }
                if (!optionSet.add(repairDesc)) {
                    throw new ServiceException("同一故障下维修说明不能重复");
                }
            }
        }
    }

    private List<FaultRepairConfigFaultDTO> normalizeFaultDtos(List<FaultRepairConfigFaultDTO> faults) {
        if (faults == null) {
            return Collections.emptyList();
        }
        List<FaultRepairConfigFaultDTO> result = new ArrayList<>();
        for (FaultRepairConfigFaultDTO item : faults) {
            if (item == null) {
                continue;
            }
            FaultRepairConfigFaultDTO dto = new FaultRepairConfigFaultDTO();
            dto.setFaultDesc(normalizeNullableText(item.getFaultDesc()));
            dto.setRepairOptions(normalizeRepairOptions(item.getRepairOptions()));
            result.add(dto);
        }
        return result;
    }

    private List<String> normalizeRepairOptions(List<String> repairOptions) {
        if (repairOptions == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String repairDesc : repairOptions) {
            String normalized = normalizeNullableText(repairDesc);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private void normalizeConfig(FaultRepairConfig entity) {
        entity.setProductCode(normalizeNullableText(entity.getProductCode()));
        entity.setProductModel(normalizeNullableText(entity.getProductModel()));
        entity.setRemark(normalizeNullableText(entity.getRemark()));
    }

    private FaultRepairConfig findMatchedConfig(Long companyId, String productCode, String productModel) {
        if (companyId == null) {
            return null;
        }
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, companyId)
                .eq(FaultRepairConfig::getStatus, STATUS_ENABLED)
                .orderByDesc(FaultRepairConfig::getUpdateTime)
                .orderByDesc(FaultRepairConfig::getId);
        List<FaultRepairConfig> candidates = faultRepairConfigMapper.selectList(wrapper);
        FaultRepairConfig bestMatch = null;
        int bestScore = -1;
        for (FaultRepairConfig candidate : candidates) {
            int score = calculateMatchScore(candidate, productCode, productModel);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }
        return bestScore < 0 ? null : bestMatch;
    }

    private int calculateMatchScore(FaultRepairConfig candidate, String productCode, String productModel) {
        String configProductCode = normalizeNullableText(candidate.getProductCode());
        String configProductModel = normalizeNullableText(candidate.getProductModel());
        int score = 0;
        if (configProductCode != null) {
            if (productCode == null || !StrUtil.equals(productCode, configProductCode)) {
                return -1;
            }
            score += 4;
        }
        if (configProductModel != null) {
            if (productModel == null || !StrUtil.equals(productModel, configProductModel)) {
                return -1;
            }
            score += 2;
        }
        return score;
    }

    private String buildFaultSummary(List<FaultRepairConfigFaultVO> faults) {
        if (CollUtil.isEmpty(faults)) {
            return "";
        }
        return faults.stream()
                .map(FaultRepairConfigFaultVO::getFaultDesc)
                .collect(Collectors.joining("；"));
    }

    private String normalizeNullableText(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
