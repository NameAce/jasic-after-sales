package com.jasic.aftersales.system.service;

/**
 * 同步任务执行 Service
 *
 * @author Zoro
 * @date 2026/04/12
 */
public interface ISyncTaskExecutionService {

    /**
     * 提交手动执行任务
     *
     * @param taskId 任务ID
     * @return 日志ID
     */
    Long submitManualExecution(Long taskId);

    /**
     * 执行定时任务
     *
     * @param taskId 任务ID
     */
    void executeScheduled(Long taskId);
}
