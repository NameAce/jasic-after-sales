package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderProxyCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderSendExpressDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderUpdateProductModelDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderUpstreamCreateDTO;
import com.jasic.aftersales.system.domain.query.WorkOrderHqSiteOrderQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderHqSiteSummaryQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderCreateBarcodeInfoVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderHqSiteSummaryVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderUserOptionVO;
import com.jasic.aftersales.system.service.IWorkOrderService;
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
 * 工单查询控制器
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Api(tags = "工单查询")
@RestController
@RequestMapping("/system/work-order")
public class WorkOrderController extends BaseController {

    /**workOrderService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private IWorkOrderService workOrderService;

    /**
     * 分页查询工单列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询工单列表")
    @SaCheckPermission("workorder:list")
    @GetMapping("/list")
    public Result<PageResult<WorkOrderListVO>> list(WorkOrderQuery query) {
        return Result.ok(workOrderService.listPage(query));
    }

    /**
     * 按状态统计工单数量
     *
     * @param query 查询参数
     * @return 状态统计结果
     */
    @ApiOperation(value = "按状态统计工单数量")
    @SaCheckPermission("workorder:list")
    @GetMapping("/status-count")
    public Result<List<WorkOrderStatusCountVO>> statusCount(WorkOrderQuery query) {
        return Result.ok(workOrderService.countByStatus(query));
    }

    /**
     * 查询总部网点工单汇总。
     *
     * @param query 查询参数
     * @return 网点汇总
     */
    @ApiOperation(value = "查询总部网点工单汇总")
    @SaCheckPermission("workorder:list")
    @GetMapping("/hq-site-summary")
    public Result<List<WorkOrderHqSiteSummaryVO>> hqSiteSummary(WorkOrderHqSiteSummaryQuery query) {
        return Result.ok(workOrderService.listHqSiteSummary(query));
    }

    /**
     * 查询总部网点工单只读列表。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "查询总部网点工单只读列表")
    @SaCheckPermission("workorder:list")
    @GetMapping("/hq-site-orders")
    public Result<PageResult<WorkOrderListVO>> hqSiteOrders(WorkOrderHqSiteOrderQuery query) {
        return Result.ok(workOrderService.listHqSiteOrders(query));
    }

    /**
     * 查询工单详情
     *
     * @param workOrderId 工单ID
     * @return 工单详情
     */
    @ApiOperation(value = "查询工单详情")
    @SaCheckPermission("workorder:list")
    @GetMapping("/{workOrderId}")
    public Result<WorkOrderDetailVO> getById(@PathVariable Long workOrderId) {
        return Result.ok(workOrderService.getById(workOrderId));
    }

    /**
     * 查询建单可选归属总部
     *
     * @return 公司选项
     */
    @ApiOperation(value = "查询建单可选归属总部")
    @SaCheckPermission("workorder:add")
    @GetMapping("/create-hq-options")
    public Result<List<SysCompanySimpleVO>> listCreateHqOptions() {
        return Result.ok(workOrderService.listCreateHqOptions());
    }

    /**
     * 查询可派单人员
     *
     * @param workOrderId 工单ID
     * @return 人员选项
     */
    @ApiOperation(value = "查询可派单人员")
    @SaCheckPermission("workorder:assign")
    @GetMapping("/{workOrderId}/assign-user-options")
    public Result<List<WorkOrderUserOptionVO>> listAssignUserOptions(@PathVariable Long workOrderId) {
        return Result.ok(workOrderService.listAssignUserOptions(workOrderId));
    }

    /**
     * 查询可转单目标
     *
     * @param workOrderId 工单ID
     * @return 公司选项
     */
    @ApiOperation(value = "查询可转单目标")
    @SaCheckPermission("workorder:transfer")
    @GetMapping("/{workOrderId}/transfer-target-options")
    public Result<List<SysCompanySimpleVO>> listTransferTargetOptions(@PathVariable Long workOrderId) {
        return Result.ok(workOrderService.listTransferTargetOptions(workOrderId));
    }

