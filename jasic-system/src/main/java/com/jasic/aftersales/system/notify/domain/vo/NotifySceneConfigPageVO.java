package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景配置列表行。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景配置列表行")
@Data
public class NotifySceneConfigPageVO implements Serializable {

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

    /**状态，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "场景状态：1启用，0停用")
    private Integer status;

    /**enabledTargetTypes 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "已启用目标类型")
    private List<String> enabledTargetTypes = new ArrayList<>();

    /**enabledTargetTypeDescs 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "已启用目标类型描述")
    private List<String> enabledTargetTypeDescs = new ArrayList<>();

    /**更新时间，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
