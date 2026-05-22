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
 * @author Zoro
 * @date 2026/04/01
 */
@Service
public class FaultRepairConfigServiceImpl implements IFaultRepairConfigService {

    /**STATUS_ENABLED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Integer STATUS_ENABLED = 1;

    /**faultRepairConfigMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private FaultRepairConfigMapper faultRepairConfigMapper;

    /**faultRepairConfigFaultMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private FaultRepairConfigFaultMapper faultRepairConfigFaultMapper;

    /**faultRepairConfigOptionMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private FaultRepairConfigOptionMapper faultRepairConfigOptionMapper;

    /**
     * 系统公司Mapper数据访问接口。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    /**companyTypeService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
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
            query = new FaultRepairConfigQuery();
        }
        ensureManagePermission();
        SysCompany ownerHq = resolveOwnerHqForManage(query == null ? null : query.getCompanyId());
        Page<FaultRepairConfig> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, ownerHq.getId());
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

    /**
     * 查询配置详情。
     *
     * @param id 配置ID
     * @return 配置详情
     */
    @Override
    public FaultRepairConfigVO getById(Long id, Long ownerHqId) {
        ensureManagePermission();
        SysCompany ownerHq = resolveOwnerHqForManage(ownerHqId);
        FaultRepairConfig entity = faultRepairConfigMapper.selectById(id);
        if (entity == null || !Objects.equals(entity.getCompanyId(), ownerHq.getId())) {
            throw new ServiceException("故障与维修配置不存在");
        }
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
        ensureManagePermission();
        SysCompany targetCompany = resolveOwnerHqForManage(dto.getCompanyId());
        dto.setCompanyId(targetCompany.getId());
        dto.setTargetCompanyName(targetCompany.getCompanyName());
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

    /**
     * 更新故障与维修配置。
     *
     * @param dto 配置参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(FaultRepairConfigDTO dto) {
        ensureManagePermission();
        if (dto.getId() == null) {
            throw new ServiceException("配置ID不能为空");
        }
        SysCompany ownerHq = resolveOwnerHqForManage(dto.getCompanyId());
        FaultRepairConfig current = faultRepairConfigMapper.selectById(dto.getId());
        if (current == null || !Objects.equals(current.getCompanyId(), ownerHq.getId())) {
            throw new ServiceException("故障与维修配置不存在");
        }
        if (!Objects.equals(current.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException("停用历史配置不允许编辑");
        }
        Integer targetStatus = dto.getStatus();
        if (targetStatus == null) {
            throw new ServiceException("配置状态不合法");
        }
        SysCompany targetCompany = ownerHq;
        dto.setCompanyId(targetCompany.getId());
        dto.setTargetCompanyName(targetCompany.getCompanyName());
        if (Objects.equals(targetStatus, 0)) {
            current.setStatus(0);
            faultRepairConfigMapper.updateById(current);
            return;
        }
        FaultRepairConfig entity = new FaultRepairConfig();
        BeanUtil.copyProperties(dto, entity);
        entity.setId(null);
        entity.setStatus(STATUS_ENABLED);
        normalizeConfig(entity);
        List<FaultRepairConfigFaultDTO> faults = normalizeFaultDtos(dto.getFaults());
        validateConfig(entity, current.getId());
        validateFaults(faults);
        current.setStatus(0);
        faultRepairConfigMapper.updateById(current);
        faultRepairConfigMapper.insert(entity);
        saveFaultItems(entity.getId(), faults);
    }

    /**
     * 查询可配置故障模板的总部列表。
     *
     * @return 总部选项
     */
    @Override
    public List<SysCompanySimpleVO> listCompanyOptions() {
        ensureManagePermission();
        if (isCurrentHqUser()) {
            return Collections.singletonList(buildCompanySimpleVo(requireCurrentHqCompany()));
        }
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
                normalizeNullableText(productModel));
        return buildRepairFaultOptions(config);
    }

    /**
     * 分页查询维修故障OptionsBy配置ID列表。
     *
     * @param configId config ID
     * @return 业务处理结果
     */
    @Override
    public List<WorkOrderRepairFaultOptionVO> listRepairFaultOptionsByConfigId(Long configId) {
        if (configId == null) {
            return Collections.emptyList();
        }
        FaultRepairConfig config = faultRepairConfigMapper.selectById(configId);
        return buildRepairFaultOptions(config);
    }

    /**
     * findEnabled配置IDForResolved总部。
     *
     * @param resolvedHqCompanyId resolved Hq Company ID
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param productModel productModel，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Override
    public Long findEnabledConfigIdForResolvedHq(Long resolvedHqCompanyId, String productCode, String productModel) {
        // TODO: 非合作总部配置读取属于业务兼容场景。后续需结合转单闭环、总部选择、服务关系规则进一步确认。
        FaultRepairConfig config = findMatchedConfigForResolvedHq(resolvedHqCompanyId, normalizeNullableText(productCode),
                normalizeNullableText(productModel));
        return config == null ? null : config.getId();
    }

    /**
     * 构建维修故障Options。
     *
     * @param config config，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private List<WorkOrderRepairFaultOptionVO> buildRepairFaultOptions(FaultRepairConfig config) {
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
                .orderByDesc(FaultRepairConfig::getId);
        String normalizedKeyword = normalizeNullableText(keyword);
        if (normalizedKeyword != null) {
            wrapper.like(FaultRepairConfig::getProductModel, normalizedKeyword);
        }
        List<FaultRepairConfig> configs = faultRepairConfigMapper.selectList(wrapper);
        if (configs == null || configs.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> productModels = new LinkedHashSet<>();
        for (FaultRepairConfig config : configs) {
            String productModel = normalizeNullableText(config.getProductModel());
            if (productModel != null) {
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

    /**
     * 构建公司名称Map。
     *
     * @return 业务处理结果
     */
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

