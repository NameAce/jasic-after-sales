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
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景元数据选项")
@Data
public class NotifySceneMetaOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**sceneCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "场景编码")
    private String sceneCode;

    /**sceneName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "场景名称")
    private String sceneName;

    /**bizType 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /**eventCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "事件编码")
    private String eventCode;

    /**variables 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "可用变量")
    private List<NotifyTemplateVariableMeta> variables = new ArrayList<>();

    /**targetMetas 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "支持的通知目标元数据")
    private List<NotifySceneTargetMetaVO> targetMetas = new ArrayList<>();
}
