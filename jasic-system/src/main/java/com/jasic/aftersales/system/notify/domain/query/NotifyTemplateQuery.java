package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板分页查询参数。
 *
 * <p>该对象只描述后台模板配置页的筛选条件，
 * 重构后筛选维度统一收口为通知场景、通知类型、模板名称和状态，
 * 不再接受历史组合字段。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板分页查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyTemplateQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /**
     * 通知场景编码。
     */
    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    /**
     * 模板名称。
     */
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    /**
     * 通知类型编码。
     */
    @ApiModelProperty(value = "通知类型")
    private String notifyType;

    /**
     * 状态。
     */
    @ApiModelProperty(value = "状态：1启用，0停用")
    private Integer status;
}

