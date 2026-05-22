package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单网点转单通知事件参数。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "工单网点转单通知事件参数")
@Data
public class NotifyWorkOrderTransferNoticeEventDTO implements Serializable {

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

    /** 转入后的当前处理网点ID */
    @ApiModelProperty(value = "转入后的当前处理网点ID")
    private Long toCompanyId;

    /** 转入后的当前处理网点名称 */
    @ApiModelProperty(value = "转入后的当前处理网点名称")
    private String toCompanyName;

    /** 转入后的当前处理网点联系电话 */
    @ApiModelProperty(value = "转入后的当前处理网点联系电话")
    private String toCompanyPhone;

    /** 固定提示文案 */
    @ApiModelProperty(value = "固定提示文案")
    private String transferTip;

    /** 转单次数快照 */
    @ApiModelProperty(value = "转单次数快照")
    private Integer transferCount;
}
