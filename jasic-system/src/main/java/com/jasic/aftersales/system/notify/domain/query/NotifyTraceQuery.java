package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知记录排障查询参数。
 *
 * @author Zoro
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

    /** 通知场景编码 */
    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    /** 通知目标类型 */
    @ApiModelProperty(value = "通知目标类型")
    private String targetType;

    /** 事件状态 */
    @ApiModelProperty(value = "事件状态")
    private String eventStatus;

    /** 外部分发状态 */
    @ApiModelProperty(value = "外部分发状态")
    private String dispatchStatus;

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
