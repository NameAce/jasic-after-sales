package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.SysRoleDTO;
import com.jasic.aftersales.system.domain.query.SysRoleQuery;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;
import com.jasic.aftersales.system.service.ISysRoleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 角色管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController {

    @Resource
    private ISysRoleService roleService;

    /**
     * 分页查询角色列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public Result<PageResult<SysRoleVO>> list(SysRoleQuery query) {
        if (query.getCompanyId() == null) {
            query.setCompanyId(SecurityContext.getCurrentCompanyId());
        }
        PageResult<SysRoleVO> page = roleService.listPage(query);
        return Result.ok(page);
    }

    /**
     * 查询当前公司下的角色列表（不分页，用于下拉选择）
     *
     * @return 角色列表
     */
    @GetMapping("/options")
    public Result<List<SysRoleVO>> options() {
        Long companyId = SecurityContext.getCurrentCompanyId();
        List<SysRoleVO> list = roleService.listByCompanyId(companyId);
        return Result.ok(list);
    }

    /**
     * 查询角色详情
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/{roleId}")
    public Result<SysRoleVO> getById(@PathVariable Long roleId) {
        SysRoleVO vo = roleService.getById(roleId);
        return Result.ok(vo);
    }

    /**
     * 新增角色
     *
     * @param dto 角色参数
     * @return 角色ID
     */
    @SaCheckPermission("system:role:add")
    @OperLog(title = "角色管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysRoleDTO dto) {
        Long companyId = SecurityContext.getCurrentCompanyId();
        Long id = roleService.save(companyId, dto);
        return Result.ok(id);
    }

    /**
     * 修改角色
     *
     * @param dto 角色参数
     * @return 操作结果
     */
    @SaCheckPermission("system:role:update")
    @OperLog(title = "角色管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysRoleDTO dto) {
        roleService.update(dto);
        return Result.ok();
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     * @return 操作结果
     */
    @SaCheckPermission("system:role:remove")
    @OperLog(title = "角色管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{roleId}")
    public Result<Void> remove(@PathVariable Long roleId) {
        roleService.remove(roleId);
        return Result.ok();
    }

    /**
     * 分配角色菜单
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    @SaCheckPermission("system:role:update")
    @OperLog(title = "角色管理", operType = OperTypeEnum.GRANT)
    @PutMapping("/{roleId}/menus")
    public Result<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return Result.ok();
    }
}
