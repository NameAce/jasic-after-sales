package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知场景配置分页查询参数。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景配置分页查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifySceneConfigQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /**
     * 场景名称。
     */
    @ApiModelProperty(value = "场景名称")
    private String sceneName;

    /**
     * 场景编码。
     */
    @ApiModelProperty(value = "场景编码")
    private String sceneCode;

    /**
     * 业务类型。
     */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /**
     * 通知目标类型。
     */
    @ApiModelProperty(value = "通知目标类型")
    private String targetType;
}
