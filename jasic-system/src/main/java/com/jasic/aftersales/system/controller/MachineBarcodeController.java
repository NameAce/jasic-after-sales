package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.query.MachineBarcodeQuery;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.service.IMachineBarcodeService;
import com.jasic.aftersales.system.service.ISyncTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import io.swagger.annotations.Api;

/**
 * 条码档案管理控制器
 *
 * @author Zoro
 * @date 2026/04/01
 */
@Api(tags = "条码档案管理")
@RestController
@RequestMapping("/system/machine-barcode")
public class MachineBarcodeController extends BaseController {

    /**machineBarcodeService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private IMachineBarcodeService machineBarcodeService;

    /**syncTaskService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private ISyncTaskService syncTaskService;

    /**
     * 分页查询条码档案列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @SaCheckPermission("system:machineBarcode:list")
    @GetMapping("/list")
    public Result<PageResult<MachineBarcodeVO>> list(MachineBarcodeQuery query) {
        return Result.ok(machineBarcodeService.listPage(query));
    }

    /**
     * 按主键查询条码档案详情。
     *
     * @param id 条码档案ID
     * @param ownerHqId 总部公司ID
     * @param targetCompanyId 目标公司ID
     * @return 条码档案详情
     */
    @SaCheckPermission("system:machineBarcode:list")
    @GetMapping("/{id}")
    public Result<MachineBarcodeVO> getById(@PathVariable Long id,
                                            @RequestParam(required = false) Long ownerHqId,
                                            @RequestParam(required = false) Long targetCompanyId) {
        return Result.ok(machineBarcodeService.getById(id, ownerHqId, targetCompanyId));
    }

    /**
     * 查询可选总部公司列表。
     *
     * @return 总部公司选项
     */
    @SaCheckPermission("system:machineBarcode:list")
    @GetMapping("/hq-options")
    public Result<List<SysCompanySimpleVO>> listHqOptions() {
        return Result.ok(machineBarcodeService.listHqCompanyOptions());
    }

    /**
     * 触发条码档案全量同步任务。
     *
     * @return 同步任务ID
     */
    @SaCheckPermission("system:machineBarcode:sync")
    @OperLog(title = "条码档案同步任务", operType = OperTypeEnum.OTHER)
    @PostMapping("/full-sync")
    public Result<Long> fullSync() {
        return Result.ok(syncTaskService.executeDefaultMachineBarcodeTask());
    }
}
