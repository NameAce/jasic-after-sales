package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端服务网点选项
 *
 * @author Zoro
 * @date 2026/04/01
 */
@ApiModel(description = "C端服务网点选项")
@Data
public class CustomerServiceCompanyOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司ID */
    @ApiModelProperty(value = "公司ID")
    private Long id;

    /** 公司名称 */
    @ApiModelProperty(value = "公司名称")
    private String companyName;

    /** 公司编码 */
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /** 公司类型编码 */
    @ApiModelProperty(value = "公司类型编码")
    private String typeCode;

    /** 公司类型名称 */
    @ApiModelProperty(value = "公司类型名称")
    private String typeName;

    /** 联系电话 */
    @ApiModelProperty(value = "联系电话")
    private String contactPhone;

    /** 地址 */
    @ApiModelProperty(value = "地址")
    private String address;
}
