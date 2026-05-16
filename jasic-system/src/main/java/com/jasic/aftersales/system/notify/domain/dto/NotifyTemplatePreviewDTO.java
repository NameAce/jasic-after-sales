package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * 通知模板预览参数。
 *
 * <p>预览接口用于在模板未保存前校验变量占位符和渲染结果，
 * 因此这里直接接收当前编辑态的 `sceneCode` 和模板内容，而不是只传模板主键。
 * 可用变量白名单由场景注册表控制，前端不再提交历史组合字段。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板预览参数")
@Data
public class NotifyTemplatePreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知场景编码。
     */
    @ApiModelProperty(value = "通知场景编码", required = true)
    @NotBlank(message = "通知场景编码不能为空")
    private String sceneCode;

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
     * 预览时使用的变量快照。
     */
    @ApiModelProperty(value = "预览变量")
    private Map<String, Object> variables;
}

