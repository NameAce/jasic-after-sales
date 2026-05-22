package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.service.ISyncTaskExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.annotation.Resource;

/**
 * 同步任务 Quartz Job。
 *
 * <p>该类只负责从 Quartz 上下文取出任务ID，并委托给同步任务执行服务，不承载业务规则。</p>
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Slf4j
@DisallowConcurrentExecution
public class SyncTaskQuartzJob implements Job {

    /**
     * 同步任务执行服务服务依赖。
     *
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     */
    @Resource
    private ISyncTaskExecutionService syncTaskExecutionService;

    /**
     * 处理execute业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @throws JobExecutionException 异常场景
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        Long taskId = dataMap.getLong("taskId");
        try {
            syncTaskExecutionService.executeScheduled(taskId);
        } catch (Exception ex) {
            log.error("执行同步任务失败，taskId={}", taskId, ex);
            throw new JobExecutionException(ex);
        }
    }
}


