package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * C 端条码信息
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class CustomerBarcodeInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 机器条码 */
    private String barcode;

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 品牌编码 */
    private String brandCode;

    /** 质保状态 */
    private String warrantyStatus;

    /** 归属总部ID */
    private Long hqCompanyId;

    /** 归属总部名称 */
    private String hqCompanyName;
}
