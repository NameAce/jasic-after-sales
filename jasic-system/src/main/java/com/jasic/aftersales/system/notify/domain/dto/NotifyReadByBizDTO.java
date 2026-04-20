package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 按业务对象标记已读参数。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "按业务对象标记已读参数")
@Data
public class NotifyReadByBizDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 当前用户ID */
    @ApiModelProperty(value = "当前用户ID")
    private Long receiverId;
}
