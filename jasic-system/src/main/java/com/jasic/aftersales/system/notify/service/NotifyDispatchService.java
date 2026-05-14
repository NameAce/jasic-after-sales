package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notify dispatch service.
 *
 * @author Codex
 * @date 2026/04/21
 */
public interface NotifyDispatchService {

    /**
     * 创建分发。
     *
     * @param dispatch 参数
     * @return 处理结果
     */
    Long createDispatch(SysNotifyDispatch dispatch);

    /**
     * 根据ID查询通知分发详情。
     *
     * @param id 参数
     * @return 处理结果
     */
    SysNotifyDispatch getById(Long id);

    /**
     * 分页查询SendableDispatches列表。
     *
     * @param now 参数
     * @param limit 参数
     * @return 处理结果
     */
    List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit);

    /**
     * 标记Processing。
     *
     * @param dispatchId 参数
     * @return 处理结果
     */
    boolean markProcessing(Long dispatchId);

    /**
     * 标记Success。
     *
     * @param dispatchId 参数
     * @param resultCode 参数
     * @param resultMessage 参数
     * @param channelResponseJson 参数
     */
    void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson);

    /**
     * 标记Failed。
     *
     * @param dispatchId 参数
     * @param retryCount 参数
     * @param nextRetryTime 参数
     * @param resultCode 参数
     * @param resultMessage 参数
     * @param channelResponseJson 参数
     */
    void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime,
                    String resultCode, String resultMessage, String channelResponseJson);

    /**
     * 标记Skipped。
     *
     * @param dispatchId 参数
     * @param resultCode 参数
     * @param resultMessage 参数
     * @param channelResponseJson 参数
     */
    void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson);

    /**
     * 消费PendingDispatches。
     *
     * @return 处理结果
     */
    int consumePendingDispatches();
}




