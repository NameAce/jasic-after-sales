package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.dashboard.PlatformDashboardHomeVO;

/**
 * 平台首页 Service。
 *
 * <p>该接口只负责平台“治理看板”聚合编排。
 * 本轮只返回组织治理、账号治理和基础配置三块，不混入工单、CRM 同步、消息治理或趋势图。</p>
 *
 * @author Zoro
 * @date 2026/05/20
 */
public interface IPlatformDashboardService {

    /**
     * 查询平台治理看板。
     *
     * <p>该方法会按当前平台登录上下文聚合组织治理、账号治理和基础配置规模数据。</p>
     *
     * @return 平台治理看板数据
     */
    PlatformDashboardHomeVO getHome();
}
