package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步任务日志返回对象
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "同步任务日志返回对象")
@Data
public class SyncTaskLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 任务ID */
    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    /** 任务名称 */
    @ApiModelProperty(value = "任务名称")
    private String taskName;

    /** 执行状态 */
    @ApiModelProperty(value = "执行状态")
    private String status;

    /** 开始时间 */
    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    /** 结束时间 */
    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    /** 数据开始时间 */
    @ApiModelProperty(value = "数据开始时间")
    private LocalDateTime dataStartTime;

    /** 数据结束时间 */
    @ApiModelProperty(value = "数据结束时间")
    private LocalDateTime dataEndTime;

    /** 执行信息 */
    @ApiModelProperty(value = "执行信息")
    private String message;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}
