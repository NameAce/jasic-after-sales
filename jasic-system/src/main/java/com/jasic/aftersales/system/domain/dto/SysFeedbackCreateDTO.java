package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 反馈提交参数。
 *
 * <p>该参数对象同时适用于终端用户和网点用户提交反馈，
 * 字段层只保留两端共用的业务输入，提交主体、来源快照和归属总部由后端根据登录态和业务规则自动解析。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@ApiModel(description = "反馈提交参数")
@Data
public class SysFeedbackCreateDTO {

    /** 反馈内容 */
    @ApiModelProperty(value = "反馈内容", required = true)
    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 500, message = "反馈内容长度不能超过500个字符")
    private String content;
}
