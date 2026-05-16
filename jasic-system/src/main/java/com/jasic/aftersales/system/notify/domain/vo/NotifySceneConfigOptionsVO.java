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
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景配置页元数据")
@Data
public class NotifySceneConfigOptionsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "场景元数据选项")
    private List<NotifySceneMetaOptionVO> sceneOptions = new ArrayList<>();

    @ApiModelProperty(value = "通知目标类型选项")
    private List<NotifyTemplateEnumOptionVO> targetTypeOptions = new ArrayList<>();

    @ApiModelProperty(value = "跳转类型选项")
    private List<NotifyTemplateEnumOptionVO> routeTypeOptions = new ArrayList<>();
}
