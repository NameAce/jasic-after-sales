package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景目标配置保存参数。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标配置保存参数")
@Data
public class NotifySceneTargetConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知目标类型。
     */
    @ApiModelProperty(value = "通知目标类型", required = true)
    private String targetType;

    /**
     * 是否启用：1 启用，0 停用。
     */
    @ApiModelProperty(value = "是否启用：1启用，0停用", required = true)
    private Integer enabled;

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
     * 小程序场景。
     */
    @ApiModelProperty(value = "小程序场景，B/C")
    private String channelScene;

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
     * 备注。
     */
    @ApiModelProperty(value = "备注")
    private String remark;
}
