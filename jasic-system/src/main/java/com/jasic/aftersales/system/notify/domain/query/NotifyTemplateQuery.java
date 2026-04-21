package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Notification template query.
 *
 * @author Codex
 * @date 2026/04/20
 */
@ApiModel(description = "通知模板查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyTemplateQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "模板编码")
    private String templateCode;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "模板来源")
    private String templateSource;

    @ApiModelProperty(value = "事件类型")
    private String eventType;
}
