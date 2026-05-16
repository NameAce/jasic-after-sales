package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 通知模板渠道配置参数。
 *
 * <p>该对象描述 `sys_notify_template_channel` 的单条配置。
 * 本次重构后渠道配置改为按 `sceneCode` 维护，`sceneCode` 本身由接口路径承载，
 * DTO 只保留单条渠道记录自己的启停状态和配置内容。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板渠道配置参数")
@Data
public class NotifyTemplateChannelDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 渠道类型编码。
     */
    @ApiModelProperty(value = "渠道类型", required = true)
    @NotBlank(message = "渠道类型不能为空")
    private String channelType;

    /**
     * 渠道启停状态。
     */
    @ApiModelProperty(value = "渠道状态：1启用，0停用", required = true)
    @NotNull(message = "渠道状态不能为空")
    private Integer channelEnabled;

    /**
     * 第三方模板 ID。
     */
    @ApiModelProperty(value = "小程序订阅消息模板ID")
    private String templateId;

    /**
     * 小程序场景。
     */
    @ApiModelProperty(value = "小程序场景，B/C")
    private String channelScene;

    /**
     * 页面路径模板。
     */
    @ApiModelProperty(value = "页面路径模板")
    private String pagePathTemplate;

    /**
     * 字段映射。
     */
    @ApiModelProperty(value = "字段映射")
    private List<NotifyChannelFieldMappingDTO> fieldMapping;

    /**
     * 原始配置 JSON。
     */
    @ApiModelProperty(value = "原始配置JSON")
    private String configJson;

    /**
     * 备注。
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
