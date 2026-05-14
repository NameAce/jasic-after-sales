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
     * CRM仓库扫描出库同步服务服务依赖。
     *
     * @return 处理结果
     */
    @Resource
    private ICrmWarehouseScanOutstorageSyncService crmWarehouseScanOutstorageSyncService;

    /**
     * 获取Code相关数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return HANDLER_CODE;
    }

    /**
     * 获取仓库扫描出库同步任务名称。
     *
     * @return 处理结果
     */
    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    /**
     * execute。
     *
     * @param task 参数
     * @param context 参数
     * @return 处理结果
     */
    @Override
    public SyncTaskExecutionResult execute(SyncTask task, SyncTaskExecutionContext context) {
        // 调用syncIncremental方法，复用统一能力并保证业务规则一致。
        CrmWarehouseScanOutstorageSyncSummaryVO summary = crmWarehouseScanOutstorageSyncService.syncIncremental();
        return SyncTaskExecutionResult.builder()
                .dataStartTime(context.getLastSuccessEndTime())
                .dataEndTime(context.getExecutionTime())
                .message(String.format("销售出库扫码同步完成：明细 %d 条，影响条码 %d 条，更新本地条码 %d 条，未匹配 %d 条",
                        defaultInt(summary.getSyncedDetailCount()),
                        defaultInt(summary.getAffectedBarcodeCount()),
                        defaultInt(summary.getUpdatedMachineBarcodeCount()),
                        defaultInt(summary.getUnmatchedBarcodeCount())))
                // 调用build方法，复用统一能力并保证业务规则一致。
                .build();
    }

    /**
     * defaultInt。
     *
     * @param value 参数
     * @return 处理结果
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}


