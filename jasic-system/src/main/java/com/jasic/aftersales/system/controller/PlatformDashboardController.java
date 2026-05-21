package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.system.domain.vo.dashboard.PlatformDashboardHomeVO;
import com.jasic.aftersales.system.service.IPlatformDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 平台首页控制器。
 *
 * <p>该控制器只提供平台治理类首页专用聚合接口，
 * 不承载工单业务统计，也不影响原有组织管理列表接口。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Api(tags = "平台首页")
@RestController
@SaCheckLogin
@RequestMapping("/dashboard/platform")
public class PlatformDashboardController {

    /**
     * 平台首页 Service。
     */
    @Resource
    private IPlatformDashboardService platformDashboardService;

    /**
     * 查询平台首页总览。
     *
     * <p>该接口一次返回平台首页所需的全部数据块，
     * 包括组织治理概览、主体类型分布和操作日志近七天趋势。</p>
     *
     * @return 平台首页总览
     */
    @ApiOperation(value = "查询平台首页总览")
    @GetMapping("/home")
    public Result<PlatformDashboardHomeVO> home() {
        return Result.ok(platformDashboardService.getHome());
    }
}
