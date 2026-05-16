package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景目标配置详情。
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标配置详情")
@Data
public class NotifySceneTargetConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "通知目标类型")
    private String targetType;

    @ApiModelProperty(value = "通知目标类型描述")
    private String targetTypeDesc;

    @ApiModelProperty(value = "接收对象类型")
    private String receiverType;

    @ApiModelProperty(value = "接收对象类型描述")
    private String receiverTypeDesc;

    @ApiModelProperty(value = "接收对象说明")
    private String receiverDesc;

    @ApiModelProperty(value = "渠道类型")
    private String channelType;

    @ApiModelProperty(value = "渠道类型描述")
    private String channelTypeDesc;

    @ApiModelProperty(value = "是否启用：1启用，0停用")
    private Integer enabled;

    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    @ApiModelProperty(value = "内容模板")
    private String contentTemplate;

    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    @ApiModelProperty(value = "跳转值模板")
    private String routeValueTemplate;

    @ApiModelProperty(value = "小程序订阅消息模板ID")
    private String templateId;

    @ApiModelProperty(value = "小程序页面路径模板")
    private String pagePathTemplate;

    @ApiModelProperty(value = "小程序字段映射")
    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();

    @ApiModelProperty(value = "备注")
    private String remark;
}
