package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 通知人工标记死信参数。
 *
 * @author Zoro
 * @date 2026/05/14
 */
@ApiModel(description = "通知人工标记死信参数")
@Data
public class NotifyManualDeadDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 处理原因 */
    @ApiModelProperty(value = "处理原因", required = true)
    @NotBlank(message = "处理原因不能为空")
    private String reason;
}
