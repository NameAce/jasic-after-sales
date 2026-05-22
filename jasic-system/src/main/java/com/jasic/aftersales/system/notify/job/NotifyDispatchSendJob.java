package com.jasic.aftersales.system.notify.job;

import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 通知分发发送任务。
 *
 * <p>任务执行前会先恢复超时卡住的分发任务，确保 `PROCESSING` 状态不会永久占住发送队列。</p>
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Slf4j
@DisallowConcurrentExecution
public class NotifyDispatchSendJob implements Job {

    /**notifyDispatchService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private NotifyDispatchService notifyDispatchService;

    /**dispatchProcessingTimeoutMinutes 字段，用于当前类内部业务处理。*/
    @Value("${jasic.notify.dispatch-processing-timeout-minutes:" + NotifyConstants.DISPATCH_PROCESSING_TIMEOUT_MINUTES + "}")
    private long dispatchProcessingTimeoutMinutes = NotifyConstants.DISPATCH_PROCESSING_TIMEOUT_MINUTES;

    /**
     * 执行分发发送任务。
     *
     * @param context Quartz 上下文
     * @throws JobExecutionException 任务异常
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(dispatchProcessingTimeoutMinutes);
            int recoveredCount = notifyDispatchService.recoverTimeoutProcessingDispatches(timeoutBefore);
            if (recoveredCount > 0) {
                log.warn("Recover timeout notify dispatches finished. recoveredCount={}", recoveredCount);
            }
            int successCount = notifyDispatchService.consumePendingDispatches();
            log.info("Notify dispatch send job finished. recoveredCount={}, successCount={}", recoveredCount, successCount);
        } catch (Exception ex) {
            log.error("Notify dispatch send job failed", ex);
            throw new JobExecutionException(ex);
        }
    }
}
