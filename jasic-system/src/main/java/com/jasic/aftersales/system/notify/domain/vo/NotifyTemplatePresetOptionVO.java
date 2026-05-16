package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.support.NotifyTemplateVariableMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通知模板预置组合选项。
 *
 * <p>该对象用于承载后端白名单里的可新增模板组合，以及该组合对应的默认模板内容和变量说明。
 * 前端新增模板时只能从这些组合里选，避免绕过后端确认口径创建未知场景。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板预置组合选项")
@Data
public class NotifyTemplatePresetOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型编码。
     */
    @ApiModelProperty(value = "业务类型编码")
    private String bizType;

    /**
     * 业务类型说明。
     */
    @ApiModelProperty(value = "业务类型说明")
    private String bizTypeDesc;

    /**
     * 触发场景编码。
     */
    @ApiModelProperty(value = "触发场景编码")
    private String triggerScene;

    /**
     * 触发场景说明。
     */
    @ApiModelProperty(value = "触发场景说明")
    private String triggerSceneDesc;

    /**
     * 通知类型编码。
     */
    @ApiModelProperty(value = "通知类型编码")
    private String notifyType;

    /**
     * 通知类型说明。
     */
    @ApiModelProperty(value = "通知类型说明")
    private String notifyTypeDesc;

    /**
     * 渠道场景编码。
     */
    @ApiModelProperty(value = "渠道场景编码")
    private String channelScene;

    /**
     * 渠道场景说明。
     */
    @ApiModelProperty(value = "渠道场景说明")
    private String channelSceneDesc;

    /**
     * 接收对象类型编码。
     */
    @ApiModelProperty(value = "接收对象类型编码")
    private String receiverType;

    /**
     * 接收对象类型说明。
     */
    @ApiModelProperty(value = "接收对象类型说明")
    private String receiverTypeDesc;

    /**
     * 接收对象展示说明。
     */
    @ApiModelProperty(value = "接收对象说明")
    private String receiverDesc;

    /**
     * 默认模板名称。
     */
    @ApiModelProperty(value = "默认模板名称")
    private String defaultTemplateName;

    /**
     * 默认标题模板。
     */
    @ApiModelProperty(value = "默认标题模板")
    private String defaultTitleTemplate;

    /**
     * 默认内容模板。
     */
    @ApiModelProperty(value = "默认内容模板")
    private String defaultContentTemplate;

    /**
     * 默认跳转类型。
     */
    @ApiModelProperty(value = "默认跳转类型")
    private String defaultRouteType;

    /**
     * 默认跳转值模板。
     */
    @ApiModelProperty(value = "默认跳转值模板")
    private String defaultRouteValueTemplate;

    /**
     * 可用变量说明。
     */
    @ApiModelProperty(value = "可用变量说明")
    private List<NotifyTemplateVariableMeta> variables;
}
