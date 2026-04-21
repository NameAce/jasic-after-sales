package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Notification template view object.
 *
 * @author Codex
 * @date 2026/04/20
 */
@ApiModel(description = "通知模板返回对象")
@Data
public class NotifyTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "模板编码")
    private String templateCode;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "模板来源")
    private String templateSource;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "事件类型")
    private String eventType;

    @ApiModelProperty(value = "消息类型")
    private String messageType;

    @ApiModelProperty(value = "通知总开关")
    private Integer notifyEnabled;

    @ApiModelProperty(value = "覆盖开关")
    private Integer overrideEnabled;

    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    @ApiModelProperty(value = "摘要模板")
    private String summaryTemplate;

    @ApiModelProperty(value = "跳转值模板")
    private String routeValueTemplate;

    @ApiModelProperty(value = "变量说明")
    private String variablesJson;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
