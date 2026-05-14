package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanySyncSummaryVO;
import com.jasic.aftersales.system.service.ICrmBizCompanySnapshotService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * CRM 公司快照同步处理器。
 *
 * <p>负责把同步任务中心的执行上下文转换成 CRM 公司快照服务所需的时间窗口参数。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Component
public class BizCompanySnapshotSyncTaskHandler implements SyncTaskHandler {

    public static final String HANDLER_CODE = "bizCompanySnapshotSync";
    private static final String HANDLER_NAME = "CRM公司快照同步";

    /**
     * ?????
     *
     * @return ?????
     */
    @Resource
    private ICrmBizCompanySnapshotService crmBizCompanySnapshotService;

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
        LocalDateTime earliestChangeTime = crmBizCompanySnapshotService.getEarliestChangeTime();
        if (earliestChangeTime == null) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(null)
                    .dataEndTime(context.getExecutionTime())
                    .message("未查询到可同步的 CRM 公司数据")
                    .build();
        }

        LocalDateTime dataEndTime = context.getExecutionTime();
        // 为降低边界漏数风险，非首次同步时向前回退 1 天做重叠窗口，再由快照层 upsert 去重。
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

        CrmBizCompanySyncSummaryVO summary = crmBizCompanySnapshotService.syncByTimeRange(dataStartTime, dataEndTime);
        return SyncTaskExecutionResult.builder()
                .dataStartTime(dataStartTime)
                .dataEndTime(dataEndTime)
                .message(String.format("CRM 公司快照同步完成：处理 %d 条，新增 %d 条，更新 %d " +
                                "条",
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
