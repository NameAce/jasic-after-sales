package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysMenuCopyDTO;
import com.jasic.aftersales.system.domain.dto.SysMenuDTO;
import com.jasic.aftersales.system.domain.entity.SysMenu;
import com.jasic.aftersales.system.domain.vo.SysMenuVO;
import com.jasic.aftersales.system.service.ISysMenuService;
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
 * 菜单管理控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController {

    @Resource
    private ISysMenuService menuService;

    /**
     * 查询菜单树（按主体类型）
     *
     * @param subjectType 主体类型（PLATFORM/HQ/SERVICE）
     * @return 菜单树
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/tree")
    public Result<List<SysMenuVO>> tree(@RequestParam String subjectType) {
        List<SysMenuVO> tree = menuService.listMenuTreeBySubjectType(subjectType);
        return Result.ok(tree);
    }

    /**
     * 查询菜单列表（平铺，按主体类型）
     *
     * @param subjectType 主体类型
     * @return 菜单列表
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public Result<List<SysMenu>> list(@RequestParam String subjectType) {
        List<SysMenu> list = menuService.listBySubjectType(subjectType);
        return Result.ok(list);
    }

    /**
     * 查询菜单详情
     *
     * @param menuId 菜单ID
     * @return 菜单详情
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/{menuId}")
    public Result<SysMenu> getById(@PathVariable Long menuId) {
        SysMenu menu = menuService.getById(menuId);
        return Result.ok(menu);
    }

    /**
     * 新增菜单
     *
     * @param dto 菜单参数
     * @return 菜单ID
     */
    @SaCheckPermission("system:menu:add")
    @OperLog(title = "菜单管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysMenuDTO dto) {
        Long id = menuService.save(dto);
        return Result.ok(id);
    }

    /**
     * 修改菜单
     *
     * @param dto 菜单参数
     * @return 操作结果
     */
    @SaCheckPermission("system:menu:update")
    @OperLog(title = "菜单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysMenuDTO dto) {
        menuService.update(dto);
        return Result.ok();
    }

    /**
     * 删除菜单
     *
     * @param menuId 菜单ID
     * @return 操作结果
     */
    @SaCheckPermission("system:menu:remove")
    @OperLog(title = "菜单管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{menuId}")
    public Result<Void> remove(@PathVariable Long menuId) {
        menuService.remove(menuId);
        return Result.ok();
    }

    /**
     * 查询公司类型已分配的菜单树（用于角色模板分配菜单时展示可选范围）
     *
     * @param typeCode 公司类型编码
     * @return 菜单树
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/type-code-tree")
    public Result<List<SysMenuVO>> typeCodeTree(@RequestParam String typeCode) {
        List<SysMenuVO> tree = menuService.listMenuTreeByTypeCode(typeCode);
        return Result.ok(tree);
    }

    /**
     * 查询公司类型已分配的菜单ID列表
     *
     * @param typeCode 公司类型编码
     * @return 菜单ID列表
     */
    @SaCheckPermission("system:menu:list")
    @GetMapping("/type-code-menu-ids")
    public Result<List<Long>> typeCodeMenuIds(@RequestParam String typeCode) {
        List<Long> menuIds = menuService.listTypeCodeMenuIds(typeCode);
        return Result.ok(menuIds);
    }

    /**
     * 分配公司类型的菜单上限
     *
     * @param typeCode 公司类型编码
     * @param menuIds  菜单ID列表
     * @return 操作结果
     */
    @SaCheckPermission("system:menu:update")
    @OperLog(title = "公司类型菜单分配", operType = OperTypeEnum.GRANT)
    @PutMapping("/assign-type-code-menus")
    public Result<Void> assignTypeCodeMenus(@RequestParam String typeCode,
                                            @RequestBody List<Long> menuIds) {
        menuService.assignTypeCodeMenus(typeCode, menuIds);
        return Result.ok();
    }

    /**
     * 从源主体拷贝菜单到目标主体
     *
     * @param dto 拷贝参数（源主体、目标主体、可选菜单ID列表）
     * @return 拷贝的菜单数量
     */
    @SaCheckPermission("system:menu:add")
    @OperLog(title = "菜单拷贝", operType = OperTypeEnum.INSERT)
    @PostMapping("/copy")
    public Result<Integer> copyMenus(@Validated @RequestBody SysMenuCopyDTO dto) {
        int count = menuService.copyMenus(dto.getSourceSubjectType(), dto.getTargetSubjectType(), dto.getMenuIds());
        return Result.ok(count);
    }
}
