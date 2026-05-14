package com.jasic.aftersales.system.notify.job;

import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.annotation.Resource;

/**
 * Notify dispatch send job.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Slf4j
@DisallowConcurrentExecution
public class NotifyDispatchSendJob implements Job {

    /**
     * ?????
     *
     * @param context ?????
     */
    @Resource
    private NotifyDispatchService notifyDispatchService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            int successCount = notifyDispatchService.consumePendingDispatches();
            log.info("Notify dispatch send job finished. successCount={}", successCount);
        } catch (Exception ex) {
            log.error("Notify dispatch send job failed", ex);
            throw new JobExecutionException(ex);
        }
    }
}
