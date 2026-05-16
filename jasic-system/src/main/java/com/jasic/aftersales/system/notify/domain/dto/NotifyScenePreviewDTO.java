package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知场景目标预览参数。
 *
 * <p>预览接口允许直接携带当前页面尚未保存的目标配置，
 * 这样后台维护时可以先校验渲染结果，再决定是否保存。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标预览参数")
@Data
public class NotifyScenePreviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 场景编码。
     */
    @ApiModelProperty(value = "场景编码", required = true)
    private String sceneCode;

    /**
     * 通知目标类型。
     */
    @ApiModelProperty(value = "通知目标类型", required = true)
    private String targetType;

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
     * 小程序订阅消息模板 ID。
     */
    @ApiModelProperty(value = "小程序订阅消息模板ID")
    private String templateId;

    /**
     * 小程序页面路径模板。
     */
    @ApiModelProperty(value = "小程序页面路径模板")
    private String pagePathTemplate;

    /**
     * 小程序字段映射。
     */
    @ApiModelProperty(value = "小程序字段映射")
    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();

    /**
     * 预览变量。
     */
    @ApiModelProperty(value = "预览变量")
    private Map<String, Object> variables = new LinkedHashMap<>();
}
