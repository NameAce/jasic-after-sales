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
 * @author Zoro
 * @date 2026/04/01
 */
@Service
public class MachineBarcodeServiceImpl implements IMachineBarcodeService {

    /**STATUS_ENABLED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Integer STATUS_ENABLED = 1;
    /**STATUS_DISABLED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Integer STATUS_DISABLED = 0;

    /**
     * 机器条码Mapper数据访问接口。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    /**sysCompanyMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    /**companyTypeService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**companyDataAccessService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private CompanyDataAccessService companyDataAccessService;

    /**
     * 查询listPage相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
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
        Page<MachineBarcode> result = machineBarcodeMapper.selectPage(page, wrapper);
        List<MachineBarcodeVO> records = buildMachineBarcodeVOList(result.getRecords());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据ID查询机器条码详情。
     *
     * @param ownerHqIdParam ownerHqIdParam，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * 分页查询总部公司Options列表。
     *
     * @return 业务处理结果
     */
    @Override
    public List<SysCompanySimpleVO> listHqCompanyOptions() {
        if (!SecurityContext.isPlatformUser()) {
            if (SubjectTypeEnum.HQ.getCode().equals(SecurityContext.getCurrentSubjectType())) {
                Long currentCompanyId = SecurityContext.getCurrentCompanyId();
                if (currentCompanyId == null) {
                    throw new ServiceException("缺少公司数据访问上下文");
                }
                validateHqCompany(currentCompanyId, buildSubjectTypeMap());
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
     * 新增机器条码。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
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
        validateMachineBarcode(entity, null, buildSubjectTypeMap());
        machineBarcodeMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新机器条码。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
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
        validateMachineBarcode(entity, entity.getId(), buildSubjectTypeMap());
        LambdaQueryWrapper<MachineBarcode> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(MachineBarcode::getId, entity.getId())
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        if (machineBarcodeMapper.update(entity, updateWrapper) == 0) {
            throw new ServiceException("无权操作该条码档案");
        }
    }

    /**
     * 删除机器条码。
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
        if (machineBarcodeMapper.delete(deleteWrapper) == 0) {
            throw new ServiceException("无权操作该条码档案");
        }
    }

    /**
     * importItems。
     *
     * @param items 业务数据集合，用于批量校验、转换或返回组装。
     * @return 业务处理结果
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
     * 构建机器条码视图列表。
     *
     * @param records records，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * 校验机器条码。
     *
     * @param entity entity，当前业务处理所需的输入值。
     * @param currentId current ID
     * @param subjectTypeMap 业务映射数据，用于批量组装或快速查找。
     */
    private void validateMachineBarcode(MachineBarcode entity, Long currentId, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        validateStatus(entity.getStatus());
        validateUniqueBarcode(entity, currentId);
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    /**
     * 校验ImportPayload项。
     *
     * @param entity entity，当前业务处理所需的输入值。
     * @param subjectTypeMap 业务映射数据，用于批量组装或快速查找。
     */
    private void validateImportPayloadItem(MachineBarcode entity, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        validateStatus(entity.getStatus());
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    /**
     * 校验状态。
     *
     * @param status 业务状态编码，用于状态流转或展示判断。
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
     * 校验Unique条码。
     *
     * @param entity entity，当前业务处理所需的输入值。
     * @param currentId current ID
     */
    private void validateUniqueBarcode(MachineBarcode entity, Long currentId) {
        MachineBarcode duplicated = findByBarcodeAndOwnerHq(entity.getBarcode(), entity.getHqCompanyId());
        if (duplicated != null && !Objects.equals(duplicated.getId(), currentId)) {
            throw new ServiceException("机器条码已存在");
        }
    }

    /**
     * 校验总部公司。
     *
     * @param hqCompanyId hq Company ID
     * @param subjectTypeMap 业务映射数据，用于批量组装或快速查找。
     */
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

    /**
     * findBy条码AndOwner总部。
     *
     * @param barcode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    private MachineBarcode findByBarcodeAndOwnerHq(String barcode, Long ownerHqId) {
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, barcode)
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * selectByIDAndOwner总部。
     *
     * @return 业务处理结果
     */
    private MachineBarcode selectByIdAndOwnerHq(Long id, Long ownerHqId) {
        if (id == null || ownerHqId == null) {
            return null;
        }
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getId, id)
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * 解析Owner总部ID。
     *
     * @return 业务处理结果
     */
    private Long resolveOwnerHqId(Long ownerHqId, Long targetCompanyId) {
        Long requestedOwnerHqId = ownerHqId != null ? ownerHqId : targetCompanyId;
        if (!SecurityContext.isPlatformUser() && requestedOwnerHqId == null) {
            requestedOwnerHqId = SecurityContext.getCurrentCompanyId();
        }
        return companyDataAccessService.resolveOwnerHqTarget(requestedOwnerHqId);
    }

    /**
     * 构建主体类型Map。
     *
     * @return 业务处理结果
     */
    private Map<String, String> buildSubjectTypeMap() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
    }

    /**
     * 构建类型名称Map。
     *
     * @return 业务处理结果
     */
    private Map<String, String> buildTypeNameMap() {
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
    }

    /**
     * 分页查询类型CodesBy主体类型列表。
     *
     * @param companyTypes 公司业务对象或公司相关值，用于归属、权限或展示。
     * @param subjectType subjectType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private List<String> listTypeCodesBySubjectType(List<SysCompanyType> companyTypes, String subjectType) {
        return companyTypes.stream()
                .filter(type -> subjectType.equals(type.getSubjectType()))
                .map(SysCompanyType::getTypeCode)
                .collect(Collectors.toList());
    }

    /**
     * 构建公司SimpleVo。
     *
     * @param company 公司业务对象或公司相关值，用于归属、权限或展示。
     * @param typeNameMap 业务映射数据，用于批量组装或快速查找。
     * @return 业务处理结果
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
     * 应用Default状态。
     *
     * @param entity entity，当前业务处理所需的输入值。
     */
    private void applyDefaultStatus(MachineBarcode entity) {
        if (entity.getStatus() == null) {
            entity.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * 规范化实体。
     *
     * @param entity entity，当前业务处理所需的输入值。
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
     * 规范化RequiredText。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String normalizeRequiredText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 规范化OptionalText。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String normalizeOptionalText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}



