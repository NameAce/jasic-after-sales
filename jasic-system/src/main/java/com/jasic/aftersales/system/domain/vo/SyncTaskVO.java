package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步任务返回对象
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "同步任务返回对象")
@Data
public class SyncTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 任务编码 */
    @ApiModelProperty(value = "任务编码")
    private String taskCode;

    /** 任务名称 */
    @ApiModelProperty(value = "任务名称")
    private String taskName;

    /** 处理器编码 */
    @ApiModelProperty(value = "处理器编码")
    private String handlerCode;

    /** 处理器名称 */
    @ApiModelProperty(value = "处理器名称")
    private String handlerName;

    /** Cron 表达式 */
    @ApiModelProperty(value = "Cron表达式")
    private String cronExpression;

    /** 状态 */
    @ApiModelProperty(value = "状态（1=启用，0=停用）")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 最近执行状态 */
    @ApiModelProperty(value = "最近执行状态")
    private String lastStatus;

    /** 最近开始时间 */
    @ApiModelProperty(value = "最近开始时间")
    private LocalDateTime lastStartTime;

    /** 最近结束时间 */
    @ApiModelProperty(value = "最近结束时间")
    private LocalDateTime lastEndTime;

    /** 最近执行信息 */
    @ApiModelProperty(value = "最近执行信息")
    private String lastMessage;

    /** 下一次触发时间 */
    @ApiModelProperty(value = "下一次触发时间")
    private LocalDateTime nextFireTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
