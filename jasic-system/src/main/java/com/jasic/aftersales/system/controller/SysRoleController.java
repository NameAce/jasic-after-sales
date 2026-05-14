package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysRoleDTO;
import com.jasic.aftersales.system.domain.query.SysRoleQuery;
import com.jasic.aftersales.system.domain.vo.DataScopeOptionVO;
import com.jasic.aftersales.system.domain.vo.SysRoleVO;
import com.jasic.aftersales.system.service.CompanyDataAccessService;
import com.jasic.aftersales.system.service.ISysRoleService;
import com.jasic.aftersales.system.service.SysDataScopeRuleService;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 角色管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Api(tags = "角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController {

    @Resource
    private ISysRoleService roleService;

    @Resource
    private SysDataScopeRuleService dataScopeRuleService;

    @Resource
    private CompanyDataAccessService companyDataAccessService;

    /**
     * 分页查询角色列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询角色列表")
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public Result<PageResult<SysRoleVO>> list(SysRoleQuery query) {
        Long targetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(query.getTargetCompanyId());
        query.setTargetCompanyId(targetCompanyId);
        return Result.ok(companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                targetCompanyId,
                () -> roleService.listPage(query)
        ));
    }

    /**
     * 查询当前公司下的角色列表（不分页，用于下拉选择）
     *
     * @return 角色列表
     */
    @ApiOperation(value = "查询当前公司下的角色列表（不分页，用于下拉选择）")
    @GetMapping("/options")
    public Result<List<SysRoleVO>> options(@RequestParam(required = false) Long targetCompanyId) {
        Long resolvedTargetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(targetCompanyId);
        return Result.ok(companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                resolvedTargetCompanyId,
                () -> roleService.listByCompanyId(resolvedTargetCompanyId)
        ));
    }

    /**
     * 查询当前公司的数据范围选项。
     *
     * @return 数据范围选项
     */
    @ApiOperation(value = "查询当前公司的数据范围选项。")
    @SaCheckPermission("system:role:list")
    @GetMapping("/data-scope-options")
    public Result<List<DataScopeOptionVO>> dataScopeOptions(@RequestParam(required = false) Long targetCompanyId) {
        Long resolvedTargetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(targetCompanyId);
        return Result.ok(companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                resolvedTargetCompanyId,
                () -> dataScopeRuleService.listOptionsByCompanyId(resolvedTargetCompanyId)
        ));
    }

    /**
     * 查询角色详情
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    @ApiOperation(value = "查询角色详情")
    @SaCheckPermission("system:role:list")
    @GetMapping("/{roleId}")
    public Result<SysRoleVO> getById(@PathVariable Long roleId,
                                     @RequestParam(required = false) Long targetCompanyId) {
        Long resolvedTargetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(targetCompanyId);
        return Result.ok(companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                resolvedTargetCompanyId,
                () -> roleService.getById(roleId)
        ));
    }

    /**
     * 新增角色
     *
     * @param dto 角色参数
     * @return 角色ID
     */
    @ApiOperation(value = "新增角色")
    @SaCheckPermission("system:role:add")
    @OperLog(title = "角色管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysRoleDTO dto) {
        Long targetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(dto.getTargetCompanyId());
        dto.setTargetCompanyId(targetCompanyId);
        return Result.ok(companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                targetCompanyId,
                () -> roleService.save(targetCompanyId, dto)
        ));
    }

    /**
     * 修改角色
     *
     * @param dto 角色参数
     * @return 操作结果
     */
    @ApiOperation(value = "修改角色")
    @SaCheckPermission("system:role:update")
    @OperLog(title = "角色管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysRoleDTO dto) {
        Long targetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(dto.getTargetCompanyId());
        dto.setTargetCompanyId(targetCompanyId);
        companyDataAccessService.runWithCurrentCompanyOwnedTarget(targetCompanyId, () -> roleService.update(dto));
        return Result.ok();
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     * @return 操作结果
     */
    @ApiOperation(value = "删除角色")
    @SaCheckPermission("system:role:remove")
    @OperLog(title = "角色管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{roleId}")
    public Result<Void> remove(@PathVariable Long roleId,
                               @RequestParam(required = false) Long targetCompanyId) {
        Long resolvedTargetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(targetCompanyId);
        companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                resolvedTargetCompanyId,
                () -> roleService.remove(roleId)
        );
        return Result.ok();
    }

    /**
     * 分配角色菜单
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     * @return 操作结果
     */
    @ApiOperation(value = "分配角色菜单")
    @SaCheckPermission("system:role:update")
    @OperLog(title = "角色管理", operType = OperTypeEnum.GRANT)
    @PutMapping("/{roleId}/menus")
    public Result<Void> assignMenus(@PathVariable Long roleId,
                                    @RequestParam(required = false) Long targetCompanyId,
                                    @RequestBody List<Long> menuIds) {
        Long resolvedTargetCompanyId = companyDataAccessService.resolveCurrentCompanyOwnedTarget(targetCompanyId);
        companyDataAccessService.runWithCurrentCompanyOwnedTarget(
                resolvedTargetCompanyId,
                () -> roleService.assignMenus(roleId, menuIds)
        );
        return Result.ok();
    }
}
