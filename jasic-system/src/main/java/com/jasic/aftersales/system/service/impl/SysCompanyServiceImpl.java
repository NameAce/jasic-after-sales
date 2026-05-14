package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.CompanyCategoryEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysCompanyDTO;
import com.jasic.aftersales.system.domain.entity.SysArea;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplate;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.query.SysCompanyQuery;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.service.ICompanyGeoResolver;
import com.jasic.aftersales.system.service.ISysAreaService;
import com.jasic.aftersales.system.service.ISysCompanyService;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.ISysRoleTemplateService;
import com.jasic.aftersales.system.service.support.SysUserIdentityValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 公司管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@Service
public class SysCompanyServiceImpl implements ISysCompanyService {

    private static final String DEFAULT_PASSWORD = "Jasic@123";
    private static final String SOURCE_TYPE_CRM = "CRM";
    private static final String SOURCE_TYPE_MANUAL = "MANUAL";
    private static final String GEOCODE_STATUS_SUCCESS = "SUCCESS";
    private static final String GEOCODE_STATUS_FAILED = "FAILED";
    private static final Integer STATUS_ENABLED = 1;
    private static final Integer STATUS_DISABLED = 0;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleTemplateMapper sysRoleTemplateMapper;

    @Resource
    private ISysRoleTemplateService roleTemplateService;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**
     * 系统配置服务服务依赖。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private ISysConfigService configService;

    @Resource
    private ICompanyGeoResolver companyGeoResolver;

    @Resource
    private ISysAreaService sysAreaService;

    @Resource
    private SysUserIdentityValidator userIdentityValidator;

