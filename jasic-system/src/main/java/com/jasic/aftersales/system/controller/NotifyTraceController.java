package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.notify.domain.dto.NotifyManualDeadDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceDispatchDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceEventDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTracePageVO;
import com.jasic.aftersales.system.notify.service.NotifyTraceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 通知记录排障控制器。
 *
 * @author Codex
 * @date 2026/05/14
 */
@Api(tags = "通知记录排障")
@RestController
@RequestMapping("/system/notify/trace")
public class NotifyTraceController {

    @Resource
    private NotifyTraceService notifyTraceService;

    /**
     * 分页查询通知记录。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询通知记录")
    @SaCheckPermission("system:notifyTrace:list")
    @GetMapping("/page")
    public Result<PageResult<NotifyTracePageVO>> page(NotifyTraceQuery query) {
        return Result.ok(notifyTraceService.listPage(query));
    }

    /**
     * 查询事件详情。
     *
     * @param id 事件ID
     * @return 事件详情
     */
    @ApiOperation(value = "查看事件详情")
    @SaCheckPermission("system:notifyTrace:view")
    @GetMapping("/event/{id}")
    public Result<NotifyTraceEventDetailVO> eventDetail(@PathVariable Long id) {
        return Result.ok(notifyTraceService.getEventDetail(id));
    }

    /**
     * 查询分发详情。
     *
     * @param id 分发任务ID
     * @return 分发详情
     */
    @ApiOperation(value = "查看分发详情")
    @SaCheckPermission("system:notifyTrace:view")
    @GetMapping("/dispatch/{id}")
    public Result<NotifyTraceDispatchDetailVO> dispatchDetail(@PathVariable Long id) {
        return Result.ok(notifyTraceService.getDispatchDetail(id));
    }

    /**
     * 人工重试事件。
     *
     * @param id 事件ID
     * @return 操作结果
     */
    @ApiOperation(value = "手动重试事件")
    @SaCheckPermission("system:notifyTrace:retry")
    @OperLog(title = "通知记录排障", operType = OperTypeEnum.UPDATE)
    @PostMapping("/event/{id}/retry")
    public Result<Void> retryEvent(@PathVariable Long id) {
        // 人工重试要复用服务层状态边界，避免控制层直接更新数据库破坏幂等保护。
        notifyTraceService.retryEvent(id);
        return Result.ok();
    }

    /**
     * 人工重试分发任务。
     *
     * @param id 分发任务ID
     * @return 操作结果
     */
    @ApiOperation(value = "手动重试分发任务")
    @SaCheckPermission("system:notifyTrace:retry")
    @OperLog(title = "通知记录排障", operType = OperTypeEnum.UPDATE)
    @PostMapping("/dispatch/{id}/retry")
    public Result<Void> retryDispatch(@PathVariable Long id) {
        // 分发重试必须走统一服务入口，确保失败上下文和渠道响应快照被正确清理。
        notifyTraceService.retryDispatch(id);
        return Result.ok();
    }

    /**
     * 人工标记事件死信。
     *
     * @param id 事件ID
     * @param dto 处理原因
     * @return 操作结果
     */
    @ApiOperation(value = "事件标记不再处理")
    @SaCheckPermission("system:notifyTrace:dead")
    @OperLog(title = "通知记录排障", operType = OperTypeEnum.UPDATE)
    @PostMapping("/event/{id}/dead")
    public Result<Void> deadEvent(@PathVariable Long id, @Validated @RequestBody NotifyManualDeadDTO dto) {
        // 死信标记需要写清人工关闭原因，后续排障才能区分自动失败和人工终止。
        notifyTraceService.markEventDead(id, dto.getReason());
        return Result.ok();
    }

    /**
     * 人工标记分发任务死信。
     *
     * @param id 分发任务ID
     * @param dto 处理原因
     * @return 操作结果
     */
    @ApiOperation(value = "分发任务标记不再处理")
    @SaCheckPermission("system:notifyTrace:dead")
    @OperLog(title = "通知记录排障", operType = OperTypeEnum.UPDATE)
    @PostMapping("/dispatch/{id}/dead")
    public Result<Void> deadDispatch(@PathVariable Long id, @Validated @RequestBody NotifyManualDeadDTO dto) {
        // 分发死信除了终止重试，还要落人工关闭结果码，便于后续筛选 DEAD_MANUAL_CLOSED。
        notifyTraceService.markDispatchDead(id, dto.getReason());
        return Result.ok();
    }
}
