package com.jasic.aftersales.system.service.support;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 同步任务异步执行器
 *
 * @author Codex
 * @date 2026/04/12
 */
@Component
public class SyncTaskAsyncExecutor {

    /**
     * ?????
     *
     * @param taskId task ID
     * @param logId log ID
     */
    @Resource
    private SyncTaskExecutionRunner syncTaskExecutionRunner;

    @Async
    public void executeAsync(Long taskId, Long logId) {
        syncTaskExecutionRunner.executeWithLog(taskId, logId);
    }
}
