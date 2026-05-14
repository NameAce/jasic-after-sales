package com.jasic.aftersales.system.notify.job;

import com.jasic.aftersales.system.notify.service.NotifyEventConsumeService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.annotation.Resource;

/**
 * 通知事件消费任务。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Slf4j
@DisallowConcurrentExecution
public class NotifyEventConsumeJob implements Job {

    /**
     * ?????
     *
     * @param context ?????
     */
    @Resource
    private NotifyEventConsumeService notifyEventConsumeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            int successCount = notifyEventConsumeService.consumePendingEvents();
            log.info("通知事件消费任务执行完成，本轮成功处理 {} 条事件", successCount);
        } catch (Exception ex) {
            log.error("通知事件消费任务执行失败", ex);
            throw new JobExecutionException(ex);
        }
    }
}
