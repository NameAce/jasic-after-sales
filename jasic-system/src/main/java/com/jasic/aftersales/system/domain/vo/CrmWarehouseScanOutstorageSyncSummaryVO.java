package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * CRM 销售出库扫码同步摘要
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Data
public class CrmWarehouseScanOutstorageSyncSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 同步明细数量 */
    private Integer syncedDetailCount;

    /** 影响条码数量 */
    private Integer affectedBarcodeCount;

    /** 更新本地条码数量 */
    private Integer updatedMachineBarcodeCount;

    /** 未匹配本地条码数量 */
    private Integer unmatchedBarcodeCount;

    /** 最近同步的源主键 */
    private Long latestSourceId;
}
