package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 后台受理 / 修改受理参数。
 *
 * <p>首次受理和修改受理使用同一组入参：
 * 都必须携带反馈 ID 和最新的受理回复内容。
 * 具体是走首次受理还是修改受理，由调用的接口动作决定。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@ApiModel(description = "后台受理/修改受理参数")
@Data
public class SysFeedbackAcceptDTO {

    /** 反馈 ID */
    @ApiModelProperty(value = "反馈ID", required = true)
    @NotNull(message = "反馈ID不能为空")
    private Long id;

    /** 受理回复 */
    @ApiModelProperty(value = "受理回复", required = true)
    @NotBlank(message = "受理回复不能为空")
    @Size(max = 200, message = "受理回复长度不能超过200个字符")
    private String acceptReply;
}
