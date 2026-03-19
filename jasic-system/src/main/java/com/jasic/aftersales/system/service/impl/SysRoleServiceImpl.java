package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysRoleDTO;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysRoleMenu;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.query.SysRoleQuery;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysRoleMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.service.ISysRoleService;
import com.jasic.aftersales.system.service.SysPermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理 Service 实现类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Service
public class SysRoleServiceImpl implements ISysRoleService {

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysPermissionService sysPermissionService;

    /**
     * 分页查询角色列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<SysRoleVO> listPage(SysRoleQuery query) {
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (query.getCompanyId() != null) {
            wrapper.eq(SysRole::getCompanyId, query.getCompanyId());
        }
        if (StrUtil.isNotBlank(query.getRoleName())) {
            wrapper.like(SysRole::getRoleName, query.getRoleName());
        }
        if (StrUtil.isNotBlank(query.getRoleKey())) {
            wrapper.like(SysRole::getRoleKey, query.getRoleKey());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysRole::getOrderNum);
        Page<SysRole> result = sysRoleMapper.selectPage(page, wrapper);
        List<SysRoleVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询公司下的角色列表（不分页）
     *
     * @param companyId 公司ID
     * @return 角色列表
     */
    @Override
    public List<SysRoleVO> listByCompanyId(Long companyId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCompanyId, companyId)
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getOrderNum);
        List<SysRole> list = sysRoleMapper.selectList(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 查询角色详情（含菜单ID列表）
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    @Override
    public SysRoleVO getById(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        SysRoleVO vo = convertToVO(role);
        LambdaQueryWrapper<SysRoleMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(menuWrapper);
        List<Long> menuIds = roleMenus == null ? Collections.emptyList()
                : roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        vo.setMenuIds(menuIds);
        return vo;
    }

    /**
     * 新增角色
     *
     * @param companyId 公司ID
     * @param dto       角色参数
     * @return 角色ID
     */
    @Override
    public Long save(Long companyId, SysRoleDTO dto) {
        // 校验角色标识唯一性
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCompanyId, companyId)
                .eq(SysRole::getRoleKey, dto.getRoleKey());
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("角色标识已存在");
        }
        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        role.setCompanyId(companyId);
        role.setIsSystem(0);
        role.setStatus(1);
        if (role.getOrderNum() == null) {
            role.setOrderNum(0);
        }
        sysRoleMapper.insert(role);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            batchInsertRoleMenu(role.getId(), dto.getMenuIds());
        }
        return role.getId();
    }

    /**
     * 修改角色（含菜单分配）
     *
     * @param dto 角色参数
     */
    @Override
    public void update(SysRoleDTO dto) {
        if (dto.getId() == null) {
            throw new ServiceException("角色ID不能为空");
        }
        SysRole role = sysRoleMapper.selectById(dto.getId());
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        BeanUtil.copyProperties(dto, role);
        // 保持 companyId、isSystem 不变（DTO 无此字段，copyProperties 不会覆盖）
        sysRoleMapper.updateById(role);
        if (dto.getMenuIds() != null) {
            LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(SysRoleMenu::getRoleId, role.getId());
            sysRoleMenuMapper.delete(delWrapper);
            if (!dto.getMenuIds().isEmpty()) {
                batchInsertRoleMenu(role.getId(), dto.getMenuIds());
            }
        }
        kickAffectedUsers(role.getId());
    }

    /**
     * 删除角色（系统角色不可删除）
     *
     * @param roleId 角色ID
     */
    @Override
    public void remove(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new ServiceException("系统角色不允许删除");
        }
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getRoleId, roleId);
        if (sysUserRoleMapper.selectCount(userRoleWrapper) > 0) {
            throw new ServiceException("角色已分配给用户，请先取消分配");
        }
        LambdaQueryWrapper<SysRoleMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuMapper.delete(menuWrapper);
        sysRoleMapper.deleteById(roleId);
    }

    /**
     * 分配角色菜单
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     */
    @Override
    public void assignMenus(Long roleId, List<Long> menuIds) {
        LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuMapper.delete(delWrapper);
        if (menuIds != null && !menuIds.isEmpty()) {
            batchInsertRoleMenu(roleId, menuIds);
        }
        kickAffectedUsers(roleId);
    }

    /**
     * 实体转 VO（不含 menuIds）
     */
    private SysRoleVO convertToVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        BeanUtil.copyProperties(role, vo);
        return vo;
    }

    /**
     * 批量插入角色-菜单关联
     */
    private void batchInsertRoleMenu(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            sysRoleMenuMapper.insert(rm);
        }
    }

    /**
     * 踢出受影响的用户（角色变更后需重新登录）
     */
    private void kickAffectedUsers(Long roleId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getRoleId, roleId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return;
        }
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
        for (Long userId : userIds) {
            sysPermissionService.clearAllPermsCache(userId);
            StpUtil.kickout(userId);
        }
    }
}
