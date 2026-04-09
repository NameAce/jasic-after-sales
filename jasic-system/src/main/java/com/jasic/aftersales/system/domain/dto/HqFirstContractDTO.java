package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 总部-一级签约新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "总部-一级签约新增/修改参数")
@Data
public class HqFirstContractDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必传） */
    @ApiModelProperty(value = "主键（修改时必传）")
    private Long id;

    /** 总部公司ID */
    @ApiModelProperty(value = "总部公司ID")
    private Long hqCompanyId;

    /** 一级网点公司ID */
    @ApiModelProperty(value = "一级网点公司ID")
    private Long firstCompanyId;

    /** 大区ID */
    @ApiModelProperty(value = "大区ID")
    private Long regionId;

    /** 合同编号 */
    @ApiModelProperty(value = "合同编号")
    private String contractNo;

    /** 状态（1=有效，0=终止） */
    @ApiModelProperty(value = "状态（1=有效，0=终止）")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
