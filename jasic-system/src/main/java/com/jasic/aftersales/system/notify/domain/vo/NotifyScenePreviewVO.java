package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景目标预览结果。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标预览结果")
@Data
public class NotifyScenePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**sceneCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "场景编码")
    private String sceneCode;

    /**sceneName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "场景名称")
    private String sceneName;

    /**targetType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "通知目标类型")
    private String targetType;

    /**targetTypeDesc 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "通知目标类型描述")
    private String targetTypeDesc;

    /**title 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "标题")
    private String title;

    /**content 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "内容")
    private String content;

    /**routeType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /**routeValue 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "跳转值")
    private String routeValue;

    /**pagePath 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序页面路径")
    private String pagePath;

    /**fieldMapping 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "小程序字段映射预览")
    private List<NotifyScenePreviewFieldMappingVO> fieldMapping = new ArrayList<>();

    /**errors 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "错误列表")
    private List<String> errors = new ArrayList<>();
}