    /**
     * 查询维修/复检登记可选故障与维修说明。
     *
     * 这里不在接口层硬编码单一 repair 权限，避免复检动作复用同一配置接口时，
     * 被提前拦在接口层；实例级权限仍由 service 内的 repair/review 判断兜底。
     *
     * @param workOrderId 工单ID
     * @return 故障与维修说明选项
     */
    @ApiOperation(value = "查询维修/复检登记可选故障与维修说明")
    @GetMapping("/{workOrderId}/repair-fault-options")
    public Result<List<WorkOrderRepairFaultOptionVO>> listRepairFaultOptions(@PathVariable Long workOrderId) {
        return Result.ok(workOrderService.listRepairFaultOptions(workOrderId));
    }

    /**
     * 查询维修/复检前可补录的机器型号选项。
     *
     * 这里同样不在接口层硬编码 repair/review 某一个权限点，
     * 统一由 service 按工单实例权限判断当前人是否允许补录。
     *
     * @param workOrderId 工单ID
     * @param keyword 机型关键字
     * @return 机器型号选项
     */
    @ApiOperation(value = "查询维修/复检前可补录的机器型号选项")
    @GetMapping("/{workOrderId}/repair-product-model-options")
    public Result<List<String>> listRepairProductModelOptions(@PathVariable Long workOrderId,
                                                              @RequestParam(required = false) String keyword) {
        return Result.ok(workOrderService.listRepairProductModelOptions(workOrderId, keyword));
    }

    /**
     * 查询代客户填写条码信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    @ApiOperation(value = "查询代客户填写条码信息")
    @SaCheckPermission("workorder:add")
    @GetMapping("/create/proxy/barcode-info")
    public Result<WorkOrderCreateBarcodeInfoVO> getProxyCreateBarcodeInfo(@RequestParam(required = false) String barcode) {
        return Result.ok(workOrderService.getProxyCreateBarcodeInfo(barcode));
    }

    /**
     * 查询二级报修一级条码信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    @ApiOperation(value = "查询二级报修一级条码信息")
    @SaCheckPermission("workorder:add")
    @GetMapping("/create/upstream-first/barcode-info")
    public Result<WorkOrderCreateBarcodeInfoVO> getUpstreamFirstCreateBarcodeInfo(@RequestParam(required = false) String barcode) {
        return Result.ok(workOrderService.getUpstreamFirstCreateBarcodeInfo(barcode));
    }

    /**
     * 查询二级无码报修一级可选目标公司
     *
     * @return 公司选项
     */
    @ApiOperation(value = "查询二级无码报修一级可选目标公司")
    @SaCheckPermission("workorder:add")
    @GetMapping("/create/upstream-first/target-options")
    public Result<List<SysCompanySimpleVO>> listUpstreamFirstCreateTargetOptions() {
        return Result.ok(workOrderService.listUpstreamFirstCreateTargetOptions());
    }

    /**
     * 查询一级报修佳士条码信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    @ApiOperation(value = "查询一级报修佳士条码信息")
    @SaCheckPermission("workorder:add")
    @GetMapping("/create/upstream-hq/barcode-info")
    public Result<WorkOrderCreateBarcodeInfoVO> getUpstreamHqCreateBarcodeInfo(@RequestParam(required = false) String barcode,
                                                                               @RequestParam(required = false) Long targetCompanyId) {
        return Result.ok(workOrderService.getUpstreamHqCreateBarcodeInfo(barcode, targetCompanyId));
    }

    /**
     * 代客户填写创建工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @ApiOperation(value = "代客户填写创建工单")
    @SaCheckPermission("workorder:add")
    @OperLog(title = "工单管理", operType = OperTypeEnum.INSERT)
    @PostMapping("/create/proxy")
    public Result<Long> createProxy(@Validated @RequestBody WorkOrderProxyCreateDTO dto) {
        return Result.ok(workOrderService.createProxy(dto));
    }

    /**
     * 二级报修一级创建工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @ApiOperation(value = "二级报修一级创建工单")
    @SaCheckPermission("workorder:add")
    @OperLog(title = "工单管理", operType = OperTypeEnum.INSERT)
    @PostMapping("/create/upstream-first")
    public Result<Long> createUpstreamFirst(@Validated @RequestBody WorkOrderUpstreamCreateDTO dto) {
        return Result.ok(workOrderService.createUpstreamFirst(dto));
    }

    /**
     * 一级报修佳士创建工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @ApiOperation(value = "一级报修佳士创建工单")
    @SaCheckPermission("workorder:add")
    @OperLog(title = "工单管理", operType = OperTypeEnum.INSERT)
    @PostMapping("/create/upstream-hq")
    public Result<Long> createUpstreamHq(@Validated @RequestBody WorkOrderUpstreamCreateDTO dto) {
        return Result.ok(workOrderService.createUpstreamHq(dto));
    }

    /**
     * 派单
     *
     * @param dto 派单参数
     * @return 操作结果
     */
    @ApiOperation(value = "派单")
    @SaCheckPermission("workorder:assign")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/assign")
    public Result<Void> assign(@Validated @RequestBody WorkOrderAssignDTO dto) {
        workOrderService.assign(dto);
        return Result.ok();
    }

