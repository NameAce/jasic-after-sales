package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.support.NotifyTemplateVariableMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景元数据选项。
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景元数据选项")
@Data
public class NotifySceneMetaOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "场景编码")
    private String sceneCode;

    @ApiModelProperty(value = "场景名称")
    private String sceneName;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "事件编码")
    private String eventCode;

    @ApiModelProperty(value = "可用变量")
    private List<NotifyTemplateVariableMeta> variables = new ArrayList<>();

    @ApiModelProperty(value = "支持的通知目标元数据")
    private List<NotifySceneTargetMetaVO> targetMetas = new ArrayList<>();
}
