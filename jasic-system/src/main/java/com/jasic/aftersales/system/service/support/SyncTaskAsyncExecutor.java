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
     * 同步任务执行Runner字段。
     *
     * @param taskId task ID
     * @param logId log ID
     */
    @Resource
    private SyncTaskExecutionRunner syncTaskExecutionRunner;

    /**
     * 处理executeAsync业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param taskId 参数
     * @param logId 参数
     */
    @Async
    public void executeAsync(Long taskId, Long logId) {
        // 调用executeWithLog方法，复用统一能力并保证业务规则一致。
        syncTaskExecutionRunner.executeWithLog(taskId, logId);
    }
}


