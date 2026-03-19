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
import com.jasic.aftersales.system.service.ISysUserService;
import com.jasic.aftersales.system.service.SysPermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<SysUserVO> listPage(SysUserQuery query) {
        List<Long> userIds = null;
        if (query.getCompanyId() != null) {
            LambdaQueryWrapper<SysUserCompany> ucWrapper = new LambdaQueryWrapper<>();
            ucWrapper.eq(SysUserCompany::getCompanyId, query.getCompanyId());
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
    public SysUserVO getById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在或已删除");
        }

        SysUserVO vo = convertToVO(user);

        // 查询用户关联公司列表
        LambdaQueryWrapper<SysUserCompany> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(SysUserCompany::getUserId, userId);
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(ucWrapper);
        if (userCompanies != null && !userCompanies.isEmpty()) {
            List<Long> companyIds = userCompanies.stream()
                    .map(SysUserCompany::getCompanyId)
                    .collect(Collectors.toList());
            vo.setCompanies(buildCompanySimpleList(companyIds));
        } else {
            vo.setCompanies(Collections.emptyList());
        }

        // 查询用户角色（当前公司下）
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId != null) {
            LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
            urWrapper.eq(SysUserRole::getUserId, userId);
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(urWrapper);
            if (userRoles != null && !userRoles.isEmpty()) {
                List<Long> roleIds = userRoles.stream()
                        .map(SysUserRole::getRoleId)
                        .collect(Collectors.toList());
                LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
                roleWrapper.in(SysRole::getId, roleIds)
                        .eq(SysRole::getCompanyId, currentCompanyId);
                List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);
                vo.setRoles(roles == null ? Collections.emptyList()
                        : roles.stream().map(this::convertRoleToVO).collect(Collectors.toList()));
            } else {
                vo.setRoles(Collections.emptyList());
            }
        } else {
            vo.setRoles(Collections.emptyList());
        }

        return vo;
    }

    /**
     * 新增用户
     *
     * @param dto 用户参数
     * @return 用户ID
     */
    @Override
    public Long save(SysUserDTO dto) {
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, dto.getUsername());
        if (sysUserMapper.selectCount(usernameWrapper) > 0) {
            throw new ServiceException("用户名已存在");
        }

        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        sysUserMapper.insert(user);

        if (dto.getCompanyIds() != null && !dto.getCompanyIds().isEmpty()) {
            for (int i = 0; i < dto.getCompanyIds().size(); i++) {
                SysUserCompany uc = new SysUserCompany();
                uc.setUserId(user.getId());
                uc.setCompanyId(dto.getCompanyIds().get(i));
                uc.setIsDefault(i == 0 ? 1 : 0);
                sysUserCompanyMapper.insert(uc);
            }
        }

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }

        return user.getId();
    }

    /**
     * 修改用户
     *
     * @param dto 用户参数
     */
    @Override
    public void update(SysUserDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("用户ID不能为空");
        }

        SysUser user = sysUserMapper.selectById(dto.getId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        BeanUtil.copyProperties(dto, user, "password", "id");
        sysUserMapper.updateById(user);

        if (dto.getCompanyIds() != null) {
            LambdaQueryWrapper<SysUserCompany> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(SysUserCompany::getUserId, user.getId());
            sysUserCompanyMapper.delete(delWrapper);
            if (!dto.getCompanyIds().isEmpty()) {
                for (int i = 0; i < dto.getCompanyIds().size(); i++) {
                    SysUserCompany uc = new SysUserCompany();
                    uc.setUserId(user.getId());
                    uc.setCompanyId(dto.getCompanyIds().get(i));
                    uc.setIsDefault(i == 0 ? 1 : 0);
                    sysUserCompanyMapper.insert(uc);
                }
            }
        }

        if (dto.getRoleIds() != null) {
            LambdaQueryWrapper<SysUserRole> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(SysUserRole::getUserId, user.getId());
            sysUserRoleMapper.delete(delWrapper);
            if (!dto.getRoleIds().isEmpty()) {
                for (Long roleId : dto.getRoleIds()) {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(roleId);
                    sysUserRoleMapper.insert(ur);
                }
            }
        }

        sysPermissionService.clearAllPermsCache(user.getId());
        StpUtil.kickout(user.getId());
    }

    /**
     * 删除用户（逻辑删除）
     *
     * @param userId 用户ID
     */
    @Override
    public void remove(Long userId) {
        sysUserMapper.deleteById(userId);

        LambdaQueryWrapper<SysUserCompany> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(SysUserCompany::getUserId, userId);
        sysUserCompanyMapper.delete(ucWrapper);

        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(urWrapper);

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
    public void kickout(Long userId) {
        sysPermissionService.clearAllPermsCache(userId);
        StpUtil.kickout(userId);
    }

    /**
     * 分配用户角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        LambdaQueryWrapper<SysUserRole> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SysUserRole::getUserId, userId);
        sysUserRoleMapper.delete(delWrapper);

        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }

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
     * 根据公司ID列表构建公司简要信息列表
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
