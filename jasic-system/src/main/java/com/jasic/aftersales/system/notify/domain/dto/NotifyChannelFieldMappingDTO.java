package com.jasic.aftersales.system.notify.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Notify channel field mapping DTO.
 *
 * @author Codex
 * @date 2026/04/21
 */
@ApiModel(description = "Notify channel field mapping DTO")
@Data
public class NotifyChannelFieldMappingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "Channel template field")
    private String field;

    @ApiModelProperty(value = "Variable expression or fixed text")
    private String value;
}
