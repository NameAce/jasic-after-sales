package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.dashboard.ServiceDashboardHomeVO;

/**
 * 服务主体首页 Service。
 *
 * <p>该接口只负责服务主体首页专用聚合编排，
 * 不承载列表页分页查询，也不混入总部或平台首页字段。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public interface IServiceDashboardService {

    /**
     * 查询服务主体首页总览。
     *
     * <p>该方法会按当前登录上下文聚合服务主体首页所需的概览、状态分布、
     * 近七天趋势和最新历史待办列表；不接受前端额外指定主体口径。</p>
     *
     * @return 服务主体首页总览
     */
    ServiceDashboardHomeVO getHome();
}
