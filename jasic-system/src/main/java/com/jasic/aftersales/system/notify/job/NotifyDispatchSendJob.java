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
     * 通知分发服务服务依赖。
     *
     * @param context 参数
     */
    @Resource
    private NotifyDispatchService notifyDispatchService;

    /**
     * 处理execute业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param context 参数
     * @throws JobExecutionException 异常场景
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            // 调用consumePendingDispatches方法，复用统一能力并保证业务规则一致。
            int successCount = notifyDispatchService.consumePendingDispatches();
            // 调用info方法，复用统一能力并保证业务规则一致。
            log.info("Notify dispatch send job finished. successCount={}", successCount);
        } catch (Exception ex) {
            // 调用error方法，复用统一能力并保证业务规则一致。
            log.error("Notify dispatch send job failed", ex);
            throw new JobExecutionException(ex);
        }
    }
}


