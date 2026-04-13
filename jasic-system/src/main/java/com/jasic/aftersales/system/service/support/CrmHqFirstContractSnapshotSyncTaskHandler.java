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
 * @author Codex
 * @date 2026/04/12
 */
@Component
public class CrmHqFirstContractSnapshotSyncTaskHandler implements SyncTaskHandler {

    public static final String HANDLER_CODE = "crmHqFirstContractSnapshotSync";
    private static final String HANDLER_NAME = "CRM签约快照同步";

    @Resource
    private ICrmHqFirstContractSnapshotService crmHqFirstContractSnapshotService;

    @Override
    public String getCode() {
        return HANDLER_CODE;
    }

    @Override
    public String getName() {
        return HANDLER_NAME;
    }

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

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
