package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.RoleConstants;
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
import com.jasic.aftersales.system.service.SysDataScopeRuleService;
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

    /**
     * 系统权限服务服务依赖。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Resource
    private SysPermissionService sysPermissionService;

    @Resource
    private SysDataScopeRuleService dataScopeRuleService;

    /**
     * 分页查询角色列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<SysRoleVO> listPage(SysRoleQuery query) {
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (query.getTargetCompanyId() != null) {
            // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysRole::getCompanyId, query.getTargetCompanyId());
        }
        if (StrUtil.isNotBlank(query.getRoleName())) {
            // 调用getRoleName方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysRole::getRoleName, query.getRoleName());
        }
        if (StrUtil.isNotBlank(query.getRoleKey())) {
            // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
            wrapper.like(SysRole::getRoleKey, query.getRoleKey());
        }
        if (query.getStatus() != null) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysRole::getStatus, query.getStatus());
        }
        // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByAsc(SysRole::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
        Page<SysRole> result = sysRoleMapper.selectPage(page, wrapper);
        List<SysRoleVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                // 调用toList方法，复用统一能力并保证业务规则一致。
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
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysRole::getOrderNum);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        // 调用convertToVO方法，复用统一能力并保证业务规则一致。
        SysRoleVO vo = convertToVO(role);
        LambdaQueryWrapper<SysRoleMenu> menuWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        menuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        // 调用selectList方法，复用统一能力并保证业务规则一致。
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(menuWrapper);
        List<Long> menuIds = roleMenus == null ? Collections.emptyList()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                : roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        // 调用setMenuIds方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        dataScopeRuleService.validateByCompanyId(companyId, dto.getDataScope());
        // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
        validateCustomRoleKey(dto.getRoleKey());
        // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
        validateRoleKeyUnique(companyId, dto.getRoleKey(), null);
        // 调用SysRole方法，复用统一能力并保证业务规则一致。
        SysRole role = new SysRole();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, role);
        // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
        role.setCompanyId(companyId);
        // 调用setIsSystem方法，复用统一能力并保证业务规则一致。
        role.setIsSystem(0);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        role.setStatus(1);
        if (role.getOrderNum() == null) {
            // 调用setOrderNum方法，复用统一能力并保证业务规则一致。
            role.setOrderNum(0);
        }
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleMapper.insert(role);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            // 调用getMenuIds方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        SysRole role = sysRoleMapper.selectById(dto.getId());
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        // 说明：执行该步骤以保证业务流程正确。
        dataScopeRuleService.validateByCompanyId(role.getCompanyId(), dto.getDataScope());
        // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
        validateRoleKeyEditable(role, dto.getRoleKey());
        // 调用getRoleKey方法，复用统一能力并保证业务规则一致。
        validateCustomRoleKey(role.getIsSystem(), dto.getRoleKey());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        validateRoleKeyUnique(role.getCompanyId(), dto.getRoleKey(), role.getId());
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(dto, role);
        // 保持 companyId、isSystem 不变（DTO 无此字段，copyProperties 不会覆盖）
        sysRoleMapper.updateById(role);
        if (dto.getMenuIds() != null) {
            LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            delWrapper.eq(SysRoleMenu::getRoleId, role.getId());
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleMenuMapper.delete(delWrapper);
            if (!dto.getMenuIds().isEmpty()) {
                // 调用getMenuIds方法，复用统一能力并保证业务规则一致。
                batchInsertRoleMenu(role.getId(), dto.getMenuIds());
            }
        }
        // 调用getId方法，复用统一能力并保证业务规则一致。
        kickAffectedUsers(role.getId());
    }

    /**
     * 删除角色（系统角色不可删除）
     *
     * @param roleId 角色ID
     */
    @Override
    public void remove(Long roleId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new ServiceException("系统角色不允许删除");
        }
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        userRoleWrapper.eq(SysUserRole::getRoleId, roleId);
        if (sysUserRoleMapper.selectCount(userRoleWrapper) > 0) {
            throw new ServiceException("角色已分配给用户，请先取消分配");
        }
        LambdaQueryWrapper<SysRoleMenu> menuWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        menuWrapper.eq(SysRoleMenu::getRoleId, roleId);
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleMenuMapper.delete(menuWrapper);
        // 调用deleteById方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        delWrapper.eq(SysRoleMenu::getRoleId, roleId);
        // 说明：执行该步骤以保证业务流程正确。
        sysRoleMenuMapper.delete(delWrapper);
        if (menuIds != null && !menuIds.isEmpty()) {
            // 调用batchInsertRoleMenu方法，复用统一能力并保证业务规则一致。
            batchInsertRoleMenu(roleId, menuIds);
        }
        // 调用kickAffectedUsers方法，复用统一能力并保证业务规则一致。
        kickAffectedUsers(roleId);
    }

    /**
     * 实体转VO（不含menuIds）
     */
    private SysRoleVO convertToVO(SysRole role) {
        // 调用SysRoleVO方法，复用统一能力并保证业务规则一致。
        SysRoleVO vo = new SysRoleVO();
        // 调用copyProperties方法，复用统一能力并保证业务规则一致。
        BeanUtil.copyProperties(role, vo);
        return vo;
    }

    /**
     * 批量插入角色-菜单关联
     */
    private void batchInsertRoleMenu(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            // 调用SysRoleMenu方法，复用统一能力并保证业务规则一致。
            SysRoleMenu rm = new SysRoleMenu();
            // 调用setRoleId方法，复用统一能力并保证业务规则一致。
            rm.setRoleId(roleId);
            // 调用setMenuId方法，复用统一能力并保证业务规则一致。
            rm.setMenuId(menuId);
            // 说明：执行该步骤以保证业务流程正确。
            sysRoleMenuMapper.insert(rm);
        }
    }

    /**
     * 校验系统角色标识不可修改。
     *
     * @param role    角色实体
     * @param roleKey 角色标识
     */
    private void validateRoleKeyEditable(SysRole role, String roleKey) {
        if (role.getIsSystem() != null && role.getIsSystem() == 1
                && StrUtil.isNotBlank(roleKey) && !roleKey.equals(role.getRoleKey())) {
            throw new ServiceException("系统角色标识不允许修改");
        }
    }

    /**
     * 校验自定义角色不允许占用系统保留标识。
     *
     * @param roleKey 角色标识
     */
    private void validateCustomRoleKey(String roleKey) {
        // 调用validateCustomRoleKey方法，复用统一能力并保证业务规则一致。
        validateCustomRoleKey(0, roleKey);
    }

    /**
     * 校验自定义角色不允许占用系统保留标识。
     *
     * @param isSystem 是否系统角色
     * @param roleKey  角色标识
     */
    private void validateCustomRoleKey(Integer isSystem, String roleKey) {
        if (isSystem != null && isSystem == 1) {
            return;
        }
        if (RoleConstants.RESERVED_ROLE_KEYS.contains(roleKey)) {
            throw new ServiceException("该角色标识为系统保留值，请更换后重试");
        }
    }

    /**
     * 校验角色标识在公司内唯一。
     *
     * @param companyId     公司ID
     * @param roleKey       角色标识
     * @param excludeRoleId 排除的角色ID
     */
    private void validateRoleKeyUnique(Long companyId, String roleKey, Long excludeRoleId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getCompanyId, companyId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysRole::getRoleKey, roleKey);
        if (excludeRoleId != null) {
            // 调用ne方法，复用统一能力并保证业务规则一致。
            wrapper.ne(SysRole::getId, excludeRoleId);
        }
        // 说明：执行该步骤以保证业务流程正确。
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("角色标识已存在");
        }
    }

    /**
     * 踢出受影响的用户（角色变更后需重新登录）
     *
     * @param roleId 角色ID
     */
    private void kickAffectedUsers(Long roleId) {
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysUserRole::getRoleId, roleId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return;
        }
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        for (Long userId : userIds) {
            // 说明：执行该步骤以保证业务流程正确。
            sysPermissionService.clearAllPermsCache(userId);
            // 调用kickout方法，复用统一能力并保证业务规则一致。
            StpUtil.kickout(userId);
        }
    }
}


