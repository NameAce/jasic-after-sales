package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeDTO;
import com.jasic.aftersales.system.domain.dto.MachineBarcodeImportItemDTO;
import com.jasic.aftersales.system.domain.query.MachineBarcodeQuery;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeSyncResultVO;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.service.IMachineBarcodeService;
import com.jasic.aftersales.system.service.IMachineBarcodeSyncService;
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
import javax.validation.Valid;
import java.util.List;

/**
 * 条码档案管理控制器
 *
 * @author Codex
 * @date 2026/04/01
 */
@RestController
@RequestMapping("/system/machine-barcode")
public class MachineBarcodeController extends BaseController {

    @Resource
    private IMachineBarcodeService machineBarcodeService;

    @Resource
    private IMachineBarcodeSyncService machineBarcodeSyncService;

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

    @SaCheckPermission("system:machineBarcode:list")
    @GetMapping("/hq-company-options")
    public Result<List<SysCompanySimpleVO>> listHqCompanyOptions() {
        return Result.ok(machineBarcodeService.listHqCompanyOptions());
    }

    @SaCheckPermission("system:machineBarcode:add")
    @OperLog(title = "条码档案", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody MachineBarcodeDTO dto) {
        return Result.ok(machineBarcodeService.save(dto));
    }

    @SaCheckPermission("system:machineBarcode:update")
    @OperLog(title = "条码档案", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody MachineBarcodeDTO dto) {
        machineBarcodeService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:machineBarcode:remove")
    @OperLog(title = "条码档案", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        machineBarcodeService.remove(id);
        return Result.ok();
    }

    @SaCheckPermission("system:machineBarcode:import")
    @OperLog(title = "条码档案导入", operType = OperTypeEnum.INSERT)
    @PostMapping("/import")
    public Result<Integer> importItems(@RequestBody List<@Valid MachineBarcodeImportItemDTO> items) {
        return Result.ok(machineBarcodeService.importItems(items));
    }

    @SaCheckPermission("system:machineBarcode:import")
    @OperLog(title = "条码档案全量同步", operType = OperTypeEnum.OTHER)
    @PostMapping("/full-sync")
    public Result<MachineBarcodeSyncResultVO> fullSync() {
        return Result.ok(machineBarcodeSyncService.fullSyncFromCrm());
    }
}
