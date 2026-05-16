package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;

import java.util.Map;

/**
 * 通知模板运行时渲染服务。
 *
 * <p>该接口只暴露事件消费、消息构建和渠道分发所需的运行时渲染能力。
 * Phase 1 起模板命中条件统一改为 `sceneCode`，不再按历史组合字段查询模板。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public interface NotifyTemplateRenderService {

    /**
     * 按通知场景渲染当前启用模板。
     *
     * @param sceneCode 通知场景编码
     * @param variables 模板变量
     * @return 渲染结果
     */
    NotifyTemplateRenderResult render(String sceneCode, Map<String, Object> variables);
}
