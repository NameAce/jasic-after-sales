package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysDictDataDTO;
import com.jasic.aftersales.system.domain.query.SysDictDataQuery;
import com.jasic.aftersales.system.domain.vo.SysDictDataVO;
import com.jasic.aftersales.system.service.ISysDictDataService;
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
import java.util.List;

/**
 * 字典数据控制器
 *
 * @author Codex
 * @date 2026/03/19
 */
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController extends BaseController {

    @Resource
    private ISysDictDataService dictDataService;

    @SaCheckPermission("system:dictData:list")
    @GetMapping("/list")
    public Result<PageResult<SysDictDataVO>> list(SysDictDataQuery query) {
        return Result.ok(dictDataService.listPage(query));
    }

    @SaCheckPermission("system:dictData:list")
    @GetMapping("/{id}")
    public Result<SysDictDataVO> getById(@PathVariable Long id) {
        return Result.ok(dictDataService.getById(id));
    }

    @GetMapping("/type/{dictType}")
    public Result<List<SysDictDataVO>> listByType(@PathVariable String dictType) {
        return Result.ok(dictDataService.listByType(dictType));
    }

    @SaCheckPermission("system:dictData:add")
    @OperLog(title = "字典数据管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysDictDataDTO dto) {
        return Result.ok(dictDataService.save(dto));
    }

    @SaCheckPermission("system:dictData:update")
    @OperLog(title = "字典数据管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysDictDataDTO dto) {
        dictDataService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:dictData:remove")
    @OperLog(title = "字典数据管理", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        dictDataService.remove(id);
        return Result.ok();
    }
}
