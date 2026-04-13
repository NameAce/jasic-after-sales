package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 同步任务新增/修改参数
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "同步任务新增/修改参数")
@Data
public class SyncTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 任务编码 */
    @ApiModelProperty(value = "任务编码", required = true)
    @NotBlank(message = "任务编码不能为空")
    @Size(max = 64, message = "任务编码长度不能超过64个字符")
    private String taskCode;

    /** 任务名称 */
    @ApiModelProperty(value = "任务名称", required = true)
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称长度不能超过128个字符")
    private String taskName;

    /** 处理器编码 */
    @ApiModelProperty(value = "处理器编码", required = true)
    @NotBlank(message = "处理器不能为空")
    @Size(max = 64, message = "处理器编码长度不能超过64个字符")
    private String handlerCode;

    /** Cron 表达式 */
    @ApiModelProperty(value = "Cron 表达式", required = true)
    @NotBlank(message = "Cron表达式不能为空")
    @Size(max = 128, message = "Cron表达式长度不能超过128个字符")
    private String cronExpression;

    /** 状态 */
    @ApiModelProperty(value = "状态（1=启用，0=停用）", required = true)
    @NotNull(message = "任务状态不能为空")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String remark;
}
