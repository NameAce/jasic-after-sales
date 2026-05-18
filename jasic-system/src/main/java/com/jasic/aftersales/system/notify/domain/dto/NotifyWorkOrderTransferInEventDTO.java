package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单转入通知事件参数。
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "工单转入通知事件参数")
@Data
public class NotifyWorkOrderTransferInEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long workOrderId;

    /** 工单号 */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /** 转入后的当前承接网点ID */
    @ApiModelProperty(value = "转入后的当前承接网点ID")
    private Long currentAcceptCompanyId;

    /** 转入后的当前承接网点名称 */
    @ApiModelProperty(value = "转入后的当前承接网点名称")
    private String currentAcceptCompanyName;

    /** 客户名称快照 */
    @ApiModelProperty(value = "客户名称快照")
    private String customerName;

    /** 客户联系电话快照 */
    @ApiModelProperty(value = "客户联系电话快照")
    private String customerMobile;

    /** 转出网点ID */
    @ApiModelProperty(value = "转出网点ID")
    private Long fromCompanyId;

    /** 转出网点名称 */
    @ApiModelProperty(value = "转出网点名称")
    private String fromCompanyName;

    /** 转单次数快照 */
    @ApiModelProperty(value = "转单次数快照")
    private Integer transferCount;
}
