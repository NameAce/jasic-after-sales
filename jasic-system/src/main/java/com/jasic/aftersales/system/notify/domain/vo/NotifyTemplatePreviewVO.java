package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通知模板预览结果。
 *
 * <p>该对象只承载本次预览渲染结果，
 * 不代表模板已经保存，也不代表该模板一定处于启用状态。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板预览结果")
@Data
public class NotifyTemplatePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标题预览。
     */
    @ApiModelProperty(value = "标题预览")
    private String title;

    /**
     * 内容预览。
     */
    @ApiModelProperty(value = "内容预览")
    private String content;

    /**
     * 跳转类型。
     */
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /**
     * 跳转值预览。
     */
    @ApiModelProperty(value = "跳转值预览")
    private String routeValue;

    /**
     * 预览过程中发现的校验错误。
     */
    @ApiModelProperty(value = "校验错误")
    private List<String> errors;
}
