package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.dto.SysRoleDTO;
import com.jasic.aftersales.system.domain.query.SysRoleQuery;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;

import java.util.List;

/**
 * 角色管理 Service 接口
 *
 * @author Zoro
 * @date 2026/03/18
 */
public interface ISysRoleService {

    /**
     * 分页查询角色列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<SysRoleVO> listPage(SysRoleQuery query);

    /**
     * 查询公司下的角色列表（不分页）
     *
     * @param companyId 公司ID
     * @return 角色列表
     */
    List<SysRoleVO> listByCompanyId(Long companyId);

    /**
     * 查询角色详情（含菜单ID列表）
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    SysRoleVO getById(Long roleId);

    /**
     * 新增角色
     *
     * @param companyId 公司ID
     * @param dto       角色参数
     * @return 角色ID
     */
    Long save(Long companyId, SysRoleDTO dto);

    /**
     * 修改角色（含菜单分配）
     *
     * @param dto 角色参数
     */
    void update(SysRoleDTO dto);

    /**
     * 删除角色（系统角色不可删除）
     *
     * @param roleId 角色ID
     */
    void remove(Long roleId);

    /**
     * 分配角色菜单
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     */
    void assignMenus(Long roleId, List<Long> menuIds);
}
