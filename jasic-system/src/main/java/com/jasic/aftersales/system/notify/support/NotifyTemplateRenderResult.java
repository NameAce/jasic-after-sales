package com.jasic.aftersales.system.notify.support;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知模板运行时渲染结果。
 *
 * <p>该对象服务当前发送链路，负责承载模板是否可用、命中的模板身份以及最终渲染出的标题、
 * 内容和跳转信息。当前主体链路仍沿用 `templateCode` 字段名传递模板身份，
 * Phase 1 起该字段实际承载的是 `sceneCode`。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@Data
public class NotifyTemplateRenderResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否存在可用模板。
     */
    private boolean notifyEnabled;

    /**
     * 实际参与渲染的通知场景编码。
     *
     * <p>模板缺失或停用时仍会回填该字段，便于外部分发跳过记录和排障页面定位到业务场景。</p>
     */
    private String sceneCode;

    /**
     * 通知场景名称。
     *
     * <p>该名称来自场景注册表，不来自模板表，避免模板停用或缺失时排障输出只剩技术编码。</p>
     */
    private String sceneName;

    /**
     * 实际命中的模板身份。
     */
    private String templateCode;

    /**
     * 实际命中的模板名称。
     *
     * <p>该字段用于分发 payload 和排障详情保存模板快照，避免模板后续改名后历史记录失去上下文。</p>
     */
    private String templateName;

    /**
     * 渲染后的标题。
     */
    private String title;

    /**
     * 渲染后的内容。
     */
    private String summary;

    /**
     * 跳转类型。
     */
    private String routeType;

    /**
     * 渲染后的跳转值。
     */
    private String routeValue;

    /**
     * 渲染错误列表。
     */
    private final List<String> errors = new ArrayList<>();

    /**
     * 追加渲染错误。
     *
     * @param error 错误信息
     */
    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errors.add(error);
        }
    }
}
