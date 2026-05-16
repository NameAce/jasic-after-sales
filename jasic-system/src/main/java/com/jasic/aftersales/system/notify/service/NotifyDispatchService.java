package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知分发服务。
 *
 * <p>负责外部通知分发任务的查询、发送、重试、死信和人工恢复，
 * 让具体渠道发送器只关注“如何发”，不承担读写状态机的职责。</p>
 *
 * @author Codex
 * @date 2026/04/21
 */
public interface NotifyDispatchService {

    /**
     * 创建分发任务。
     *
     * @param dispatch 分发任务
     * @return 主键ID
     */
    Long createDispatch(SysNotifyDispatch dispatch);

    /**
     * 根据ID查询分发任务。
     *
     * @param id 主键ID
     * @return 分发任务
     */
    SysNotifyDispatch getById(Long id);

    /**
     * 查询可发送分发任务。
     *
     * <p>只返回待发送任务或已经到达重试时间的失败任务，
     * 死信、跳过和处理中任务不允许被自动发送任务再次捞取。</p>
     *
     * @param now 当前时间
     * @param limit 查询上限
     * @return 可发送分发任务
     */
    List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit);

    /**
     * 查询发送超时的分发任务。
     *
     * @param timeoutBefore 超时截止时间
     * @param limit 查询上限
     * @return 超时任务列表
     */
    List<SysNotifyDispatch> listTimeoutProcessingDispatches(LocalDateTime timeoutBefore, Integer limit);

    /**
     * 抢占分发任务为发送中状态。
     *
     * @param dispatchId 分发任务ID
     * @return 是否抢占成功
     */
    boolean markProcessing(Long dispatchId);

    /**
     * 标记分发成功。
     *
     * @param dispatchId 分发任务ID
     * @param resultCode 结果码
     * @param resultMessage 结果说明
     * @param channelResponseJson 渠道响应快照
     */
    void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson);

    /**
     * 标记分发失败。
     *
     * @param dispatchId 分发任务ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     * @param resultCode 结果码
     * @param resultMessage 结果说明
     * @param channelResponseJson 渠道响应快照
     */
    void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime,
                    String resultCode, String resultMessage, String channelResponseJson);

    /**
     * 标记分发跳过。
     *
     * <p>业务条件不满足时直接进入终态，后续不会参与自动重试。</p>
     *
     * @param dispatchId 分发任务ID
     * @param resultCode 结果码
     * @param resultMessage 结果说明
     * @param channelResponseJson 渠道响应快照
     */
    void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson);

    /**
     * 恢复发送超时的分发任务。
     *
     * @param timeoutBefore 超时截止时间
     * @return 恢复数量
     */
    int recoverTimeoutProcessingDispatches(LocalDateTime timeoutBefore);

    /**
     * 标记分发任务进入死信。
     *
     * @param dispatchId 分发任务ID
     * @param resultCode 结果码
     * @param resultMessage 死信说明
     */
    void markDead(Long dispatchId, String resultCode, String resultMessage);

    /**
     * 将失败或死信任务重置为待发送。
     *
     * @param dispatchId 分发任务ID
     */
    void resetForRetry(Long dispatchId);

    /**
     * 消费当前批次待发送任务。
     *
     * @return 成功处理数量
     */
    int consumePendingDispatches();
}
