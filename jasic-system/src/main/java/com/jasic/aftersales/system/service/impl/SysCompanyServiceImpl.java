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

    /**DEFAULT_PASSWORD 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String DEFAULT_PASSWORD = "Jasic@123";
    /**SOURCE_TYPE_CRM 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String SOURCE_TYPE_CRM = "CRM";
    /**SOURCE_TYPE_MANUAL 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String SOURCE_TYPE_MANUAL = "MANUAL";
    /**GEOCODE_STATUS_SUCCESS 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String GEOCODE_STATUS_SUCCESS = "SUCCESS";
    /**GEOCODE_STATUS_FAILED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String GEOCODE_STATUS_FAILED = "FAILED";
    /**STATUS_ENABLED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Integer STATUS_ENABLED = 1;
    /**STATUS_DISABLED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final Integer STATUS_DISABLED = 0;

    /**sysCompanyMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    /**sysUserCompanyMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    /**sysUserMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserMapper sysUserMapper;

    /**sysUserRoleMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    /**sysRoleTemplateMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysRoleTemplateMapper sysRoleTemplateMapper;

    /**roleTemplateService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ISysRoleTemplateService roleTemplateService;

    /**companyTypeService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**
     * 系统配置服务服务依赖。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
    @Resource
    private ISysConfigService configService;

    /**companyGeoResolver 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ICompanyGeoResolver companyGeoResolver;

    /**sysAreaService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ISysAreaService sysAreaService;

    /**userIdentityValidator 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserIdentityValidator userIdentityValidator;

    /**
     * 查询listPage相关业务数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
    @Override
    public PageResult<SysCompany> listPage(SysCompanyQuery query) {
        Page<SysCompany> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getCompanyName())) {
            wrapper.like(SysCompany::getCompanyName, query.getCompanyName());
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            applyCategoryFilter(wrapper, query.getCategory());
        } else if (StrUtil.isNotBlank(query.getTypeCode())) {
            wrapper.eq(SysCompany::getTypeCode, query.getTypeCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysCompany::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysCompany::getId);
        Page<SysCompany> result = sysCompanyMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 应用分类Filter。
     *
     * @param wrapper wrapper，当前业务处理所需的输入值。
     * @param category category，当前业务处理所需的输入值。
     */
    private void applyCategoryFilter(LambdaQueryWrapper<SysCompany> wrapper, String category) {
        CompanyCategoryEnum categoryEnum = CompanyCategoryEnum.getByCode(category);
        if (categoryEnum == null) {
            return;
        }
        switch (categoryEnum) {
            case HQ:
                List<SysCompanyType> allTypes = companyTypeService.listAll();
                List<String> hqTypeCodes = allTypes.stream()
                        .filter(t -> SubjectTypeEnum.HQ.getCode().equals(t.getSubjectType()))
                        .map(SysCompanyType::getTypeCode)
                        .collect(Collectors.toList());
                if (!hqTypeCodes.isEmpty()) {
                    wrapper.in(SysCompany::getTypeCode, hqTypeCodes);
                } else {
                    wrapper.eq(SysCompany::getTypeCode, "__none__");
                }
                break;
            case FIRST_LEVEL:
                wrapper.in(SysCompany::getTypeCode, CompanyCategoryEnum.getFirstLevelTypeCodes());
                break;
            case SECOND_LEVEL:
                wrapper.in(SysCompany::getTypeCode, CompanyCategoryEnum.getSecondLevelTypeCodes());
                break;
            default:
                break;
        }
    }

    /**
     * 根据ID查询公司详情。
     *
     * @return 业务处理结果
     */
    @Override
    public SysCompany getById(Long id) {
        return sysCompanyMapper.selectById(id);
    }

    /**
     * 新增公司。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SysCompanyDTO dto) {
        normalizeDto(dto);
        String subjectType = validateCompanyType(dto.getTypeCode());
        validateCompanyStatus(dto.getStatus());
        validateCompanyCodeRequired(subjectType, dto.getCompanyCode());
        validateCompanyCodeUnique(null, dto.getCompanyCode());
        applyCreateSourceType(dto);
        validateSourceType(dto.getSourceType());
        dto.setSalesOrg(validateSalesOrg(null, subjectType, dto.getSalesOrg()));
        validateAdminTemplate(dto.getTypeCode());
        validateAdminLoginIdentity(dto.getAdminUsername(), dto.getContactPhone());

        ResolvedRegion resolvedRegion = resolveRegion(dto);
        SysCompany company = new SysCompany();
        BeanUtil.copyProperties(dto, company);
        applyResolvedRegion(company, resolvedRegion);
        company.setFullAddress(buildFullAddress(company));
        applyDefaultStatus(company);
        applyGeocodeResult(company, resolveGeoLocationSafely(company.getFullAddress()));
        sysCompanyMapper.insert(company);

        Long adminRoleId = roleTemplateService.initCompanyRoles(company.getId(), dto.getTypeCode());
        createDefaultAdmin(dto, company.getId(), adminRoleId);
        return company.getId();
    }

    /**
     * 更新公司。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    public void update(SysCompanyDTO dto) {
        normalizeDto(dto);
        if (dto.getId() == null) {
            throw new ServiceException("公司ID不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(dto.getId());
        if (company == null) {
            throw new ServiceException("公司不存在");
        }

        String subjectType = validateCompanyType(dto.getTypeCode());
        validateCompanyStatus(dto.getStatus());
        validateCompanyCodeRequired(subjectType, dto.getCompanyCode());
        validateCompanyCodeUnique(dto.getId(), dto.getCompanyCode());
        dto.setSourceType(StrUtil.blankToDefault(company.getSourceType(), SOURCE_TYPE_MANUAL));
        validateSourceType(dto.getSourceType());
        dto.setSalesOrg(validateSalesOrg(dto.getId(), subjectType, dto.getSalesOrg()));

        ResolvedRegion resolvedRegion = resolveRegion(dto);
        boolean shouldResolve = shouldResolveAddress(company, dto);

        BeanUtil.copyProperties(dto, company);
        applyResolvedRegion(company, resolvedRegion);
        company.setFullAddress(buildFullAddress(company));
        if (shouldResolve) {
            applyGeocodeResult(company, resolveGeoLocationSafely(company.getFullAddress()));
        }
        if (company.getStatus() == null) {
            company.setStatus(STATUS_ENABLED);
        }
        sysCompanyMapper.updateById(company);
    }

    /**
     * 删除公司。
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getCompanyId, id);
        if (sysUserCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("该公司下存在用户，不允许删除");
        }
        sysCompanyMapper.deleteById(id);
    }

    /**
     * 创建DefaultAdmin。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @param adminRoleId admin Role ID
     */
    private void createDefaultAdmin(SysCompanyDTO dto, Long companyId, Long adminRoleId) {
        String initPassword = StrUtil.blankToDefault(
                configService.getValueByKey("org.company.adminInitPassword"), DEFAULT_PASSWORD);

        SysUser adminUser = new SysUser();
        adminUser.setUsername(dto.getAdminUsername());
        adminUser.setPassword(BCrypt.hashpw(initPassword, BCrypt.gensalt()));
        adminUser.setRealName(dto.getContactName());
        adminUser.setPhone(dto.getContactPhone());
        adminUser.setStatus(STATUS_ENABLED);
        sysUserMapper.insert(adminUser);

        SysUserCompany userCompany = new SysUserCompany();
        userCompany.setUserId(adminUser.getId());
        userCompany.setCompanyId(companyId);
        userCompany.setIsDefault(1);
        // 公司创建时生成的默认管理员账号即该公司的主账号，后续通知等场景统一按该标记快速定位。
        userCompany.setIsPrimaryAccount(1);
        sysUserCompanyMapper.insert(userCompany);

        if (adminRoleId != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(adminUser.getId());
            userRole.setRoleId(adminRoleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 校验公司类型。
     *
     * @param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
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
     * @param status 业务状态编码，用于状态流转或展示判断。
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
     * @param subjectType subjectType，当前业务处理所需的输入值。
     * @param companyCode 公司业务对象或公司相关值，用于归属、权限或展示。
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
     * @param companyCode 公司业务对象或公司相关值，用于归属、权限或展示。
     */
    private void validateCompanyCodeUnique(Long currentId, String companyCode) {
        if (StrUtil.isBlank(companyCode)) {
            return;
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getCompanyCode, companyCode);
        if (currentId != null) {
            wrapper.ne(SysCompany::getId, currentId);
        }
        if (sysCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("公司编码已存在");
        }
    }

    /**
     * apply创建来源类型。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void applyCreateSourceType(SysCompanyDTO dto) {
        if (StrUtil.isBlank(dto.getSourceType())) {
            dto.setSourceType(SOURCE_TYPE_MANUAL);
        }
    }

    /**
     * 校验来源类型。
     *
     * @param sourceType sourceType，当前业务处理所需的输入值。
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
     * @param subjectType subjectType，当前业务处理所需的输入值。
     * @param salesOrg salesOrg，当前业务处理所需的输入值。
     * @return 业务处理结果
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
        wrapper.eq(SysCompany::getSalesOrg, salesOrg);
        if (currentId != null) {
            wrapper.ne(SysCompany::getId, currentId);
        }
        if (sysCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("销售组织已绑定其他总部公司");
        }
        return salesOrg;
    }

    /**
     * 校验Admin模板。
     *
     * @param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。
     */
    private void validateAdminTemplate(String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> adminTplWrapper = new LambdaQueryWrapper<>();
        adminTplWrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                .eq(SysRoleTemplate::getIsAdmin, 1);
        if (sysRoleTemplateMapper.selectCount(adminTplWrapper) == 0) {
            throw new ServiceException("请先维护该公司类型（" + typeCode + "）的管理员角色模板");
        }
    }

    /**
     * 校验AdminUsername。
     *
     * @param adminUsername 用户业务对象或用户相关值，用于操作人或归属判断。
     */
    private void validateAdminUsername(String adminUsername) {
        if (StrUtil.isBlank(adminUsername)) {
            throw new ServiceException("管理员用户名不能为空");
        }
    }

    /**
     * 校验AdminLogin身份。
     *
     * @param adminUsername 用户业务对象或用户相关值，用于操作人或归属判断。
     * @param contactPhone contactPhone，当前业务处理所需的输入值。
     */
    private void validateAdminLoginIdentity(String adminUsername, String contactPhone) {
        validateAdminUsername(adminUsername);
        userIdentityValidator.validateLoginIdentityUnique(null, adminUsername, contactPhone);
    }

    /**
     * shouldResolveAddress。
     *
     * @param originalCompany 公司业务对象或公司相关值，用于归属、权限或展示。
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
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
                || !StrUtil.equals(normalizeNullableText(originalCompany.getDetailAddress()), dto.getDetailAddress());
    }

    /**
     * 应用Default状态。
     *
     * @param company 公司业务对象或公司相关值，用于归属、权限或展示。
     */
    private void applyDefaultStatus(SysCompany company) {
        if (company.getStatus() == null) {
            company.setStatus(STATUS_ENABLED);
        }
    }

    /**
     * 规范化Dto。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void normalizeDto(SysCompanyDTO dto) {
        dto.setCompanyName(normalizeRequiredText(dto.getCompanyName()));
        dto.setCompanyShortName(normalizeNullableText(dto.getCompanyShortName()));
        dto.setCompanyCode(normalizeNullableText(dto.getCompanyCode()));
        dto.setTypeCode(normalizeRequiredText(dto.getTypeCode()));
        dto.setContactName(normalizeRequiredText(dto.getContactName()));
        dto.setContactPhone(normalizeRequiredText(dto.getContactPhone()));
        dto.setProvinceCode(normalizeRequiredText(dto.getProvinceCode()));
        dto.setProvinceName(normalizeNullableText(dto.getProvinceName()));
        dto.setCityCode(normalizeRequiredText(dto.getCityCode()));
        dto.setCityName(normalizeNullableText(dto.getCityName()));
        dto.setDistrictCode(normalizeRequiredText(dto.getDistrictCode()));
        dto.setDistrictName(normalizeNullableText(dto.getDistrictName()));
        dto.setDetailAddress(normalizeRequiredText(dto.getDetailAddress()));
        dto.setAdminUsername(normalizeNullableText(dto.getAdminUsername()));
        dto.setServicePhone(normalizeNullableText(dto.getServicePhone()));
        dto.setSourceType(normalizeNullableText(dto.getSourceType()));
        dto.setSalesOrg(normalizeNullableText(dto.getSalesOrg()));
        dto.setRemark(normalizeNullableText(dto.getRemark()));
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
     * 规范化NullableText。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String normalizeNullableText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 解析地区。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    private ResolvedRegion resolveRegion(SysCompanyDTO dto) {
        Map<String, SysArea> areaMap = sysAreaService.getByAreaCodes(
                Arrays.asList(dto.getProvinceCode(), dto.getCityCode(), dto.getDistrictCode()));
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
     * @param area area，当前业务处理所需的输入值。
     * @param areaCode 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param expectedLevel expectedLevel，当前业务处理所需的输入值。
     * @param label label，当前业务处理所需的输入值。
     * @return 业务处理结果
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
     * @param company 公司业务对象或公司相关值，用于归属、权限或展示。
     * @param resolvedRegion resolvedRegion，当前业务处理所需的输入值。
     */
    private void applyResolvedRegion(SysCompany company, ResolvedRegion resolvedRegion) {
        company.setProvinceCode(resolvedRegion.getProvince().getAreaCode());
        company.setProvinceName(resolvedRegion.getProvince().getAreaName());
        company.setCityCode(resolvedRegion.getCity().getAreaCode());
        company.setCityName(resolvedRegion.getCity().getAreaName());
        company.setDistrictCode(resolvedRegion.getDistrict().getAreaCode());
        company.setDistrictName(resolvedRegion.getDistrict().getAreaName());
    }

    /**
     * 构建FullAddress。
     *
     * @param company 公司业务对象或公司相关值，用于归属、权限或展示。
     * @return 业务处理结果
     */
    private String buildFullAddress(SysCompany company) {
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, company.getProvinceName());
        if (!shouldSkipCityInFullAddress(company.getProvinceName(), company.getCityName())) {
            appendAddressPart(builder, company.getCityName());
        }
        appendAddressPart(builder, company.getDistrictName());
        appendAddressPart(builder, company.getDetailAddress());
        return builder.toString();
    }

    /**
     * shouldSkipCityInFullAddress。
     *
     * @param provinceName provinceName，当前业务处理所需的输入值。
     * @param cityName cityName，当前业务处理所需的输入值。
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
     * @param builder builder，当前业务处理所需的输入值。
     * @param value value，当前业务处理所需的输入值。
     */
    private void appendAddressPart(StringBuilder builder, String value) {
        String normalized = normalizeNullableText(value);
        if (normalized != null) {
            builder.append(normalized);
        }
    }

    /**
     * applyGeocode结果。
     *
     * @param company 公司业务对象或公司相关值，用于归属、权限或展示。
     * @param geocodeResult 业务编码，用于匹配枚举、配置或外部系统数据。
     */
    private void applyGeocodeResult(SysCompany company, GeocodeResult geocodeResult) {
        company.setGeocodeStatus(geocodeResult.getStatus());
        company.setLongitude(geocodeResult.getLongitude());
        company.setLatitude(geocodeResult.getLatitude());
    }

    /**
     * 解析地理LocationSafely。
     *
     * @param fullAddress fullAddress，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private GeocodeResult resolveGeoLocationSafely(String fullAddress) {
        try {
            ICompanyGeoResolver.GeoLocation geoLocation = companyGeoResolver.resolve(fullAddress);
            return GeocodeResult.success(geoLocation);
        } catch (ServiceException ex) {
            log.warn("Company geocode failed, fullAddress={}, message={}", fullAddress, ex.getMessage());
            return GeocodeResult.failed();
        }
    }

    /**ResolvedRegion 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static final class ResolvedRegion {

        /**
     * 系统Area字段。
     *
     * @param province province，当前业务处理所需的输入值。
     * @param city city，当前业务处理所需的输入值。
     * @param district district，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private final SysArea province;

        /**city 字段，用于当前类内部业务处理。*/
        private final SysArea city;

        /**district 字段，用于当前类内部业务处理。*/
        private final SysArea district;

        /**
     * 构造系统公司实例。
     *
     * @param province province，当前业务处理所需的输入值。
     * @param city city，当前业务处理所需的输入值。
     * @param district district，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private ResolvedRegion(SysArea province, SysArea city, SysArea district) {
            this.province = province;
            this.city = city;
            this.district = district;
        }

        /**
     * 获取Province。
     *
     * @return 业务处理结果
         */
        private SysArea getProvince() {
            return province;
        }

        /**
     * 获取City。
     *
     * @return 业务处理结果
         */
        private SysArea getCity() {
            return city;
        }

        /**
     * 获取District。
     *
     * @return 业务处理结果
         */
        private SysArea getDistrict() {
            return district;
        }
    }

    /**GeocodeResult 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static final class GeocodeResult {

        /**
     * 系统公司状态。
     *
     * @param status 业务状态编码，用于状态流转或展示判断。
     * @param longitude longitude，当前业务处理所需的输入值。
     * @param latitude latitude，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private final String status;

        /**longitude 字段，用于当前类内部业务处理。*/
        private final BigDecimal longitude;

        /**latitude 字段，用于当前类内部业务处理。*/
        private final BigDecimal latitude;

        /**
     * 构造系统公司实例。
     *
     * @param status 业务状态编码，用于状态流转或展示判断。
     * @param longitude longitude，当前业务处理所需的输入值。
     * @param latitude latitude，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private GeocodeResult(String status, BigDecimal longitude, BigDecimal latitude) {
            this.status = status;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        /**
     * success。
     *
     * @param geoLocation geoLocation，当前业务处理所需的输入值。
     * @return 业务处理结果
         */
        private static GeocodeResult success(ICompanyGeoResolver.GeoLocation geoLocation) {
            return new GeocodeResult(GEOCODE_STATUS_SUCCESS, geoLocation.getLongitude(), geoLocation.getLatitude());
        }

        /**
     * failed。
     *
     * @return 业务处理结果
         */
        private static GeocodeResult failed() {
            return new GeocodeResult(GEOCODE_STATUS_FAILED, null, null);
        }

        /**
     * 获取状态。
     *
     * @return 业务处理结果
         */
        private String getStatus() {
            return status;
        }

        /**
     * 获取Longitude。
     *
     * @return 业务处理结果
         */
        private BigDecimal getLongitude() {
            return longitude;
        }

        /**
     * 获取Latitude。
     *
     * @return 业务处理结果
         */
        private BigDecimal getLatitude() {
            return latitude;
        }
    }
}




