package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.dashboard.PlatformDashboardHomeVO;

/**
 * 平台首页 Service。
 *
 * <p>该接口只负责平台治理类首页聚合编排，
 * 不混入工单、待办流转或总部网点统计。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public interface IPlatformDashboardService {

    /**
     * 查询平台首页总览。
     *
     * <p>该方法会按当前平台登录上下文聚合组织治理概览、
     * 主体类型分布和操作日志近七天趋势。</p>
     *
     * @return 平台首页总览
     */
    PlatformDashboardHomeVO getHome();
}
