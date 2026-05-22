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
 * @author Zoro
 * @date 2026/04/12
 */
@Component
public class WarehouseScanOutstorageSyncTaskHandler implements SyncTaskHandler {

    /**HANDLER_CODE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String HANDLER_CODE = "warehouseScanOutstorageSync";
    /**HANDLER_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String HANDLER_NAME = "销售出库扫码同步";

    /**
     * CRM仓库扫描出库同步服务服务依赖。
     *
     * @return 业务处理结果
     */
    @Resource
    private ICrmWarehouseScanOutstorageSyncService crmWarehouseScanOutstorageSyncService;

    /**
     * 获取Code相关数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
    @Override
    public String getCode() {
        return HANDLER_CODE;
    }

    /**
     * 获取仓库扫描出库同步任务名称。
     *
     * @return 业务处理结果
     */
    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    /**
     * execute。
     *
     * @param task task，当前业务处理所需的输入值。
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @return 业务处理结果
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
     * defaultInt。
     *
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}


