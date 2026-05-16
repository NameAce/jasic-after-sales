package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.support.NotifyTemplateVariableMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景配置详情。
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景配置详情")
@Data
public class NotifySceneConfigDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "场景编码")
    private String sceneCode;

    @ApiModelProperty(value = "场景名称")
    private String sceneName;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "事件编码")
    private String eventCode;

    @ApiModelProperty(value = "场景状态：1启用，0停用")
    private Integer status;

    @ApiModelProperty(value = "场景备注")
    private String remark;

    @ApiModelProperty(value = "可用变量")
    private List<NotifyTemplateVariableMeta> variables = new ArrayList<>();

    @ApiModelProperty(value = "目标配置列表")
    private List<NotifySceneTargetConfigVO> targetConfigs = new ArrayList<>();
}
