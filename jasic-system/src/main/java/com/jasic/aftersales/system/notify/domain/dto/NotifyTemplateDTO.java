package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Notification template create/update DTO.
 *
 * @author Codex
 * @date 2026/04/20
 */
@ApiModel(description = "通知模板新增/修改参数")
@Data
public class NotifyTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "模板编码", required = true)
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "通知总开关", required = true)
    @NotNull(message = "通知总开关不能为空")
    private Integer notifyEnabled;

    @ApiModelProperty(value = "覆盖开关", required = true)
    @NotNull(message = "覆盖开关不能为空")
    private Integer overrideEnabled;

    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    @ApiModelProperty(value = "摘要模板")
    private String summaryTemplate;

    @ApiModelProperty(value = "跳转值模板")
    private String routeValueTemplate;

    @ApiModelProperty(value = "备注")
    private String remark;
}
