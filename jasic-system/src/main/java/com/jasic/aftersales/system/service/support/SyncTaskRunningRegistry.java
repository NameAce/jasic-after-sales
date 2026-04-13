package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同步任务运行态注册表。
 *
 * <p>用于在单实例内保护同一个任务不被重复执行。该组件只处理进程内并发，不承担分布式锁职责。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Component
public class SyncTaskRunningRegistry {

    /** 正在执行中的任务ID集合。 */
    private final Set<Long> runningTaskIds = ConcurrentHashMap.newKeySet();

    /**
     * 标记任务进入运行态。
     *
     * @param taskId   任务ID
     * @param taskName 任务名称
     */
    public void lock(Long taskId, String taskName) {
        if (!runningTaskIds.add(taskId)) {
            throw new ServiceException("任务“" + taskName + "”正在执行中，请稍后重试");
        }
    }

    /**
     * 释放任务运行态。
     *
     * @param taskId 任务ID
     */
    public void unlock(Long taskId) {
        runningTaskIds.remove(taskId);
    }
}
