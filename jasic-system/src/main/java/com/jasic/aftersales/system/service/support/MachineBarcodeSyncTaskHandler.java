package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.vo.MachineBarcodeSyncResultVO;
import com.jasic.aftersales.system.service.IMachineBarcodeSyncService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 条码档案同步处理器
 *
 * @author Codex
 * @date 2026/04/12
 */
@Component
public class MachineBarcodeSyncTaskHandler implements SyncTaskHandler {

    public static final String HANDLER_CODE = "machineBarcodeSync";
    private static final String HANDLER_NAME = "条码档案同步";

    @Resource
    private IMachineBarcodeSyncService machineBarcodeSyncService;

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
        LocalDateTime earliestAddTime = machineBarcodeSyncService.getEarliestAddTime();
        if (earliestAddTime == null) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(null)
                    .dataEndTime(context.getExecutionTime())
                    .message("未查询到可同步的 CRM 条码数据")
                    .build();
        }

        LocalDateTime dataEndTime = context.getExecutionTime();
        LocalDateTime dataStartTime = context.getLastSuccessEndTime() == null
                ? earliestAddTime
                : context.getLastSuccessEndTime().minusDays(7);
        if (dataStartTime.isBefore(earliestAddTime)) {
            dataStartTime = earliestAddTime;
        }
        if (!dataStartTime.isBefore(dataEndTime)) {
            return SyncTaskExecutionResult.builder()
                    .dataStartTime(dataStartTime)
                    .dataEndTime(dataEndTime)
                    .message("本次无需同步")
                    .build();
        }

        MachineBarcodeSyncResultVO summary = machineBarcodeSyncService.syncByAddTimeRange(dataStartTime, dataEndTime);

        return SyncTaskExecutionResult.builder()
                .dataStartTime(dataStartTime)
                .dataEndTime(dataEndTime)
                .message(buildMessage(summary))
                .build();
    }

    private String buildMessage(MachineBarcodeSyncResultVO summary) {
        return String.format("条码同步完成：处理 %d 条，新增 %d 条，跳过 %d 条，总部未匹配 %d 条，总部冲突 %d 条，物料未匹配 %d 条",
                defaultInt(summary.getBarcodeProcessedCount()),
                defaultInt(summary.getInsertedCount()),
                defaultInt(summary.getSkippedExistingCount()),
                defaultInt(summary.getHqUnmatchedCount()),
                defaultInt(summary.getHqConflictCount()),
                defaultInt(summary.getProductUnmatchedCount()));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
