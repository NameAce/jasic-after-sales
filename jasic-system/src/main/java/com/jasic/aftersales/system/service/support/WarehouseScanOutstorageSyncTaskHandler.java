package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.vo.CrmWarehouseScanOutstorageSyncSummaryVO;
import com.jasic.aftersales.system.service.ICrmWarehouseScanOutstorageSyncService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * CRM 销售出库扫码同步处理器。
 *
 * <p>该处理器不自行计算时间窗口，而是调用基于主键游标的增量同步服务。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Component
public class WarehouseScanOutstorageSyncTaskHandler implements SyncTaskHandler {

    public static final String HANDLER_CODE = "warehouseScanOutstorageSync";
    private static final String HANDLER_NAME = "销售出库扫码同步";

    /**
     * ?????
     *
     * @return ?????
     */
    @Resource
    private ICrmWarehouseScanOutstorageSyncService crmWarehouseScanOutstorageSyncService;

    @Override
    public String getCode() {
        return HANDLER_CODE;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    /**
     * ?????
     *
     * @param task ????
     * @param context ?????
     * @return ????
     */
    @Override
    public SyncTaskExecutionResult execute(SyncTask task, SyncTaskExecutionContext context) {
        CrmWarehouseScanOutstorageSyncSummaryVO summary = crmWarehouseScanOutstorageSyncService.syncIncremental();
        return SyncTaskExecutionResult.builder()
                .dataStartTime(context.getLastSuccessEndTime())
                .dataEndTime(context.getExecutionTime())
                .message(String.format("销售出库扫码同步完成：明细 %d 条，影响条码 %d 条，更新本地条码 %d 条，未匹配 %d 条",
                        defaultInt(summary.getSyncedDetailCount()),
                        defaultInt(summary.getAffectedBarcodeCount()),
                        defaultInt(summary.getUpdatedMachineBarcodeCount()),
                        defaultInt(summary.getUnmatchedBarcodeCount())))
                .build();
    }

    /**
     * ??????
     *
     * @param value ???
     * @return ????
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
