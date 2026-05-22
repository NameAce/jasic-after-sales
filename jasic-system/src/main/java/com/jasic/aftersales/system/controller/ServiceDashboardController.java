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
 * 服务网点主体首页控制器。
 *
 * <p>该控制器只提供服务网点“服务工作台”聚合接口。
 * 一级网点和二级网点本轮统一处理，不承载我的事项或一级管理二级的专项首页。</p>
 *
 * @author Zoro
 * @date 2026/05/20
 */
@Api(tags = "服务网点主体首页")
@RestController
@SaCheckLogin
@RequestMapping("/dashboard/service")
public class ServiceDashboardController {

    /**
     * 服务网点主体首页 Service。
     */
    @Resource
    private IServiceDashboardService serviceDashboardService;

    /**
     * 查询服务网点主体首页总览。
     *
     * <p>该接口一次返回服务工作台所需的全部数据块：
     * 当前承接工单、已转出、历史参与入口和近七天事件趋势。</p>
     *
     * @return 服务网点主体首页总览
     */
    @ApiOperation(value = "查询服务网点主体首页总览")
    @GetMapping("/home")
    public Result<ServiceDashboardHomeVO> home() {
        return Result.ok(serviceDashboardService.getHome());
    }
}