    /**
     * 查询listPage相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public PageResult<SysCompany> listPage(SysCompanyQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysCompany> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getCompanyName())) {
            // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysCompany::getCompanyName, query.getCompanyName());
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            // 调用getCategory方法，复用统一能力并保证业务规则一致。
            applyCategoryFilter(wrapper, query.getCategory());
        } else if (StrUtil.isNotBlank(query.getTypeCode())) {
            // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysCompany::getTypeCode, query.getTypeCode());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysCompany::getStatus, query.getStatus());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SysCompany::getId);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SysCompany> result = sysCompanyMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 应用分类Filter。
     *
     * @param wrapper 参数
     * @param category 参数
     */
    private void applyCategoryFilter(LambdaQueryWrapper<SysCompany> wrapper, String category) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        CompanyCategoryEnum categoryEnum = CompanyCategoryEnum.getByCode(category);
        if (categoryEnum == null) {
            return;
        }
        switch (categoryEnum) {
            case HQ:
                // 调用listAll方法，复用统一能力并保证业务规则一致。
                List<SysCompanyType> allTypes = companyTypeService.listAll();
                List<String> hqTypeCodes = allTypes.stream()
                        .filter(t -> SubjectTypeEnum.HQ.getCode().equals(t.getSubjectType()))
                        .map(SysCompanyType::getTypeCode)
                        // 调用toList方法，复用统一能力并保证业务规则一致。
                        .collect(Collectors.toList());
                if (!hqTypeCodes.isEmpty()) {
                    // 调用in方法，复用统一能力并保证业务规则一致。
                    wrapper.in(SysCompany::getTypeCode, hqTypeCodes);
                } else {
                    // 调用eq方法，复用统一能力并保证业务规则一致。
                    wrapper.eq(SysCompany::getTypeCode, "__none__");
                }
                break;
            case FIRST_LEVEL:
                // 调用getFirstLevelTypeCodes方法，复用统一能力并保证业务规则一致。
                wrapper.in(SysCompany::getTypeCode, CompanyCategoryEnum.getFirstLevelTypeCodes());
                break;
            case SECOND_LEVEL:
                // 调用getSecondLevelTypeCodes方法，复用统一能力并保证业务规则一致。
                wrapper.in(SysCompany::getTypeCode, CompanyCategoryEnum.getSecondLevelTypeCodes());
                break;
            default:
                break;
        }
    }

    /**
     * 根据ID查询公司详情。
     *
     * @return 处理结果
     */
    @Override
    public SysCompany getById(Long id) {
        return sysCompanyMapper.selectById(id);
    }

    /**
     * 新增公司。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SysCompanyDTO dto) {
        // 调用normalizeDto方法，复用统一能力并保证业务规则一致。
        normalizeDto(dto);
        // 说明：执行该步骤以保证业务流程正确。
        String subjectType = validateCompanyType(dto.getTypeCode());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        validateCompanyStatus(dto.getStatus());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        validateCompanyCodeRequired(subjectType, dto.getCompanyCode());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        validateCompanyCodeUnique(null, dto.getCompanyCode());
        // 调用applyCreateSourceType方法，复用统一能力并保证业务规则一致。
        applyCreateSourceType(dto);
        // 调用getSourceType方法，复用统一能力并保证业务规则一致。
        validateSourceType(dto.getSourceType());
        // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
        dto.setSalesOrg(validateSalesOrg(null, subjectType, dto.getSalesOrg()));
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        validateAdminTemplate(dto.getTypeCode());
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        validateAdminLoginIdentity(dto.getAdminUsername(), dto.getContactPhone());

        // 调用resolveRegion方法，复用统一能力并保证业务规则一致。
        ResolvedRegion resolvedRegion = resolveRegion(dto);
        // 调用SysCompany方法，复用统一能力并保证业务规则一致。
        SysCompany company = new SysCompany();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, company);
        // 调用applyResolvedRegion方法，复用统一能力并保证业务规则一致。
        applyResolvedRegion(company, resolvedRegion);
        // 调用buildFullAddress方法，复用统一能力并保证业务规则一致。
        company.setFullAddress(buildFullAddress(company));
        // 调用applyDefaultStatus方法，复用统一能力并保证业务规则一致。
        applyDefaultStatus(company);
        // 调用getFullAddress方法，复用统一能力并保证业务规则一致。
        applyGeocodeResult(company, resolveGeoLocationSafely(company.getFullAddress()));
        // 说明：执行该步骤以保证业务流程正确。
        sysCompanyMapper.insert(company);

        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        Long adminRoleId = roleTemplateService.initCompanyRoles(company.getId(), dto.getTypeCode());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        createDefaultAdmin(dto, company.getId(), adminRoleId);
        return company.getId();
    }

    /**
     * 更新公司。
     *
     * @param dto 参数
     */
    @Override
    public void update(SysCompanyDTO dto) {
        // 调用normalizeDto方法，复用统一能力并保证业务规则一致。
        normalizeDto(dto);
        if (dto.getId() == null) {
            throw new ServiceException("公司ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(dto.getId());
        if (company == null) {
            throw new ServiceException("公司不存在");
        }

        // 说明：执行该步骤以保证业务流程正确。
        String subjectType = validateCompanyType(dto.getTypeCode());
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        validateCompanyStatus(dto.getStatus());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        validateCompanyCodeRequired(subjectType, dto.getCompanyCode());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        validateCompanyCodeUnique(dto.getId(), dto.getCompanyCode());
        // 调用getSourceType方法，复用统一能力并保证业务规则一致。
        dto.setSourceType(StrUtil.blankToDefault(company.getSourceType(), SOURCE_TYPE_MANUAL));
        // 调用getSourceType方法，复用统一能力并保证业务规则一致。
        validateSourceType(dto.getSourceType());
        // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
        dto.setSalesOrg(validateSalesOrg(dto.getId(), subjectType, dto.getSalesOrg()));

        // 调用resolveRegion方法，复用统一能力并保证业务规则一致。
        ResolvedRegion resolvedRegion = resolveRegion(dto);
        // 调用shouldResolveAddress方法，复用统一能力并保证业务规则一致。
        boolean shouldResolve = shouldResolveAddress(company, dto);

        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, company);
        // 调用applyResolvedRegion方法，复用统一能力并保证业务规则一致。
        applyResolvedRegion(company, resolvedRegion);
        // 调用buildFullAddress方法，复用统一能力并保证业务规则一致。
        company.setFullAddress(buildFullAddress(company));
        if (shouldResolve) {
            // 调用getFullAddress方法，复用统一能力并保证业务规则一致。
            applyGeocodeResult(company, resolveGeoLocationSafely(company.getFullAddress()));
        }
        if (company.getStatus() == null) {
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            company.setStatus(STATUS_ENABLED);
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysCompanyMapper.updateById(company);
    }

    /**
     * 删除公司。
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUserCompany::getCompanyId, id);
        // 说明：执行该步骤以保证业务流程正确。
        if (sysUserCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该公司下存在用户，不允许删除");
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysCompanyMapper.deleteById(id);
    }

    /**
     * 创建DefaultAdmin。
     *
     * @param dto 参数
     * @param adminRoleId admin Role ID
     */
    private void createDefaultAdmin(SysCompanyDTO dto, Long companyId, Long adminRoleId) {
        String initPassword = StrUtil.blankToDefault(
                // 调用getValueByKey方法，复用统一能力并保证业务规则一致。
                configService.getValueByKey("org.company.adminInitPassword"), DEFAULT_PASSWORD);

        // 调用SysUser方法，复用统一能力并保证业务规则一致。
        SysUser adminUser = new SysUser();
        // 调用getAdminUsername方法，复用统一能力并保证业务规则一致。
        adminUser.setUsername(dto.getAdminUsername());
        // 调用gensalt方法，复用统一能力并保证业务规则一致。
        adminUser.setPassword(BCrypt.hashpw(initPassword, BCrypt.gensalt()));
        // 调用getContactName方法，复用统一能力并保证业务规则一致。
        adminUser.setRealName(dto.getContactName());
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        adminUser.setPhone(dto.getContactPhone());
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        adminUser.setStatus(STATUS_ENABLED);
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.insert(adminUser);

        // 调用SysUserCompany方法，复用统一能力并保证业务规则一致。
        SysUserCompany userCompany = new SysUserCompany();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        userCompany.setUserId(adminUser.getId());
        // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
        userCompany.setCompanyId(companyId);
        // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
        userCompany.setIsDefault(1);
        // 调用insert方法，复用统一能力并保证业务规则一致。
        sysUserCompanyMapper.insert(userCompany);

        if (adminRoleId != null) {
            // 调用SysUserRole方法，复用统一能力并保证业务规则一致。
            SysUserRole userRole = new SysUserRole();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            userRole.setUserId(adminUser.getId());
            // 调用setRoleId方法，复用统一能力并保证业务规则一致。
            userRole.setRoleId(adminRoleId);
            // 调用insert方法，复用统一能力并保证业务规则一致。
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 校验公司类型。
     *
     * @param typeCode 参数
     * @return 处理结果
     */
    private String validateCompanyType(String typeCode) {
        for (SysCompanyType item : companyTypeService.listAll()) {
            if (StrUtil.equals(item.getTypeCode(), typeCode)) {
                return item.getSubjectType();
            }
        }
        throw new ServiceException("公司类型不存在");
    }

    /**
     * 校验公司状态。
     *
     * @param status 参数
     */
    private void validateCompanyStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (!Objects.equals(STATUS_ENABLED, status) && !Objects.equals(STATUS_DISABLED, status)) {
            throw new ServiceException("公司状态不合法");
        }
    }

    /**
     * 校验公司编码Required。
     *
     * @param subjectType 参数
     * @param companyCode 参数
     */
    private void validateCompanyCodeRequired(String subjectType, String companyCode) {
        if (SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            return;
        }
        if (StrUtil.isBlank(companyCode)) {
            throw new ServiceException("公司编码不能为空");
        }
    }

    /**
     * 校验公司编码Unique。
     *
     * @param currentId current ID
     * @param companyCode 参数
     */
    private void validateCompanyCodeUnique(Long currentId, String companyCode) {
        if (StrUtil.isBlank(companyCode)) {
            return;
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysCompany::getCompanyCode, companyCode);
        if (currentId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysCompany::getId, currentId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (sysCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("公司编码已存在");
        }
    }

    /**
     * apply创建来源类型。
     *
     * @param dto 参数
     */
    private void applyCreateSourceType(SysCompanyDTO dto) {
        if (StrUtil.isBlank(dto.getSourceType())) {
            // 调用setSourceType方法，复用统一能力并保证业务规则一致。
            dto.setSourceType(SOURCE_TYPE_MANUAL);
        }
    }

    /**
     * 校验来源类型。
     *
     * @param sourceType 参数
     */
    private void validateSourceType(String sourceType) {
        if (!StrUtil.equalsAny(sourceType, SOURCE_TYPE_CRM, SOURCE_TYPE_MANUAL)) {
            throw new ServiceException("公司来源类型不合法");
        }
    }

    /**
     * 校验SalesOrg。
     *
     * @param currentId current ID
     * @param subjectType 参数
     * @param salesOrg 参数
     * @return 处理结果
     */
    private String validateSalesOrg(Long currentId, String subjectType, String salesOrg) {
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            if (StrUtil.isNotBlank(salesOrg)) {
                throw new ServiceException("非总部公司不能维护销售组织");
            }
            return null;
        }
        if (StrUtil.isBlank(salesOrg)) {
            throw new ServiceException("总部公司必须维护销售组织");
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysCompany::getSalesOrg, salesOrg);
        if (currentId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysCompany::getId, currentId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (sysCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("销售组织已绑定其他总部公司");
        }
        return salesOrg;
    }

    /**
     * 校验Admin模板。
     *
     * @param typeCode 参数
     */
    private void validateAdminTemplate(String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> adminTplWrapper = new LambdaQueryWrapper<>();
        adminTplWrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysRoleTemplate::getIsAdmin, 1);
        // 说明：执行该步骤以保证业务流程正确。
        if (sysRoleTemplateMapper.selectCount(adminTplWrapper) == 0) {
            throw new ServiceException("请先维护该公司类型（" + typeCode + "）的管理员角色模板");
        }
    }

    /**
     * 校验AdminUsername。
     *
     * @param adminUsername 参数
     */
    private void validateAdminUsername(String adminUsername) {
        if (StrUtil.isBlank(adminUsername)) {
            throw new ServiceException("管理员用户名不能为空");
        }
    }

    /**
     * 校验AdminLogin身份。
     *
     * @param adminUsername 参数
     * @param contactPhone 参数
     */
    private void validateAdminLoginIdentity(String adminUsername, String contactPhone) {
        // 调用validateAdminUsername方法，复用统一能力并保证业务规则一致。
        validateAdminUsername(adminUsername);
        // 调用validateLoginIdentityUnique方法，复用统一能力并保证业务规则一致。
        userIdentityValidator.validateLoginIdentityUnique(null, adminUsername, contactPhone);
    }

    /**
     * shouldResolveAddress。
     *
     * @param originalCompany 参数
     * @param dto 参数
     */
    private boolean shouldResolveAddress(SysCompany originalCompany, SysCompanyDTO dto) {
        if (originalCompany.getLongitude() == null || originalCompany.getLatitude() == null) {
            return true;
        }
        if (!StrUtil.equals(GEOCODE_STATUS_SUCCESS, originalCompany.getGeocodeStatus())) {
            return true;
        }
        return !StrUtil.equals(normalizeNullableText(originalCompany.getProvinceCode()), dto.getProvinceCode())
                || !StrUtil.equals(normalizeNullableText(originalCompany.getCityCode()), dto.getCityCode())
                || !StrUtil.equals(normalizeNullableText(originalCompany.getDistrictCode()), dto.getDistrictCode())
                // 调用getDetailAddress方法，复用统一能力并保证业务规则一致。
                || !StrUtil.equals(normalizeNullableText(originalCompany.getDetailAddress()), dto.getDetailAddress());
    }

    /**
     * 应用Default状态。
     *
     * @param company 参数
     */
    private void applyDefaultStatus(SysCompany company) {
        if (company.getStatus() == null) {
            // 调用setStatus方法，复用统一能力并保证业务规则一致。
            company.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * 规范化Dto。
     *
     * @param dto 参数
     */
    private void normalizeDto(SysCompanyDTO dto) {
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        dto.setCompanyName(normalizeRequiredText(dto.getCompanyName()));
        // 调用getCompanyShortName方法，复用统一能力并保证业务规则一致。
        dto.setCompanyShortName(normalizeNullableText(dto.getCompanyShortName()));
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        dto.setCompanyCode(normalizeNullableText(dto.getCompanyCode()));
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        dto.setTypeCode(normalizeRequiredText(dto.getTypeCode()));
        // 调用getContactName方法，复用统一能力并保证业务规则一致。
        dto.setContactName(normalizeRequiredText(dto.getContactName()));
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        dto.setContactPhone(normalizeRequiredText(dto.getContactPhone()));
        // 调用getProvinceCode方法，复用统一能力并保证业务规则一致。
        dto.setProvinceCode(normalizeRequiredText(dto.getProvinceCode()));
        // 调用getProvinceName方法，复用统一能力并保证业务规则一致。
        dto.setProvinceName(normalizeNullableText(dto.getProvinceName()));
        // 调用getCityCode方法，复用统一能力并保证业务规则一致。
        dto.setCityCode(normalizeRequiredText(dto.getCityCode()));
        // 调用getCityName方法，复用统一能力并保证业务规则一致。
        dto.setCityName(normalizeNullableText(dto.getCityName()));
        // 调用getDistrictCode方法，复用统一能力并保证业务规则一致。
        dto.setDistrictCode(normalizeRequiredText(dto.getDistrictCode()));
        // 调用getDistrictName方法，复用统一能力并保证业务规则一致。
        dto.setDistrictName(normalizeNullableText(dto.getDistrictName()));
        // 调用getDetailAddress方法，复用统一能力并保证业务规则一致。
        dto.setDetailAddress(normalizeRequiredText(dto.getDetailAddress()));
        // 调用getAdminUsername方法，复用统一能力并保证业务规则一致。
        dto.setAdminUsername(normalizeNullableText(dto.getAdminUsername()));
        // 调用getServicePhone方法，复用统一能力并保证业务规则一致。
        dto.setServicePhone(normalizeNullableText(dto.getServicePhone()));
        // 调用getSourceType方法，复用统一能力并保证业务规则一致。
        dto.setSourceType(normalizeNullableText(dto.getSourceType()));
        // 调用getSalesOrg方法，复用统一能力并保证业务规则一致。
        dto.setSalesOrg(normalizeNullableText(dto.getSalesOrg()));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        dto.setRemark(normalizeNullableText(dto.getRemark()));
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
     * 解析地区。
     *
     * @param dto 参数
     * @return 处理结果
     */
    private ResolvedRegion resolveRegion(SysCompanyDTO dto) {
        Map<String, SysArea> areaMap = sysAreaService.getByAreaCodes(
                // 调用getDistrictCode方法，复用统一能力并保证业务规则一致。
                Arrays.asList(dto.getProvinceCode(), dto.getCityCode(), dto.getDistrictCode()));
        // 说明：执行该步骤以保证业务流程正确。
        SysArea province = requireArea(areaMap.get(dto.getProvinceCode()), dto.getProvinceCode(),
                ISysAreaService.LEVEL_PROVINCE, "省份");
        SysArea city = requireArea(areaMap.get(dto.getCityCode()), dto.getCityCode(),
                ISysAreaService.LEVEL_CITY, "城市");
        SysArea district = requireArea(areaMap.get(dto.getDistrictCode()), dto.getDistrictCode(),
                ISysAreaService.LEVEL_DISTRICT, "区县");
        if (!StrUtil.equals(province.getAreaCode(), city.getParentCode())) {
            throw new ServiceException("城市与省份不匹配");
        }
        if (!StrUtil.equals(city.getAreaCode(), district.getParentCode())) {
            throw new ServiceException("区县与城市不匹配");
        }
        return new ResolvedRegion(province, city, district);
    }

    /**
     * requireArea。
     *
     * @param area 参数
     * @param areaCode 参数
     * @param expectedLevel 参数
     * @param label 参数
     * @return 处理结果
     */
    private SysArea requireArea(SysArea area, String areaCode, String expectedLevel, String label) {
        if (area == null || !StrUtil.equals(expectedLevel, area.getAreaLevel())) {
            throw new ServiceException(label + "编码无效：" + areaCode);
        }
        return area;
    }

    /**
     * applyResolved地区。
     *
     * @param company 参数
     * @param resolvedRegion 参数
     */
    private void applyResolvedRegion(SysCompany company, ResolvedRegion resolvedRegion) {
        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        company.setProvinceCode(resolvedRegion.getProvince().getAreaCode());
        // 调用getAreaName方法，复用统一能力并保证业务规则一致。
        company.setProvinceName(resolvedRegion.getProvince().getAreaName());
        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        company.setCityCode(resolvedRegion.getCity().getAreaCode());
        // 调用getAreaName方法，复用统一能力并保证业务规则一致。
        company.setCityName(resolvedRegion.getCity().getAreaName());
        // 调用getAreaCode方法，复用统一能力并保证业务规则一致。
        company.setDistrictCode(resolvedRegion.getDistrict().getAreaCode());
        // 调用getAreaName方法，复用统一能力并保证业务规则一致。
        company.setDistrictName(resolvedRegion.getDistrict().getAreaName());
    }

    /**
     * 构建FullAddress。
     *
     * @param company 参数
     * @return 处理结果
     */
    private String buildFullAddress(SysCompany company) {
        // 调用StringBuilder方法，复用统一能力并保证业务规则一致。
        StringBuilder builder = new StringBuilder();
        // 调用getProvinceName方法，复用统一能力并保证业务规则一致。
        appendAddressPart(builder, company.getProvinceName());
        if (!shouldSkipCityInFullAddress(company.getProvinceName(), company.getCityName())) {
            // 调用getCityName方法，复用统一能力并保证业务规则一致。
            appendAddressPart(builder, company.getCityName());
        }
        // 调用getDistrictName方法，复用统一能力并保证业务规则一致。
        appendAddressPart(builder, company.getDistrictName());
        // 调用getDetailAddress方法，复用统一能力并保证业务规则一致。
        appendAddressPart(builder, company.getDetailAddress());
        return builder.toString();
    }

    /**
     * shouldSkipCityInFullAddress。
     *
     * @param provinceName 参数
     * @param cityName 参数
     */
    private boolean shouldSkipCityInFullAddress(String provinceName, String cityName) {
        if (StrUtil.isBlank(cityName)) {
            return true;
        }
        if (StrUtil.equals(provinceName, cityName)) {
            return true;
        }
        return cityName.contains("直辖县级行政区划");
    }

    /**
     * appendAddressPart。
     *
     * @param builder 参数
     * @param value 参数
     */
    private void appendAddressPart(StringBuilder builder, String value) {
        // 调用normalizeNullableText方法，复用统一能力并保证业务规则一致。
        String normalized = normalizeNullableText(value);
        if (normalized != null) {
            // 调用append方法，复用统一能力并保证业务规则一致。
            builder.append(normalized);
        }
    }

    /**
     * applyGeocode结果。
     *
     * @param company 参数
     * @param geocodeResult 参数
     */
    private void applyGeocodeResult(SysCompany company, GeocodeResult geocodeResult) {
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        company.setGeocodeStatus(geocodeResult.getStatus());
        // 调用getLongitude方法，复用统一能力并保证业务规则一致。
        company.setLongitude(geocodeResult.getLongitude());
        // 调用getLatitude方法，复用统一能力并保证业务规则一致。
        company.setLatitude(geocodeResult.getLatitude());
    }

    /**
     * 解析地理LocationSafely。
     *
     * @param fullAddress 参数
     * @return 处理结果
     */
    private GeocodeResult resolveGeoLocationSafely(String fullAddress) {
        try {
            // 调用resolve方法，复用统一能力并保证业务规则一致。
            ICompanyGeoResolver.GeoLocation geoLocation = companyGeoResolver.resolve(fullAddress);
            return GeocodeResult.success(geoLocation);
        } catch (ServiceException ex) {
            // 调用getMessage方法，复用统一能力并保证业务规则一致。
            log.warn("Company geocode failed, fullAddress={}, message={}", fullAddress, ex.getMessage());
            return GeocodeResult.failed();
        }
    }

    private static final class ResolvedRegion {

        /**
     * 系统Area字段。
     *
     * @param province 参数
     * @param city 参数
     * @param district 参数
     * @return 处理结果
         */
        private final SysArea province;

        private final SysArea city;

        private final SysArea district;

        /**
     * 构造系统公司实例。
     *
     * @param province 参数
     * @param city 参数
     * @param district 参数
     * @return 处理结果
         */
        private ResolvedRegion(SysArea province, SysArea city, SysArea district) {
            this.province = province;
            this.city = city;
            this.district = district;
        }

        /**
     * 获取Province。
     *
     * @return 处理结果
         */
        private SysArea getProvince() {
            return province;
        }

        /**
     * 获取City。
     *
     * @return 处理结果
         */
        private SysArea getCity() {
            return city;
        }

        /**
     * 获取District。
     *
     * @return 处理结果
         */
        private SysArea getDistrict() {
            return district;
        }
    }

    private static final class GeocodeResult {

        /**
     * 系统公司状态。
     *
     * @param status 参数
     * @param longitude 参数
     * @param latitude 参数
     * @return 处理结果
         */
        private final String status;

        private final BigDecimal longitude;

        private final BigDecimal latitude;

        /**
     * 构造系统公司实例。
     *
     * @param status 参数
     * @param longitude 参数
     * @param latitude 参数
     * @return 处理结果
         */
        private GeocodeResult(String status, BigDecimal longitude, BigDecimal latitude) {
            this.status = status;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        /**
     * success。
     *
     * @param geoLocation 参数
     * @return 处理结果
         */
        private static GeocodeResult success(ICompanyGeoResolver.GeoLocation geoLocation) {
            return new GeocodeResult(GEOCODE_STATUS_SUCCESS, geoLocation.getLongitude(), geoLocation.getLatitude());
        }

        /**
     * failed。
     *
     * @return 处理结果
         */
        private static GeocodeResult failed() {
            return new GeocodeResult(GEOCODE_STATUS_FAILED, null, null);
        }

        /**
     * 获取状态。
     *
     * @return 处理结果
         */
        private String getStatus() {
            return status;
        }

        /**
     * 获取Longitude。
     *
     * @return 处理结果
         */
        private BigDecimal getLongitude() {
            return longitude;
        }

        /**
     * 获取Latitude。
     *
     * @return 处理结果
         */
        private BigDecimal getLatitude() {
            return latitude;
        }
    }
}