    /**
     * 构建故障VoMap。
     *
     * @return 业务处理结果
     */
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

    /**
     * 构建故障SummaryMap。
     *
     * @return 业务处理结果
     */
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

    /**
     * 构建OptionMap。
     *
     * @return 业务处理结果
     */
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

    /**
     * 新增故障Items。
     *
     * @param configId config ID
     * @param faults faults，当前业务处理所需的输入值。
     */
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

    /**
     * 删除故障Items。
     *
     * @param configId config ID
     */
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

    /**
     * ensureManage权限。
     */
    private void ensureManagePermission() {
        if (SecurityContext.isPlatformUser()) {
            return;
        }
        if (isCurrentHqUser()) {
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
     * @return 业务处理结果
     */
    private SysCompany resolveOwnerHqForManage(Long requestedCompanyId) {
        if (SecurityContext.isPlatformUser()) {
            if (requestedCompanyId == null) {
                throw new ServiceException("缺少目标公司上下文");
            }
            return requireCompany(requestedCompanyId);
        }
        if (isCurrentHqUser()) {
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
     * @return 业务处理结果
     */
    private SysCompany requireCurrentHqCompany() {
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("当前总部不能为空");
        }
        return requireCompany(currentCompanyId);
    }

    /**
     * require公司。
     *
     * @return 业务处理结果
     */
    private SysCompany requireCompany(Long companyId) {
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        Map<String, String> subjectTypeMap = companyTypeService.listAll().stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()))) {
            throw new ServiceException("归属总部必须是总部公司");
        }
        return company;
    }

    /**
     * 构建公司SimpleVo。
     *
     * @param company 公司业务对象或公司相关值，用于归属、权限或展示。
     * @return 业务处理结果
     */
    private SysCompanySimpleVO buildCompanySimpleVo(SysCompany company) {
        SysCompanySimpleVO vo = new SysCompanySimpleVO();
        vo.setId(company.getId());
        vo.setCompanyName(company.getCompanyName());
        vo.setCompanyCode(company.getCompanyCode());
        vo.setTypeCode(company.getTypeCode());
        Map<String, String> typeNameMap = companyTypeService.listAll().stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
        vo.setTypeName(typeNameMap.get(company.getTypeCode()));
        return vo;
    }

    /**
     * 校验配置。
     *
     * @param entity entity，当前业务处理所需的输入值。
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
        validateCompany(entity.getCompanyId());
        if (Objects.equals(entity.getStatus(), STATUS_ENABLED)) {
            validateUniqueEnabled(entity.getCompanyId(), entity.getProductCode(), entity.getProductModel(), currentId);
        }
    }

    /**
     * 校验公司。
     */
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

    /**
     * 校验UniqueEnabled。
     *
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param productModel productModel，当前业务处理所需的输入值。
     * @param currentId current ID
     */
    private void validateUniqueEnabled(Long companyId, String productCode, String productModel, Long currentId) {
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, companyId)
                .eq(FaultRepairConfig::getStatus, STATUS_ENABLED);
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

    /**
     * 校验Faults。
     *
     * @param faults faults，当前业务处理所需的输入值。
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
     * @param faults faults，当前业务处理所需的输入值。
     * @return 业务处理结果
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
            FaultRepairConfigFaultDTO dto = new FaultRepairConfigFaultDTO();
            dto.setFaultDesc(normalizeNullableText(item.getFaultDesc()));
            dto.setRepairOptions(normalizeRepairOptions(item.getRepairOptions()));
            result.add(dto);
        }
        return result;
    }

    /**
     * 规范化维修Options。
     *
     * @param repairOptions repairOptions，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
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

    /**
     * 规范化配置。
     *
     * @param entity entity，当前业务处理所需的输入值。
     */
    private void normalizeConfig(FaultRepairConfig entity) {
        entity.setProductCode(normalizeNullableText(entity.getProductCode()));
        entity.setProductModel(normalizeNullableText(entity.getProductModel()));
        entity.setRemark(normalizeNullableText(entity.getRemark()));
    }

    /**
     * findMatched配置ForResolved总部。
     *
     * @param resolvedHqCompanyId resolved Hq Company ID
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param productModel productModel，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private FaultRepairConfig findMatchedConfigForResolvedHq(Long resolvedHqCompanyId, String productCode, String productModel) {
        if (resolvedHqCompanyId == null) {
            return null;
        }
        LambdaQueryWrapper<FaultRepairConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FaultRepairConfig::getCompanyId, resolvedHqCompanyId)
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

    /**
     * calculateMatchScore。
     *
     * @param candidate 时间值，用于业务节点记录或时效判断。
     * @param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param productModel productModel，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private int calculateMatchScore(FaultRepairConfig candidate, String productCode, String productModel) {
        String configProductCode = normalizeNullableText(candidate.getProductCode());
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
     * @param faults faults，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String buildFaultSummary(List<FaultRepairConfigFaultVO> faults) {
        if (CollUtil.isEmpty(faults)) {
            return "";
        }
        return faults.stream()
                .map(FaultRepairConfigFaultVO::getFaultDesc)
                .collect(Collectors.joining("；"));
    }

    /**
     * 规范化NullableText。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String normalizeNullableText(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}