    /**
     * 维修员接单
     *
     * @param dto 接单参数
     * @return 操作结果
     */
    @ApiOperation(value = "维修员接单")
    @SaCheckPermission("workorder:accept")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/tech-accept")
    public Result<Void> techAccept(@Validated @RequestBody WorkOrderTechAcceptDTO dto) {
        workOrderService.techAccept(dto);
        return Result.ok();
    }

    /**
     * 转单
     *
     * @param dto 转单参数
     * @return 操作结果
     */
    @ApiOperation(value = "转单")
    @SaCheckPermission("workorder:transfer")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/transfer")
    public Result<Void> transfer(@Validated @RequestBody WorkOrderTransferDTO dto) {
        workOrderService.transfer(dto);
        return Result.ok();
    }

    /**
     * 提交维修登记
     *
     * @param dto 维修参数
     * @return 操作结果
     */
    @ApiOperation(value = "维修登记")
    @SaCheckPermission("workorder:repair")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/repair")
    public Result<Void> saveRepair(@Validated @RequestBody WorkOrderRepairDTO dto) {
        workOrderService.saveRepair(dto);
        return Result.ok();
    }

    /**
     * 保存复检记录
     *
     * @param dto 复检参数
     * @return 操作结果
     */
    @ApiOperation(value = "保存复检记录")
    @SaCheckPermission("workorder:review")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/review")
    public Result<Void> saveReview(@Validated @RequestBody WorkOrderReviewDTO dto) {
        workOrderService.saveReview(dto);
        return Result.ok();
    }

    /**
     * 补录维修/复检前缺失的机器型号。
     *
     * @param dto 补录参数
     * @return 操作结果
     */
    @ApiOperation(value = "补录维修/复检前缺失的机器型号")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/repair-product-model")
    public Result<Void> updateRepairProductModel(@Validated @RequestBody WorkOrderUpdateProductModelDTO dto) {
        workOrderService.updateRepairProductModel(dto);
        return Result.ok();
    }

    /**
     * 上传寄件快递单号
     *
     * <p>本轮该接口不再使用 `workorder:assign` 基础权限点，
     * 最终放行由服务层按“工单可见 + 寄修待接单窗口 + 首条 CREATE 建单人本人”统一兜底校验。</p>
     *
     * @param dto 寄件快递单号参数
     * @return 操作结果
     */
    @ApiOperation(value = "上传寄件快递单号")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/send-express")
    public Result<Void> updateSendExpress(@Validated @RequestBody WorkOrderSendExpressDTO dto) {
        workOrderService.updateSendExpress(dto);
        return Result.ok();
    }

    /**
     * 关闭工单
     *
     * @param dto 关闭参数
     * @return 操作结果
     */
    @ApiOperation(value = "关闭工单")
    @SaCheckPermission("workorder:close")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/close")
    public Result<Void> close(@Validated @RequestBody WorkOrderCloseDTO dto) {
        workOrderService.close(dto);
        return Result.ok();
    }
}
