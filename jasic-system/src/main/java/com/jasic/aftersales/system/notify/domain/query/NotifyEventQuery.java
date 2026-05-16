package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知事件查询参数。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "通知事件查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyEventQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 幂等键 */
    @ApiModelProperty(value = "幂等键")
    private String eventKey;

    /** 事件类型 */
    @ApiModelProperty(value = "事件类型")
    private String eventType;

    /** 通知场景编码 */
    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 接收人ID */
    @ApiModelProperty(value = "接收人ID")
    private Long receiverId;

    /** 事件状态 */
    @ApiModelProperty(value = "事件状态")
    private String status;
}
