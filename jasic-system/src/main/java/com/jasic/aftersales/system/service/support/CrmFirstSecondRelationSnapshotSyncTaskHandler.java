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
     * CRM一级二级关系快照服务服务依赖。
     *
     * @return 处理结果
     */
    @Resource
    private ICrmFirstSecondRelationSnapshotService crmFirstSecondRelationSnapshotService;

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
     * 获取CRM一级二级关系快照同步任务名称。
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
        // 调用getEarliestChangeTime方法，复用统一能力并保证业务规则一致。
        LocalDateTime earliestChangeTime = crmFirstSecondRelationSnapshotService.getEarliestChangeTime();
        if (earliestChangeTime == null) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(null)
                    .dataEndTime(context.getExecutionTime())
                    .message("未查询到可同步的 CRM 一级二级关系数据")
                    // 调用build方法，复用统一能力并保证业务规则一致。
                    .build();
        }

        // 调用getExecutionTime方法，复用统一能力并保证业务规则一致。
        LocalDateTime dataEndTime = context.getExecutionTime();
        LocalDateTime dataStartTime = context.getLastSuccessEndTime() == null
                ? earliestChangeTime
                // 调用minusDays方法，复用统一能力并保证业务规则一致。
                : context.getLastSuccessEndTime().minusDays(1);
        if (dataStartTime.isBefore(earliestChangeTime)) {
            dataStartTime = earliestChangeTime;
        }
        if (!dataStartTime.isBefore(dataEndTime)) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(dataStartTime)
                    .dataEndTime(dataEndTime)
                    .message("本次无需同步")
                    // 调用build方法，复用统一能力并保证业务规则一致。
                    .build();
        }

        // 调用syncByTimeRange方法，复用统一能力并保证业务规则一致。
        CrmFirstSecondRelationSyncSummaryVO summary = crmFirstSecondRelationSnapshotService.syncByTimeRange(dataStartTime, dataEndTime);
        return SyncTaskExecutionResult.builder()
                .dataStartTime(dataStartTime)
                .dataEndTime(dataEndTime)
                .message(String.format("CRM 一级二级关系来源快照同步完成：处理 %d 条，新增 %d 条，更新 %d 条",
                        defaultInt(summary.getProcessedCount()),
                        defaultInt(summary.getInsertedCount()),
                        defaultInt(summary.getUpdatedCount())))
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


