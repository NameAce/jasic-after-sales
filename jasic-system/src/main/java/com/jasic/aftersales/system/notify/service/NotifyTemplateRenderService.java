package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;

import java.util.Map;

/**
 * 通知模板运行时渲染服务。
 *
 * <p>阶段一完成后，运行时渲染不再按“单场景单模板”读取旧模板表，
 * 而是按 `sceneCode + targetType` 从新的通知目标配置表读取模板内容。
 * 为了降低对阶段一之外代码的改动，接口保留旧的按 `sceneCode` 渲染入口，
 * 它会自动回退到注册表声明的默认目标类型。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public interface NotifyTemplateRenderService {

    /**
     * 按场景编码渲染默认通知目标。
     *
     * @param sceneCode 通知场景编码
     * @param variables 模板变量
     * @return 渲染结果
     */
    NotifyTemplateRenderResult render(String sceneCode, Map<String, Object> variables);

    /**
     * 按场景编码和通知目标类型渲染指定目标。
     *
     * @param sceneCode 通知场景编码
     * @param targetType 通知目标类型
     * @param variables 模板变量
     * @return 渲染结果
     */
    NotifyTemplateRenderResult render(String sceneCode, String targetType, Map<String, Object> variables);
}
