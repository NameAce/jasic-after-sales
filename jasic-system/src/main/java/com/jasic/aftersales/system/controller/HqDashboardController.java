package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.system.domain.vo.dashboard.HqDashboardHomeVO;
import com.jasic.aftersales.system.service.IHqDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 总部首页控制器。
 *
 * <p>该控制器只提供总部“调度看板”聚合接口。
 * 本轮总部首页只返回当前总部承接工单池、已转出和近七天事件趋势，不承载待办、网点履约、SLA 或风险指标。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Api(tags = "总部首页")
@RestController
@SaCheckLogin
@RequestMapping("/dashboard/hq")
public class HqDashboardController {

    /**
     * 总部首页 Service。
     */
    @Resource
    private IHqDashboardService hqDashboardService;

    /**
     * 查询总部首页总览。
     *
     * <p>该接口一次返回调度看板所需的全部数据块。
     * 工单卡片的 routeTarget 由后端生成，前端只负责按 routeName 和 query 跳转。</p>
     *
     * @return 总部首页总览
     */
    @ApiOperation(value = "查询总部首页总览")
    @GetMapping("/home")
    public Result<HqDashboardHomeVO> home() {
        return Result.ok(hqDashboardService.getHome());
    }
}
