package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 公司地址簿新增参数。
 *
 * @author Codex
 * @date 2026/04/11
 */
@ApiModel(description = "公司地址簿新增参数")
@Data
public class CompanyAddressCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
}
