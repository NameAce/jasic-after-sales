package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 后台受理反馈参数。
 *
 * @author Codex
 * @date 2026/05/28
 */
@ApiModel(description = "后台受理反馈参数")
@Data
public class SysFeedbackAcceptDTO {

    /** 反馈 ID */
    @ApiModelProperty(value = "反馈ID", required = true)
    @NotNull(message = "反馈ID不能为空")
    private Long id;
}
