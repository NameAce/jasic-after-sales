package com.jasic.aftersales.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.system.domain.vo.dashboard.ServiceDashboardHomeVO;
import com.jasic.aftersales.system.service.IServiceDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 服务主体首页控制器。
 *
 * <p>该控制器只提供服务主体首页专用聚合接口，
 * 不承载工单列表、待办列表等非首页业务入口。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@Api(tags = "服务主体首页")
@RestController
@SaCheckLogin
@RequestMapping("/dashboard/service")
public class ServiceDashboardController {

    /**
     * 服务主体首页 Service。
     */
    @Resource
    private IServiceDashboardService serviceDashboardService;

    /**
     * 查询服务主体首页总览。
     *
     * <p>该接口一次返回服务主体首页所需的全部数据块，
     * 包括概览卡片、工单状态、近七天趋势和最新历史待办。</p>
     *
     * @return 服务主体首页总览
     */
    @ApiOperation(value = "查询服务主体首页总览")
    @GetMapping("/home")
    public Result<ServiceDashboardHomeVO> home() {
        return Result.ok(serviceDashboardService.getHome());
    }
}
