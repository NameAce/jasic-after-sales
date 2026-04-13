package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.system.domain.entity.SyncTask;

/**
 * 同步任务处理器扩展点。
 *
 * <p>每一种外部同步能力都通过一个独立处理器暴露给同步任务中心，调度层只关心处理器编码、
 * 名称和统一执行入口，不感知具体业务明细。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
public interface SyncTaskHandler {

    /**
     * 返回处理器编码。
     *
     * <p>编码需要全局唯一，并与同步任务表中的 {@code handler_code} 一一对应。</p>
     *
     * @return 处理器编码
     */
    String getCode();

    /**
     * 返回处理器展示名称。
     *
     * @return 处理器名称
     */
    String getName();

    /**
     * 执行同步任务。
     *
     * @param task    当前同步任务配置
     * @param context 本次执行上下文，包含触发时间和最近成功时间
     * @return 执行结果，供日志中心记录数据窗口和结果摘要
     */
    SyncTaskExecutionResult execute(SyncTask task, SyncTaskExecutionContext context);
}
