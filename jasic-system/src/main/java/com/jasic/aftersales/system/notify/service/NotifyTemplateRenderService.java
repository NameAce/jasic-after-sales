package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;

import java.util.Map;

/**
 * 通知模板运行时渲染服务。
 *
 * <p>当前运行时只允许按“场景 + 目标”读取通知模板配置，避免再次回退到旧的单场景模板口径。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public interface NotifyTemplateRenderService {

    /**
     * 按通知场景和通知目标类型渲染指定目标。
     *
     * @param sceneCode 通知场景编码
     * @param targetType 通知目标类型
     * @param variables 模板变量
     * @return 渲染结果
     */
    NotifyTemplateRenderResult render(String sceneCode, String targetType, Map<String, Object> variables);
}
