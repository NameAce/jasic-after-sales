package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Notification template preview result.
 *
 * @author Codex
 * @date 2026/04/20
 */
@ApiModel(description = "通知模板预览结果")
@Data
public class NotifyTemplatePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "是否发送通知")
    private Boolean notifyEnabled;

    @ApiModelProperty(value = "实际使用模板来源")
    private String templateSource;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "摘要")
    private String summary;

    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    @ApiModelProperty(value = "跳转值")
    private String routeValue;

    @ApiModelProperty(value = "错误信息")
    private List<String> errors;
}
