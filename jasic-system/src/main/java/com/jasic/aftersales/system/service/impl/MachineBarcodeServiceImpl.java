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
     * 机器条码Mapper数据访问接口。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    @Resource
    private CompanyDataAccessService companyDataAccessService;

    /**
     * 查询listPage相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<MachineBarcodeVO> listPage(MachineBarcodeQuery query) {
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long ownerHqId = resolveOwnerHqId(query.getOwnerHqId(), query.getTargetCompanyId());
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<MachineBarcode> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(MachineBarcode::getHqCompanyId, ownerHqId);
        if (StrUtil.isNotBlank(query.getBarcode())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getBarcode, query.getBarcode().trim());
        }
        if (StrUtil.isNotBlank(query.getDeliverNumber())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getDeliverNumber, query.getDeliverNumber().trim());
        }
        if (StrUtil.isNotBlank(query.getCustId())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getCustId, query.getCustId().trim());
        }
        if (StrUtil.isNotBlank(query.getSalesOrg())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getSalesOrg, query.getSalesOrg().trim());
        }
        if (StrUtil.isNotBlank(query.getProductCode())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getProductCode, query.getProductCode().trim());
        }
        if (StrUtil.isNotBlank(query.getMachineNo())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getMachineNo, query.getMachineNo().trim());
        }
        if (StrUtil.isNotBlank(query.getProductModel())) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            wrapper.like(MachineBarcode::getProductModel, query.getProductModel().trim());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(MachineBarcode::getStatus, query.getStatus());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(MachineBarcode::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<MachineBarcode> result = machineBarcodeMapper.selectPage(page, wrapper);
        // 调用getRecords方法，复用统一能力并保证业务规则一致。
        List<MachineBarcodeVO> records = buildMachineBarcodeVOList(result.getRecords());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据ID查询机器条码详情。
     *
     * @param ownerHqIdParam 参数
     * @return 处理结果
     */
    @Override
    public MachineBarcodeVO getById(Long id, Long ownerHqIdParam, Long targetCompanyId) {
        // 调用resolveOwnerHqId方法，复用统一能力并保证业务规则一致。
        Long ownerHqId = resolveOwnerHqId(ownerHqIdParam, targetCompanyId);
        // 调用selectByIdAndOwnerHq方法，复用统一能力并保证业务规则一致。
        MachineBarcode entity = selectByIdAndOwnerHq(id, ownerHqId);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        return buildMachineBarcodeVOList(Collections.singletonList(entity)).get(0);
    }

    /**
     * 分页查询总部公司Options列表。
     *
     * @return 处理结果
     */
    @Override
    public List<SysCompanySimpleVO> listHqCompanyOptions() {
        if (!SecurityContext.isPlatformUser()) {
            if (SubjectTypeEnum.HQ.getCode().equals(SecurityContext.getCurrentSubjectType())) {
                // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
                Long currentCompanyId = SecurityContext.getCurrentCompanyId();
                if (currentCompanyId == null) {
                    throw new ServiceException("缺少公司数据访问上下文");
                }
                // 说明：执行该步骤以保证业务流程正确。
                validateHqCompany(currentCompanyId, buildSubjectTypeMap());
                // 说明：执行该步骤以保证业务流程正确。
                SysCompany company = sysCompanyMapper.selectById(currentCompanyId);
                if (company == null || !Objects.equals(company.getStatus(), STATUS_ENABLED)) {
                    throw new ServiceException("当前总部不存在或已停用");
                }
                return Collections.singletonList(buildCompanySimpleVo(company, buildTypeNameMap()));
            }
            throw new ServiceException("当前公司不支持维护条码档案");
        }
        // 调用listAll方法，复用统一能力并保证业务规则一致。
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        List<String> hqTypeCodes = listTypeCodesBySubjectType(companyTypes, SubjectTypeEnum.HQ.getCode());
        if (CollUtil.isEmpty(hqTypeCodes)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, hqTypeCodes)
                .eq(SysCompany::getStatus, STATUS_ENABLED)
                .orderByAsc(SysCompany::getCompanyName)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysCompany::getId);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
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
     * 新增机器条码。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(MachineBarcodeDTO dto) {
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long ownerHqId = resolveOwnerHqId(dto.getOwnerHqId(), dto.getTargetCompanyId());
        // 调用MachineBarcode方法，复用统一能力并保证业务规则一致。
        MachineBarcode entity = new MachineBarcode();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        entity.setHqCompanyId(ownerHqId);
        // 调用normalizeEntity方法，复用统一能力并保证业务规则一致。
        normalizeEntity(entity);
        // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
        applyDefaultStatus(entity);
        // 说明：执行该步骤以保证业务流程正确。
        validateMachineBarcode(entity, null, buildSubjectTypeMap());
        // 说明：执行该步骤以保证业务流程正确。
        machineBarcodeMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新机器条码。
     *
     * @param dto 参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MachineBarcodeDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("条码档案ID不能为空");
        }
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long ownerHqId = resolveOwnerHqId(dto.getOwnerHqId(), dto.getTargetCompanyId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        MachineBarcode entity = selectByIdAndOwnerHq(dto.getId(), ownerHqId);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, entity);
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        entity.setHqCompanyId(ownerHqId);
        // 调用normalizeEntity方法，复用统一能力并保证业务规则一致。
        normalizeEntity(entity);
        // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
        applyDefaultStatus(entity);
        // 说明：执行该步骤以保证业务流程正确。
        validateMachineBarcode(entity, entity.getId(), buildSubjectTypeMap());
        LambdaQueryWrapper<MachineBarcode> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(MachineBarcode::getId, entity.getId())
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 调用resolveOwnerHqId方法，复用统一能力并保证业务规则一致。
        Long ownerHqId = resolveOwnerHqId(null, null);
        // 调用selectByIdAndOwnerHq方法，复用统一能力并保证业务规则一致。
        MachineBarcode entity = selectByIdAndOwnerHq(id, ownerHqId);
        if (entity == null) {
            throw new ServiceException("条码档案不存在");
        }
        LambdaQueryWrapper<MachineBarcode> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(MachineBarcode::getId, id)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // 说明：执行该步骤以保证业务流程正确。
        if (machineBarcodeMapper.delete(deleteWrapper) == 0) {
            throw new ServiceException("无权操作该条码档案");
        }
    }

    /**
     * importItems。
     *
     * @param items 参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer importItems(List<MachineBarcodeImportItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            throw new ServiceException("导入数据不能为空");
        }

        // 调用buildSubjectTypeMap方法，复用统一能力并保证业务规则一致。
        Map<String, String> subjectTypeMap = buildSubjectTypeMap();
        Set<String> payloadBarcodes = new LinkedHashSet<>();
        List<MachineBarcode> normalizedItems = new ArrayList<>();
        for (MachineBarcodeImportItemDTO item : items) {
            // 调用MachineBarcode方法，复用统一能力并保证业务规则一致。
            MachineBarcode incoming = new MachineBarcode();
            // 调用copyProperties方法，复用统一能力并保证业务规则一致。
            BeanUtil.copyProperties(item, incoming);
            // 调用normalizeEntity方法，复用统一能力并保证业务规则一致。
            normalizeEntity(incoming);
            // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
            applyDefaultStatus(incoming);
            // 说明：执行该步骤以保证业务流程正确。
            validateImportPayloadItem(incoming, subjectTypeMap);
            if (!payloadBarcodes.add(incoming.getBarcode())) {
                throw new ServiceException("导入数据存在重复条码：" + incoming.getBarcode());
            }
            // 调用add方法，复用统一能力并保证业务规则一致。
            normalizedItems.add(incoming);
        }

        int count = 0;
        for (MachineBarcode incoming : normalizedItems) {
            // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
            MachineBarcode existing = findByBarcodeAndOwnerHq(incoming.getBarcode(), incoming.getHqCompanyId());
            if (existing == null) {
                // 调用validateMachineBarcode方法，复用统一能力并保证业务规则一致。
                validateMachineBarcode(incoming, null, subjectTypeMap);
                // 说明：执行该步骤以保证业务流程正确。
                machineBarcodeMapper.insert(incoming);
            } else {
                // 调用copyProperties方法，复用统一能力并保证业务规则一致。
                BeanUtil.copyProperties(incoming, existing);
                // 调用getId方法，复用统一能力并保证业务规则一致。
                validateMachineBarcode(existing, existing.getId(), subjectTypeMap);
                LambdaQueryWrapper<MachineBarcode> updateWrapper = new LambdaQueryWrapper<>();
                updateWrapper.eq(MachineBarcode::getId, existing.getId())
                        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
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
     * @param records 参数
     * @return 处理结果
     */
    private List<MachineBarcodeVO> buildMachineBarcodeVOList(List<MachineBarcode> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }

        List<Long> hqCompanyIds = records.stream()
                .map(MachineBarcode::getHqCompanyId)
                .filter(Objects::nonNull)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());

        Map<Long, SysCompany> companyMap = new HashMap<>();
        if (CollUtil.isNotEmpty(hqCompanyIds)) {
            LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
            // 调用in方法，复用统一能力并保证业务规则一致。
            wrapper.in(SysCompany::getId, hqCompanyIds);
            // 说明：执行该步骤以保证业务流程正确。
            List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
            companyMap = companies.stream()
                    // 调用toMap方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toMap(SysCompany::getId, company -> company, (a, b) -> a));
        }

        List<MachineBarcodeVO> result = new ArrayList<>();
        for (MachineBarcode record : records) {
            // 调用copyProperties方法，复用统一能力并保证业务规则一致。
            MachineBarcodeVO vo = BeanUtil.copyProperties(record, MachineBarcodeVO.class);
            // 调用resolveLastOutDate方法，复用统一能力并保证业务规则一致。
            vo.setLastOutDate(MachineBarcodeWarrantyResolver.resolveLastOutDate(record));
            // 调用resolveWarrantyStatus方法，复用统一能力并保证业务规则一致。
            vo.setWarrantyStatus(MachineBarcodeWarrantyResolver.resolveWarrantyStatus(record));
            // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
            SysCompany company = companyMap.get(record.getHqCompanyId());
            if (company != null) {
                // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
                vo.setHqCompanyName(company.getCompanyName());
            }
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 校验机器条码。
     *
     * @param entity 参数
     * @param currentId current ID
     * @param subjectTypeMap 参数
     */
    private void validateMachineBarcode(MachineBarcode entity, Long currentId, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateStatus(entity.getStatus());
        // 调用validateUniqueBarcode方法，复用统一能力并保证业务规则一致。
        validateUniqueBarcode(entity, currentId);
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    /**
     * 校验ImportPayload项。
     *
     * @param entity 参数
     * @param subjectTypeMap 参数
     */
    private void validateImportPayloadItem(MachineBarcode entity, Map<String, String> subjectTypeMap) {
        if (StrUtil.isBlank(entity.getBarcode())) {
            throw new ServiceException("机器条码不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateStatus(entity.getStatus());
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        validateHqCompany(entity.getHqCompanyId(), subjectTypeMap);
    }

    /**
     * 校验状态。
     *
     * @param status 参数
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
     * @param entity 参数
     * @param currentId current ID
     */
    private void validateUniqueBarcode(MachineBarcode entity, Long currentId) {
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        MachineBarcode duplicated = findByBarcodeAndOwnerHq(entity.getBarcode(), entity.getHqCompanyId());
        if (duplicated != null && !Objects.equals(duplicated.getId(), currentId)) {
            throw new ServiceException("机器条码已存在");
        }
    }

    /**
     * 校验总部公司。
     *
     * @param hqCompanyId hq Company ID
     * @param subjectTypeMap 参数
     */
    private void validateHqCompany(Long hqCompanyId, Map<String, String> subjectTypeMap) {
        if (hqCompanyId == null) {
            throw new ServiceException("归属总部不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        String subjectType = subjectTypeMap.get(company.getTypeCode());
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            throw new ServiceException("归属公司不是总部类型");
        }
    }

    /**
     * findBy条码AndOwner总部。
     *
     * @param barcode 参数
     * @return 处理结果
     */
    private MachineBarcode findByBarcodeAndOwnerHq(String barcode, Long ownerHqId) {
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, barcode)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // 说明：执行该步骤以保证业务流程正确。
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * selectByIDAndOwner总部。
     *
     * @return 处理结果
     */
    private MachineBarcode selectByIdAndOwnerHq(Long id, Long ownerHqId) {
        if (id == null || ownerHqId == null) {
            return null;
        }
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getId, id)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(MachineBarcode::getHqCompanyId, ownerHqId);
        // 说明：执行该步骤以保证业务流程正确。
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * 解析Owner总部ID。
     *
     * @return 处理结果
     */
    private Long resolveOwnerHqId(Long ownerHqId, Long targetCompanyId) {
        Long requestedOwnerHqId = ownerHqId != null ? ownerHqId : targetCompanyId;
        if (!SecurityContext.isPlatformUser() && requestedOwnerHqId == null) {
            // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
            requestedOwnerHqId = SecurityContext.getCurrentCompanyId();
        }
        return companyDataAccessService.resolveOwnerHqTarget(requestedOwnerHqId);
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
     * 构建类型名称Map。
     *
     * @return 处理结果
     */
    private Map<String, String> buildTypeNameMap() {
        // 调用listAll方法，复用统一能力并保证业务规则一致。
        List<SysCompanyType> companyTypes = companyTypeService.listAll();
        return companyTypes.stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
    }

    /**
     * 分页查询类型CodesBy主体类型列表。
     *
     * @param companyTypes 参数
     * @param subjectType 参数
     * @return 处理结果
     */
    private List<String> listTypeCodesBySubjectType(List<SysCompanyType> companyTypes, String subjectType) {
        return companyTypes.stream()
                .filter(type -> subjectType.equals(type.getSubjectType()))
                .map(SysCompanyType::getTypeCode)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * 构建公司SimpleVo。
     *
     * @param company 参数
     * @param typeNameMap 参数
     * @return 处理结果
     */
    private SysCompanySimpleVO buildCompanySimpleVo(SysCompany company, Map<String, String> typeNameMap) {
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
        return vo;
    }

    /**
     * 应用Default状态。
     *
     * @param entity 参数
     */
    private void applyDefaultStatus(MachineBarcode entity) {
        if (entity.getStatus() == null) {
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            entity.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * 规范化实体。
     *
     * @param entity 参数
     */
    private void normalizeEntity(MachineBarcode entity) {
        // 调用getBarcode方法，复用统一能力并保证业务规则一致。
        entity.setBarcode(normalizeRequiredText(entity.getBarcode()));
        // 调用getDeliverNumber方法，复用统一能力并保证业务规则一致。
        entity.setDeliverNumber(normalizeOptionalText(entity.getDeliverNumber()));
        // 调用getCustId方法，复用统一能力并保证业务规则一致。
        entity.setCustId(normalizeOptionalText(entity.getCustId()));
        // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
        entity.setSalesOrg(normalizeOptionalText(entity.getSalesOrg()));
        // 调用getProductCode方法，复用统一能力并保证业务规则一致。
        entity.setProductCode(normalizeOptionalText(entity.getProductCode()));
        // 调用getProductName方法，复用统一能力并保证业务规则一致。
        entity.setProductName(normalizeOptionalText(entity.getProductName()));
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        entity.setProductModel(normalizeOptionalText(entity.getProductModel()));
        // 调用getMachineNo方法，复用统一能力并保证业务规则一致。
        entity.setMachineNo(normalizeOptionalText(entity.getMachineNo()));
        // 调用getBrandCode方法，复用统一能力并保证业务规则一致。
        entity.setBrandCode(normalizeOptionalText(entity.getBrandCode()));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        entity.setRemark(normalizeOptionalText(entity.getRemark()));
    }

    /**
     * 规范化RequiredText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeRequiredText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 规范化OptionalText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeOptionalText(String value) {
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}



