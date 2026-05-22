package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

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

    /**主键ID，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "公司ID，修改时必传")
    private Long id;

    /**companyName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "公司名称", required = true)
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    /**companyShortName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "公司简称")
    private String companyShortName;

    /**companyCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "公司编码")
    private String companyCode;

    /**typeCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "公司类型编码", required = true)
    @NotBlank(message = "公司类型不能为空")
    private String typeCode;

    /**contactName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "联系人", required = true)
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /**contactPhone 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "联系电话", required = true)
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /**provinceCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "省份编码", required = true)
    @NotBlank(message = "省份不能为空")
    private String provinceCode;

    /**provinceName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "省份名称")
    private String provinceName;

    /**cityCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "城市编码", required = true)
    @NotBlank(message = "城市不能为空")
    private String cityCode;

    /**cityName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "城市名称")
    private String cityName;

    /**districtCode 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "区县编码", required = true)
    @NotBlank(message = "区县不能为空")
    private String districtCode;

    /**districtName 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "区县名称")
    private String districtName;

    /**detailAddress 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "详细地址", required = true)
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

    /**adminUsername 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "管理员用户名，新增时必填")
    private String adminUsername;

    /**servicePhone 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "客服电话")
    private String servicePhone;

    /**sourceType 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    /**salesOrg 字段，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "销售组织")
    private String salesOrg;

    /**状态，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "状态（1=正常，0=停用）")
    private Integer status;

    /**备注，由接口调用方提交并参与服务层业务校验。*/
    @ApiModelProperty(value = "备注")
    private String remark;
}
