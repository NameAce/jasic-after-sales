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
 * @author Zoro
 * @date 2026/04/12
 */
@Component
public class MachineBarcodeSyncTaskHandler implements SyncTaskHandler {

    /**HANDLER_CODE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String HANDLER_CODE = "machineBarcodeSync";
    /**HANDLER_NAME 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String HANDLER_NAME = "条码档案同步";

    /**
     * 机器条码同步服务服务依赖。
     *
     * @return 业务处理结果
     */
    @Resource
    private IMachineBarcodeSyncService machineBarcodeSyncService;

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
     * 获取机器条码同步任务名称。
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

    /**
     * 构建消息。
     *
     * @param summary summary，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String buildMessage(MachineBarcodeSyncResultVO summary) {
        return String.format("条码同步完成：处理 %d 条，新增 %d 条，跳过 %d 条，总部未匹配 %d 条，总部冲突 %d 条，物料未匹配 %d 条",
                defaultInt(summary.getBarcodeProcessedCount()),
                defaultInt(summary.getInsertedCount()),
                defaultInt(summary.getSkippedExistingCount()),
                defaultInt(summary.getHqUnmatchedCount()),
                defaultInt(summary.getHqConflictCount()),
                defaultInt(summary.getProductUnmatchedCount()));
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


