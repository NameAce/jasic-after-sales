package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.dashboard.HqDashboardHomeVO;

/**
 * 总部首页 Service。
 *
 * <p>该接口只负责总部“调度看板”聚合编排。
 * 本轮只返回当前总部承接工单池、已转出和近七天事件趋势，不混入网点履约或待办数据。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public interface IHqDashboardService {

    /**
     * 查询总部调度看板。
     *
     * <p>该方法会按当前总部登录上下文聚合首页数据，
     * 不接受前端额外传入公司口径、主体口径或网点范围参数。</p>
     *
     * @return 总部调度看板数据
     */
    HqDashboardHomeVO getHome();
}
