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

    /**
     * 系统公司Mapper数据访问接口。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**
     * 分页查询故障与维修配置。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<FaultRepairConfigVO> listPage(FaultRepairConfigQuery query) {
        if (query == null) {
            // 调用FaultRepairConfigQuery方法，复用统一能力并保证业务规则一致。
            query = new FaultRepairConfigQuery();
        }
        // 说明：执行该步骤以保证业务流程正确。
        ensureManagePermission();
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany ownerHq = resolveOwnerHqForManage(query == null ? null : query.getCompanyId());
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<FaultRepairConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        wrapper.eq(FaultRepairConfig::getCompanyId, ownerHq.getId());
        if (StrUtil.isNotBlank(query.getProductCode())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(FaultRepairConfig::getProductCode, query.getProductCode().trim());
        }
        if (StrUtil.isNotBlank(query.getProductModel())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(FaultRepairConfig::getProductModel, query.getProductModel().trim());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(FaultRepairConfig::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getFaultDesc())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            List<Long> configIds = listConfigIdsByFaultDesc(query.getFaultDesc().trim());
            if (configIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            // 调用in方法，复用统一能力并保证业务规则一致。
            wrapper.in(FaultRepairConfig::getId, configIds);
        }
        wrapper.orderByDesc(FaultRepairConfig::getUpdateTime)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(FaultRepairConfig::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<FaultRepairConfig> result = faultRepairConfigMapper.selectPage(page, wrapper);
        // 调用getRecords方法，复用统一能力并保证业务规则一致。
        List<FaultRepairConfigVO> records = buildConfigVos(result.getRecords(), false);
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询配置详情。
     *
     * @param id 配置ID
     * @return 配置详情
     */
    @Override
    public FaultRepairConfigVO getById(Long id, Long ownerHqId) {
        // 说明：执行该步骤以保证业务流程正确。
        ensureManagePermission();
        // 调用resolveOwnerHqForManage方法，复用统一能力并保证业务规则一致。
        SysCompany ownerHq = resolveOwnerHqForManage(ownerHqId);
        // 说明：执行该步骤以保证业务流程正确。
        FaultRepairConfig entity = faultRepairConfigMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getCompanyId(), ownerHq.getId())) {
            throw new ServiceException("故障与维修配置不存在");
        }
        // 调用singletonList方法，复用统一能力并保证业务规则一致。
        List<FaultRepairConfigVO> records = buildConfigVos(Collections.singletonList(entity), true);
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * 新增故障与维修配置。
     *
     * @param dto 配置参数
     * @return 配置ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(FaultRepairConfigDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        ensureManagePermission();
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany targetCompany = resolveOwnerHqForManage(dto.getCompanyId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        dto.setCompanyId(targetCompany.getId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        dto.setTargetCompanyName(targetCompany.getCompanyName());
        // 调用FaultRepairConfig方法，复用统一能力并保证业务规则一致。
        FaultRepairConfig entity = new FaultRepairConfig();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用normalizeConfig方法，复用统一能力并保证业务规则一致。
        normalizeConfig(entity);
        // 调用getFaults方法，复用统一能力并保证业务规则一致。
        List<FaultRepairConfigFaultDTO> faults = normalizeFaultDtos(dto.getFaults());
        // 调用validateConfig方法，复用统一能力并保证业务规则一致。
        validateConfig(entity, null);
        // 调用validateFaults方法，复用统一能力并保证业务规则一致。
        validateFaults(faults);
        // 说明：执行该步骤以保证业务流程正确。
        faultRepairConfigMapper.insert(entity);
        // 调用getId方法，复用统一能力并保证业务规则一致。
        saveFaultItems(entity.getId(), faults);
        return entity.getId();
    }

    /**
     * 更新故障与维修配置。
     *
     * @param dto 配置参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(FaultRepairConfigDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        ensureManagePermission();
        if (dto.getId() == null) {
            throw new ServiceException("配置ID不能为空");
        }
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany ownerHq = resolveOwnerHqForManage(dto.getCompanyId());
        // 说明：执行该步骤以保证业务流程正确。
        FaultRepairConfig current = faultRepairConfigMapper.selectById(dto.getId());
        if (current == null || !Objects.equals(current.getCompanyId(), ownerHq.getId())) {
            throw new ServiceException("故障与维修配置不存在");
        }
        if (!Objects.equals(current.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException("停用历史配置不允许编辑");
        }
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        Integer targetStatus = dto.getStatus();
        if (targetStatus == null) {
            throw new ServiceException("配置状态不合法");
        }
        SysCompany targetCompany = ownerHq;
        // 调用getId方法，复用统一能力并保证业务规则一致。
        dto.setCompanyId(targetCompany.getId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        dto.setTargetCompanyName(targetCompany.getCompanyName());
        if (Objects.equals(targetStatus, 0)) {
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            current.setStatus(0);
            // 说明：执行该步骤以保证业务流程正确。
            faultRepairConfigMapper.updateById(current);
            return;
        }
        // 调用FaultRepairConfig方法，复用统一能力并保证业务规则一致。
        FaultRepairConfig entity = new FaultRepairConfig();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用setId方法，复用统一能力并保证业务规则一致。
        entity.setId(null);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        entity.setStatus(STATUS_ENABLED);
        // 调用normalizeConfig方法，复用统一能力并保证业务规则一致。
        normalizeConfig(entity);
        // 调用getFaults方法，复用统一能力并保证业务规则一致。
        List<FaultRepairConfigFaultDTO> faults = normalizeFaultDtos(dto.getFaults());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        validateConfig(entity, current.getId());
        // 调用validateFaults方法，复用统一能力并保证业务规则一致。
        validateFaults(faults);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        current.setStatus(0);
        // 调用updateById方法，复用统一能力并保证业务规则一致。
        faultRepairConfigMapper.updateById(current);
        // 调用insert方法，复用统一能力并保证业务规则一致。
        faultRepairConfigMapper.insert(entity);
        // 调用getId方法，复用统一能力并保证业务规则一致。
        saveFaultItems(entity.getId(), faults);
    }

    /**
     * 查询可配置故障模板的总部列表。
     *
     * @return 总部选项
     */
    @Override
    public List<SysCompanySimpleVO> listCompanyOptions() {
        // 说明：执行该步骤以保证业务流程正确。
        ensureManagePermission();
        if (isCurrentHqUser()) {
            return Collections.singletonList(buildCompanySimpleVo(requireCurrentHqCompany()));
        }
        // 调用listAll方法，复用统一能力并保证业务规则一致。
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        List<String> hqTypeCodes = companyTypes.stream()
                .filter(item -> SubjectTypeEnum.HQ.getCode().equals(item.getSubjectType()))
                .map(SysCompanyType::getTypeCode)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        if (hqTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, hqTypeCodes)
                .eq(SysCompany::getStatus, STATUS_ENABLED)
                .orderByAsc(SysCompany::getCompanyName)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysCompany::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
        Map<String, String> typeNameMap = companyTypes.stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
        List<SysCompanySimpleVO> result = new ArrayList<>();
        for (SysCompany company : companies) {
            // 调用SysCompanySimpleVO方法，复用统一能力并保证业务规则一致。
            SysCompanySimpleVO vo = new SysCompanySimpleVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(company.getId());
            // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
            vo.setCompanyName(company.getCompanyName());
            // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
            vo.setCompanyCode(company.getCompanyCode());
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            vo.setTypeCode(company.getTypeCode());
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            vo.setTypeName(typeNameMap.get(company.getTypeCode()));
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 按总部和产品维度查询维修登记可选的故障配置。
     *
     * @param companyId 总部公司ID
     * @param productCode 产品编码
     * @param productModel 产品型号
     * @return 故障配置选项
     */
    @Override
    public List<WorkOrderRepairFaultOptionVO> listRepairFaultOptionsForResolvedHq(Long resolvedHqCompanyId,
                                                                                  String productCode,
                                                                                  String productModel) {
        // TODO: 非合作总部配置读取属于业务兼容场景。后续需结合转单闭环、总部选择、服务关系规则进一步确认。
        FaultRepairConfig config = findMatchedConfigForResolvedHq(resolvedHqCompanyId, normalizeNullableText(productCode),
                // 调用normalizeNullableText方法，复用统一能力并保证业务规则一致。
                normalizeNullableText(productModel));
        return buildRepairFaultOptions(config);
    }

    /**
     * 分页查询维修故障OptionsBy配置ID列表。
     *
     * @param configId config ID
     * @return 处理结果
     */
    @Override
    public List<WorkOrderRepairFaultOptionVO> listRepairFaultOptionsByConfigId(Long configId) {
        if (configId == null) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        FaultRepairConfig config = faultRepairConfigMapper.selectById(configId);
        return buildRepairFaultOptions(config);
    }

    /**
     * findEnabled配置IDForResolved总部。
     *
     * @param resolvedHqCompanyId resolved Hq Company ID
     * @param productCode 参数
     * @param productModel 参数
     * @return 处理结果
     */
    @Override
    public Long findEnabledConfigIdForResolvedHq(Long resolvedHqCompanyId, String productCode, String productModel) {
        // TODO: 非合作总部配置读取属于业务兼容场景。后续需结合转单闭环、总部选择、服务关系规则进一步确认。
        FaultRepairConfig config = findMatchedConfigForResolvedHq(resolvedHqCompanyId, normalizeNullableText(productCode),
                // 调用normalizeNullableText方法，复用统一能力并保证业务规则一致。
                normalizeNullableText(productModel));
        return config == null ? null : config.getId();
    }

    /**
     * 构建维修故障Options。
     *
     * @param config 参数
     * @return 处理结果
     */
    private List<WorkOrderRepairFaultOptionVO> buildRepairFaultOptions(FaultRepairConfig config) {
        if (config == null) {
            return Collections.emptyList();
        }
        // 调用singletonList方法，复用统一能力并保证业务规则一致。
        List<FaultRepairConfigVO> records = buildConfigVos(Collections.singletonList(config), true);
        if (records.isEmpty() || CollUtil.isEmpty(records.get(0).getFaults())) {
            return Collections.emptyList();
        }
        List<WorkOrderRepairFaultOptionVO> result = new ArrayList<>();
        for (FaultRepairConfigFaultVO fault : records.get(0).getFaults()) {
            // 调用WorkOrderRepairFaultOptionVO方法，复用统一能力并保证业务规则一致。
            WorkOrderRepairFaultOptionVO vo = new WorkOrderRepairFaultOptionVO();
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            vo.setFaultDesc(fault.getFaultDesc());
            // 调用getRepairOptions方法，复用统一能力并保证业务规则一致。
            vo.setRepairOptions(fault.getRepairOptions());
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 查询指定总部下启用状态的机型选项。
     *
     * @param companyId 归属总部ID
     * @param keyword 机型关键字
     * @return 机型选项
     */
    @Override
    public List<String> listEnabledProductModelsForResolvedHq(Long resolvedHqCompanyId, String keyword) {
        // TODO: 非合作总部配置读取属于业务兼容场景。后续需结合转单闭环、总部选择、服务关系规则进一步确认。
        if (resolvedHqCompanyId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, resolvedHqCompanyId)
                .eq(FaultRepairConfig::getStatus, STATUS_ENABLED)
                .isNotNull(FaultRepairConfig::getProductModel)
                .orderByAsc(FaultRepairConfig::getProductModel)
                .orderByDesc(FaultRepairConfig::getUpdateTime)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(FaultRepairConfig::getId);
        // 调用normalizeNullableText方法，复用统一能力并保证业务规则一致。
        String normalizedKeyword = normalizeNullableText(keyword);
        if (normalizedKeyword != null) {
            // 调用like方法，复用统一能力并保证业务规则一致。
            wrapper.like(FaultRepairConfig::getProductModel, normalizedKeyword);
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfig> configs = faultRepairConfigMapper.selectList(wrapper);
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> productModels = new LinkedHashSet<>();
        for (FaultRepairConfig config : configs) {
            // 调用getProductModel方法，复用统一能力并保证业务规则一致。
            String productModel = normalizeNullableText(config.getProductModel());
            if (productModel != null) {
                // 调用add方法，复用统一能力并保证业务规则一致。
                productModels.add(productModel);
            }
        }
        return new ArrayList<>(productModels);
    }

    /**
     * 通过故障描述反查配置ID，用于列表页按故障筛选。
     *
     * @param faultDesc 故障描述
     * @return 配置ID列表
     */
    private List<Long> listConfigIdsByFaultDesc(String faultDesc) {
        LambdaQueryWrapper<FaultRepairConfigFault> wrapper = new LambdaQueryWrapper<>();
        // 调用like方法，复用统一能力并保证业务规则一致。
        wrapper.like(FaultRepairConfigFault::getFaultDesc, faultDesc);
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(wrapper);
        if (faults.isEmpty()) {
            return Collections.emptyList();
        }
        return faults.stream()
                .map(FaultRepairConfigFault::getConfigId)
                .filter(Objects::nonNull)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * 组装配置展示对象，并按需加载故障与维修说明明细。
     *
     * @param records 配置记录
     * @param includeFaults 是否包含完整故障明细
     * @return 展示对象列表
     */
    private List<FaultRepairConfigVO> buildConfigVos(List<FaultRepairConfig> records, boolean includeFaults) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        Set<Long> companyIds = records.stream()
                .map(FaultRepairConfig::getCompanyId)
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用buildCompanyNameMap方法，复用统一能力并保证业务规则一致。
        Map<Long, String> companyNameMap = buildCompanyNameMap(companyIds);
        Set<Long> configIds = records.stream()
                .map(FaultRepairConfig::getId)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<FaultRepairConfigFaultVO>> faultMap = includeFaults
                ? buildFaultVoMap(configIds)
                // 调用emptyMap方法，复用统一能力并保证业务规则一致。
                : Collections.emptyMap();
        Map<Long, String> faultSummaryMap = includeFaults
                ? Collections.emptyMap()
                // 调用buildFaultSummaryMap方法，复用统一能力并保证业务规则一致。
                : buildFaultSummaryMap(configIds);

        List<FaultRepairConfigVO> result = new ArrayList<>();
        for (FaultRepairConfig record : records) {
            // 调用copyProperties方法，复用统一能力并保证业务规则一致。
            FaultRepairConfigVO vo = BeanUtil.copyProperties(record, FaultRepairConfigVO.class);
            // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
            vo.setCompanyName(companyNameMap.get(record.getCompanyId()));
            if (includeFaults) {
                // 调用emptyList方法，复用统一能力并保证业务规则一致。
                List<FaultRepairConfigFaultVO> faults = faultMap.getOrDefault(record.getId(), Collections.emptyList());
                // 调用setFaults方法，复用统一能力并保证业务规则一致。
                vo.setFaults(faults);
                // 调用buildFaultSummary方法，复用统一能力并保证业务规则一致。
                vo.setFaultDescSummary(buildFaultSummary(faults));
            } else {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                vo.setFaultDescSummary(faultSummaryMap.get(record.getId()));
            }
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 构建公司名称Map。
     *
     * @return 处理结果
     */
    private Map<Long, String> buildCompanyNameMap(Set<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        // 调用in方法，复用统一能力并保证业务规则一致。
        wrapper.in(SysCompany::getId, companyIds);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
        return companies.stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompany::getId, SysCompany::getCompanyName, (a, b) -> a));
    }

    /**
     * 构建故障VoMap。
     *
     * @return 处理结果
     */
    private Map<Long, List<FaultRepairConfigFaultVO>> buildFaultVoMap(Set<Long> configIds) {
        if (configIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FaultRepairConfigFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.in(FaultRepairConfigFault::getConfigId, configIds)
                .orderByAsc(FaultRepairConfigFault::getConfigId)
                .orderByAsc(FaultRepairConfigFault::getSortNum)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(FaultRepairConfigFault::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(faultWrapper);
        if (faults.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> faultIds = faults.stream()
                .map(FaultRepairConfigFault::getId)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        // 调用buildOptionMap方法，复用统一能力并保证业务规则一致。
        Map<Long, List<String>> optionMap = buildOptionMap(faultIds);
        Map<Long, List<FaultRepairConfigFaultVO>> result = new LinkedHashMap<>();
        for (FaultRepairConfigFault fault : faults) {
            // 调用FaultRepairConfigFaultVO方法，复用统一能力并保证业务规则一致。
            FaultRepairConfigFaultVO vo = new FaultRepairConfigFaultVO();
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            vo.setFaultDesc(fault.getFaultDesc());
            // 调用emptyList方法，复用统一能力并保证业务规则一致。
            vo.setRepairOptions(optionMap.getOrDefault(fault.getId(), Collections.emptyList()));
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.computeIfAbsent(fault.getConfigId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 构建故障SummaryMap。
     *
     * @return 处理结果
     */
    private Map<Long, String> buildFaultSummaryMap(Set<Long> configIds) {
        if (configIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FaultRepairConfigFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.in(FaultRepairConfigFault::getConfigId, configIds)
                .orderByAsc(FaultRepairConfigFault::getConfigId)
                .orderByAsc(FaultRepairConfigFault::getSortNum)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(FaultRepairConfigFault::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(faultWrapper);
        Map<Long, List<String>> grouped = new LinkedHashMap<>();
        for (FaultRepairConfigFault fault : faults) {
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            grouped.computeIfAbsent(fault.getConfigId(), key -> new ArrayList<>()).add(fault.getFaultDesc());
        }
        Map<Long, String> result = new HashMap<>();
        // 调用join方法，复用统一能力并保证业务规则一致。
        grouped.forEach((configId, descs) -> result.put(configId, StrUtil.join("；", descs)));
        return result;
    }

    /**
     * 构建OptionMap。
     *
     * @return 处理结果
     */
    private Map<Long, List<String>> buildOptionMap(Set<Long> faultIds) {
        if (faultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<FaultRepairConfigOption> optionWrapper = new LambdaQueryWrapper<>();
        optionWrapper.in(FaultRepairConfigOption::getFaultId, faultIds)
                .orderByAsc(FaultRepairConfigOption::getFaultId)
                .orderByAsc(FaultRepairConfigOption::getSortNum)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(FaultRepairConfigOption::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfigOption> options = faultRepairConfigOptionMapper.selectList(optionWrapper);
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (FaultRepairConfigOption option : options) {
            // 调用getRepairDesc方法，复用统一能力并保证业务规则一致。
            result.computeIfAbsent(option.getFaultId(), key -> new ArrayList<>()).add(option.getRepairDesc());
        }
        return result;
    }

    /**
     * 新增故障Items。
     *
     * @param configId config ID
     * @param faults 参数
     */
    private void saveFaultItems(Long configId, List<FaultRepairConfigFaultDTO> faults) {
        int faultSort = 1;
        for (FaultRepairConfigFaultDTO item : faults) {
            // 调用FaultRepairConfigFault方法，复用统一能力并保证业务规则一致。
            FaultRepairConfigFault fault = new FaultRepairConfigFault();
            // 调用setConfigId方法，复用统一能力并保证业务规则一致。
            fault.setConfigId(configId);
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            fault.setFaultDesc(item.getFaultDesc());
            // 调用setSortNum方法，复用统一能力并保证业务规则一致。
            fault.setSortNum(faultSort++);
            // 说明：执行该步骤以保证业务流程正确。
            faultRepairConfigFaultMapper.insert(fault);

            int optionSort = 1;
            for (String repairDesc : item.getRepairOptions()) {
                // 调用FaultRepairConfigOption方法，复用统一能力并保证业务规则一致。
                FaultRepairConfigOption option = new FaultRepairConfigOption();
                // 调用getId方法，复用统一能力并保证业务规则一致。
                option.setFaultId(fault.getId());
                // 调用setRepairDesc方法，复用统一能力并保证业务规则一致。
                option.setRepairDesc(repairDesc);
                // 调用setSortNum方法，复用统一能力并保证业务规则一致。
                option.setSortNum(optionSort++);
                // 调用insert方法，复用统一能力并保证业务规则一致。
                faultRepairConfigOptionMapper.insert(option);
            }
        }
    }

    /**
     * 删除故障Items。
     *
     * @param configId config ID
     */
    private void removeFaultItems(Long configId) {
        LambdaQueryWrapper<FaultRepairConfigFault> faultWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        faultWrapper.eq(FaultRepairConfigFault::getConfigId, configId);
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfigFault> faults = faultRepairConfigFaultMapper.selectList(faultWrapper);
        if (!faults.isEmpty()) {
            // 调用toSet方法，复用统一能力并保证业务规则一致。
            Set<Long> faultIds = faults.stream().map(FaultRepairConfigFault::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<FaultRepairConfigOption> optionWrapper = new LambdaQueryWrapper<>();
            // 调用in方法，复用统一能力并保证业务规则一致。
            optionWrapper.in(FaultRepairConfigOption::getFaultId, faultIds);
            // 说明：执行该步骤以保证业务流程正确。
            faultRepairConfigOptionMapper.delete(optionWrapper);
        }
        // 调用delete方法，复用统一能力并保证业务规则一致。
        faultRepairConfigFaultMapper.delete(faultWrapper);
    }

    /**
     * ensureManage权限。
     */
    private void ensureManagePermission() {
        if (SecurityContext.isPlatformUser()) {
            return;
        }
        if (isCurrentHqUser()) {
            // 说明：执行该步骤以保证业务流程正确。
            requireCurrentHqCompany();
            return;
        }
        throw new ServiceException("当前公司不支持维护故障与维修配置");
    }

    /**
     * 判断是否Current总部用户。
     */
    private boolean isCurrentHqUser() {
        return SubjectTypeEnum.HQ.getCode().equals(SecurityContext.getCurrentSubjectType());
    }

    /**
     * 解析Owner总部ForManage。
     *
     * @param requestedCompanyId requested Company ID
     * @return 处理结果
     */
    private SysCompany resolveOwnerHqForManage(Long requestedCompanyId) {
        if (SecurityContext.isPlatformUser()) {
            if (requestedCompanyId == null) {
                throw new ServiceException("缺少目标公司上下文");
            }
            // 说明：执行该步骤以保证业务流程正确。
            return requireCompany(requestedCompanyId);
        }
        if (isCurrentHqUser()) {
            // 调用requireCurrentHqCompany方法，复用统一能力并保证业务规则一致。
            SysCompany currentHq = requireCurrentHqCompany();
            if (requestedCompanyId != null && !Objects.equals(requestedCompanyId, currentHq.getId())) {
                throw new ServiceException("无权查看当前总部之外的配置");
            }
            return currentHq;
        }
        throw new ServiceException("当前公司不支持维护故障与维修配置");
    }

    /**
     * requireCurrent总部公司。
     *
     * @return 处理结果
     */
    private SysCompany requireCurrentHqCompany() {
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("当前总部不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        return requireCompany(currentCompanyId);
    }

    /**
     * require公司。
     *
     * @return 处理结果
     */
    private SysCompany requireCompany(Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        Map<String, String> subjectTypeMap = companyTypeService.listAll().stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()))) {
            throw new ServiceException("归属总部必须是总部公司");
        }
        return company;
    }

    /**
     * 构建公司SimpleVo。
     *
     * @param company 参数
     * @return 处理结果
     */
    private SysCompanySimpleVO buildCompanySimpleVo(SysCompany company) {
        // 调用SysCompanySimpleVO方法，复用统一能力并保证业务规则一致。
        SysCompanySimpleVO vo = new SysCompanySimpleVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(company.getId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        vo.setCompanyName(company.getCompanyName());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        vo.setCompanyCode(company.getCompanyCode());
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        vo.setTypeCode(company.getTypeCode());
        Map<String, String> typeNameMap = companyTypeService.listAll().stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        vo.setTypeName(typeNameMap.get(company.getTypeCode()));
        return vo;
    }

    /**
     * 校验配置。
     *
     * @param entity 参数
     * @param currentId current ID
     */
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
        // 说明：执行该步骤以保证业务流程正确。
        validateCompany(entity.getCompanyId());
        if (Objects.equals(entity.getStatus(), STATUS_ENABLED)) {
            // 调用getProductModel方法，复用统一能力并保证业务规则一致。
            validateUniqueEnabled(entity.getCompanyId(), entity.getProductCode(), entity.getProductModel(), currentId);
        }
    }

    /**
     * 校验公司。
     */
    private void validateCompany(Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        Map<String, String> subjectTypeMap = companyTypeService.listAll().stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()))) {
            throw new ServiceException("归属总部必须是总部公司");
        }
    }

    /**
     * 校验UniqueEnabled。
     *
     * @param productCode 参数
     * @param productModel 参数
     * @param currentId current ID
     */
    private void validateUniqueEnabled(Long companyId, String productCode, String productModel, Long currentId) {
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, companyId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(FaultRepairConfig::getStatus, STATUS_ENABLED);
        if (productCode == null) {
            // 调用isNull方法，复用统一能力并保证业务规则一致。
            wrapper.isNull(FaultRepairConfig::getProductCode);
        } else {
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(FaultRepairConfig::getProductCode, productCode);
        }
        if (productModel == null) {
            // 调用isNull方法，复用统一能力并保证业务规则一致。
            wrapper.isNull(FaultRepairConfig::getProductModel);
        } else {
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(FaultRepairConfig::getProductModel, productModel);
        }
        // 说明：执行该步骤以保证业务流程正确。
        FaultRepairConfig exists = faultRepairConfigMapper.selectOne(wrapper);
        if (exists != null && (currentId == null || !exists.getId().equals(currentId))) {
            throw new ServiceException("当前归属总部下已存在相同产品配置");
        }
    }

    /**
     * 校验Faults。
     *
     * @param faults 参数
     */
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

    /**
     * 规范化故障Dtos。
     *
     * @param faults 参数
     * @return 处理结果
     */
    private List<FaultRepairConfigFaultDTO> normalizeFaultDtos(List<FaultRepairConfigFaultDTO> faults) {
        if (faults == null) {
            return Collections.emptyList();
        }
        List<FaultRepairConfigFaultDTO> result = new ArrayList<>();
        for (FaultRepairConfigFaultDTO item : faults) {
            if (item == null) {
                continue;
            }
            // 调用FaultRepairConfigFaultDTO方法，复用统一能力并保证业务规则一致。
            FaultRepairConfigFaultDTO dto = new FaultRepairConfigFaultDTO();
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            dto.setFaultDesc(normalizeNullableText(item.getFaultDesc()));
            // 调用getRepairOptions方法，复用统一能力并保证业务规则一致。
            dto.setRepairOptions(normalizeRepairOptions(item.getRepairOptions()));
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(dto);
        }
        return result;
    }

    /**
     * 规范化维修Options。
     *
     * @param repairOptions 参数
     * @return 处理结果
     */
    private List<String> normalizeRepairOptions(List<String> repairOptions) {
        if (repairOptions == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String repairDesc : repairOptions) {
            // 调用normalizeNullableText方法，复用统一能力并保证业务规则一致。
            String normalized = normalizeNullableText(repairDesc);
            if (normalized != null) {
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(normalized);
            }
        }
        return result;
    }

    /**
     * 规范化配置。
     *
     * @param entity 参数
     */
    private void normalizeConfig(FaultRepairConfig entity) {
        // 调用getProductCode方法，复用统一能力并保证业务规则一致。
        entity.setProductCode(normalizeNullableText(entity.getProductCode()));
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        entity.setProductModel(normalizeNullableText(entity.getProductModel()));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        entity.setRemark(normalizeNullableText(entity.getRemark()));
    }

    /**
     * findMatched配置ForResolved总部。
     *
     * @param resolvedHqCompanyId resolved Hq Company ID
     * @param productCode 参数
     * @param productModel 参数
     * @return 处理结果
     */
    private FaultRepairConfig findMatchedConfigForResolvedHq(Long resolvedHqCompanyId, String productCode, String productModel) {
        if (resolvedHqCompanyId == null) {
            return null;
        }
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, resolvedHqCompanyId)
                .eq(FaultRepairConfig::getStatus, STATUS_ENABLED)
                .orderByDesc(FaultRepairConfig::getUpdateTime)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(FaultRepairConfig::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<FaultRepairConfig> candidates = faultRepairConfigMapper.selectList(wrapper);
        FaultRepairConfig bestMatch = null;
        int bestScore = -1;
        for (FaultRepairConfig candidate : candidates) {
            // 调用calculateMatchScore方法，复用统一能力并保证业务规则一致。
            int score = calculateMatchScore(candidate, productCode, productModel);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }
        return bestScore < 0 ? null : bestMatch;
    }

    /**
     * calculateMatchScore。
     *
     * @param candidate 参数
     * @param productCode 参数
     * @param productModel 参数
     * @return 处理结果
     */
    private int calculateMatchScore(FaultRepairConfig candidate, String productCode, String productModel) {
        // 调用getProductCode方法，复用统一能力并保证业务规则一致。
        String configProductCode = normalizeNullableText(candidate.getProductCode());
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        String configProductModel = normalizeNullableText(candidate.getProductModel());
        int score = 0;
        if (configProductModel != null) {
            if (productModel == null || !StrUtil.equals(productModel, configProductModel)) {
                return -1;
            }
            score += 2;
        }
        if (configProductCode != null) {
            if (productCode != null) {
                if (!StrUtil.equals(productCode, configProductCode)) {
                    return -1;
                }
                score += 4;
            } else if (productModel == null) {
                return -1;
            }
        }
        return score;
    }

    /**
     * 构建故障Summary。
     *
     * @param faults 参数
     * @return 处理结果
     */
    private String buildFaultSummary(List<FaultRepairConfigFaultVO> faults) {
        if (CollUtil.isEmpty(faults)) {
            return "";
        }
        return faults.stream()
                .map(FaultRepairConfigFaultVO::getFaultDesc)
                // 调用joining方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.joining("；"));
    }

    /**
     * 规范化NullableText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeNullableText(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}


