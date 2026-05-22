package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.ResetPwdDTO;
import com.jasic.aftersales.system.domain.dto.SysUserDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.query.SysUserQuery;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;
import com.jasic.aftersales.system.domain.vo.SysUserVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.service.CompanyDataAccessService;
import com.jasic.aftersales.system.service.ISysUserService;
import com.jasic.aftersales.system.service.SysPermissionService;
import com.jasic.aftersales.system.service.support.SysUserIdentityValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysUserServiceImpl implements ISysUserService {

    /**sysUserMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserMapper sysUserMapper;

    /**sysUserCompanyMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    /**sysUserRoleMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    /**sysCompanyMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysCompanyMapper sysCompanyMapper;

    /**sysPermissionService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysPermissionService sysPermissionService;

    /**sysRoleMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private SysRoleMapper sysRoleMapper;

    /**
     * 系统用户身份校验字段。
     *
     * @param query 查询条件，包含分页、筛选和权限收口所需字段。
     * @return 业务处理结果
     */
    @Resource
    private SysUserIdentityValidator userIdentityValidator;

    /**companyDataAccessService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private CompanyDataAccessService companyDataAccessService;

    /**
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<SysUserVO> listPage(SysUserQuery query) {
        List<Long> userIds = null;
        if (query.getTargetCompanyId() == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        if (query.getTargetCompanyId() != null) {
            LambdaQueryWrapper<SysUserCompany> ucWrapper = new LambdaQueryWrapper<>();
            ucWrapper.eq(SysUserCompany::getCompanyId, query.getTargetCompanyId());
            List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucWrapper);
            if (userCompanies == null || userCompanies.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            userIds = userCompanies.stream()
                    .map(SysUserCompany::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
        }

        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (StrUtil.isNotBlank(query.getRealName())) {
            wrapper.like(SysUser::getRealName, query.getRealName());
        }
        if (StrUtil.isNotBlank(query.getPhone())) {
            wrapper.eq(SysUser::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (userIds != null && !userIds.isEmpty()) {
            wrapper.in(SysUser::getId, userIds);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        List<SysUserVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @Override
    public SysUserVO getById(Long userId, Long targetCompanyId) {
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        validateUserInCompany(userId, resolvedTargetCompanyId);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在或已删除");
        }

        SysUserVO vo = convertToVO(user);

        // 查询用户关联公司列表
        vo.setCompanies(buildCompanySimpleList(Collections.singletonList(resolvedTargetCompanyId)));

        // 查询用户角色（当前公司下）
        vo.setRoles(listUserRolesInCompany(userId, resolvedTargetCompanyId));

        return vo;
    }

    /**
     * 新增用户
     *
     * @param dto 用户参数
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long save(SysUserDTO dto) {
        Long targetCompanyId = resolveTargetCompanyId(dto.getTargetCompanyId());
        normalizeUserDto(dto);
        userIdentityValidator.validateLoginIdentityUnique(null, dto.getUsername(), dto.getPhone());

        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        sysUserMapper.insert(user);

        saveUserCompanies(user.getId(), Collections.singletonList(targetCompanyId));

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            validateRoleIdsBelongToCompany(dto.getRoleIds(), targetCompanyId);
            insertUserRoles(user.getId(), dto.getRoleIds());
        }

        return user.getId();
    }

    /**
     * 修改用户
     *
     * @param dto 用户参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(SysUserDTO dto) {
        Long targetCompanyId = resolveTargetCompanyId(dto.getTargetCompanyId());
        if (dto.getId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        normalizeUserDto(dto);

        SysUser user = sysUserMapper.selectById(dto.getId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        validateUserInCompany(user.getId(), targetCompanyId);
        userIdentityValidator.validateLoginIdentityUnique(user.getId(), dto.getUsername(), dto.getPhone());

        BeanUtil.copyProperties(dto, user, "password", "id");
        sysUserMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            replaceUserRolesInCompany(user.getId(), targetCompanyId, dto.getRoleIds());
        }

        sysPermissionService.clearAllPermsCache(user.getId());
        StpUtil.kickout(user.getId());
    }

    /**
     * 删除用户（逻辑删除）
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void remove(Long userId, Long targetCompanyId) {
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        validateUserInCompany(userId, resolvedTargetCompanyId);
        deleteUserRolesInCompany(userId, resolvedTargetCompanyId);
        LambdaQueryWrapper<SysUserCompany> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(SysUserCompany::getUserId, userId)
                .eq(SysUserCompany::getCompanyId, resolvedTargetCompanyId);
        sysUserCompanyMapper.delete(ucWrapper);

        LambdaQueryWrapper<SysUserCompany> remainingWrapper = new LambdaQueryWrapper<>();
        remainingWrapper.eq(SysUserCompany::getUserId, userId);
        if (sysUserCompanyMapper.selectCount(remainingWrapper) == 0) {
            sysUserMapper.deleteById(userId);
        }

        sysPermissionService.clearAllPermsCache(userId);
        StpUtil.kickout(userId);
    }

    /**
     * 重置密码
     *
     * @param dto 重置密码参数
     */
    @Override
    public void resetPwd(ResetPwdDTO dto) {
        Long targetCompanyId = resolveTargetCompanyId(dto.getTargetCompanyId());
        validateUserInCompany(dto.getUserId(), targetCompanyId);
        SysUser user = sysUserMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        sysUserMapper.updateById(user);
        StpUtil.kickout(dto.getUserId());
    }

    /**
     * 强制下线指定用户
     *
     * @param userId 用户ID
     */
    @Override
    public void kickout(Long userId, Long targetCompanyId) {
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        validateUserInCompany(userId, resolvedTargetCompanyId);
        sysPermissionService.clearAllPermsCache(userId);
        StpUtil.kickout(userId);
    }

    /**
     * 分配用户角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignRoles(Long userId, Long targetCompanyId, List<Long> roleIds) {
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        validateUserInCompany(userId, resolvedTargetCompanyId);
        replaceUserRolesInCompany(userId, resolvedTargetCompanyId, roleIds);
        sysPermissionService.clearAllPermsCache(userId);
        StpUtil.kickout(userId);
    }

    /**
     * 用户实体转 VO（基础字段）
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    private SysUserVO convertToVO(SysUser user) {
        SysUserVO vo = new SysUserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    /**
     * 角色实体转 VO
     *
     * @param role 角色实体
     * @return 角色 VO
     */
    private SysRoleVO convertRoleToVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        BeanUtil.copyProperties(role, vo);
        return vo;
    }

    /**
     * 统一去除输入首尾空白，保证唯一性校验和落库口径一致。
     *
     * @param dto 用户参数
     */
    private void normalizeUserDto(SysUserDTO dto) {
        dto.setUsername(StrUtil.trim(dto.getUsername()));
        dto.setRealName(StrUtil.trim(dto.getRealName()));
        dto.setPhone(StrUtil.trim(dto.getPhone()));
        dto.setEmail(StrUtil.trim(dto.getEmail()));
        dto.setRemark(StrUtil.trim(dto.getRemark()));
    }

    /**
     * 创建用户时强制要求存在当前操作公司，用于初始化用户归属公司。
     *
     * @return 当前公司ID
     */
    private Long requireCurrentCompanyIdForSave() {
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("创建用户时必须存在当前操作公司");
        }
        return currentCompanyId;
    }

    /**
     * 去重并过滤空公司ID，避免重复写入用户公司关系。
     *
     * @param companyIds 原始公司ID列表
     * @return 清洗后的公司ID列表
     */
    private List<Long> sanitizeCompanyIds(List<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyList();
        }
        return companyIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 保存用户与公司的归属关系，默认把首个公司标记为默认公司。
     *
     * @param userId 用户ID
     * @param companyIds 公司ID列表
     */
    private Long resolveTargetCompanyId(Long targetCompanyId) {
        return companyDataAccessService.resolveCurrentCompanyTarget(targetCompanyId);
    }

    /**
     * 校验用户In公司。
     */
    private void validateUserInCompany(Long userId, Long companyId) {
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }
        if (companyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getUserId, userId)
                .eq(SysUserCompany::getCompanyId, companyId);
        if (sysUserCompanyMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("无权操作目标公司用户关系");
        }
    }

    /**
     * 分页查询用户RolesIn公司列表。
     *
     * @return 业务处理结果
     */
    private List<SysRoleVO> listUserRolesInCompany(Long userId, Long companyId) {
        List<Long> companyRoleIds = listRoleIdsByCompanyId(companyId);
        if (companyRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .in(SysUserRole::getRoleId, companyRoleIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles == null ? Collections.emptyList()
                : roles.stream().map(this::convertRoleToVO).collect(Collectors.toList());
    }

    /**
     * 替换用户RolesIn公司。
     */
    private void replaceUserRolesInCompany(Long userId, Long companyId, List<Long> roleIds) {
        deleteUserRolesInCompany(userId, companyId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        validateRoleIdsBelongToCompany(roleIds, companyId);
        insertUserRoles(userId, roleIds);
    }

    /**
     * 删除用户RolesIn公司。
     */
    private void deleteUserRolesInCompany(Long userId, Long companyId) {
        List<Long> companyRoleIds = listRoleIdsByCompanyId(companyId);
        if (companyRoleIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .in(SysUserRole::getRoleId, companyRoleIds);
        sysUserRoleMapper.delete(wrapper);
    }

    /**
     * 校验角色IdsBelongTo公司。
     */
    private void validateRoleIdsBelongToCompany(List<Long> roleIds, Long companyId) {
        Set<Long> distinctRoleIds = normalizeRoleIds(roleIds);
        if (distinctRoleIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCompanyId, companyId)
                .in(SysRole::getId, distinctRoleIds);
        Long count = sysRoleMapper.selectCount(wrapper);
        if (count == null || count.intValue() != distinctRoleIds.size()) {
            throw new ServiceException("存在不属于目标公司的角色");
        }
    }

    /**
     * 新增用户Roles。
     */
    private void insertUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : normalizeRoleIds(roleIds)) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            sysUserRoleMapper.insert(ur);
        }
    }

    /**
     * 分页查询角色IdsBy公司ID列表。
     *
     * @return 业务处理结果
     */
    private List<Long> listRoleIdsByCompanyId(Long companyId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCompanyId, companyId);
        List<SysRole> roles = sysRoleMapper.selectList(wrapper);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream().map(SysRole::getId).collect(Collectors.toList());
    }

    /**
     * 规范化角色Ids。
     *
     * @return 业务处理结果
     */
    private Set<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 新增用户Companies。
     */
    private void saveUserCompanies(Long userId, List<Long> companyIds) {
        for (int i = 0; i < companyIds.size(); i++) {
            SysUserCompany uc = new SysUserCompany();
            uc.setUserId(userId);
            uc.setCompanyId(companyIds.get(i));
            uc.setIsDefault(i == 0 ? 1 : 0);
            // 通过用户管理新增的账号统一视为子账号；只有公司创建时自动生成的默认管理员账号才标记为主账号。
            uc.setIsPrimaryAccount(0);
            sysUserCompanyMapper.insert(uc);
        }
    }

    /**
     * 根据公司ID列表组装用于详情展示的公司简要信息。
     *
     * @param companyIds 公司ID列表
     * @return 公司简要信息列表
     */
    private List<SysCompanySimpleVO> buildCompanySimpleList(List<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysCompanySimpleVO> result = new ArrayList<>();
        for (Long companyId : companyIds) {
            SysCompany company = sysCompanyMapper.selectById(companyId);
            if (company == null) {
                continue;
            }
            SysCompanySimpleVO vo = new SysCompanySimpleVO();
            vo.setId(company.getId());
            vo.setCompanyName(company.getCompanyName());
            vo.setCompanyCode(company.getCompanyCode());
            vo.setTypeCode(company.getTypeCode());
            result.add(vo);
        }
        return result;
    }
}




