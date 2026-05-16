package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通知模板配置页元数据返回对象。
 *
 * <p>该对象集中返回新增模板所需的通知场景元数据和渠道类型选项。
 * 它只服务后台配置页，不参与发送链路的模板命中或分发构建。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板配置页元数据")
@Data
public class NotifyTemplateOptionsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 可维护通知场景列表。
     */
    @ApiModelProperty(value = "通知场景选项")
    private List<NotifySceneOptionVO> sceneOptions;

    /**
     * 渠道类型选项。
     */
    @ApiModelProperty(value = "渠道类型选项")
    private List<NotifyTemplateEnumOptionVO> channelTypeOptions;
}

