package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.query.MachineBarcodeQuery;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeVO;
import com.jasic.aftersales.system.service.IMachineBarcodeService;
import com.jasic.aftersales.system.service.ISyncTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import io.swagger.annotations.Api;

/**
 * 条码档案管理控制器
 *
 * @author Codex
 * @date 2026/04/01
 */
@Api(tags = "条码档案管理")
@RestController
@RequestMapping("/system/machine-barcode")
public class MachineBarcodeController extends BaseController {

    @Resource
    private IMachineBarcodeService machineBarcodeService;

    @Resource
    private ISyncTaskService syncTaskService;

    @SaCheckPermission("system:machineBarcode:list")
    @GetMapping("/list")
    public Result<PageResult<MachineBarcodeVO>> list(MachineBarcodeQuery query) {
        return Result.ok(machineBarcodeService.listPage(query));
    }

    @SaCheckPermission("system:machineBarcode:list")
    @GetMapping("/{id}")
    public Result<MachineBarcodeVO> getById(@PathVariable Long id) {
        return Result.ok(machineBarcodeService.getById(id));
    }

    @SaCheckPermission("system:machineBarcode:sync")
    @OperLog(title = "条码档案同步任务", operType = OperTypeEnum.OTHER)
    @PostMapping("/full-sync")
    public Result<Long> fullSync() {
        return Result.ok(syncTaskService.executeDefaultMachineBarcodeTask());
    }
}
