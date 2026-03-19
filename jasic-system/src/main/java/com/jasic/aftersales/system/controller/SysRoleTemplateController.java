package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysRoleTemplateDTO;
import com.jasic.aftersales.system.domain.vo.SysRoleTemplateVO;
import com.jasic.aftersales.system.service.ISysRoleTemplateService;
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
 * 角色模板管理控制器（平台管理员使用）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/system/role-template")
public class SysRoleTemplateController extends BaseController {

    @Resource
    private ISysRoleTemplateService roleTemplateService;

    /**
     * 查询角色模板列表
     *
     * @param typeCode 公司类型编码
     * @return 模板列表
     */
    @SaCheckPermission("system:roleTemplate:list")
    @GetMapping("/list")
    public Result<List<SysRoleTemplateVO>> list(@RequestParam(value = "typeCode", required = false) String typeCode) {
        List<SysRoleTemplateVO> list = roleTemplateService.listByTypeCode(typeCode);
        return Result.ok(list);
    }

    /**
     * 查询模板详情
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    @SaCheckPermission("system:roleTemplate:list")
    @GetMapping("/{templateId}")
    public Result<SysRoleTemplateVO> getById(@PathVariable Long templateId) {
        SysRoleTemplateVO vo = roleTemplateService.getById(templateId);
        return Result.ok(vo);
    }

    /**
     * 新增角色模板
     *
     * @param dto 模板参数
     * @return 模板ID
     */
    @SaCheckPermission("system:roleTemplate:add")
    @OperLog(title = "角色模板管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysRoleTemplateDTO dto) {
        Long id = roleTemplateService.save(dto);
        return Result.ok(id);
    }

    /**
     * 修改角色模板
     *
     * @param dto 模板参数
     * @return 操作结果
     */
    @SaCheckPermission("system:roleTemplate:update")
    @OperLog(title = "角色模板管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysRoleTemplateDTO dto) {
        roleTemplateService.update(dto);
        return Result.ok();
    }

    /**
     * 删除角色模板
     *
     * @param templateId 模板ID
     * @return 操作结果
     */
    @SaCheckPermission("system:roleTemplate:remove")
    @OperLog(title = "角色模板管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{templateId}")
    public Result<Void> remove(@PathVariable Long templateId) {
        roleTemplateService.remove(templateId);
        return Result.ok();
    }

    /**
     * 同步模板到已有公司
     *
     * @param templateId 模板ID
     * @return 操作结果
     */
    @SaCheckPermission("system:roleTemplate:sync")
    @OperLog(title = "角色模板管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/{templateId}/sync")
    public Result<Void> sync(@PathVariable Long templateId) {
        roleTemplateService.syncToCompanies(templateId);
        return Result.ok();
    }
}
