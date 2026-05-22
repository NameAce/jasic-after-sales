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
 * <p>该控制器只提供平台“治理看板”聚合接口。
 * 本轮平台首页只包含组织治理、账号治理和基础配置三块，不承载工单、CRM 同步、消息治理、趋势图或完整度评分。</p>
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
     * <p>该接口一次返回治理看板所需的三块数据。
     * 平台首页接口不得调用工单统计接口，也不得依赖工单表。</p>
     *
     * @return 平台首页总览
     */
    @ApiOperation(value = "查询平台首页总览")
    @GetMapping("/home")
    public Result<PlatformDashboardHomeVO> home() {
        return Result.ok(platformDashboardService.getHome());
    }
}
