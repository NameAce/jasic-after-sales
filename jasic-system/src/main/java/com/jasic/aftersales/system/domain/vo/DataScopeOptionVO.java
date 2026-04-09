package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 数据范围选项
 *
 * @author Zoro
 * @date 2026/03/25
 */
@ApiModel(description = "数据范围选项")
@Data
public class DataScopeOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 选项值 */
    @ApiModelProperty(value = "选项值")
    private String value;

    /** 展示文案 */
    @ApiModelProperty(value = "展示文案")
    private String label;

    /** 是否默认选项 */
    @ApiModelProperty(value = "是否默认选项")
    private Boolean defaultOption;
}
