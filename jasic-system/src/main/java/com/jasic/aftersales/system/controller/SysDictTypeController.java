package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysDictTypeDTO;
import com.jasic.aftersales.system.domain.query.SysDictTypeQuery;
import com.jasic.aftersales.system.domain.vo.SysDictTypeVO;
import com.jasic.aftersales.system.service.ISysDictTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 字典类型控制器
 *
 * @author Codex
 * @date 2026/03/19
 */
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController extends BaseController {

    @Resource
    private ISysDictTypeService dictTypeService;

    @SaCheckPermission("system:dictType:list")
    @GetMapping("/list")
    public Result<PageResult<SysDictTypeVO>> list(SysDictTypeQuery query) {
        return Result.ok(dictTypeService.listPage(query));
    }

    @SaCheckPermission("system:dictType:list")
    @GetMapping("/{id}")
    public Result<SysDictTypeVO> getById(@PathVariable Long id) {
        return Result.ok(dictTypeService.getById(id));
    }

    @SaCheckPermission("system:dictType:add")
    @OperLog(title = "字典类型管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysDictTypeDTO dto) {
        return Result.ok(dictTypeService.save(dto));
    }

    @SaCheckPermission("system:dictType:update")
    @OperLog(title = "字典类型管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysDictTypeDTO dto) {
        dictTypeService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:dictType:remove")
    @OperLog(title = "字典类型管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        dictTypeService.remove(id);
        return Result.ok();
    }

    @SaCheckPermission("system:dictType:refresh")
    @OperLog(title = "字典类型管理", operType = OperTypeEnum.OTHER)
    @DeleteMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        dictTypeService.refreshCache();
        return Result.ok();
    }
}
