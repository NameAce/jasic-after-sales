package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.vo.CrmWarehouseScanOutstorageSyncSummaryVO;

/**
 * CRM 销售出库扫码同步服务接口。
 *
 * <p>该服务包含两层动作：</p>
 * <ul>
 *     <li>先将 {@code saas_warehouse_scan_outstorage} 明细落入本地快照表；</li>
 *     <li>再按条码聚合最早扫码时间，回写本地 {@code machine_barcode.dealer_out_date}。</li>
 * </ul>
 *
 * @author Codex
 * @date 2026/04/12
 */
public interface ICrmWarehouseScanOutstorageSyncService {

    /**
     * 执行销售出库扫码增量同步。
     *
     * <p>同步会保留 CRM 原始明细，不自动新建本地条码档案。若快照中的条码在本地未匹配，
     * 仅记录未匹配数量。</p>
     *
     * @return 同步摘要
     */
    CrmWarehouseScanOutstorageSyncSummaryVO syncIncremental();
}
