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
import com.jasic.aftersales.system.service.ISysCompanyService;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.ISysRoleTemplateService;
import com.jasic.aftersales.system.service.support.SysUserIdentityValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 公司管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysCompanyServiceImpl implements ISysCompanyService {

    private static final String DEFAULT_PASSWORD = "Jasic@123";
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

    @Resource
    private ISysConfigService configService;

    @Resource
    private ICompanyGeoResolver companyGeoResolver;

    @Resource
    private SysUserIdentityValidator userIdentityValidator;

    /**
     * 分页查询公司列表
     *
     * @param query 查询参数
     * @return 分页结果
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
     * 按业务分类过滤公司
     *
     * @param wrapper 查询条件
     * @param category 分类编码
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
     * 根据ID查询公司
     *
     * @param id 主键ID
     * @return 公司实体
     */
    @Override
    public SysCompany getById(Long id) {
        return sysCompanyMapper.selectById(id);
    }

    /**
     * 新增公司
     *
     * @param dto 公司参数
     * @return 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SysCompanyDTO dto) {
        normalizeDto(dto);
        validateCompanyType(dto.getTypeCode());
        validateCompanyStatus(dto.getStatus());
        validateCompanyCodeUnique(null, dto.getCompanyCode());
        validateAdminTemplate(dto.getTypeCode());
        validateAdminLoginIdentity(dto.getAdminUsername(), dto.getContactPhone());

        ICompanyGeoResolver.GeoLocation geoLocation = companyGeoResolver.resolve(dto.getAddress());

        SysCompany company = new SysCompany();
        BeanUtil.copyProperties(dto, company);
        applyDefaultStatus(company);
        company.setLongitude(geoLocation.getLongitude());
        company.setLatitude(geoLocation.getLatitude());
        sysCompanyMapper.insert(company);

        Long adminRoleId = roleTemplateService.initCompanyRoles(company.getId(), dto.getTypeCode());
        createDefaultAdmin(dto, company.getId(), adminRoleId);
        return company.getId();
    }

    /**
     * 修改公司
     *
     * @param dto 公司参数
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

        validateCompanyType(dto.getTypeCode());
        validateCompanyStatus(dto.getStatus());
        validateCompanyCodeUnique(dto.getId(), dto.getCompanyCode());

        BigDecimal originalLongitude = company.getLongitude();
        BigDecimal originalLatitude = company.getLatitude();
        String originalAddress = normalizeNullableText(company.getAddress());

        BeanUtil.copyProperties(dto, company);
        if (shouldResolveAddress(originalAddress, dto.getAddress(), originalLongitude, originalLatitude)) {
            ICompanyGeoResolver.GeoLocation geoLocation = companyGeoResolver.resolve(dto.getAddress());
            company.setLongitude(geoLocation.getLongitude());
            company.setLatitude(geoLocation.getLatitude());
        } else {
            company.setLongitude(originalLongitude);
            company.setLatitude(originalLatitude);
        }
        if (company.getStatus() == null) {
            company.setStatus(STATUS_ENABLED);
        }
        sysCompanyMapper.updateById(company);
    }

    /**
     * 删除公司
     *
     * @param id 主键ID
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
        sysUserCompanyMapper.insert(userCompany);

        if (adminRoleId != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(adminUser.getId());
            userRole.setRoleId(adminRoleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    private void validateCompanyType(String typeCode) {
        boolean matched = companyTypeService.listAll().stream()
                .anyMatch(item -> StrUtil.equals(item.getTypeCode(), typeCode));
        if (!matched) {
            throw new ServiceException("公司类型不存在");
        }
    }

    private void validateCompanyStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (!Objects.equals(STATUS_ENABLED, status) && !Objects.equals(STATUS_DISABLED, status)) {
            throw new ServiceException("公司状态不合法");
        }
    }

    private void validateCompanyCodeUnique(Long currentId, String companyCode) {
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompany::getCompanyCode, companyCode);
        if (currentId != null) {
            wrapper.ne(SysCompany::getId, currentId);
        }
        if (sysCompanyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("公司编码已存在");
        }
    }

    private void validateAdminTemplate(String typeCode) {
        LambdaQueryWrapper<SysRoleTemplate> adminTplWrapper = new LambdaQueryWrapper<>();
        adminTplWrapper.eq(SysRoleTemplate::getTypeCode, typeCode)
                .eq(SysRoleTemplate::getIsAdmin, 1);
        if (sysRoleTemplateMapper.selectCount(adminTplWrapper) == 0) {
            throw new ServiceException("请先维护该公司类型（" + typeCode + "）的管理员角色模板");
        }
    }

    private void validateAdminUsername(String adminUsername) {
        if (StrUtil.isBlank(adminUsername)) {
            throw new ServiceException("管理员用户名不能为空");
        }
    }

    private void validateAdminLoginIdentity(String adminUsername, String contactPhone) {
        validateAdminUsername(adminUsername);
        userIdentityValidator.validateLoginIdentityUnique(null, adminUsername, contactPhone);
    }

    private boolean shouldResolveAddress(String originalAddress, String targetAddress,
                                         BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return true;
        }
        return !StrUtil.equals(originalAddress, targetAddress);
    }

    private void applyDefaultStatus(SysCompany company) {
        if (company.getStatus() == null) {
            company.setStatus(STATUS_ENABLED);
        }
    }

    private void normalizeDto(SysCompanyDTO dto) {
        dto.setCompanyName(normalizeRequiredText(dto.getCompanyName()));
        dto.setCompanyCode(normalizeRequiredText(dto.getCompanyCode()));
        dto.setTypeCode(normalizeRequiredText(dto.getTypeCode()));
        dto.setContactName(normalizeRequiredText(dto.getContactName()));
        dto.setContactPhone(normalizeRequiredText(dto.getContactPhone()));
        dto.setAddress(normalizeRequiredText(dto.getAddress()));
        dto.setAdminUsername(normalizeNullableText(dto.getAdminUsername()));
        dto.setRemark(normalizeNullableText(dto.getRemark()));
    }

    private String normalizeRequiredText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    private String normalizeNullableText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }
}
