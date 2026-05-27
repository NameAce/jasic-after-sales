package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单待派单通知事件参数。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "工单待派单通知事件参数")
@Data
public class NotifyWorkOrderAcceptEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long workOrderId;

    /** 工单号 */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /** 当前承接网点ID */
    @ApiModelProperty(value = "当前承接网点ID")
    private Long currentAcceptCompanyId;

    /** 当前承接网点名称 */
    @ApiModelProperty(value = "当前承接网点名称")
    private String currentAcceptCompanyName;

    /** 客户名称快照 */
    @ApiModelProperty(value = "客户名称快照")
    private String customerName;

    /** 客户联系电话快照 */
    @ApiModelProperty(value = "客户联系电话快照")
    private String customerMobile;
}
