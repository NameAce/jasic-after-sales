package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysConfigDTO;
import com.jasic.aftersales.system.domain.query.SysConfigQuery;
import com.jasic.aftersales.system.domain.vo.SysConfigVO;
import com.jasic.aftersales.system.service.ISysConfigService;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 参数设置控制器
 *
 * @author Codex
 * @date 2026/03/19
 */
@Api(tags = "参数设置")
@RestController
@RequestMapping("/system/config")
public class SysConfigController extends BaseController {

    @Resource
    private ISysConfigService configService;

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/list")
    public Result<PageResult<SysConfigVO>> list(SysConfigQuery query) {
        return Result.ok(configService.listPage(query));
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @return ??????
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/{id}")
    public Result<SysConfigVO> getById(@PathVariable Long id) {
        return Result.ok(configService.getById(id));
    }

    /**
     * ??Value By Key?
     *
     * @param configKey ??
     * @return ??????
     */
    @GetMapping("/key/{configKey}")
    public Result<String> getValueByKey(@PathVariable String configKey) {
        return Result.ok(configService.getValueByKey(configKey));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ??????
     */
    @SaCheckPermission("system:config:add")
    @OperLog(title = "参数设置", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysConfigDTO dto) {
        return Result.ok(configService.save(dto));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ??????
     */
    @SaCheckPermission("system:config:update")
    @OperLog(title = "参数设置", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysConfigDTO dto) {
        configService.update(dto);
        return Result.ok();
    }

    /**
     * ?????
     *
     * @param id ??ID
     * @return ??????
     */
    @SaCheckPermission("system:config:remove")
    @OperLog(title = "参数设置", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        configService.remove(id);
        return Result.ok();
    }

    /**
     * ?? refreshCache ?????
     *
     * @return ??????
     */
    @SaCheckPermission("system:config:refresh")
    @OperLog(title = "参数设置", operType = OperTypeEnum.OTHER)
    @DeleteMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.ok();
    }
}
