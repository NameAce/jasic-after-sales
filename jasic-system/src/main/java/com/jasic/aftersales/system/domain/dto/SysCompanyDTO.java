package com.jasic.aftersales.system.domain.dto;

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
@Data
public class SysCompanyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司ID（修改时必传） */
    private Long id;

    /** 公司名称 */
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    /** 公司编码 */
    @NotBlank(message = "公司编码不能为空")
    private String companyCode;

    /** 公司类型编码 */
    @NotBlank(message = "公司类型不能为空")
    private String typeCode;

    /** 联系人 */
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    /** 公司地址 */
    @NotBlank(message = "公司地址不能为空")
    private String address;

    /** 管理员用户名（新增时必填） */
    private String adminUsername;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 状态（1=正常，0=停用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
