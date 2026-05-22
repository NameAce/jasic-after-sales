package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 行政区划下拉选项
 *
 * @author Zoro
 * @date 2026/04/17
 */
@ApiModel(description = "行政区划下拉选项")
@Data
public class SysAreaOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**areaCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "行政区编码")
    private String areaCode;

    /**areaName 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "行政区名称")
    private String areaName;

    /**parentCode 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "父级编码")
    private String parentCode;

    /**areaLevel 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "层级")
    private String areaLevel;

    /**leaf 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "是否叶子节点")
    private Boolean leaf;
}
