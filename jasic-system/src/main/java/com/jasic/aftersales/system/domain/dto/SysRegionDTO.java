package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 大区新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "大区新增/修改参数")
@Data
public class SysRegionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必传） */
    @ApiModelProperty(value = "主键（修改时必传）")
    private Long id;

    /** 目标总部公司ID */
    @ApiModelProperty(value = "目标总部公司ID")
    private Long targetCompanyId;

    /** 大区名称 */
    @ApiModelProperty(value = "大区名称")
    private String regionName;

    /** 大区编码 */
    @ApiModelProperty(value = "大区编码")
    private String regionCode;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
