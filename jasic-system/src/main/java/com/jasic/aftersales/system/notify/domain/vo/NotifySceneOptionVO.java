package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.support.NotifyTemplateVariableMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通知场景选项返回对象。
 *
 * <p>该对象用于模板配置页展示可维护的通知场景，以及每个场景对应的默认模板、
 * 默认路由、变量元数据和接收对象说明。
 * 它只服务后台配置元数据返回，不参与运行时模板渲染。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景选项")
@Data
public class NotifySceneOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    @ApiModelProperty(value = "通知场景名称")
    private String sceneName;

    @ApiModelProperty(value = "通知类型编码")
    private String notifyType;

    @ApiModelProperty(value = "通知类型说明")
    private String notifyTypeDesc;

    @ApiModelProperty(value = "接收对象类型编码")
    private String receiverType;

    @ApiModelProperty(value = "接收对象类型说明")
    private String receiverTypeDesc;

    @ApiModelProperty(value = "接收对象说明")
    private String receiverDesc;

    @ApiModelProperty(value = "渠道类型编码")
    private String channelType;

    @ApiModelProperty(value = "渠道类型说明")
    private String channelTypeDesc;

    @ApiModelProperty(value = "默认模板名称")
    private String defaultTemplateName;

    @ApiModelProperty(value = "默认标题模板")
    private String defaultTitleTemplate;

    @ApiModelProperty(value = "默认内容模板")
    private String defaultContentTemplate;

    @ApiModelProperty(value = "默认跳转类型")
    private String defaultRouteType;

    @ApiModelProperty(value = "默认跳转值模板")
    private String defaultRouteValueTemplate;

    @ApiModelProperty(value = "可用变量元数据")
    private List<NotifyTemplateVariableMeta> variables;
}
