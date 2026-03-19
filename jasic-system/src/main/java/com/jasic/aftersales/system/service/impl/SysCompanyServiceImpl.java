package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysCompanyDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysRoleTemplate;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.common.enums.CompanyCategoryEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.query.SysCompanyQuery;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRoleTemplateMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.service.ISysCompanyService;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import com.jasic.aftersales.system.service.ISysRoleTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
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
        // 业务分类优先于 typeCode
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
     * @param category 分类编码（HQ/FIRST_LEVEL/SECOND_LEVEL）
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
                    wrapper.eq(SysCompany::getTypeCode, "__none__"); // 无匹配类型时返回空
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
     * 新增公司（自动初始化角色 + 创建默认管理员账号）
     * <p>
     * 流程：
     * 1. 校验公司编码唯一性
     * 2. 校验该公司类型是否存在管理员角色模板
     * 3. 校验管理员用户名、手机号唯一性
     * 4. 插入公司记录
     * 5. 根据模板初始化角色（管理员角色 + 功能角色）
     * 6. 创建默认管理员用户
     * 7. 关联用户-公司
     * 8. 分配管理员角色
     * </p>
     *
     * @param dto 公司参数
     * @return 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SysCompanyDTO dto) {
        // 1. 校验公司编码唯一性
        LambdaQueryWrapper<SysCompany> companyWrapper = new LambdaQueryWrapper<>();
        companyWrapper.eq(SysCompany::getCompanyCode, dto.getCompanyCode());
        if (sysCompanyMapper.selectCount(companyWrapper) > 0) {
            throw new ServiceException("公司编码已存在");
        }

        // 2. 校验该公司类型是否存在管理员角色模板
        LambdaQueryWrapper<SysRoleTemplate> adminTplWrapper = new LambdaQueryWrapper<>();
        adminTplWrapper.eq(SysRoleTemplate::getTypeCode, dto.getTypeCode())
                .eq(SysRoleTemplate::getIsAdmin, 1);
        if (sysRoleTemplateMapper.selectCount(adminTplWrapper) == 0) {
            throw new ServiceException("请先维护该公司类型（" + dto.getTypeCode() + "）的管理员角色模板");
        }

        // 3. 校验管理员用户名唯一性
        if (StrUtil.isBlank(dto.getAdminUsername())) {
            throw new ServiceException("管理员用户名不能为空");
        }
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, dto.getAdminUsername());
        if (sysUserMapper.selectCount(usernameWrapper) > 0) {
            throw new ServiceException("管理员用户名（" + dto.getAdminUsername() + "）已存在，请修改");
        }

        // 4. 校验手机号唯一性
        LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(SysUser::getPhone, dto.getContactPhone());
        if (sysUserMapper.selectCount(phoneWrapper) > 0) {
            throw new ServiceException("联系电话（" + dto.getContactPhone() + "）已被其他用户使用，请确认");
        }

        // 5. 插入公司记录
        SysCompany company = new SysCompany();
        BeanUtil.copyProperties(dto, company);
        if (company.getStatus() == null) {
            company.setStatus(1);
        }
        sysCompanyMapper.insert(company);

        // 6. 根据模板初始化角色，获取管理员角色ID
        Long adminRoleId = roleTemplateService.initCompanyRoles(company.getId(), dto.getTypeCode());

        // 7. 创建默认管理员用户
        SysUser adminUser = new SysUser();
        adminUser.setUsername(dto.getAdminUsername());
        adminUser.setPassword(BCrypt.hashpw(DEFAULT_PASSWORD, BCrypt.gensalt()));
        adminUser.setRealName(dto.getContactName());
        adminUser.setPhone(dto.getContactPhone());
        adminUser.setStatus(1);
        sysUserMapper.insert(adminUser);

        // 8. 关联用户-公司
        SysUserCompany userCompany = new SysUserCompany();
        userCompany.setUserId(adminUser.getId());
        userCompany.setCompanyId(company.getId());
        userCompany.setIsDefault(1);
        sysUserCompanyMapper.insert(userCompany);

        // 9. 分配管理员角色
        if (adminRoleId != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(adminUser.getId());
            userRole.setRoleId(adminRoleId);
            sysUserRoleMapper.insert(userRole);
        }

        return company.getId();
    }

    /**
     * 修改公司
     *
     * @param dto 公司参数
     */
    @Override
    public void update(SysCompanyDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("公司ID不能为空");
        }
        SysCompany company = sysCompanyMapper.selectById(dto.getId());
        if (company == null) {
            throw new ServiceException("公司不存在");
        }
        BeanUtil.copyProperties(dto, company);
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
}
