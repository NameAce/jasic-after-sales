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
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标配置详情")
@Data
public class NotifySceneTargetConfigVO implements Serializable {

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

    /**channelType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "渠道类型")
    private String channelType;

    /**channelTypeDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "渠道类型描述")
    private String channelTypeDesc;

    /**enabled 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否启用：1启用，0停用")
    private Integer enabled;

    /**titleTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    /**contentTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "内容模板")
    private String contentTemplate;

    /**routeType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /**routeValueTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "跳转值模板")
    private String routeValueTemplate;

    /**templateId 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序订阅消息模板ID")
    private String templateId;

    /**channelScene 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序场景，B/C")
    private String channelScene;

    /**channelSceneDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序场景描述")
    private String channelSceneDesc;

    /**pagePathTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序页面路径模板")
    private String pagePathTemplate;

    /**fieldMapping 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序字段映射")
    private List<NotifyChannelFieldMappingDTO> fieldMapping = new ArrayList<>();

    /**备注，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "备注")
    private String remark;
}
