package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * C 端服务网点选项
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class CustomerServiceCompanyOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司ID */
    private Long id;

    /** 公司名称 */
    private String companyName;

    /** 公司编码 */
    private String companyCode;

    /** 公司类型编码 */
    private String typeCode;

    /** 公司类型名称 */
    private String typeName;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 距离（公里，保留两位小数） */
    private BigDecimal distanceKm;
}
