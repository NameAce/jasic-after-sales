package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景配置页元数据。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景配置页元数据")
@Data
public class NotifySceneConfigOptionsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**sceneOptions 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "场景元数据选项")
    private List<NotifySceneMetaOptionVO> sceneOptions = new ArrayList<>();

    /**targetTypeOptions 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "通知目标类型选项")
    private List<NotifyTemplateEnumOptionVO> targetTypeOptions = new ArrayList<>();

    /**channelSceneOptions 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序场景选项")
    private List<NotifyTemplateEnumOptionVO> channelSceneOptions = new ArrayList<>();

    /**routeTypeOptions 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "跳转类型选项")
    private List<NotifyTemplateEnumOptionVO> routeTypeOptions = new ArrayList<>();
}
