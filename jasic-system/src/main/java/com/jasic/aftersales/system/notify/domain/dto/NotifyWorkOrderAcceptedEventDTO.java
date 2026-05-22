package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单接单成功提醒事件参数。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "工单接单成功提醒事件参数")
@Data
public class NotifyWorkOrderAcceptedEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID")
    private Long workOrderId;

    /** 工单号 */
    @ApiModelProperty(value = "工单号")
    private String orderNo;

    /** 客户ID */
    @ApiModelProperty(value = "客户ID")
    private Long customerId;

    /** 客户openid */
    @ApiModelProperty(value = "客户openid")
    private String customerOpenid;

    /** 当前服务网点ID */
    @ApiModelProperty(value = "当前服务网点ID")
    private Long companyId;

    /** 当前服务网点名称 */
    @ApiModelProperty(value = "当前服务网点名称")
    private String companyName;

    /** 当前服务网点联系电话 */
    @ApiModelProperty(value = "当前服务网点联系电话")
    private String companyPhone;
}
