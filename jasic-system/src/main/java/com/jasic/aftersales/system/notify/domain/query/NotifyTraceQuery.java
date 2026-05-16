package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知记录排障查询参数。
 *
 * @author Codex
 * @date 2026/05/14
 */
@ApiModel(description = "通知记录排障查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyTraceQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 业务编号 */
    @ApiModelProperty(value = "业务编号")
    private String bizNo;

    /** 事件类型 */
    @ApiModelProperty(value = "事件类型")
    private String eventType;

    /** 通知场景编码 */
    @ApiModelProperty(value = "通知场景编码")
    private String templateCode;

    /** 渠道类型 */
    @ApiModelProperty(value = "渠道类型")
    private String channelType;

    /** 接收对象类型 */
    @ApiModelProperty(value = "接收对象类型")
    private String receiverType;

    /** 接收对象ID */
    @ApiModelProperty(value = "接收对象ID")
    private Long receiverId;

    /** 当前状态 */
    @ApiModelProperty(value = "当前状态")
    private String status;

    /** 结果编码 */
    @ApiModelProperty(value = "结果编码")
    private String resultCode;

    /** 开始时间 */
    @ApiModelProperty(value = "开始时间，格式 yyyy-MM-dd HH:mm:ss")
    private String beginTime;

    /** 结束时间 */
    @ApiModelProperty(value = "结束时间，格式 yyyy-MM-dd HH:mm:ss")
    private String endTime;
}
