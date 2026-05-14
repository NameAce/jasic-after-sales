package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationSyncSummaryVO;
import com.jasic.aftersales.system.service.ICrmFirstSecondRelationSnapshotService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * CRM 一级二级关系来源快照同步处理器
 *
 * @author Codex
 * @date 2026/04/17
 */
@Component
public class CrmFirstSecondRelationSnapshotSyncTaskHandler implements SyncTaskHandler {

    public static final String HANDLER_CODE = "crmFirstSecondRelationSnapshotSync";
    private static final String HANDLER_NAME = "CRM一级二级关系来源快照同步";

    /**
     * ?????
     *
     * @return ?????
     */
    @Resource
    private ICrmFirstSecondRelationSnapshotService crmFirstSecondRelationSnapshotService;

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
        LocalDateTime earliestChangeTime = crmFirstSecondRelationSnapshotService.getEarliestChangeTime();
        if (earliestChangeTime == null) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(null)
                    .dataEndTime(context.getExecutionTime())
                    .message("未查询到可同步的 CRM 一级二级关系数据")
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

        CrmFirstSecondRelationSyncSummaryVO summary = crmFirstSecondRelationSnapshotService.syncByTimeRange(dataStartTime, dataEndTime);
        return SyncTaskExecutionResult.builder()
                .dataStartTime(dataStartTime)
                .dataEndTime(dataEndTime)
                .message(String.format("CRM 一级二级关系来源快照同步完成：处理 %d 条，新增 %d 条，更新 %d 条",
                        defaultInt(summary.getProcessedCount()),
                        defaultInt(summary.getInsertedCount()),
                        defaultInt(summary.getUpdatedCount())))
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
