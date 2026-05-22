package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.dashboard.ServiceDashboardHomeVO;

/**
 * 服务网点主体首页 Service。
 *
 * <p>该接口只负责服务网点“服务工作台”聚合编排。
 * 一级网点和二级网点统一使用当前服务公司口径，不承载我的事项或一级管理二级专项首页。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public interface IServiceDashboardService {

    /**
     * 查询服务网点服务工作台。
     *
     * <p>该方法会按当前登录上下文聚合当前承接工单、已转出、历史参与入口和近七天事件趋势；
     * 不接受前端额外指定主体口径。</p>
     *
     * @return 服务网点服务工作台数据
     */
    ServiceDashboardHomeVO getHome();
}
