package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 通知模板启停参数。
 *
 * <p>该对象专门服务模板启用和停用接口，
 * 用于把模板内容编辑和模板状态流转分开，避免编辑接口顺带修改状态造成语义混乱。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板启停参数")
@Data
public class NotifyTemplateStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态：1 启用，0 停用。
     */
    @ApiModelProperty(value = "状态：1启用，0停用", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;
}
