package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractSyncSummaryVO;
import com.jasic.aftersales.system.service.ICrmHqFirstContractSnapshotService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * CRM 签约快照同步处理器。
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Component
public class CrmHqFirstContractSnapshotSyncTaskHandler implements SyncTaskHandler {

    /**HANDLER_CODE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String HANDLER_CODE = "crmHqFirstContractSnapshotSync";
    /**HANDLER_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String HANDLER_NAME = "CRM签约快照同步";

    /**
     * CRM总部一级合同快照服务服务依赖。
     *
     * @return 业务处理结果
     */
    @Resource
    private ICrmHqFirstContractSnapshotService crmHqFirstContractSnapshotService;

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
     * 获取CRM总部一级合同快照同步任务名称。
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
        LocalDateTime earliestChangeTime = crmHqFirstContractSnapshotService.getEarliestChangeTime();
        if (earliestChangeTime == null) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(null)
                    .dataEndTime(context.getExecutionTime())
                    .message("未查询到可同步的 CRM 签约数据")
                    .build();
        }

        LocalDateTime dataEndTime = context.getExecutionTime();
        LocalDateTime dataStartTime = context.getLastSuccessEndTime() == null
                ? earliestChangeTime
                : context.getLastSuccessEndTime().minusDays(1);
        if (dataStartTime.isBefore(earliestChangeTime)) {
            dataStartTime = earliestChangeTime;
        }
        if (!dataStartTime.isBefore(dataEndTime)) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(dataStartTime)
                    .dataEndTime(dataEndTime)
                    .message("本次无需同步")
                    .build();
        }

        CrmHqFirstContractSyncSummaryVO summary = crmHqFirstContractSnapshotService.syncByTimeRange(dataStartTime, dataEndTime);
        return SyncTaskExecutionResult.builder()
                .dataStartTime(dataStartTime)
                .dataEndTime(dataEndTime)
                .message(String.format("CRM 签约快照同步完成：处理 %d 条，新增 %d 条，更新 %d 条",
                        defaultInt(summary.getProcessedCount()),
                        defaultInt(summary.getInsertedCount()),
                        defaultInt(summary.getUpdatedCount())))
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


