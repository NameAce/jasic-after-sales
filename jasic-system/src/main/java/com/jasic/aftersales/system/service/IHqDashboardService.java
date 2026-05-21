package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.dashboard.HqDashboardHomeVO;

/**
 * 总部首页 Service。
 *
 * <p>该接口只负责总部首页专用聚合编排，
 * 在服务主体首页公共能力基础上，补充总部网点汇总与待接单排行。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public interface IHqDashboardService {

    /**
     * 查询总部首页总览。
     *
     * <p>该方法会按当前总部登录上下文聚合首页数据，
     * 不接受前端额外传入公司口径、主体口径或网点范围参数。</p>
     *
     * @return 总部首页总览
     */
    HqDashboardHomeVO getHome();
}
