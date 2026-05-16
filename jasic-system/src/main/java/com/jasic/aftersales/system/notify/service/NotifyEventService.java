package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知事件服务。
 *
 * <p>负责通知事件的查询、抢占、状态流转和可靠性恢复，
 * 让上层消费编排只关注“如何处理事件”，不直接关心底层状态更新细节。</p>
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface NotifyEventService {

    /**
     * 创建通知事件。
     *
     * @param notifyEvent 通知事件
     * @return 主键ID
     */
    Long createEvent(SysNotifyEvent notifyEvent);

    /**
     * 根据ID查询通知事件。
     *
     * @param id 主键ID
     * @return 通知事件
     */
    SysNotifyEvent getById(Long id);

    /**
     * 根据幂等键查询通知事件。
     *
     * @param eventKey 幂等键
     * @return 通知事件
     */
    SysNotifyEvent getByEventKey(String eventKey);

    /**
     * 查询可消费事件列表。
     *
     * <p>只返回新建事件或已经到达自动重试时间的失败事件，
     * 死信和处理中事件不能被自动消费任务再次捞取。</p>
     *
     * @param now 当前时间
     * @param limit 查询上限
     * @return 待消费事件列表
     */
    List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit);

    /**
     * 查询处理超时的事件。
     *
     * @param timeoutBefore 超时截止时间
     * @param limit 查询上限
     * @return 处理超时事件
     */
    List<SysNotifyEvent> listTimeoutProcessingEvents(LocalDateTime timeoutBefore, Integer limit);

    /**
     * 按条件查询通知事件。
     *
     * @param query 查询条件
     * @return 通知事件列表
     */
    List<SysNotifyEvent> listByQuery(NotifyEventQuery query);

    /**
     * 更新事件状态。
     *
     * @param eventId 事件ID
     * @param status 事件状态
     */
    void updateStatus(Long eventId, String status);

    /**
     * 抢占事件为处理中状态。
     *
     * <p>只有新建事件或满足自动重试条件的失败事件允许被抢占，
     * 抢占成功后会记录处理开始时间，作为后续超时恢复的依据。</p>
     *
     * @param eventId 事件ID
     * @return 是否抢占成功
     */
    boolean markProcessing(Long eventId);

    /**
     * 标记事件消费成功。
     *
     * @param eventId 事件ID
     */
    void markSuccess(Long eventId);

    /**
     * 标记事件消费失败。
     *
     * @param eventId 事件ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     * @param errorMessage 最近一次失败原因
     */
    void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage);

    /**
     * 更新事件重试信息。
     *
     * <p>主要用于进入死信前先落表最新重试次数，
     * 避免最终死信记录丢失最后一次失败尝试的次数快照。</p>
     *
     * @param eventId 事件ID
     * @param retryCount 重试次数
     * @param nextRetryTime 下次重试时间
     * @param errorMessage 最近一次失败原因
     */
    void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage);

    /**
     * 恢复处理超时的事件。
     *
     * @param timeoutBefore 超时截止时间
     * @return 恢复数量
     */
    int recoverTimeoutProcessingEvents(LocalDateTime timeoutBefore);

    /**
     * 标记事件进入死信。
     *
     * @param eventId 事件ID
     * @param errorMessage 死信原因
     */
    void markDead(Long eventId, String errorMessage);

    /**
     * 将失败或死信事件重置为待处理。
     *
     * <p>该方法用于人工重试入口，把自动任务已经放弃的事件重新放回待消费队列。</p>
     *
     * @param eventId 事件ID
     */
    void resetForRetry(Long eventId);
}
