package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.FaultRepairConfigDTO;
import com.jasic.aftersales.system.domain.query.FaultRepairConfigQuery;
import com.jasic.aftersales.system.domain.vo.FaultRepairConfigVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import org.springframework.validation.annotation.Validated;
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
 * 故障与维修配置控制器
 *
 * @author Codex
 * @date 2026/04/01
 */
@Api(tags = "故障与维修配置")
@RestController
@RequestMapping("/system/fault-repair-config")
public class FaultRepairConfigController extends BaseController {

    @Resource
    private IFaultRepairConfigService faultRepairConfigService;

    /**
     * 分页查询故障维修配置列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @SaCheckPermission("system:faultRepairConfig:list")
    @GetMapping("/list")
    public Result<PageResult<FaultRepairConfigVO>> list(FaultRepairConfigQuery query) {
        return Result.ok(faultRepairConfigService.listPage(query));
    }

    /**
     * 根据ID查询故障维修配置详情。
     *
     * @return 处理结果
     */
    @SaCheckPermission("system:faultRepairConfig:list")
    @GetMapping("/{id}")
    public Result<FaultRepairConfigVO> getById(@PathVariable Long id,
                                               @RequestParam(required = false) Long ownerHqId) {
        return Result.ok(faultRepairConfigService.getById(id, ownerHqId));
    }

    /**
     * 分页查询公司Options列表。
     *
     * @return 处理结果
     */
    @SaCheckPermission("system:faultRepairConfig:list")
    @GetMapping("/company-options")
    public Result<List<SysCompanySimpleVO>> listCompanyOptions() {
        return Result.ok(faultRepairConfigService.listCompanyOptions());
    }

    /**
     * 新增故障维修配置。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @SaCheckPermission("system:faultRepairConfig:add")
    @OperLog(title = "故障与维修配置", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody FaultRepairConfigDTO dto) {
        return Result.ok(faultRepairConfigService.save(dto));
    }

    /**
     * 更新故障维修配置。
     *
     * @param dto 参数
     * @return 处理结果
     */
    @SaCheckPermission("system:faultRepairConfig:update")
    @OperLog(title = "故障与维修配置", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody FaultRepairConfigDTO dto) {
        // 调用update方法，复用统一能力并保证业务规则一致。
        faultRepairConfigService.update(dto);
        return Result.ok();
    }
}


