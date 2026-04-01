package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 条码档案查询参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MachineBarcodeQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 机器条码 */
    private String barcode;

    /** 归属总部ID */
    private Long hqCompanyId;

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 品牌编码 */
    private String brandCode;

    /** 质保状态 */
    private String warrantyStatus;

    /** 状态 */
    private Integer status;
}
