package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SysConfigDTO;
import com.jasic.aftersales.system.domain.query.SysConfigQuery;
import com.jasic.aftersales.system.domain.vo.SysConfigGroupVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 参数设置控制器。
 *
 * <p>该控制器保留旧参数设置页的分页维护接口，同时提供非分页分组查询接口给前端展示全部配置分组。
 * 分组查询不替代旧列表，也不引入完整配置中心模型。</p>
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
     * 分页查询配置列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/list")
    public Result<PageResult<SysConfigVO>> list(SysConfigQuery query) {
        return Result.ok(configService.listPage(query));
    }

    /**
     * 查询配置分组及各分组下全部配置项。
     *
     * @param includeLegacy 是否包含 legacy 历史废弃分组；默认 false，避免新配置页误展示废弃通知配置
     * @return 固定顺序的配置分组列表
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/grouped")
    public Result<List<SysConfigGroupVO>> grouped(@RequestParam(defaultValue = "false") Boolean includeLegacy) {
        return Result.ok(configService.listGroups(includeLegacy));
    }

    /**
     * 根据ID查询配置详情。
     *
     * @return 处理结果
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/{id}")
    public Result<SysConfigVO> getById(@PathVariable Long id) {
        return Result.ok(configService.getById(id));
    }

    /**
     * 获取值ByKey。
     *
     * @param configKey 参数
     * @return 处理结果
     */
    @GetMapping("/key/{configKey}")
    public Result<String> getValueByKey(@PathVariable String configKey) {
        return Result.ok(configService.getValueByKey(configKey));
    }

    /**
     * 新增配置。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @SaCheckPermission("system:config:add")
    @OperLog(title = "参数设置", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SysConfigDTO dto) {
        return Result.ok(configService.save(dto));
    }

    /**
     * 更新配置。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @SaCheckPermission("system:config:update")
    @OperLog(title = "参数设置", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SysConfigDTO dto) {
        // 调用update方法，复用统一能力并保证业务规则一致。
        configService.update(dto);
        return Result.ok();
    }

    /**
     * 删除配置。
     *
     * @return 处理结果
     */
    @SaCheckPermission("system:config:remove")
    @OperLog(title = "参数设置", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        // 调用remove方法，复用统一能力并保证业务规则一致。
        configService.remove(id);
        return Result.ok();
    }

    /**
     * 刷新配置缓存。
     *
     * @return 处理结果
     */
    @SaCheckPermission("system:config:refresh")
    @OperLog(title = "参数设置", operType = OperTypeEnum.OTHER)
    @DeleteMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        // 调用refreshCache方法，复用统一能力并保证业务规则一致。
        configService.refreshCache();
        return Result.ok();
    }
}


