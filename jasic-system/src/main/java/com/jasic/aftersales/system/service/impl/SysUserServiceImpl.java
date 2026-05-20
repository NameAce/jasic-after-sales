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

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private SysRoleMapper sysRoleMapper;

    /**
     * 系统用户身份校验字段。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private SysUserIdentityValidator userIdentityValidator;

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
            // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
            ucWrapper.eq(SysUserCompany::getCompanyId, query.getTargetCompanyId());
            // 说明：执行该步骤以保证业务流程正确。
            List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucWrapper);
            if (userCompanies == null || userCompanies.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            userIds = userCompanies.stream()
                    .map(SysUserCompany::getUserId)
                    .distinct()
                    // 调用toList方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toList());
        }

        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getUsername())) {
            // 调用getUsername方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (StrUtil.isNotBlank(query.getRealName())) {
            // 调用getRealName方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysUser::getRealName, query.getRealName());
        }
        if (StrUtil.isNotBlank(query.getPhone())) {
            // 调用getPhone方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysUser::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (userIds != null && !userIds.isEmpty()) {
            // 调用in方法，复用统一能力并保证业务规则一致。
            wrapper.in(SysUser::getId, userIds);
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SysUser::getCreateTime);

        // 调用selectPage方法，复用统一能力并保证业务规则一致。
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        List<SysUserVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
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
        // 调用resolveTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        validateUserInCompany(userId, resolvedTargetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在或已删除");
        }

        // 调用convertToVO方法，复用统一能力并保证业务规则一致。
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
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetCompanyId = resolveTargetCompanyId(dto.getTargetCompanyId());
        // 调用normalizeUserDto方法，复用统一能力并保证业务规则一致。
        normalizeUserDto(dto);
        // 说明：执行该步骤以保证业务流程正确。
        userIdentityValidator.validateLoginIdentityUnique(null, dto.getUsername(), dto.getPhone());

        // 调用SysUser方法，复用统一能力并保证业务规则一致。
        SysUser user = new SysUser();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, user);
        // 调用gensalt方法，复用统一能力并保证业务规则一致。
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        // 调用getStatus方法，复用统一能力并保证业务规则一致。
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.insert(user);

        // 调用singletonList方法，复用统一能力并保证业务规则一致。
        saveUserCompanies(user.getId(), Collections.singletonList(targetCompanyId));

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            // 调用getRoleIds方法，复用统一能力并保证业务规则一致。
            validateRoleIdsBelongToCompany(dto.getRoleIds(), targetCompanyId);
            // 调用getRoleIds方法，复用统一能力并保证业务规则一致。
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
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetCompanyId = resolveTargetCompanyId(dto.getTargetCompanyId());
        if (dto.getId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        // 调用normalizeUserDto方法，复用统一能力并保证业务规则一致。
        normalizeUserDto(dto);

        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = sysUserMapper.selectById(dto.getId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 说明：执行该步骤以保证业务流程正确。
        validateUserInCompany(user.getId(), targetCompanyId);
        // 调用getPhone方法，复用统一能力并保证业务规则一致。
        userIdentityValidator.validateLoginIdentityUnique(user.getId(), dto.getUsername(), dto.getPhone());

        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, user, "password", "id");
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            // 调用getRoleIds方法，复用统一能力并保证业务规则一致。
            replaceUserRolesInCompany(user.getId(), targetCompanyId, dto.getRoleIds());
        }

        // 调用getId方法，复用统一能力并保证业务规则一致。
        sysPermissionService.clearAllPermsCache(user.getId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
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
        // 调用resolveTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        validateUserInCompany(userId, resolvedTargetCompanyId);
        // 调用deleteUserRolesInCompany方法，复用统一能力并保证业务规则一致。
        deleteUserRolesInCompany(userId, resolvedTargetCompanyId);
        LambdaQueryWrapper<SysUserCompany> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(SysUserCompany::getUserId, userId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysUserCompany::getCompanyId, resolvedTargetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        sysUserCompanyMapper.delete(ucWrapper);

        LambdaQueryWrapper<SysUserCompany> remainingWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        remainingWrapper.eq(SysUserCompany::getUserId, userId);
        // 说明：执行该步骤以保证业务流程正确。
        if (sysUserCompanyMapper.selectCount(remainingWrapper) == 0) {
            // 调用deleteById方法，复用统一能力并保证业务规则一致。
            sysUserMapper.deleteById(userId);
        }

        // 调用clearAllPermsCache方法，复用统一能力并保证业务规则一致。
        sysPermissionService.clearAllPermsCache(userId);
        // 调用kickout方法，复用统一能力并保证业务规则一致。
        StpUtil.kickout(userId);
    }

    /**
     * 重置密码
     *
     * @param dto 重置密码参数
     */
    @Override
    public void resetPwd(ResetPwdDTO dto) {
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetCompanyId = resolveTargetCompanyId(dto.getTargetCompanyId());
        // 说明：执行该步骤以保证业务流程正确。
        validateUserInCompany(dto.getUserId(), targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        SysUser user = sysUserMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        // 调用gensalt方法，复用统一能力并保证业务规则一致。
        user.setPassword(BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt()));
        // 说明：执行该步骤以保证业务流程正确。
        sysUserMapper.updateById(user);
        // 调用getUserId方法，复用统一能力并保证业务规则一致。
        StpUtil.kickout(dto.getUserId());
    }

    /**
     * 强制下线指定用户
     *
     * @param userId 用户ID
     */
    @Override
    public void kickout(Long userId, Long targetCompanyId) {
        // 调用resolveTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        validateUserInCompany(userId, resolvedTargetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        sysPermissionService.clearAllPermsCache(userId);
        // 调用kickout方法，复用统一能力并保证业务规则一致。
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
        // 调用resolveTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long resolvedTargetCompanyId = resolveTargetCompanyId(targetCompanyId);
        // 说明：执行该步骤以保证业务流程正确。
        validateUserInCompany(userId, resolvedTargetCompanyId);
        // 调用replaceUserRolesInCompany方法，复用统一能力并保证业务规则一致。
        replaceUserRolesInCompany(userId, resolvedTargetCompanyId, roleIds);
        // 说明：执行该步骤以保证业务流程正确。
        sysPermissionService.clearAllPermsCache(userId);
        // 调用kickout方法，复用统一能力并保证业务规则一致。
        StpUtil.kickout(userId);
    }

    /**
     * 用户实体转 VO（基础字段）
     *
     * @param user 用户实体
     * @return 用户 VO
     */
    private SysUserVO convertToVO(SysUser user) {
        // 调用SysUserVO方法，复用统一能力并保证业务规则一致。
        SysUserVO vo = new SysUserVO();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
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
        // 调用SysRoleVO方法，复用统一能力并保证业务规则一致。
        SysRoleVO vo = new SysRoleVO();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(role, vo);
        return vo;
    }

    /**
     * 统一去除输入首尾空白，保证唯一性校验和落库口径一致。
     *
     * @param dto 用户参数
     */
    private void normalizeUserDto(SysUserDTO dto) {
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        dto.setUsername(StrUtil.trim(dto.getUsername()));
        // 调用getRealName方法，复用统一能力并保证业务规则一致。
        dto.setRealName(StrUtil.trim(dto.getRealName()));
        // 调用getPhone方法，复用统一能力并保证业务规则一致。
        dto.setPhone(StrUtil.trim(dto.getPhone()));
        // 调用getEmail方法，复用统一能力并保证业务规则一致。
        dto.setEmail(StrUtil.trim(dto.getEmail()));
        // 调用getRemark方法，复用统一能力并保证业务规则一致。
        dto.setRemark(StrUtil.trim(dto.getRemark()));
    }

    /**
     * 创建用户时强制要求存在当前操作公司，用于初始化用户归属公司。
     *
     * @return 当前公司ID
     */
    private Long requireCurrentCompanyIdForSave() {
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
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
                // 调用toList方法，复用统一能力并保证业务规则一致。
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
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysUserCompany::getCompanyId, companyId);
        // 说明：执行该步骤以保证业务流程正确。
        if (sysUserCompanyMapper.selectCount(wrapper) == 0) {
            throw new ServiceException("无权操作目标公司用户关系");
        }
    }

    /**
     * 分页查询用户RolesIn公司列表。
     *
     * @return 处理结果
     */
    private List<SysRoleVO> listUserRolesInCompany(Long userId, Long companyId) {
        // 调用listRoleIdsByCompanyId方法，复用统一能力并保证业务规则一致。
        List<Long> companyRoleIds = listRoleIdsByCompanyId(companyId);
        if (companyRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(SysUserRole::getRoleId, companyRoleIds);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        // 调用selectBatchIds方法，复用统一能力并保证业务规则一致。
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles == null ? Collections.emptyList()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                : roles.stream().map(this::convertRoleToVO).collect(Collectors.toList());
    }

    /**
     * 替换用户RolesIn公司。
     */
    private void replaceUserRolesInCompany(Long userId, Long companyId, List<Long> roleIds) {
        // 调用deleteUserRolesInCompany方法，复用统一能力并保证业务规则一致。
        deleteUserRolesInCompany(userId, companyId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        // 说明：执行该步骤以保证业务流程正确。
        validateRoleIdsBelongToCompany(roleIds, companyId);
        // 调用insertUserRoles方法，复用统一能力并保证业务规则一致。
        insertUserRoles(userId, roleIds);
    }

    /**
     * 删除用户RolesIn公司。
     */
    private void deleteUserRolesInCompany(Long userId, Long companyId) {
        // 调用listRoleIdsByCompanyId方法，复用统一能力并保证业务规则一致。
        List<Long> companyRoleIds = listRoleIdsByCompanyId(companyId);
        if (companyRoleIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(SysUserRole::getRoleId, companyRoleIds);
        // 说明：执行该步骤以保证业务流程正确。
        sysUserRoleMapper.delete(wrapper);
    }

    /**
     * 校验角色IdsBelongTo公司。
     */
    private void validateRoleIdsBelongToCompany(List<Long> roleIds, Long companyId) {
        // 调用normalizeRoleIds方法，复用统一能力并保证业务规则一致。
        Set<Long> distinctRoleIds = normalizeRoleIds(roleIds);
        if (distinctRoleIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCompanyId, companyId)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(SysRole::getId, distinctRoleIds);
        // 说明：执行该步骤以保证业务流程正确。
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
            // 调用SysUserRole方法，复用统一能力并保证业务规则一致。
            SysUserRole ur = new SysUserRole();
            // 调用setUserId方法，复用统一能力并保证业务规则一致。
            ur.setUserId(userId);
            // 调用setRoleId方法，复用统一能力并保证业务规则一致。
            ur.setRoleId(roleId);
            // 说明：执行该步骤以保证业务流程正确。
            sysUserRoleMapper.insert(ur);
        }
    }

    /**
     * 分页查询角色IdsBy公司ID列表。
     *
     * @return 处理结果
     */
    private List<Long> listRoleIdsByCompanyId(Long companyId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysRole::getCompanyId, companyId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysRole> roles = sysRoleMapper.selectList(wrapper);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream().map(SysRole::getId).collect(Collectors.toList());
    }

    /**
     * 规范化角色Ids。
     *
     * @return 处理结果
     */
    private Set<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 新增用户Companies。
     */
    private void saveUserCompanies(Long userId, List<Long> companyIds) {
        for (int i = 0; i < companyIds.size(); i++) {
            // 调用SysUserCompany方法，复用统一能力并保证业务规则一致。
            SysUserCompany uc = new SysUserCompany();
            // 调用setUserId方法，复用统一能力并保证业务规则一致。
            uc.setUserId(userId);
            // 调用get方法，复用统一能力并保证业务规则一致。
            uc.setCompanyId(companyIds.get(i));
            // 调用setIsDefault方法，复用统一能力并保证业务规则一致。
            uc.setIsDefault(i == 0 ? 1 : 0);
            // 通过用户管理新增的账号统一视为子账号；只有公司创建时自动生成的默认管理员账号才标记为主账号。
            uc.setIsPrimaryAccount(0);
            // 说明：执行该步骤以保证业务流程正确。
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
            // 说明：执行该步骤以保证业务流程正确。
            SysCompany company = sysCompanyMapper.selectById(companyId);
            if (company == null) {
                continue;
            }
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
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }
}




