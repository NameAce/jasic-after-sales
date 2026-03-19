package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.common.core.controller.BaseController;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.enums.OperTypeEnum;
import com.jasic.aftersales.system.domain.entity.SysOperLog;
import com.jasic.aftersales.system.domain.query.SysOperLogQuery;
import com.jasic.aftersales.system.service.ISysOperLogService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志控制器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/log/oper-log")
public class SysOperLogController extends BaseController {

    @Resource
    private ISysOperLogService operLogService;

    /**
     * 分页查询操作日志
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @SaCheckPermission("log:operLog:list")
    @GetMapping("/list")
    public Result<PageResult<SysOperLog>> list(SysOperLogQuery query) {
        PageResult<SysOperLog> page = operLogService.listPage(query);
        return Result.ok(page);
    }

    /**
     * 清空操作日志
     *
     * @return 操作结果
     */
    @SaCheckPermission("log:operLog:remove")
    @OperLog(title = "操作日志", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        operLogService.clean();
        return Result.ok();
    }

    /**
     * 批量删除操作日志
     *
     * @param ids 主键ID，逗号分隔
     * @return 操作结果
     */
    @SaCheckPermission("log:operLog:remove")
    @OperLog(title = "操作日志", operType = OperTypeEnum.DELETE)
    @DeleteMapping("/{ids}")
    public Result<Void> remove(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        operLogService.removeByIds(idList);
        return Result.ok();
    }
}
