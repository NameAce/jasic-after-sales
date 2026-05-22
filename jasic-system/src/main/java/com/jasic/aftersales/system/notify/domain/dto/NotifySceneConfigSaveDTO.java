package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通知场景配置保存参数。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景配置保存参数")
@Data
public class NotifySceneConfigSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 场景状态：1 启用，0 停用。
     */
    @ApiModelProperty(value = "场景状态：1启用，0停用", required = true)
    private Integer status;

    /**
     * 场景备注。
     */
    @ApiModelProperty(value = "场景备注")
    private String remark;

    /**
     * 当前场景下的全部目标配置。
     */
    @ApiModelProperty(value = "当前场景下的全部目标配置")
    private List<NotifySceneTargetConfigDTO> targetConfigs = new ArrayList<>();
}
