package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 行政区划下拉选项
 *
 * @author Codex
 * @date 2026/04/17
 */
@ApiModel(description = "行政区划下拉选项")
@Data
public class SysAreaOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "行政区编码")
    private String areaCode;

    @ApiModelProperty(value = "行政区名称")
    private String areaName;

    @ApiModelProperty(value = "父级编码")
    private String parentCode;

    @ApiModelProperty(value = "层级")
    private String areaLevel;

    @ApiModelProperty(value = "是否叶子节点")
    private Boolean leaf;
}
