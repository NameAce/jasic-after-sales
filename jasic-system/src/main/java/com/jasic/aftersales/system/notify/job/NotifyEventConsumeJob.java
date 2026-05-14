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
     * 通知事件消费服务服务依赖。
     *
     * @param context 参数
     */
    @Resource
    private NotifyEventConsumeService notifyEventConsumeService;

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
            // 调用consumePendingEvents方法，复用统一能力并保证业务规则一致。
            int successCount = notifyEventConsumeService.consumePendingEvents();
            // 调用info方法，复用统一能力并保证业务规则一致。
            log.info("通知事件消费任务执行完成，本轮成功处理 {} 条事件", successCount);
        } catch (Exception ex) {
            // 调用error方法，复用统一能力并保证业务规则一致。
            log.error("通知事件消费任务执行失败", ex);
            throw new JobExecutionException(ex);
        }
    }
}


