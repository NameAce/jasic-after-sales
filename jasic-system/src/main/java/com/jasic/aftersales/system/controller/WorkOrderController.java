package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderQuoteDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderSendExpressDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
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
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 工单查询控制器
 *
 * @author Codex
 * @date 2026/03/26
 */
@RestController
@RequestMapping("/system/work-order")
public class WorkOrderController extends BaseController {

    @Resource
    private IWorkOrderService workOrderService;

    /**
     * 分页查询工单列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("workorder:list")
    @GetMapping("/list")
    public Result<PageResult<WorkOrderListVO>> list(WorkOrderQuery query) {
        if (query.getCompanyId() == null) {
            query.setCompanyId(SecurityContext.getCurrentCompanyId());
        }
        return Result.ok(workOrderService.listPage(query));
    }

    /**
     * 按状态统计工单数量
     *
     * @param query 查询参数
     * @return 状态统计结果
     */
    @SaCheckPermission("workorder:list")
    @GetMapping("/status-count")
    public Result<List<WorkOrderStatusCountVO>> statusCount(WorkOrderQuery query) {
        if (query.getCompanyId() == null) {
            query.setCompanyId(SecurityContext.getCurrentCompanyId());
        }
        return Result.ok(workOrderService.countByStatus(query));
    }

    /**
     * 查询工单详情
     *
     * @param workOrderId 工单ID
     * @return 工单详情
     */
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
    @SaCheckPermission("workorder:transfer")
    @GetMapping("/{workOrderId}/transfer-target-options")
    public Result<List<SysCompanySimpleVO>> listTransferTargetOptions(@PathVariable Long workOrderId) {
        return Result.ok(workOrderService.listTransferTargetOptions(workOrderId));
    }

    /**
     * 查询维修登记可选故障与维修说明
     *
     * @param workOrderId 工单ID
     * @return 故障与维修说明选项
     */
    @SaCheckPermission("workorder:repair")
    @GetMapping("/{workOrderId}/repair-fault-options")
    public Result<List<WorkOrderRepairFaultOptionVO>> listRepairFaultOptions(@PathVariable Long workOrderId) {
        return Result.ok(workOrderService.listRepairFaultOptions(workOrderId));
    }

    /**
     * 创建工单
     *
     * @param dto 工单参数
     * @return 工单ID
     */
    @SaCheckPermission("workorder:add")
    @OperLog(title = "工单管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody WorkOrderCreateDTO dto) {
        return Result.ok(workOrderService.save(dto));
    }

    /**
     * 派单
     *
     * @param dto 派单参数
     * @return 操作结果
     */
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
    @SaCheckPermission("workorder:transfer")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/transfer")
    public Result<Void> transfer(@Validated @RequestBody WorkOrderTransferDTO dto) {
        workOrderService.transfer(dto);
        return Result.ok();
    }

    /**
     * 保存报价
     *
     * @param dto 报价参数
     * @return 操作结果
     */
    @SaCheckPermission("workorder:quote")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/quote")
    public Result<Void> saveQuote(@Validated @RequestBody WorkOrderQuoteDTO dto) {
        workOrderService.saveQuote(dto);
        return Result.ok();
    }

    /**
     * 保存维修登记
     *
     * @param dto 维修参数
     * @return 操作结果
     */
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
    @SaCheckPermission("workorder:review")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PostMapping("/review")
    public Result<Void> saveReview(@Validated @RequestBody WorkOrderReviewDTO dto) {
        workOrderService.saveReview(dto);
        return Result.ok();
    }

    /**
     * 上传寄件快递单号
     *
     * @param dto 寄件快递单号参数
     * @return 操作结果
     */
    @SaCheckPermission("workorder:assign")
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
    @SaCheckPermission("workorder:close")
    @OperLog(title = "工单管理", operType = OperTypeEnum.UPDATE)
    @PutMapping("/close")
    public Result<Void> close(@Validated @RequestBody WorkOrderCloseDTO dto) {
        workOrderService.close(dto);
        return Result.ok();
    }
}
