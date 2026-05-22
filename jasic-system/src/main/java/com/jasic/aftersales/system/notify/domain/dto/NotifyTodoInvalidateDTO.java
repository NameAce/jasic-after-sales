package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 待办失效参数。
 *
 * @author Zoro
 * @date 2026/04/18
 */
@ApiModel(description = "待办失效参数")
@Data
public class NotifyTodoInvalidateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 失效原因 */
    @ApiModelProperty(value = "失效原因")
    private String invalidReason;
}


