package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 公司地址簿修改参数。
 *
 * @author Codex
 * @date 2026/04/11
 */
@ApiModel(description = "公司地址簿修改参数")
@Data
public class CompanyAddressUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 地址ID */
    @ApiModelProperty(value = "地址ID", required = true)
    @NotNull(message = "地址ID不能为空")
    private Long id;

    /** 联系人 */
    @ApiModelProperty(value = "联系人", required = true)
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /** 联系电话 */
    @ApiModelProperty(value = "联系电话", required = true)
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /** 详细地址 */
    @ApiModelProperty(value = "详细地址", required = true)
    @NotBlank(message = "详细地址不能为空")
    private String address;

    /** 是否默认地址（1=是，0=否） */
    @ApiModelProperty(value = "是否默认地址（1=是，0=否）")
    private Integer isDefault;

    /** 平台维护公司级数据时指定的目标公司ID */
    @ApiModelProperty(value = "目标公司ID，平台维护公司级数据时必填")
    private Long targetCompanyId;
}
