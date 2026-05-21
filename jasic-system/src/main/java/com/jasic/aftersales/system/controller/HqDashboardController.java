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
 * <p>该控制器只提供总部首页专用聚合接口，
 * 统一承接总部首页概览、趋势、网点汇总和排行能力。</p>
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
     * <p>该接口一次返回总部首页所需的全部数据块，
     * 包括概览卡片、工单状态、近七天趋势、网点汇总、待接单排行和最新动态。</p>
     *
     * @return 总部首页总览
     */
    @ApiOperation(value = "查询总部首页总览")
    @GetMapping("/home")
    public Result<HqDashboardHomeVO> home() {
        return Result.ok(hqDashboardService.getHome());
    }
}
