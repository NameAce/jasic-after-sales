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
 * <p>当前兼容接口的保存单位已经从“按 sceneCode 单条渠道配置”
 * 升级为“按 sceneCode 下的多个外部通知目标整体覆盖保存”。</p>
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
     * 通知目标类型编码。
     */
    @ApiModelProperty(value = "通知目标类型编码")
    private String targetType;

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
     * 小程序订阅消息模板ID。
     */
    @ApiModelProperty(value = "小程序订阅消息模板ID")
    private String templateId;

    /**
     * 小程序场景，B/C。
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
     * 原始配置JSON。
     */
    @ApiModelProperty(value = "原始配置JSON")
    private String configJson;

    /**
     * 备注。
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
