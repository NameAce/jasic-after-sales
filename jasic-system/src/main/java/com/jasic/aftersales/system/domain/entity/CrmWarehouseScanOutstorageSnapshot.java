package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * CRM 销售出库扫码快照实体。
 *
 * <p>快照层按明细保留外部原始扫码记录，后续再基于本地快照按条码聚合最早扫码时间，
 * 投影到本地条码档案的销售最后出库日期字段。</p>
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_warehouse_scan_outstorage_snapshot")
public class CrmWarehouseScanOutstorageSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** CRM 原始主键，对应 {@code scan_outstorage_id}。 */
    private Long sourceId;

    /** CRM 出入库单据ID，对应 {@code ware_id}。 */
    private Long wareId;

    /** CRM 仓库ID。 */
    private Long warehouseId;

    /** 外部扫码条码，对应本地 {@code machine_barcode.barcode}。 */
    private String scanCode;

    /** 扫码时间，用于聚合最早销售出库日期。 */
    private LocalDateTime scanDate;

    /** CRM 企业ID，当前仅随明细保存，不参与本期投影规则。 */
    private Long custId;

    /** CRM 产品编码，当前仅随明细保存。 */
    private String productNumeric;

    /** 本地最近一次同步该明细的时间。 */
    private LocalDateTime lastSyncTime;
}
