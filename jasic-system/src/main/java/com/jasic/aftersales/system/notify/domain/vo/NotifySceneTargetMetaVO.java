package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景目标元数据。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标元数据")
@Data
public class NotifySceneTargetMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**targetType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "通知目标类型")
    private String targetType;

    /**targetTypeDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "通知目标类型描述")
    private String targetTypeDesc;

    /**receiverType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "接收对象类型")
    private String receiverType;

    /**receiverTypeDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "接收对象类型描述")
    private String receiverTypeDesc;

    /**receiverDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "接收对象说明")
    private String receiverDesc;

    /**defaultEnabled 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认是否启用：1启用，0停用")
    private Integer defaultEnabled;

    /**defaultTemplateName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认模板名称")
    private String defaultTemplateName;

    /**defaultTitleTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认标题模板")
    private String defaultTitleTemplate;

    /**defaultContentTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认内容模板")
    private String defaultContentTemplate;

    /**defaultRouteType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认跳转类型")
    private String defaultRouteType;

    /**defaultRouteValueTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认跳转值模板")
    private String defaultRouteValueTemplate;

    /**channelType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "渠道类型")
    private String channelType;

    /**channelTypeDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "渠道类型描述")
    private String channelTypeDesc;

    /**templateId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认小程序模板ID")
    private String templateId;

    /**channelScene 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认小程序场景，B/C")
    private String channelScene;

    /**channelSceneDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认小程序场景描述")
    private String channelSceneDesc;

    /**pagePathTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认小程序页面路径模板")
    private String pagePathTemplate;

    /**fieldMapping 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "默认小程序字段映射")
    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();
}
