package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 公司新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "公司新增/修改参数")
@Data
public class SysCompanyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "公司ID，修改时必传")
    private Long id;

    @ApiModelProperty(value = "公司名称", required = true)
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    @ApiModelProperty(value = "公司类型编码", required = true)
    @NotBlank(message = "公司类型不能为空")
    private String typeCode;

    @ApiModelProperty(value = "联系人", required = true)
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @ApiModelProperty(value = "联系电话", required = true)
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @ApiModelProperty(value = "公司地址", required = true)
    @NotBlank(message = "公司地址不能为空")
    private String address;

    @ApiModelProperty(value = "省份")
    private String provinceName;

    @ApiModelProperty(value = "城市")
    private String cityName;

    @ApiModelProperty(value = "区县")
    private String districtName;

    @ApiModelProperty(value = "管理员用户名，新增时必填")
    private String adminUsername;

    @ApiModelProperty(value = "经度")
    private BigDecimal longitude;

    @ApiModelProperty(value = "纬度")
    private BigDecimal latitude;

    @ApiModelProperty(value = "客服电话")
    private String servicePhone;

    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    @ApiModelProperty(value = "状态（1=正常，0=停用）")
    private Integer status;

    @ApiModelProperty(value = "备注")
    private String remark;
}
