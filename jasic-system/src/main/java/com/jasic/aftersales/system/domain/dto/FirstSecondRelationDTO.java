package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 一级-二级从属关系新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "一级-二级从属关系新增/修改参数")
@Data
public class FirstSecondRelationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必传） */
    @ApiModelProperty(value = "主键（修改时必传）")
    private Long id;

    /** 目标总部公司ID */
    @ApiModelProperty(value = "目标总部公司ID", required = true)
    private Long targetCompanyId;

    /** 一级网点公司ID */
    @ApiModelProperty(value = "一级网点公司ID")
    private Long firstCompanyId;

    /** 二级网点公司ID */
    @ApiModelProperty(value = "二级网点公司ID")
    private Long secondCompanyId;

    /** 状态（1=有效，0=解除） */
    @ApiModelProperty(value = "状态（1=有效，0=解除）")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
