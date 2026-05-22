package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知消息日志查询参数。
 *
 * @author Zoro
 * @date 2026/04/18
 */
@ApiModel(description = "通知消息日志查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyMessageLogQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @ApiModelProperty(value = "消息ID")
    private Long messageId;
}
