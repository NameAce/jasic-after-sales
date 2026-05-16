package com.jasic.aftersales.system.notify.job;

import com.jasic.aftersales.system.notify.service.NotifyEventConsumeService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
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
 * 通知事件消费任务。
 *
 * <p>任务执行前会先恢复处理超时的事件，避免 `PROCESSING` 状态因宕机或线程中断永久卡死。</p>
 *
 * @author Codex
 * @date 2026/04/18
 */
@Slf4j
@DisallowConcurrentExecution
public class NotifyEventConsumeJob implements Job {

    @Resource
    private NotifyEventConsumeService notifyEventConsumeService;

    @Resource
    private NotifyEventService notifyEventService;

    @Value("${jasic.notify.event-processing-timeout-minutes:" + NotifyConstants.EVENT_PROCESSING_TIMEOUT_MINUTES + "}")
    private long eventProcessingTimeoutMinutes = NotifyConstants.EVENT_PROCESSING_TIMEOUT_MINUTES;

    /**
     * 执行事件消费任务。
     *
     * @param context Quartz 上下文
     * @throws JobExecutionException 任务异常
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(eventProcessingTimeoutMinutes);
            int recoveredCount = notifyEventService.recoverTimeoutProcessingEvents(timeoutBefore);
            if (recoveredCount > 0) {
                log.warn("Recover timeout notify events finished. recoveredCount={}", recoveredCount);
            }
            int successCount = notifyEventConsumeService.consumePendingEvents();
            log.info("Notify event consume job finished. recoveredCount={}, successCount={}", recoveredCount, successCount);
        } catch (Exception ex) {
            log.error("Notify event consume job failed", ex);
            throw new JobExecutionException(ex);
        }
    }
}
