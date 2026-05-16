package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 通知模板新增或修改参数。
 *
 * <p>该对象只承载模板主数据，不负责渠道参数配置。
 * 本次精简重构后模板身份统一收口为 `sceneCode`，业务类型、通知类型、接收对象和变量元数据
 * 全部由后端场景注册表提供，前端不再提交这些系统字段。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板新增或修改参数")
@Data
public class NotifyTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 通知场景编码。
     */
    @ApiModelProperty(value = "通知场景编码", required = true)
    @NotBlank(message = "通知场景编码不能为空")
    private String sceneCode;

    /**
     * 模板名称。
     */
    @ApiModelProperty(value = "模板名称", required = true)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /**
     * 标题模板。
     */
    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    /**
     * 内容模板。
     */
    @ApiModelProperty(value = "内容模板")
    private String contentTemplate;

    /**
     * 跳转类型。
     */
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /**
     * 跳转值模板。
     */
    @ApiModelProperty(value = "跳转值模板")
    private String routeValueTemplate;

    /**
     * 模板状态。
     */
    @ApiModelProperty(value = "状态：1启用，0停用")
    private Integer status;

    /**
     * 备注。
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}

