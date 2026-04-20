package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 待办完成参数。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "待办完成参数")
@Data
public class NotifyTodoCompleteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 接收人ID */
    @ApiModelProperty(value = "接收人ID")
    private Long receiverId;

    /** 完成动作编码 */
    @ApiModelProperty(value = "完成动作编码")
    private String actionCode;
}
