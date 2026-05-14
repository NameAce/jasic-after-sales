package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.dto.SyncTaskDTO;
import com.jasic.aftersales.system.domain.query.SyncTaskLogQuery;
import com.jasic.aftersales.system.domain.query.SyncTaskQuery;
import com.jasic.aftersales.system.domain.vo.SyncTaskHandlerOptionVO;
import com.jasic.aftersales.system.domain.vo.SyncTaskLogVO;
import com.jasic.aftersales.system.domain.vo.SyncTaskVO;
import com.jasic.aftersales.system.service.ISyncTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * 同步任务控制器。
 *
 * <p>负责暴露同步任务配置、执行和日志查询相关接口，便于统一管理外部系统数据同步。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Api(tags = "同步任务管理")
@RestController
@RequestMapping("/system/sync-task")
public class SyncTaskController extends BaseController {

    @Resource
    private ISyncTaskService syncTaskService;

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @ApiOperation(value = "分页查询同步任务")
    @SaCheckPermission("system:syncTask:list")
    @GetMapping("/list")
    public Result<PageResult<SyncTaskVO>> list(SyncTaskQuery query) {
        return Result.ok(syncTaskService.listPage(query));
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @return ??????
     */
    @ApiOperation(value = "查询同步任务详情")
    @SaCheckPermission("system:syncTask:list")
    @GetMapping("/{id}")
    public Result<SyncTaskVO> getById(@PathVariable Long id) {
        return Result.ok(syncTaskService.getById(id));
    }

    /**
     * ???????
     *
     * @return ????
     */
    @ApiOperation(value = "查询同步任务处理器选项")
    @SaCheckPermission("system:syncTask:list")
    @GetMapping("/handler-options")
    public Result<List<SyncTaskHandlerOptionVO>> listHandlerOptions() {
        return Result.ok(syncTaskService.listHandlerOptions());
    }

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @ApiOperation(value = "分页查询同步任务执行日志")
    @SaCheckPermission("system:syncTask:log")
    @GetMapping("/log/list")
    public Result<PageResult<SyncTaskLogVO>> listLogs(SyncTaskLogQuery query) {
        return Result.ok(syncTaskService.listLogPage(query));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ??????
     */
    @ApiOperation(value = "新增同步任务")
    @SaCheckPermission("system:syncTask:add")
    @OperLog(title = "同步任务管理", operType = OperTypeEnum.INSERT)
    @PostMapping
    public Result<Long> save(@Validated @RequestBody SyncTaskDTO dto) {
        return Result.ok(syncTaskService.save(dto));
    }

    /**
     * ?????
     *
     * @param dto ????
     * @return ??????
     */
    @ApiOperation(value = "修改同步任务")
    @SaCheckPermission("system:syncTask:update")
    @OperLog(title = "同步任务管理", operType = OperTypeEnum.UPDATE)
    @PutMapping
    public Result<Void> update(@Validated @RequestBody SyncTaskDTO dto) {
        syncTaskService.update(dto);
        return Result.ok();
    }

    /**
     * ?????
     *
     * @param id ??ID
     * @return ??????
     */
    @ApiOperation(value = "立即执行同步任务")
    @SaCheckPermission("system:syncTask:execute")
    @OperLog(title = "同步任务管理", operType = OperTypeEnum.OTHER)
    @PostMapping("/{id}/execute")
    public Result<Long> execute(@PathVariable Long id) {
        // 返回执行日志ID，前端可据此轮询或查询本次手动执行结果。
        return Result.ok(syncTaskService.execute(id));
    }
}
